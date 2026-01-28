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

import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;

import java.util.*;

/**
 * A cached snapshot with metadata for efficient incremental updates.
 *
 * <p>Contains the snapshot data along with:
 * <ul>
 *   <li>Entity ID set for detecting adds/removes</li>
 *   <li>Entity to column index mapping for fast value updates</li>
 *   <li>Creation tick for staleness detection</li>
 * </ul>
 *
 * <p>The entity-to-index mapping allows O(1) lookup of where an entity's values
 * are stored in the columnar snapshot data, enabling efficient incremental updates.
 *
 * @param snapshot the cached snapshot data
 * @param entityIds set of entity IDs in this snapshot
 * @param createdTick the tick when this snapshot was created
 * @param entityToIndex mapping from entity ID to column index in component arrays
 */
public record CachedSnapshot(
        Snapshot snapshot,
        Set<Long> entityIds,
        long createdTick,
        Map<Long, Integer> entityToIndex
) {

    /**
     * Creates a CachedSnapshot with automatic index generation.
     *
     * @param snapshot the snapshot data
     * @param orderedEntityIds ordered list of entity IDs (order determines column indices)
     * @param createdTick the tick when this snapshot was created
     * @return a new CachedSnapshot
     */
    public static CachedSnapshot create(Snapshot snapshot, List<Long> orderedEntityIds, long createdTick) {
        Set<Long> entityIdSet = new LinkedHashSet<>(orderedEntityIds);
        Map<Long, Integer> entityToIndex = new HashMap<>(orderedEntityIds.size());

        for (int i = 0; i < orderedEntityIds.size(); i++) {
            entityToIndex.put(orderedEntityIds.get(i), i);
        }

        return new CachedSnapshot(snapshot, entityIdSet, createdTick, entityToIndex);
    }

    /**
     * Creates an empty CachedSnapshot.
     *
     * @param createdTick the tick when this snapshot was created
     * @return an empty CachedSnapshot
     */
    public static CachedSnapshot empty(long createdTick) {
        return new CachedSnapshot(Snapshot.empty(), Set.of(), createdTick, Map.of());
    }

    /**
     * Checks if this cached snapshot is stale based on tick age.
     *
     * @param currentTick the current tick
     * @param maxAge maximum number of ticks before considered stale
     * @return true if the snapshot is stale and should be rebuilt
     */
    public boolean isStale(long currentTick, int maxAge) {
        return currentTick - createdTick > maxAge;
    }

    /**
     * Gets the column index for an entity ID.
     *
     * @param entityId the entity ID
     * @return the column index, or -1 if not found
     */
    public int indexOf(long entityId) {
        Integer index = entityToIndex.get(entityId);
        return index != null ? index : -1;
    }

    /**
     * Checks if this snapshot contains the specified entity.
     *
     * @param entityId the entity ID to check
     * @return true if the entity is in this snapshot
     */
    public boolean containsEntity(long entityId) {
        return entityIds.contains(entityId);
    }

    /**
     * Returns the number of entities in this snapshot.
     *
     * @return entity count
     */
    public int entityCount() {
        return entityIds.size();
    }

    /**
     * Returns an ordered list of entity IDs matching the column order.
     *
     * <p>This is useful for rebuilding the snapshot or creating deltas.
     *
     * @return ordered list of entity IDs
     */
    public List<Long> orderedEntityIds() {
        // Since entityIds is a LinkedHashSet (created in create()), iteration order is preserved
        return new ArrayList<>(entityIds);
    }
}
