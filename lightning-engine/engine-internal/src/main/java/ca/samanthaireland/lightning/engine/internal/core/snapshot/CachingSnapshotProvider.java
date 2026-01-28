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

package ca.samanthaireland.lightning.engine.internal.core.snapshot;

import ca.samanthaireland.lightning.engine.core.entity.CoreComponents;
import ca.samanthaireland.lightning.engine.core.snapshot.ComponentData;
import ca.samanthaireland.lightning.engine.core.snapshot.ModuleData;
import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleResolver;
import ca.samanthaireland.lightning.engine.internal.core.store.DirtyTrackingEntityComponentStore;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.function.Supplier;

/**
 * Caching snapshot provider that uses dirty tracking for incremental updates.
 *
 * <p>This provider wraps an underlying {@link SnapshotProviderImpl} and adds:
 * <ul>
 *   <li>Per-match snapshot caching</li>
 *   <li>Incremental updates for modified entities</li>
 *   <li>Intelligent threshold-based full rebuild</li>
 *   <li>Snapshot generation metrics</li>
 * </ul>
 *
 * <p><b>Cache Strategy:</b>
 * <ul>
 *   <li>Cache hit (no changes): Return cached snapshot immediately</li>
 *   <li>Incremental update (≤50% changed): Update only dirty entities</li>
 *   <li>Full rebuild (>50% changed or structural changes): Delegate to underlying provider</li>
 * </ul>
 *
 * <p><b>Performance Targets:</b>
 * <ul>
 *   <li>Cache hit: &lt;1ms</li>
 *   <li>Incremental update (1% dirty, 100k entities): ~5-10ms</li>
 *   <li>Full rebuild: Same as underlying provider</li>
 * </ul>
 *
 * @see SnapshotProvider
 * @see DirtyTrackingEntityComponentStore
 * @see DirtyInfo
 */
@Slf4j
public class CachingSnapshotProvider implements SnapshotProvider {

    /**
     * Default threshold above which a full rebuild is triggered instead of incremental update.
     * Value is expressed as a fraction of total entities (0.5 = 50%).
     */
    public static final double DEFAULT_REBUILD_THRESHOLD = 0.5;

    /**
     * Default maximum cache age in ticks before a forced refresh.
     */
    public static final int DEFAULT_MAX_CACHE_AGE_TICKS = 60;

    private final SnapshotProviderImpl delegate;
    private final DirtyTrackingEntityComponentStore dirtyStore;
    private final EntityComponentStore entityStore;
    private final ModuleResolver moduleResolver;
    private final Supplier<Long> tickSupplier;

    // Configuration
    private final double rebuildThreshold;
    private final int maxCacheAgeTicks;

    // Per-match cache
    private final Map<Long, CachedSnapshot> cache = new ConcurrentHashMap<>();

    // Metrics
    private final AtomicLong totalGenerations = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();
    private final AtomicLong incrementalUpdates = new AtomicLong();
    private final AtomicLong fullRebuilds = new AtomicLong();
    private final DoubleAdder totalGenerationTimeMs = new DoubleAdder();
    private volatile double lastGenerationMs;
    private volatile double maxGenerationMs;

    // Last dirty info for delta generation
    private final Map<Long, DirtyInfo> lastDirtyInfoByMatch = new ConcurrentHashMap<>();

    /**
     * Creates a CachingSnapshotProvider with default configuration.
     *
     * @param dirtyStore     the dirty tracking entity store
     * @param moduleResolver the module resolver
     * @param tickSupplier   supplier for current tick (used for staleness detection)
     */
    public CachingSnapshotProvider(
            DirtyTrackingEntityComponentStore dirtyStore,
            ModuleResolver moduleResolver,
            Supplier<Long> tickSupplier) {
        this(dirtyStore, moduleResolver, tickSupplier, DEFAULT_REBUILD_THRESHOLD, DEFAULT_MAX_CACHE_AGE_TICKS);
    }

    /**
     * Creates a CachingSnapshotProvider with custom configuration.
     *
     * @param dirtyStore       the dirty tracking entity store
     * @param moduleResolver   the module resolver
     * @param tickSupplier     supplier for current tick
     * @param rebuildThreshold fraction of entities changed that triggers full rebuild (0.0 - 1.0)
     * @param maxCacheAgeTicks maximum ticks before forced cache refresh
     */
    public CachingSnapshotProvider(
            DirtyTrackingEntityComponentStore dirtyStore,
            ModuleResolver moduleResolver,
            Supplier<Long> tickSupplier,
            double rebuildThreshold,
            int maxCacheAgeTicks) {

        this.dirtyStore = Objects.requireNonNull(dirtyStore, "dirtyStore must not be null");
        this.entityStore = dirtyStore;
        this.moduleResolver = Objects.requireNonNull(moduleResolver, "moduleResolver must not be null");
        this.tickSupplier = Objects.requireNonNull(tickSupplier, "tickSupplier must not be null");
        this.rebuildThreshold = rebuildThreshold;
        this.maxCacheAgeTicks = maxCacheAgeTicks;

        // Create delegate using the same store (but delegate reads from it directly)
        this.delegate = new SnapshotProviderImpl(dirtyStore, moduleResolver);
    }

    @Override
    public Snapshot createForMatch(long matchId) {
        long startNanos = System.nanoTime();

        try {
            Snapshot result = createForMatchInternal(matchId);
            recordMetrics(startNanos);
            return result;
        } catch (Exception e) {
            log.error("Error creating snapshot for match {}: {}", matchId, e.getMessage(), e);
            // Fall back to delegate on error
            recordMetrics(startNanos);
            return delegate.createForMatch(matchId);
        }
    }

    private Snapshot createForMatchInternal(long matchId) {
        totalGenerations.incrementAndGet();
        long currentTick = tickSupplier.get();

        // Consume dirty info
        DirtyInfo dirty = dirtyStore.consumeDirtyInfo(matchId);
        lastDirtyInfoByMatch.put(matchId, dirty);

        CachedSnapshot cached = cache.get(matchId);

        // Check if we have a valid cache
        if (cached == null) {
            // First time - full build
            log.debug("No cached snapshot for match {}, performing full build", matchId);
            return fullRebuild(matchId, currentTick);
        }

        // Check for staleness
        if (cached.isStale(currentTick, maxCacheAgeTicks)) {
            log.debug("Cached snapshot for match {} is stale (age={}), rebuilding",
                    matchId, currentTick - cached.createdTick());
            return fullRebuild(matchId, currentTick);
        }

        // Check for changes
        if (!dirty.hasChanges()) {
            // Cache hit - no changes
            cacheHits.incrementAndGet();
            log.trace("Cache hit for match {} (no changes)", matchId);
            return cached.snapshot();
        }

        // Check change threshold
        int totalEntities = cached.entityCount();
        int changedCount = dirty.totalChanges();
        double changeRatio = totalEntities > 0 ? (double) changedCount / totalEntities : 1.0;

        if (changeRatio > rebuildThreshold || dirty.hasStructuralChanges()) {
            // Too many changes or structural changes - full rebuild
            log.debug("Change ratio {} exceeds threshold {} for match {} (structural={}), " +
                            "performing full rebuild",
                    changeRatio, rebuildThreshold, matchId, dirty.hasStructuralChanges());
            return fullRebuild(matchId, currentTick);
        }

        // Incremental update
        log.debug("Performing incremental update for match {}: {} modified, {} added, {} removed",
                matchId, dirty.modifiedCount(), dirty.addedCount(), dirty.removedCount());
        return incrementalUpdate(matchId, cached, dirty, currentTick);
    }

    private Snapshot fullRebuild(long matchId, long currentTick) {
        cacheMisses.incrementAndGet();
        fullRebuilds.incrementAndGet();

        Snapshot snapshot = delegate.createForMatch(matchId);

        // Build entity ID list from snapshot
        List<Long> entityIds = extractEntityIds(snapshot);

        CachedSnapshot newCached = CachedSnapshot.create(snapshot, entityIds, currentTick);
        cache.put(matchId, newCached);

        return snapshot;
    }

    private Snapshot incrementalUpdate(long matchId, CachedSnapshot cached, DirtyInfo dirty, long currentTick) {
        cacheMisses.incrementAndGet();
        incrementalUpdates.incrementAndGet();

        // For simplicity, we currently rebuild on any structural changes
        // This could be optimized further to handle adds/removes incrementally
        if (dirty.hasStructuralChanges()) {
            return fullRebuild(matchId, currentTick);
        }

        // Update modified entities only
        Snapshot updatedSnapshot = updateModifiedEntities(cached, dirty.modified(), matchId);

        // Update cache
        List<Long> entityIds = cached.orderedEntityIds();
        CachedSnapshot newCached = CachedSnapshot.create(updatedSnapshot, entityIds, currentTick);
        cache.put(matchId, newCached);

        return updatedSnapshot;
    }

    /**
     * Updates only the modified entities in the snapshot.
     *
     * <p>This creates a new snapshot with updated values for the modified entities
     * while preserving the values for unchanged entities.
     */
    private Snapshot updateModifiedEntities(CachedSnapshot cached, Set<Long> modifiedIds, long matchId) {
        if (modifiedIds.isEmpty()) {
            return cached.snapshot();
        }

        List<ModuleData> updatedModules = new ArrayList<>();

        for (ModuleData module : cached.snapshot().modules()) {
            List<ComponentData> updatedComponents = updateModuleComponents(
                    module, cached, modifiedIds);

            updatedModules.add(ModuleData.of(
                    module.name(),
                    module.version(),
                    updatedComponents
            ));
        }

        return new Snapshot(updatedModules);
    }

    private List<ComponentData> updateModuleComponents(
            ModuleData module,
            CachedSnapshot cached,
            Set<Long> modifiedIds) {

        List<ComponentData> updatedComponents = new ArrayList<>();
        List<Long> orderedIds = cached.orderedEntityIds();

        // Get module's components for batch retrieval
        List<BaseComponent> moduleComponents = getModuleComponents(module.name());
        if (moduleComponents.isEmpty()) {
            return module.components();
        }

        // Build component list including ENTITY_ID
        List<BaseComponent> allComponents = new ArrayList<>(moduleComponents.size() + 1);
        allComponents.add(CoreComponents.ENTITY_ID);
        allComponents.addAll(moduleComponents);

        float[] buffer = new float[allComponents.size()];

        for (ComponentData originalComponent : module.components()) {
            List<Float> newValues = new ArrayList<>(originalComponent.values());

            // Find component index
            int componentIndex = -1;
            for (int i = 0; i < allComponents.size(); i++) {
                if (allComponents.get(i).getName().equals(originalComponent.name())) {
                    componentIndex = i;
                    break;
                }
            }

            if (componentIndex >= 0) {
                // Update values for modified entities
                for (Long entityId : modifiedIds) {
                    int index = cached.indexOf(entityId);
                    if (index >= 0 && index < newValues.size()) {
                        // Fetch fresh value
                        entityStore.getComponents(entityId, allComponents, buffer);
                        float newValue = buffer[componentIndex];
                        if (!Float.isNaN(newValue)) {
                            newValues.set(index, newValue);
                        }
                    }
                }
            }

            updatedComponents.add(ComponentData.of(originalComponent.name(), newValues));
        }

        return updatedComponents;
    }

    private List<BaseComponent> getModuleComponents(String moduleName) {
        for (EngineModule module : moduleResolver.resolveAllModules()) {
            if (module.getName().equals(moduleName)) {
                List<BaseComponent> components = module.createComponents();
                return components != null ? components : List.of();
            }
        }
        return List.of();
    }

    /**
     * Extracts entity IDs from a snapshot by looking at the ENTITY_ID component.
     */
    private List<Long> extractEntityIds(Snapshot snapshot) {
        for (ModuleData module : snapshot.modules()) {
            Optional<ComponentData> entityIdComponent = module.component("ENTITY_ID");
            if (entityIdComponent.isPresent()) {
                return entityIdComponent.get().values().stream()
                        .map(Float::longValue)
                        .toList();
            }
        }
        return List.of();
    }

    @Override
    public Snapshot createForMatchAndPlayer(long matchId, long playerId) {
        // Player-filtered snapshots are not cached (they're typically one-off requests)
        // Just delegate to the underlying provider
        return delegate.createForMatchAndPlayer(matchId, playerId);
    }

    /**
     * Returns the last dirty info for a match.
     *
     * <p>This is useful for delta snapshot generation - the delta provider can
     * use this to know what changed in the last snapshot generation.
     *
     * @param matchId the match ID
     * @return the last dirty info, or empty if none
     */
    public DirtyInfo getLastDirtyInfo(long matchId) {
        return lastDirtyInfoByMatch.getOrDefault(matchId, DirtyInfo.empty());
    }

    /**
     * Invalidates the cache for a specific match.
     *
     * @param matchId the match ID to invalidate
     */
    public void invalidateCache(long matchId) {
        cache.remove(matchId);
        log.debug("Cache invalidated for match {}", matchId);
    }

    /**
     * Clears all cached snapshots.
     */
    public void clearCache() {
        cache.clear();
        log.debug("All snapshot caches cleared");
    }

    /**
     * Returns the current snapshot metrics.
     *
     * @return snapshot generation metrics
     */
    public SnapshotMetrics getMetrics() {
        long total = totalGenerations.get();
        double avgMs = total > 0 ? totalGenerationTimeMs.sum() / total : 0.0;

        return new SnapshotMetrics(
                total,
                cacheHits.get(),
                cacheMisses.get(),
                incrementalUpdates.get(),
                fullRebuilds.get(),
                avgMs,
                lastGenerationMs,
                maxGenerationMs
        );
    }

    /**
     * Resets all metrics to zero.
     */
    public void resetMetrics() {
        totalGenerations.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        incrementalUpdates.set(0);
        fullRebuilds.set(0);
        totalGenerationTimeMs.reset();
        lastGenerationMs = 0.0;
        maxGenerationMs = 0.0;
        log.debug("Snapshot metrics reset");
    }

    private void recordMetrics(long startNanos) {
        double elapsedMs = (System.nanoTime() - startNanos) / 1_000_000.0;
        lastGenerationMs = elapsedMs;
        totalGenerationTimeMs.add(elapsedMs);
        if (elapsedMs > maxGenerationMs) {
            maxGenerationMs = elapsedMs;
        }
    }

    /**
     * Returns the number of cached snapshots.
     *
     * @return cache size
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Returns the underlying snapshot provider.
     *
     * @return the delegate provider
     */
    public SnapshotProviderImpl getDelegate() {
        return delegate;
    }
}
