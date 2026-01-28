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


package ca.samanthaireland.lightning.engine.core.store;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry for component permission levels and ownership information.
 *
 * <p>This interface provides methods to register and query permission levels
 * for components, including tracking which module owns each component.
 * This enables JWT-based permission checking where permissions are stored
 * as {@code moduleName.componentName.permission} claims.
 *
 * @see PermissionComponent
 * @see PermissionLevel
 */
public interface PermissionRegistry {

    /**
     * Register a permission component with its owning module.
     *
     * <p>Components that are not registered default to {@link PermissionLevel#PRIVATE}.
     *
     * @param component the permission component to register
     * @param ownerModuleName the name of the module that owns this component
     */
    void registerComponent(PermissionComponent component, String ownerModuleName);

    /**
     * Register a component (without explicit owner - uses component info only).
     *
     * @param component the permission component to register
     */
    void registerComponent(PermissionComponent component);

    /**
     * Register multiple permission components with their owning module.
     *
     * @param components the permission components to register
     * @param ownerModuleName the name of the module that owns these components
     */
    void registerComponents(Collection<PermissionComponent> components, String ownerModuleName);

    /**
     * Register multiple permission components.
     *
     * @param components the permission components to register
     */
    void registerComponents(Collection<PermissionComponent> components);

    /**
     * Get the registered permission level for a component.
     *
     * @param componentId the component ID
     * @return the permission level, or {@link PermissionLevel#PRIVATE} if not registered
     */
    PermissionLevel getPermissionLevel(long componentId);

    /**
     * Get the owner module name for a component.
     *
     * @param componentId the component ID
     * @return the owner module name, or empty if not registered
     */
    Optional<String> getOwnerModuleName(long componentId);

    /**
     * Get the component name for a component ID.
     *
     * @param componentId the component ID
     * @return the component name, or empty if not registered
     */
    Optional<String> getComponentName(long componentId);

    /**
     * Build the JWT permission key for a component.
     *
     * @param componentId the component ID
     * @return the permission key (moduleName.componentName), or empty if not registered
     */
    default Optional<String> getPermissionKey(long componentId) {
        Optional<String> moduleName = getOwnerModuleName(componentId);
        Optional<String> componentName = getComponentName(componentId);
        if (moduleName.isPresent() && componentName.isPresent()) {
            return Optional.of(moduleName.get() + "." + componentName.get());
        }
        return Optional.empty();
    }

    /**
     * Clear all registered permissions.
     *
     * <p>This is useful for hot-reloading modules, where all permissions
     * need to be re-registered.
     */
    void clear();
}
