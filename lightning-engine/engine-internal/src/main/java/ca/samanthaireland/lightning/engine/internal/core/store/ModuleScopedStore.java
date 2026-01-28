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
import ca.samanthaireland.lightning.engine.core.store.PermissionLevel;
import ca.samanthaireland.lightning.engine.core.store.PermissionRegistry;
import ca.samanthaireland.lightning.engine.auth.module.ModuleAuthToken;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A module-scoped view of an {@link EntityComponentStore} that enforces
 * permission-based access control using JWT claims.
 *
 * <p>Each module gets its own instance of this store. Permission checks
 * are performed entirely using JWT claims in the format
 * {@code moduleName.componentName} with values of {@code owner}, {@code read},
 * or {@code write}.
 *
 * <p>Permission enforcement (all based on JWT claims):
 * <ul>
 *   <li>OWNER permission - full read/write access</li>
 *   <li>READ permission - read-only access</li>
 *   <li>WRITE permission - full read/write access</li>
 *   <li>No claim - access denied</li>
 *   <li>Superuser - bypasses all permission checks</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe. Each module has its own
 * instance with an immutable auth token.
 *
 * @see PermissionRegistry
 * @see EntityComponentStore
 * @see ModuleAuthToken
 */
@Slf4j
public class ModuleScopedStore implements EntityComponentStore {

    private final EntityComponentStore delegate;
    private final PermissionRegistry permissionRegistry;
    private final ModuleAuthToken authToken;

    private ModuleScopedStore(
            EntityComponentStore delegate,
            PermissionRegistry permissionRegistry,
            ModuleAuthToken authToken) {
        this.delegate = delegate;
        this.permissionRegistry = permissionRegistry;
        this.authToken = authToken;
    }

    /**
     * Create a module-scoped store with a JWT auth token.
     *
     * <p>The auth token contains component permission claims in the format
     * {@code moduleName.componentName} with values of owner/read/write.
     *
     * @param delegate the underlying store to delegate to
     * @param permissionRegistry the registry for looking up component info (moduleName, componentName)
     * @param authToken the JWT auth token for this module
     * @return a new module-scoped store
     */
    public static ModuleScopedStore create(
            EntityComponentStore delegate,
            PermissionRegistry permissionRegistry,
            ModuleAuthToken authToken) {
        return new ModuleScopedStore(delegate, permissionRegistry, authToken);
    }

    /**
     * Create an empty module-scoped store for module initialization.
     *
     * <p>This is useful during module initialization when the module's
     * auth token is not yet available. The returned store creates a
     * temporary non-superuser token with no permissions.
     *
     * @param delegate the underlying store to delegate to
     * @param permissionRegistry the registry for looking up component info
     * @param moduleName the name of the module being initialized
     * @return a new module-scoped store with temporary limited access
     */
    public static ModuleScopedStore createEmpty(
            EntityComponentStore delegate,
            PermissionRegistry permissionRegistry,
            String moduleName) {
        // Create a temporary token with no permissions and no superuser
        ModuleAuthToken tempToken = new ModuleAuthToken(moduleName, Map.of(), false, null);
        return new ModuleScopedStore(delegate, permissionRegistry, tempToken);
    }

    /**
     * Get the underlying store.
     *
     * @return the delegate store
     */
    public EntityComponentStore getDelegate() {
        return delegate;
    }

    /**
     * Get the permission registry.
     *
     * @return the permission registry
     */
    public PermissionRegistry getPermissionRegistry() {
        return permissionRegistry;
    }

    /**
     * Get the module's auth token.
     *
     * @return the JWT auth token
     */
    public ModuleAuthToken getAuthToken() {
        return authToken;
    }

    // ==================== Permission Checking (JWT-based only) ====================

    /**
     * Check if this store has superuser privileges (from JWT claims).
     *
     * @return true if this store bypasses permission checks
     */
    public boolean isSuperuser() {
        return authToken.superuser();
    }

    /**
     * Check read access using JWT claims only.
     *
     * @param componentId the component ID to check
     * @throws EcsAccessForbiddenException if access is denied
     */
    private void checkReadAccess(long componentId) {
        // Superuser bypasses all checks
        if (authToken.superuser()) {
            return;
        }

        // Look up the component's owner module and name from the registry
        Optional<String> ownerModule = permissionRegistry.getOwnerModuleName(componentId);
        Optional<String> componentName = permissionRegistry.getComponentName(componentId);

        if (ownerModule.isEmpty() || componentName.isEmpty()) {
            // Component not registered - allow access (for core components)
            log.trace("Component {} not registered, allowing read access", componentId);
            return;
        }

        // Check JWT claims for this component
        if (!authToken.canRead(ownerModule.get(), componentName.get())) {
            String permissionKey = ownerModule.get() + "." + componentName.get();
            throw new EcsAccessForbiddenException(
                    "Cannot read component " + permissionKey + ": no JWT permission claim");
        }
    }

    /**
     * Check write access using JWT claims only.
     *
     * @param componentId the component ID to check
     * @throws EcsAccessForbiddenException if access is denied
     */
    private void checkWriteAccess(long componentId) {
        // Superuser bypasses all checks
        if (authToken.superuser()) {
            return;
        }

        // Look up the component's owner module and name from the registry
        Optional<String> ownerModule = permissionRegistry.getOwnerModuleName(componentId);
        Optional<String> componentName = permissionRegistry.getComponentName(componentId);

        if (ownerModule.isEmpty() || componentName.isEmpty()) {
            // Component not registered - allow access (for core components)
            log.trace("Component {} not registered, allowing write access", componentId);
            return;
        }

        // Check JWT claims for this component
        if (!authToken.canWrite(ownerModule.get(), componentName.get())) {
            String permissionKey = ownerModule.get() + "." + componentName.get();
            throw new EcsAccessForbiddenException(
                    "Cannot write component " + permissionKey + ": no JWT permission claim (need owner or write)");
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
