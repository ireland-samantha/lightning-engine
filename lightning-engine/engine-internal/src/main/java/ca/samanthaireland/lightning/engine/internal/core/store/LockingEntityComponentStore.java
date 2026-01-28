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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Decorator that adds thread-safe read-write locking to any {@link EntityComponentStore}.
 *
 * <p>This decorator wraps any EntityComponentStore implementation and provides
 * thread-safety using a {@link ReadWriteLock}. Multiple threads can read concurrently,
 * but writes are exclusive.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * EntityComponentStore baseStore = new ArrayEntityComponentStore(properties);
 * EntityComponentStore threadSafeStore = LockingEntityComponentStore.wrap(baseStore);
 *
 * // Can also compose with caching:
 * EntityComponentStore cachedStore = new CachedEntityComponentStore(baseStore);
 * EntityComponentStore threadSafeCachedStore = LockingEntityComponentStore.wrap(cachedStore);
 * }</pre>
 *
 * @see EntityComponentStore
 * @see CachedEntityComponentStore
 */
public class LockingEntityComponentStore implements EntityComponentStore {

    private final EntityComponentStore delegate;
    private final ReadWriteLock lock;

    private LockingEntityComponentStore(EntityComponentStore delegate, ReadWriteLock lock) {
        this.delegate = delegate;
        this.lock = lock;
    }

    /**
     * Wrap the given store with thread-safe locking using a new {@link ReentrantReadWriteLock}.
     *
     * @param delegate the underlying store to wrap
     * @return a thread-safe wrapper around the delegate
     */
    public static LockingEntityComponentStore wrap(EntityComponentStore delegate) {
        return new LockingEntityComponentStore(delegate, new ReentrantReadWriteLock());
    }

    /**
     * Wrap the given store with thread-safe locking using a custom lock.
     *
     * @param delegate the underlying store to wrap
     * @param lock the read-write lock to use
     * @return a thread-safe wrapper around the delegate
     */
    public static LockingEntityComponentStore wrap(EntityComponentStore delegate, ReadWriteLock lock) {
        return new LockingEntityComponentStore(delegate, lock);
    }

    // ==================== Lock Helper Methods ====================

    private void withWriteLock(Runnable action) {
        lock.writeLock().lock();
        try {
            action.run();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private <T> T withWriteLock(Supplier<T> action) {
        lock.writeLock().lock();
        try {
            return action.get();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private <T> T withReadLock(Supplier<T> action) {
        lock.readLock().lock();
        try {
            return action.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== Entity Lifecycle Methods ====================

    @Override
    public void reset() {
        withWriteLock(delegate::reset);
    }

    @Override
    public long createEntityForMatch(long matchId) {
        return withWriteLock(() -> delegate.createEntityForMatch(matchId));
    }

    @Override
    public void createEntity(long id) {
        withWriteLock(() -> delegate.createEntity(id));
    }

    @Override
    public void deleteEntity(long id) {
        withWriteLock(() -> delegate.deleteEntity(id));
    }

    // ==================== Component Operations ====================

    @Override
    public void removeComponent(long id, long componentId) {
        withWriteLock(() -> delegate.removeComponent(id, componentId));
    }

    @Override
    public void removeComponent(long id, BaseComponent component) {
        withWriteLock(() -> delegate.removeComponent(id, component));
    }

    @Override
    public void attachComponent(long id, long componentId, float value) {
        withWriteLock(() -> delegate.attachComponent(id, componentId, value));
    }

    @Override
    public void attachComponent(long id, BaseComponent component, float value) {
        withWriteLock(() -> delegate.attachComponent(id, component, value));
    }

    @Override
    public void attachComponents(long id, long[] componentIds, float[] values) {
        withWriteLock(() -> delegate.attachComponents(id, componentIds, values));
    }

    @Override
    public void attachComponents(long id, List<BaseComponent> components, float[] values) {
        withWriteLock(() -> delegate.attachComponents(id, components, values));
    }

    // ==================== Query Operations ====================

    @Override
    public Set<Long> getEntitiesWithComponents(long... componentIds) {
        return withReadLock(() -> delegate.getEntitiesWithComponents(componentIds));
    }

    @Override
    public Set<Long> getEntitiesWithComponents(BaseComponent... components) {
        return withReadLock(() -> delegate.getEntitiesWithComponents(components));
    }

    @Override
    public Set<Long> getEntitiesWithComponents(Collection<BaseComponent> components) {
        return withReadLock(() -> delegate.getEntitiesWithComponents(components));
    }

    // ==================== Buffer and Utility Methods ====================

    @Override
    public float[] newBuffer() {
        return delegate.newBuffer();
    }

    @Override
    public boolean isNull(float value) {
        return delegate.isNull(value);
    }

    // ==================== Component Access Methods ====================

    @Override
    public boolean hasComponent(long id, long componentId) {
        return withReadLock(() -> delegate.hasComponent(id, componentId));
    }

    @Override
    public boolean hasComponent(long id, BaseComponent component) {
        return withReadLock(() -> delegate.hasComponent(id, component));
    }

    @Override
    public float getComponent(long id, long componentId) {
        return withReadLock(() -> delegate.getComponent(id, componentId));
    }

    @Override
    public float getComponent(long id, BaseComponent component) {
        return withReadLock(() -> delegate.getComponent(id, component));
    }

    @Override
    public void getComponents(long id, long[] componentIds, float[] buf) {
        lock.readLock().lock();
        try {
            delegate.getComponents(id, componentIds, buf);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void getComponents(long id, List<BaseComponent> components, float[] buf) {
        lock.readLock().lock();
        try {
            delegate.getComponents(id, components, buf);
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== Statistics Methods ====================

    @Override
    public int getEntityCount() {
        return withReadLock(delegate::getEntityCount);
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
