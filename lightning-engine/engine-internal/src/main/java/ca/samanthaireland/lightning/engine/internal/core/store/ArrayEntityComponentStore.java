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
import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Array-based implementation of {@link EntityComponentStore}.
 *
 * <p>This implementation stores entity components in a flat array (pool) where each entity
 * occupies a contiguous block of memory. The size of each block is determined by the cardinality
 * (maximum number of components per entity).
 *
 * <p><b>Thread Safety:</b> This implementation is NOT thread-safe. For concurrent access,
 * wrap with {@link LockingEntityComponentStore}.
 *
 * <p><b>Memory Management:</b> Deleted entities' memory slots are reclaimed and reused
 * for new entities, preventing memory fragmentation.
 *
 * @see LockingEntityComponentStore
 */
@Slf4j
public class ArrayEntityComponentStore implements EntityComponentStore {

    /**
     * Sentinel value for "index not found" in internal index lookups.
     * Separate from the component NULL (Float.NaN) since this must be an int.
     */
    private static final int NO_INDEX = -1;

    private final int maxComponents;
    private final int maxEntities;
    private final AtomicLong nextEntityId = new AtomicLong(1);

    private int nextFreeRow;
    private int numberOfComponents;

    private float[] pool;
    private Index index;

    /**
     * Create a new array-based entity component store.
     */
    public ArrayEntityComponentStore(EcsProperties properties) {
        this.maxEntities = properties.maxVectors();
        this.maxComponents = properties.maxComponents();
        init();
    }

    @Override
    public void reset() {
        init();
    }

    private void init() {
        nextFreeRow = 0;
        pool = new float[maxEntities * maxComponents];
        Arrays.fill(pool, NULL);
        index = new Index();
    }

    // ==================== Entity Lifecycle Methods ====================

    @Override
    public long createEntityForMatch(long matchId) {
        long entityId = nextEntityId.getAndIncrement();
        allocateEntityRow(entityId);
        attachComponent(entityId, CoreComponents.MATCH_ID, (float) matchId);
        attachComponent(entityId, CoreComponents.ENTITY_ID, (float) entityId);
        log.trace("Created entity {} for match {}", entityId, matchId);
        return entityId;
    }

    @Override
    public void createEntity(long id) {
        allocateEntityRow(id);
    }

    @Override
    public void deleteEntity(long id) {
        int poolIndex = index.entityIdToPoolIndex(id);
        if (poolIndex != NO_INDEX) {
            index.deleteEntity(id);
            pool[poolIndex] = NULL;
        }
    }

    private int allocateEntityRow(long id) {
        int rowIndex = tryAllocateNewRow();
        if (rowIndex == NO_INDEX) {
            rowIndex = index.getNextReclaimedRow();
        }
        if (rowIndex == NO_INDEX) {
            throw new RuntimeException("Entity manager out of memory.");
        }
        index.addEntity(id, rowIndex);
        return rowIndex;
    }

    private int tryAllocateNewRow() {
        if (nextFreeRow < maxEntities) {
            int rowIndex = nextFreeRow * maxComponents;
            nextFreeRow++;
            return rowIndex;
        }
        return NO_INDEX;
    }

    private int getEntityRowOrThrow(long id) {
        int poolIndex = index.entityIdToPoolIndex(id);
        if (poolIndex == NO_INDEX) {
            throw new EntityNotFoundException("Entity with id " + id + " does not exist");
        }
        return poolIndex;
    }

    // ==================== Component Operations ====================

    @Override
    public void removeComponent(long id, long componentId) {
        int poolIndex = index.entityIdToPoolIndex(id);
        if (poolIndex != NO_INDEX) {
            setComponentValue(poolIndex, componentId, NULL);
        }
    }

    @Override
    public void removeComponent(long id, BaseComponent component) {
        removeComponent(id, component.getId());
    }

    @Override
    public void attachComponent(long id, long componentId, float value) {
        int poolIndex = getEntityRowOrThrow(id);
        int internalComponentId = getOrCreateComponentId(componentId);
        setComponentValueInternal(poolIndex, internalComponentId, value);
    }

    @Override
    public void attachComponent(long id, BaseComponent component, float value) {
        attachComponent(id, component.getId(), value);
    }

    @Override
    public void attachComponents(long id, long[] componentIds, float[] values) {
        validateBufferLengths(componentIds.length, values.length, "Component buffer not equal to value buffer");
        int poolIndex = getEntityRowOrThrow(id);
        for (int i = 0; i < componentIds.length; i++) {
            setComponentValueInternal(poolIndex, getOrCreateComponentId(componentIds[i]), values[i]);
        }
    }

    @Override
    public void attachComponents(long id, List<BaseComponent> components, float[] values) {
        attachComponents(id, extractComponentIds(components), values);
    }

    private void setComponentValue(int poolIndex, long componentId, float value) {
        setComponentValueInternal(poolIndex, index.componentIdToInternal(componentId), value);
    }

    private void setComponentValueInternal(int poolIndex, int internalComponentId, float value) {
        pool[poolIndex + internalComponentId] = value;
    }

    private int getOrCreateComponentId(long componentId) {
        int internalId = index.componentIdToInternal(componentId);
        return internalId != NO_INDEX ? internalId : index.newComponentId(componentId);
    }

    // ==================== Query Operations ====================

    @Override
    public Set<Long> getEntitiesWithComponents(long... componentIds) {
        return findEntitiesWithAllComponents(componentIds);
    }

    @Override
    public Set<Long> getEntitiesWithComponents(BaseComponent... components) {
        return getEntitiesWithComponents(extractComponentIds(components));
    }

    @Override
    public Set<Long> getEntitiesWithComponents(Collection<BaseComponent> components) {
        log.trace("Query {}", components);
        return getEntitiesWithComponents(components.toArray(new BaseComponent[0]));
    }

    private Set<Long> findEntitiesWithAllComponents(long... componentIds) {
        Set<Long> result = new LongArraySet();
        for (long entityId : index.entityIds()) {
            if (entityHasAllComponents(entityId, componentIds)) {
                result.add(entityId);
            }
        }
        return result;
    }

    private boolean entityHasAllComponents(long entityId, long... componentIds) {
        for (long componentId : componentIds) {
            if (!hasComponent(entityId, componentId)) {
                return false;
            }
        }
        return true;
    }

    // ==================== Buffer and Utility Methods ====================

    @Override
    public float[] newBuffer() {
        return new float[maxComponents];
    }

    @Override
    public boolean isNull(float value) {
        return Float.isNaN(value);
    }

    // ==================== Component Access Methods ====================

    @Override
    public boolean hasComponent(long id, long componentId) {
        if (index.componentIdToInternal(componentId) == NO_INDEX) {
            return false;
        }

        int poolIndex = index.entityIdToPoolIndex(id);
        if (poolIndex == NO_INDEX) {
            return false;
        }
        return !isNull(getComponentValueInternal(poolIndex, componentId));
    }

    @Override
    public boolean hasComponent(long id, BaseComponent component) {
        return hasComponent(id, component.getId());
    }

    @Override
    public float getComponent(long id, long componentId) {
        int poolIndex = index.entityIdToPoolIndex(id);
        return poolIndex == NO_INDEX ? NULL : getComponentValueInternal(poolIndex, componentId);
    }

    @Override
    public float getComponent(long id, BaseComponent component) {
        return getComponent(id, component.getId());
    }

    @Override
    public void getComponents(long id, long[] componentIds, float[] buffer) {
        validateBufferLengths(componentIds.length, buffer.length, "Cannot get components - buffers are not equal.");
        int poolIndex = index.entityIdToPoolIndex(id);
        if (poolIndex != NO_INDEX) {
            for (int i = 0; i < componentIds.length; i++) {
                buffer[i] = getComponentValueInternal(poolIndex, componentIds[i]);
            }
        }
    }

    @Override
    public void getComponents(long id, List<BaseComponent> components, float[] buffer) {
        getComponents(id, extractComponentIds(components), buffer);
    }

    private float getComponentValueInternal(int poolIndex, long componentId) {
        int internalId = index.componentIdToInternal(componentId);
        if (internalId == NO_INDEX) {
            return NULL;
        }
        return pool[poolIndex + internalId];
    }

    // ==================== Helper Methods ====================

    private void validateBufferLengths(int length1, int length2, String message) {
        if (length1 != length2) {
            throw new IllegalArgumentException(message);
        }
    }

    private long[] extractComponentIds(List<BaseComponent> components) {
        long[] ids = new long[components.size()];
        for (int i = 0; i < components.size(); i++) {
            ids[i] = components.get(i).getId();
        }
        return ids;
    }

    private long[] extractComponentIds(BaseComponent... components) {
        long[] ids = new long[components.length];
        for (int i = 0; i < components.length; i++) {
            ids[i] = components[i].getId();
        }
        return ids;
    }

    // ==================== Inner Classes ====================

    /**
     * Internal index for fast entity and component lookups.
     * Maps entity IDs to pool indices and tracks reclaimed rows for reuse.
     */
    private class Index {
        private final Long2IntOpenHashMap entityIdToRowIndex = new Long2IntOpenHashMap(maxEntities);
        private final IntArrayFIFOQueue reclaimedRows = new IntArrayFIFOQueue(maxEntities);

        private final Long2IntOpenHashMap componentIdToComponentIndex = new Long2IntOpenHashMap(maxComponents);

        public Index() {
            entityIdToRowIndex.defaultReturnValue(NO_INDEX);
            componentIdToComponentIndex.defaultReturnValue(NO_INDEX);
        }

        public void addEntity(long entityId, int denseId) {
            entityIdToRowIndex.put(entityId, denseId);
        }

        public int entityIdToPoolIndex(long entityId) {
            return entityIdToRowIndex.get(entityId);
        }

        public int newComponentId(long componentId) {
            numberOfComponents++;
            componentIdToComponentIndex.put(componentId, numberOfComponents);
            return numberOfComponents;
        }

        public int componentIdToInternal(long componentId) {
            return componentIdToComponentIndex.get(componentId);
        }

        public void deleteEntity(long entityId) {
            int row = entityIdToRowIndex.get(entityId);
            reclaimedRows.enqueue(row);
            entityIdToRowIndex.remove(entityId);
        }

        public int getNextReclaimedRow() {
            if (reclaimedRows.isEmpty()) {
                return NO_INDEX;
            }

            return reclaimedRows.dequeueInt();
        }

        public long[] entityIds() {
            return entityIdToRowIndex.keySet().toLongArray();
        }

        public int entityCount() {
            return entityIdToRowIndex.size();
        }
    }

    @Override
    public int getEntityCount() {
        return index.entityCount();
    }

    @Override
    public int getMaxEntities() {
        return maxEntities;
    }

    @Override
    public int getComponentTypeCount() {
        return numberOfComponents;
    }
}
