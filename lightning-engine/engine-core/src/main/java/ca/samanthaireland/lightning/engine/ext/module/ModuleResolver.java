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


package ca.samanthaireland.lightning.engine.ext.module;

import java.util.List;
import java.util.Optional;

public interface ModuleResolver {
    /**
     * Resolve a specific module by name.
     *
     * @param moduleName the name of the module to resolve
     * @return the resolved module, or null if not found
     */
    EngineModule resolveModule(String moduleName);

    /**
     * Get all available module names.
     *
     * @return list of available module names
     */
    List<String> getAvailableModules();

    /**
     * Resolve all available modules.
     *
     * @return list of all resolved modules
     */
    List<EngineModule> resolveAllModules();

    /**
     * Check if a module with the given name is available.
     *
     * @param moduleName the module name to check
     * @return true if the module is available
     */
    boolean hasModule(String moduleName);

    /**
     * Resolve a module by identifier (name + version requirement).
     *
     * <p>Returns the module only if its version is compatible with the required version.
     * Compatibility means: major versions match and actual minor version >= required minor version.
     *
     * @param identifier the module identifier with version requirement
     * @return the resolved module if found and compatible, empty otherwise
     */
    default Optional<EngineModule> resolveModule(ModuleIdentifier identifier) {
        if (identifier == null) {
            return Optional.empty();
        }
        EngineModule module = resolveModule(identifier.name());
        if (module == null) {
            return Optional.empty();
        }
        if (!module.getVersion().isCompatibleWith(identifier.version())) {
            return Optional.empty();
        }
        return Optional.of(module);
    }

    /**
     * Get all available versions for a module.
     *
     * <p>Default implementation returns a single-element list with the current version
     * if the module exists, or an empty list otherwise.
     *
     * @param moduleName the module name
     * @return list of available versions for the module
     */
    default List<ModuleVersion> getAvailableVersions(String moduleName) {
        EngineModule module = resolveModule(moduleName);
        if (module == null) {
            return List.of();
        }
        return List.of(module.getVersion());
    }
}
