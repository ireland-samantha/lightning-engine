/*
 * Copyright (c) 2026 Samantha Ireland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package ca.samanthaireland.lightning.engine.internal.core.store;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance query cache for ECS component queries.
 *
 * <p>This cache stores the results of {@code vectorsWithComponent()} queries,
 * which are expensive O(n) operations that scan all entities. The cache is
 * automatically invalidated when:
 * <ul>
 *   <li>An entity is created or deleted</li>
 *   <li>A component is attached to or removed from an entity</li>
 *   <li>The store is reset</li>
 * </ul>
 *
 * <h2>Usage Patterns</h2>
 *
 * <h3>Per-Tick Caching (Recommended)</h3>
 * <pre>{@code
 * // At start of tick
 * queryCache.beginTick();
 *
 * // During tick - queries are cached
 * Set<Long> moveables = store.vectorsWithComponentCached(cache, POSITION_X, VELOCITY_X);
 * Set<Long> damageable = store.vectorsWithComponentCached(cache, HEALTH);
 *
 * // Modifications invalidate relevant caches automatically
 * store.attachComponent(entityId, HEALTH, 100); // Invalidates HEALTH queries
 *
 * // At end of tick (optional - clears all caches)
 * queryCache.endTick();
 * }</pre>
 *
 * <h3>Long-lived Caching</h3>
 * <pre>{@code
 * // Cache persists across ticks, only invalidated by modifications
 * Set<Long> moveables = store.vectorsWithComponentCached(cache, POSITION_X, VELOCITY_X);
 *
 * // When entity changes affect the cached query, it's automatically invalidated
 * store.deleteVector(entityId); // Invalidates all caches
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>The cache is thread-safe and can be used concurrently. However, for best
 * performance in single-threaded game loops, consider using a non-concurrent
 * variant or calling {@link #beginTick()} to ensure fresh results.
 *
 * <h2>Performance Characteristics</h2>
 * <ul>
 *   <li>Cache hit: O(1) lookup + O(n) copy of result set</li>
 *   <li>Cache miss: O(n) full scan (same as uncached)</li>
 *   <li>Invalidation: O(1) per affected component signature</li>
 * </ul>
 */
public class QueryCache {

    /**
     * Cache key representing a sorted array of component IDs.
     */
    private static final class CacheKey {
        private final long[] componentIds;
        private final int hashCode;

        CacheKey(long[] componentIds) {
            // Sort for consistent keys regardless of query order
            this.componentIds = componentIds.clone();
            Arrays.sort(this.componentIds);
            this.hashCode = Arrays.hashCode(this.componentIds);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey other)) return false;
            return Arrays.equals(componentIds, other.componentIds);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public String toString() {
            return "CacheKey" + Arrays.toString(componentIds);
        }

        /**
         * Check if this key contains the given component ID.
         */
        boolean containsComponent(long componentId) {
            return Arrays.binarySearch(componentIds, componentId) >= 0;
        }
    }

    /**
     * Cached query result with metadata.
     */
    private static final class CacheEntry {
        final LongSet entityIds;
        final long createdAtVersion;

        CacheEntry(LongSet entityIds, long version) {
            this.entityIds = entityIds;
            this.createdAtVersion = version;
        }
    }

    private final Map<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();
    private final AtomicLong version = new AtomicLong(0);
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);

    /**
     * Get a cached query result, or null if not cached.
     *
     * @param componentIds the component IDs to query
     * @return cached entity set, or null if not cached
     */
    public Set<Long> get(long... componentIds) {
        CacheKey key = new CacheKey(componentIds);
        CacheEntry entry = cache.get(key);

        if (entry != null) {
            hits.incrementAndGet();
            // Return a copy to prevent external modification
            return new LongOpenHashSet(entry.entityIds);
        }

        misses.incrementAndGet();
        return null;
    }

    /**
     * Store a query result in the cache.
     *
     * @param result the query result to cache
     * @param componentIds the component IDs that were queried
     */
    public void put(Set<Long> result, long... componentIds) {
        CacheKey key = new CacheKey(componentIds);
        // Store a copy to prevent external modification
        LongSet copy = new LongOpenHashSet(result);
        cache.put(key, new CacheEntry(copy, version.get()));
    }

    /**
     * Invalidate all cache entries that include the specified component.
     * Called when a component is attached to or removed from an entity.
     *
     * <p><b>Performance Note:</b> This operation iterates through all cached queries
     * to find those containing the component. While this is O(n) where n is the number
     * of cached queries, it's typically fast because:
     * <ul>
     *   <li>The number of unique query patterns is usually small</li>
     *   <li>CacheKey uses binary search for component lookup (O(log k) where k is components in query)</li>
     *   <li>ConcurrentHashMap.removeIf is optimized for concurrent access</li>
     * </ul>
     *
     * <p>Async invalidation was considered but rejected because:
     * <ul>
     *   <li>It would introduce stale reads (queries returning outdated results)</li>
     *   <li>The synchronization overhead would likely exceed the invalidation cost</li>
     *   <li>Game simulations require consistent state within each tick</li>
     * </ul>
     *
     * @param componentId the component that was modified
     */
    public void invalidateComponent(long componentId) {
        cache.keySet().removeIf(key -> key.containsComponent(componentId));
        version.incrementAndGet();
    }

    /**
     * Invalidate all cache entries.
     * Called when an entity is created or deleted, or when the store is reset.
     */
    public void invalidateAll() {
        cache.clear();
        version.incrementAndGet();
    }

    /**
     * Begin a new tick. Clears all cached data to ensure fresh results.
     * Use this at the start of each game tick for predictable behavior.
     */
    public void beginTick() {
        cache.clear();
        // Don't increment version - we're just starting fresh
    }

    /**
     * End the current tick. Optionally clears cached data.
     * Use this at the end of each game tick to free memory.
     */
    public void endTick() {
        cache.clear();
    }

    /**
     * Get the number of cached queries.
     */
    public int size() {
        return cache.size();
    }

    /**
     * Get cache hit count (for diagnostics).
     */
    public long getHitCount() {
        return hits.get();
    }

    /**
     * Get cache miss count (for diagnostics).
     */
    public long getMissCount() {
        return misses.get();
    }

    /**
     * Get cache hit ratio (for diagnostics).
     */
    public double getHitRatio() {
        long h = hits.get();
        long m = misses.get();
        long total = h + m;
        return total == 0 ? 0.0 : (double) h / total;
    }

    /**
     * Reset cache statistics.
     */
    public void resetStats() {
        hits.set(0);
        misses.set(0);
    }

    /**
     * Clear the cache completely.
     */
    public void clear() {
        cache.clear();
        version.incrementAndGet();
    }
}
