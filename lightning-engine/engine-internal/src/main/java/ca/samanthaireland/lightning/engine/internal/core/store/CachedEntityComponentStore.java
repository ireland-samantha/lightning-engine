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

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Decorator that adds query caching to an {@link EntityComponentStore}.
 *
 * <p>This decorator wraps any EntityComponentStore implementation and caches the results
 * of {@link #getEntitiesWithComponents} queries. The cache is automatically
 * invalidated when mutations occur that could affect query results.
 *
 * <p><b>Cache Invalidation Strategy:</b>
 * <ul>
 *   <li>{@link #createEntity(long)} - invalidates all cached queries</li>
 *   <li>{@link #deleteEntity(long)} - invalidates all cached queries</li>
 *   <li>{@link #attachComponent} - invalidates queries containing that component</li>
 *   <li>{@link #attachComponents} - invalidates queries containing any of those components</li>
 *   <li>{@link #removeComponent} - invalidates queries containing that component</li>
 *   <li>{@link #reset()} - clears all cached queries</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * EntityComponentStore baseStore = new ArrayEntityComponentStore(properties);
 * EntityComponentStore cachedStore = new CachedEntityComponentStore(baseStore);
 *
 * // First query - cache miss, delegates to base store
 * Set<Long> result1 = cachedStore.getEntitiesWithComponents(0, 1);
 *
 * // Second query - cache hit, returns cached result
 * Set<Long> result2 = cachedStore.getEntitiesWithComponents(0, 1);
 *
 * // Mutation invalidates relevant cache entries
 * cachedStore.attachComponent(entityId, 0, value);
 *
 * // Next query for component 0 will be a cache miss
 * Set<Long> result3 = cachedStore.getEntitiesWithComponents(0, 1);
 * }</pre>
 *
 * <p><b>Thread Safety:</b> This decorator is NOT thread-safe. For concurrent access,
 * wrap with {@link LockingEntityComponentStore}.
 *
 * @see EntityComponentStore
 * @see QueryCache
 * @see LockingEntityComponentStore
 */
public class CachedEntityComponentStore implements EntityComponentStore {

    private final EntityComponentStore delegate;
    private final QueryCache cache;

    /**
     * Create a cached entity component store wrapping the given delegate.
     *
     * @param delegate the underlying store to wrap
     */
    public CachedEntityComponentStore(EntityComponentStore delegate) {
        this(delegate, new QueryCache());
    }

    /**
     * Create a cached entity component store with a custom cache instance.
     *
     * @param delegate the underlying store to wrap
     * @param cache the query cache to use
     */
    public CachedEntityComponentStore(EntityComponentStore delegate, QueryCache cache) {
        this.delegate = delegate;
        this.cache = cache;
    }

    /**
     * Get the underlying query cache for diagnostics or manual control.
     *
     * @return the query cache
     */
    public QueryCache getCache() {
        return cache;
    }

    /**
     * Get the underlying delegate store.
     *
     * @return the delegate store
     */
    public EntityComponentStore getDelegate() {
        return delegate;
    }

    @Override
    public void reset() {
        delegate.reset();
        cache.clear();
    }

    @Override
    public long createEntityForMatch(long matchId) {
        long entityId = delegate.createEntityForMatch(matchId);
        cache.invalidateAll();
        return entityId;
    }

    @Override
    public void createEntity(long id) {
        delegate.createEntity(id);
        cache.invalidateAll();
    }

    @Override
    public void deleteEntity(long id) {
        delegate.deleteEntity(id);
        cache.invalidateAll();
    }

    @Override
    public void removeComponent(long id, long componentId) {
        delegate.removeComponent(id, componentId);
        cache.invalidateComponent(componentId);
    }

    @Override
    public void removeComponent(long id, BaseComponent component) {
        removeComponent(id, component.getId());
    }

    @Override
    public void attachComponent(long id, long componentId, float value) {
        delegate.attachComponent(id, componentId, value);
        cache.invalidateComponent(componentId);
    }

    @Override
    public void attachComponent(long id, BaseComponent component, float value) {
        attachComponent(id, component.getId(), value);
    }

    @Override
    public void attachComponents(long id, long[] componentIds, float[] values) {
        delegate.attachComponents(id, componentIds, values);
        for (long componentId : componentIds) {
            cache.invalidateComponent(componentId);
        }
    }

    @Override
    public void attachComponents(long id, List<BaseComponent> components, float[] values) {
        delegate.attachComponents(id, components, values);
        for (BaseComponent component : components) {
            cache.invalidateComponent(component.getId());
        }
    }

    @Override
    public Set<Long> getEntitiesWithComponents(long... componentIds) {
        // Check cache first
        Set<Long> cached = cache.get(componentIds);
        if (cached != null) {
            return cached;
        }

        // Cache miss - query delegate and cache result
        Set<Long> result = delegate.getEntitiesWithComponents(componentIds);
        cache.put(result, componentIds);
        return result;
    }

    @Override
    public Set<Long> getEntitiesWithComponents(BaseComponent... components) {
        long[] ids = new long[components.length];
        for (int i = 0; i < components.length; i++) {
            ids[i] = components[i].getId();
        }
        return getEntitiesWithComponents(ids);
    }

    @Override
    public Set<Long> getEntitiesWithComponents(Collection<BaseComponent> components) {
        return getEntitiesWithComponents(components.toArray(new BaseComponent[0]));
    }

    @Override
    public float[] newBuffer() {
        return delegate.newBuffer();
    }

    @Override
    public boolean isNull(float value) {
        return delegate.isNull(value);
    }

    @Override
    public boolean hasComponent(long id, long componentId) {
        return delegate.hasComponent(id, componentId);
    }

    @Override
    public boolean hasComponent(long id, BaseComponent component) {
        return hasComponent(id, component.getId());
    }

    @Override
    public float getComponent(long id, long componentId) {
        return delegate.getComponent(id, componentId);
    }

    @Override
    public float getComponent(long id, BaseComponent component) {
        return getComponent(id, component.getId());
    }

    @Override
    public void getComponents(long id, long[] componentIds, float[] buf) {
        delegate.getComponents(id, componentIds, buf);
    }

    @Override
    public void getComponents(long id, List<BaseComponent> components, float[] buf) {
        delegate.getComponents(id, components, buf);
    }

    // ==================== Statistics Methods ====================

    @Override
    public int getEntityCount() {
        return delegate.getEntityCount();
    }

    @Override
    public int getMaxEntities() {
        return delegate.getMaxEntities();
    }

    @Override
    public int getComponentTypeCount() {
        return delegate.getComponentTypeCount();
    }
}
