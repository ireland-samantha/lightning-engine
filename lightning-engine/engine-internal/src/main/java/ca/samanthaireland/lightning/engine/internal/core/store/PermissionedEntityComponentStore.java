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

import ca.samanthaireland.lightning.engine.core.exception.EcsAccessForbiddenException;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.core.store.PermissionComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionLevel;
import ca.samanthaireland.lightning.engine.core.store.PermissionedStore;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decorator that adds module-level permission checking to an {@link EntityComponentStore}.
 *
 * <p>This decorator enforces access control based on {@link PermissionLevel}:
 * <ul>
 *   <li>{@link PermissionLevel#PRIVATE} - Only the owning module can read or write</li>
 *   <li>{@link PermissionLevel#READ} - Any module can read, only the owner can write</li>
 *   <li>{@link PermissionLevel#WRITE} - Any module can read and write</li>
 * </ul>
 *
 * <p>A module always has full access to its own components (determined by the current
 * module context), regardless of the permission level.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * EntityComponentStore baseStore = new ArrayEntityComponentStore(properties);
 * PermissionedEntityComponentStore store = PermissionedEntityComponentStore.wrap(baseStore);
 *
 * // Register components with their permissions
 * PermissionComponent privateComp = PermissionComponent.create("INTERNAL", PermissionLevel.PRIVATE);
 * PermissionComponent readComp = PermissionComponent.create("POSITION", PermissionLevel.READ);
 * store.registerComponent(privateComp);
 * store.registerComponent(readComp);
 *
 * // Set current module context before operations
 * store.setCurrentModuleContext(Set.of(privateComp.getId(), readComp.getId()));
 * try {
 *     // Operations here have access to privateComp and readComp
 *     store.attachComponent(entityId, privateComp, value);
 * } finally {
 *     store.clearCurrentModuleContext();
 * }
 * }</pre>
 *
 * <p><b>Thread Safety:</b> The permission registry is thread-safe. The current module
 * context uses ThreadLocal, so each thread has its own context. For thread-safe
 * store operations, wrap with {@link LockingEntityComponentStore}.
 *
 * @see PermissionComponent
 * @see PermissionLevel
 * @see EcsAccessForbiddenException
 */
public class PermissionedEntityComponentStore implements PermissionedStore {

    /**
     * Record to hold component registration info.
     */
    private record ComponentInfo(PermissionLevel level, String ownerModule, String componentName) {}

    private final EntityComponentStore delegate;
    private final Map<Long, PermissionLevel> componentPermissions = new ConcurrentHashMap<>();
    private final Map<Long, ComponentInfo> componentInfoMap = new ConcurrentHashMap<>();
    private final ThreadLocal<Set<Long>> currentModuleOwnedComponents = ThreadLocal.withInitial(Set::of);

    private PermissionedEntityComponentStore(EntityComponentStore delegate) {
        this.delegate = delegate;
    }

    /**
     * Wrap the given store with permission checking.
     *
     * @param delegate the underlying store to wrap
     * @return a permission-checking wrapper around the delegate
     */
    public static PermissionedEntityComponentStore wrap(EntityComponentStore delegate) {
        return new PermissionedEntityComponentStore(delegate);
    }

    /**
     * Get the underlying delegate store.
     *
     * @return the delegate store
     */
    public EntityComponentStore getDelegate() {
        return delegate;
    }

    // ==================== Permission Registration ====================

    @Override
    public void registerComponent(PermissionComponent component, String ownerModuleName) {
        componentPermissions.put(component.getId(), component.getPermissionLevel());
        componentInfoMap.put(
                component.getId(),
                new ComponentInfo(component.getPermissionLevel(), ownerModuleName, component.getName())
        );
    }

    @Override
    public void registerComponent(PermissionComponent component) {
        registerComponent(component, "Unknown");
    }

    @Override
    public void registerComponents(Collection<PermissionComponent> components, String ownerModuleName) {
        for (PermissionComponent component : components) {
            registerComponent(component, ownerModuleName);
        }
    }

    @Override
    public void registerComponents(Collection<PermissionComponent> components) {
        for (PermissionComponent component : components) {
            registerComponent(component);
        }
    }

    @Override
    public PermissionLevel getPermissionLevel(long componentId) {
        return componentPermissions.getOrDefault(componentId, PermissionLevel.PRIVATE);
    }

    @Override
    public Optional<String> getOwnerModuleName(long componentId) {
        ComponentInfo info = componentInfoMap.get(componentId);
        return info != null ? Optional.of(info.ownerModule()) : Optional.empty();
    }

    @Override
    public Optional<String> getComponentName(long componentId) {
        ComponentInfo info = componentInfoMap.get(componentId);
        return info != null ? Optional.of(info.componentName()) : Optional.empty();
    }

    @Override
    public void clear() {
        componentPermissions.clear();
        componentInfoMap.clear();
    }

    // ==================== Module Context ====================

    @Override
    public void setCurrentModuleContext(Set<Long> ownedComponentIds) {
        currentModuleOwnedComponents.set(ownedComponentIds != null ? ownedComponentIds : Set.of());
    }

    @Override
    public void clearCurrentModuleContext() {
        currentModuleOwnedComponents.remove();
    }

    @Override
    public Set<Long> getCurrentModuleContext() {
        return currentModuleOwnedComponents.get();
    }

    // ==================== Permission Checking ====================

    private boolean isOwnedByCurrentModule(long componentId) {
        return currentModuleOwnedComponents.get().contains(componentId);
    }

    private void checkReadAccess(long componentId) {
        if (isOwnedByCurrentModule(componentId)) {
            return;
        }

        PermissionLevel level = getPermissionLevel(componentId);
        if (level == PermissionLevel.PRIVATE) {
            throw new EcsAccessForbiddenException(
                    "Cannot read component " + componentId + ": permission level is PRIVATE");
        }
        // READ and WRITE both allow reading
    }

    private void checkWriteAccess(long componentId) {
        if (isOwnedByCurrentModule(componentId)) {
            return;
        }

        PermissionLevel level = getPermissionLevel(componentId);
        if (level != PermissionLevel.WRITE) {
            throw new EcsAccessForbiddenException(
                    "Cannot write component " + componentId + ": permission level is " + level);
        }
    }

    private void checkReadAccess(BaseComponent component) {
        checkReadAccess(component.getId());
    }

    private void checkWriteAccess(BaseComponent component) {
        checkWriteAccess(component.getId());
    }

    // ==================== Entity Lifecycle Methods ====================

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public long createEntityForMatch(long matchId) {
        return delegate.createEntityForMatch(matchId);
    }

    @Override
    public void createEntity(long id) {
        delegate.createEntity(id);
    }

    @Override
    public void deleteEntity(long id) {
        delegate.deleteEntity(id);
    }

    // ==================== Component Write Operations ====================

    @Override
    public void removeComponent(long id, long componentId) {
        checkWriteAccess(componentId);
        delegate.removeComponent(id, componentId);
    }

    @Override
    public void removeComponent(long id, BaseComponent component) {
        checkWriteAccess(component);
        delegate.removeComponent(id, component);
    }

    @Override
    public void attachComponent(long id, long componentId, float value) {
        checkWriteAccess(componentId);
        delegate.attachComponent(id, componentId, value);
    }

    @Override
    public void attachComponent(long id, BaseComponent component, float value) {
        checkWriteAccess(component);
        delegate.attachComponent(id, component, value);
    }

    @Override
    public void attachComponents(long id, long[] componentIds, float[] values) {
        for (long componentId : componentIds) {
            checkWriteAccess(componentId);
        }
        delegate.attachComponents(id, componentIds, values);
    }

    @Override
    public void attachComponents(long id, List<BaseComponent> components, float[] values) {
        for (BaseComponent component : components) {
            checkWriteAccess(component);
        }
        delegate.attachComponents(id, components, values);
    }

    // ==================== Component Read Operations ====================

    @Override
    public Set<Long> getEntitiesWithComponents(long... componentIds) {
        for (long componentId : componentIds) {
            checkReadAccess(componentId);
        }
        return delegate.getEntitiesWithComponents(componentIds);
    }

    @Override
    public Set<Long> getEntitiesWithComponents(BaseComponent... components) {
        for (BaseComponent component : components) {
            checkReadAccess(component);
        }
        return delegate.getEntitiesWithComponents(components);
    }

    @Override
    public Set<Long> getEntitiesWithComponents(Collection<BaseComponent> components) {
        for (BaseComponent component : components) {
            checkReadAccess(component);
        }
        return delegate.getEntitiesWithComponents(components);
    }

    @Override
    public boolean hasComponent(long id, long componentId) {
        checkReadAccess(componentId);
        return delegate.hasComponent(id, componentId);
    }

    @Override
    public boolean hasComponent(long id, BaseComponent component) {
        checkReadAccess(component);
        return delegate.hasComponent(id, component);
    }

    @Override
    public float getComponent(long id, long componentId) {
        checkReadAccess(componentId);
        return delegate.getComponent(id, componentId);
    }

    @Override
    public float getComponent(long id, BaseComponent component) {
        checkReadAccess(component);
        return delegate.getComponent(id, component);
    }

    @Override
    public void getComponents(long id, long[] componentIds, float[] buf) {
        for (long componentId : componentIds) {
            checkReadAccess(componentId);
        }
        delegate.getComponents(id, componentIds, buf);
    }

    @Override
    public void getComponents(long id, List<BaseComponent> components, float[] buf) {
        for (BaseComponent component : components) {
            checkReadAccess(component);
        }
        delegate.getComponents(id, components, buf);
    }

    // ==================== Utility Methods ====================

    @Override
    public float[] newBuffer() {
        return delegate.newBuffer();
    }

    @Override
    public boolean isNull(float value) {
        return delegate.isNull(value);
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
