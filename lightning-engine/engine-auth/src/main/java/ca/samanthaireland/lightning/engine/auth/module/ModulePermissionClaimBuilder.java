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

package ca.samanthaireland.lightning.engine.auth.module;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionLevel;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for JWT permission claims for modules.
 *
 * <p>This builder creates permission claims in the format {@code moduleName.componentName}
 * with values of OWNER, READ, or WRITE based on:
 * <ul>
 *   <li>OWNER - for the module's own components</li>
 *   <li>READ - for other modules' components with PermissionLevel.READ</li>
 *   <li>WRITE - for other modules' components with PermissionLevel.WRITE</li>
 * </ul>
 *
 * <p>Components with PermissionLevel.PRIVATE are not included unless owned by the module.
 *
 * <p>Usage:
 * <pre>{@code
 * Map<String, ComponentPermission> claims = ModulePermissionClaimBuilder.forModule("MyModule")
 *     .withOwnComponents(declaredComponents)
 *     .withAccessibleComponentsFrom(existingModules)
 *     .build();
 * }</pre>
 */
public final class ModulePermissionClaimBuilder {

    private final String moduleName;
    private final Map<String, ModuleAuthToken.ComponentPermission> claims = new HashMap<>();

    private ModulePermissionClaimBuilder(String moduleName) {
        this.moduleName = moduleName;
    }

    /**
     * Create a new builder for the given module.
     *
     * @param moduleName the name of the module to build claims for
     * @return a new builder instance
     */
    public static ModulePermissionClaimBuilder forModule(String moduleName) {
        return new ModulePermissionClaimBuilder(moduleName);
    }

    /**
     * Add OWNER claims for the module's own components.
     *
     * @param components the module's declared components
     * @return this builder for chaining
     */
    public ModulePermissionClaimBuilder withOwnComponents(List<BaseComponent> components) {
        for (BaseComponent component : components) {
            if (component instanceof PermissionComponent permissionComponent) {
                String key = ModuleAuthToken.permissionKey(moduleName, permissionComponent.getName());
                claims.put(key, ModuleAuthToken.ComponentPermission.OWNER);
            }
        }
        return this;
    }

    /**
     * Add READ/WRITE claims for accessible components from other modules.
     *
     * @param existingModules collection of already-loaded modules
     * @return this builder for chaining
     */
    public ModulePermissionClaimBuilder withAccessibleComponentsFrom(Collection<EngineModule> existingModules) {
        for (EngineModule existingModule : existingModules) {
            String existingModuleName = existingModule.getName();

            // Skip our own module
            if (existingModuleName.equals(moduleName)) {
                continue;
            }

            // Get the existing module's components
            List<BaseComponent> existingComponents = getModuleComponents(existingModule);

            // Add claims based on permission level
            for (BaseComponent component : existingComponents) {
                if (component instanceof PermissionComponent permissionComponent) {
                    PermissionLevel level = permissionComponent.getPermissionLevel();
                    String key = ModuleAuthToken.permissionKey(existingModuleName, permissionComponent.getName());

                    switch (level) {
                        case READ -> claims.put(key, ModuleAuthToken.ComponentPermission.READ);
                        case WRITE -> claims.put(key, ModuleAuthToken.ComponentPermission.WRITE);
                        case PRIVATE -> {
                            // No access to private components from other modules
                        }
                    }
                }
            }
        }
        return this;
    }

    /**
     * Build the final permission claims map.
     *
     * @return immutable map of permission keys to permission levels
     */
    public Map<String, ModuleAuthToken.ComponentPermission> build() {
        return Map.copyOf(claims);
    }

    /**
     * Get all declared components from a module.
     *
     * @param module the engine module
     * @return list of all components (flag + declared components)
     */
    private List<BaseComponent> getModuleComponents(EngineModule module) {
        List<BaseComponent> components = new ArrayList<>();

        BaseComponent flagComponent = module.createFlagComponent();
        if (flagComponent != null) {
            components.add(flagComponent);
        }

        List<BaseComponent> moduleComponents = module.createComponents();
        if (moduleComponents != null) {
            components.addAll(moduleComponents);
        }

        return components;
    }
}
