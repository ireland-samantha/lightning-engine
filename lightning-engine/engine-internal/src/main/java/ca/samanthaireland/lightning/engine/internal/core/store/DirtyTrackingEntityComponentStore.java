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

import ca.samanthaireland.lightning.engine.core.entity.CoreComponents;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.internal.core.snapshot.DirtyInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decorator that tracks dirty (modified/added/removed) entities per match.
 *
 * <p>This decorator wraps any {@link EntityComponentStore} implementation and tracks
 * which entities have been modified, added, or removed for each match. This enables
 * incremental snapshot generation by only processing changed entities.
 *
 * <p><b>Dirty Tracking Strategy:</b>
 * <ul>
 *   <li>{@link #createEntityForMatch(long)} - Marks entity as added for that match</li>
 *   <li>{@link #deleteEntity(long)} - Marks entity as removed for its match</li>
 *   <li>{@link #attachComponent}/{@link #attachComponents} - Marks entity as modified for its match</li>
 *   <li>{@link #removeComponent} - Marks entity as modified for its match</li>
 *   <li>{@link #reset()} - Clears all dirty tracking</li>
 * </ul>
 *
 * <p><b>Consumption:</b> Call {@link #consumeDirtyInfo(long)} to atomically retrieve
 * and clear the dirty state for a match. This should be called by the snapshot provider
 * before generating a snapshot.
 *
 * <p><b>Thread Safety:</b> Uses ConcurrentHashMap for thread-safe dirty tracking.
 * However, the delegate store must also be thread-safe for full thread safety.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * EntityComponentStore baseStore = new ArrayEntityComponentStore(properties);
 * DirtyTrackingEntityComponentStore dirtyStore = new DirtyTrackingEntityComponentStore(baseStore);
 *
 * // Create entity - marks as added for match
 * long entityId = dirtyStore.createEntityForMatch(matchId);
 *
 * // Modify component - marks as modified
 * dirtyStore.attachComponent(entityId, SomeComponent.POSITION_X, 10.0f);
 *
 * // Get dirty info and clear (for snapshot generation)
 * DirtyInfo dirty = dirtyStore.consumeDirtyInfo(matchId);
 * // dirty.added() contains entityId
 * }</pre>
 *
 * @see EntityComponentStore
 * @see DirtyInfo
 * @see ca.samanthaireland.lightning.engine.internal.core.snapshot.CachingSnapshotProvider
 */
public class DirtyTrackingEntityComponentStore implements EntityComponentStore {

    private final EntityComponentStore delegate;

    // Per-match dirty tracking using concurrent hash maps for thread safety
    private final Map<Long, Set<Long>> dirtyEntitiesByMatch = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> addedEntitiesByMatch = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> removedEntitiesByMatch = new ConcurrentHashMap<>();

    /**
     * Creates a dirty tracking decorator wrapping the given delegate store.
     *
     * @param delegate the underlying store to wrap
     */
    public DirtyTrackingEntityComponentStore(EntityComponentStore delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /**
     * Gets the underlying delegate store.
     *
     * @return the delegate store
     */
    public EntityComponentStore getDelegate() {
        return delegate;
    }

    /**
     * Consumes and clears the dirty state for a match.
     *
     * <p>This method atomically retrieves all dirty tracking information for the
     * specified match and clears the tracking state. After calling this method,
     * subsequent calls will return empty DirtyInfo until new changes occur.
     *
     * @param matchId the match to get dirty info for
     * @return dirty info containing modified, added, and removed entity sets
     */
    public DirtyInfo consumeDirtyInfo(long matchId) {
        Set<Long> modified = dirtyEntitiesByMatch.remove(matchId);
        Set<Long> added = addedEntitiesByMatch.remove(matchId);
        Set<Long> removed = removedEntitiesByMatch.remove(matchId);

        // Filter added entities from modified set (they're already in added)
        // Note: Transient entities (added then deleted in same interval) are handled
        // at deletion time - they don't appear in either added or removed sets
        if (added != null && modified != null) {
            // Don't report added entities as modified - they're new
            modified.removeAll(added);
        }

        return new DirtyInfo(
                modified != null ? modified : Set.of(),
                added != null ? added : Set.of(),
                removed != null ? removed : Set.of()
        );
    }

    /**
     * Peeks at the current dirty state without clearing it.
     *
     * <p>Useful for debugging or determining if any changes have occurred
     * without consuming the state.
     *
     * @param matchId the match to check
     * @return current dirty info (state is not cleared)
     */
    public DirtyInfo peekDirtyInfo(long matchId) {
        Set<Long> modified = dirtyEntitiesByMatch.get(matchId);
        Set<Long> added = addedEntitiesByMatch.get(matchId);
        Set<Long> removed = removedEntitiesByMatch.get(matchId);

        return new DirtyInfo(
                modified != null ? Set.copyOf(modified) : Set.of(),
                added != null ? Set.copyOf(added) : Set.of(),
                removed != null ? Set.copyOf(removed) : Set.of()
        );
    }

    /**
     * Clears all dirty tracking state.
     */
    public void clearDirtyState() {
        dirtyEntitiesByMatch.clear();
        addedEntitiesByMatch.clear();
        removedEntitiesByMatch.clear();
    }

    /**
     * Marks an entity as dirty (modified) for its match.
     *
     * @param entityId the entity to mark dirty
     */
    private void markDirty(long entityId) {
        float matchIdValue = delegate.getComponent(entityId, CoreComponents.MATCH_ID);
        if (!Float.isNaN(matchIdValue)) {
            long matchId = (long) matchIdValue;
            dirtyEntitiesByMatch
                    .computeIfAbsent(matchId, k -> ConcurrentHashMap.newKeySet())
                    .add(entityId);
        }
    }

    /**
     * Marks an entity as added for a match.
     *
     * @param matchId the match the entity was added to
     * @param entityId the entity that was added
     */
    private void markAdded(long matchId, long entityId) {
        addedEntitiesByMatch
                .computeIfAbsent(matchId, k -> ConcurrentHashMap.newKeySet())
                .add(entityId);
    }

    /**
     * Marks an entity as removed for a match.
     *
     * @param matchId the match the entity was removed from
     * @param entityId the entity that was removed
     */
    private void markRemoved(long matchId, long entityId) {
        removedEntitiesByMatch
                .computeIfAbsent(matchId, k -> ConcurrentHashMap.newKeySet())
                .add(entityId);
    }

    // ==================== EntityComponentStore Implementation ====================

    @Override
    public void reset() {
        delegate.reset();
        clearDirtyState();
    }

    @Override
    public long createEntityForMatch(long matchId) {
        long entityId = delegate.createEntityForMatch(matchId);
        markAdded(matchId, entityId);
        return entityId;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void createEntity(long id) {
        delegate.createEntity(id);
        // Cannot track match for deprecated createEntity - caller must manually set MATCH_ID
    }

    @Override
    public void deleteEntity(long id) {
        // Get match ID before deletion
        float matchIdValue = delegate.getComponent(id, CoreComponents.MATCH_ID);
        delegate.deleteEntity(id);

        if (!Float.isNaN(matchIdValue)) {
            long matchId = (long) matchIdValue;

            // Check if entity was added in this interval (transient entity)
            Set<Long> added = addedEntitiesByMatch.get(matchId);
            boolean wasAdded = added != null && added.remove(id);

            // Also remove from dirty set since entity no longer exists
            Set<Long> dirty = dirtyEntitiesByMatch.get(matchId);
            if (dirty != null) {
                dirty.remove(id);
            }

            // Only mark as removed if it wasn't a transient entity
            // (i.e., it existed before this tracking interval)
            if (!wasAdded) {
                markRemoved(matchId, id);
            }
        }
    }

    @Override
    public void removeComponent(long id, long componentId) {
        delegate.removeComponent(id, componentId);
        markDirty(id);
    }

    @Override
    public void removeComponent(long id, BaseComponent component) {
        delegate.removeComponent(id, component);
        markDirty(id);
    }

    @Override
    public void attachComponent(long id, long componentId, float value) {
        delegate.attachComponent(id, componentId, value);
        markDirty(id);
    }

    @Override
    public void attachComponent(long id, BaseComponent component, float value) {
        delegate.attachComponent(id, component, value);
        markDirty(id);
    }

    @Override
    public void attachComponents(long id, long[] componentIds, float[] values) {
        delegate.attachComponents(id, componentIds, values);
        markDirty(id);
    }

    @Override
    public void attachComponents(long id, List<BaseComponent> components, float[] values) {
        delegate.attachComponents(id, components, values);
        markDirty(id);
    }

    // ==================== Read-only methods (delegate directly) ====================

    @Override
    public Set<Long> getEntitiesWithComponents(long... componentIds) {
        return delegate.getEntitiesWithComponents(componentIds);
    }

    @Override
    public Set<Long> getEntitiesWithComponents(BaseComponent... components) {
        return delegate.getEntitiesWithComponents(components);
    }

    @Override
    public Set<Long> getEntitiesWithComponents(Collection<BaseComponent> components) {
        return delegate.getEntitiesWithComponents(components);
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
        return delegate.hasComponent(id, component);
    }

    @Override
    public float getComponent(long id, long componentId) {
        return delegate.getComponent(id, componentId);
    }

    @Override
    public float getComponent(long id, BaseComponent component) {
        return delegate.getComponent(id, component);
    }

    @Override
    public void getComponents(long id, long[] componentIds, float[] buf) {
        delegate.getComponents(id, componentIds, buf);
    }

    @Override
    public void getComponents(long id, List<BaseComponent> components, float[] buf) {
        delegate.getComponents(id, components, buf);
    }

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
