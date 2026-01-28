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


package ca.samanthaireland.lightning.engine.internal.ext.module;

import ca.samanthaireland.lightning.engine.ext.module.CompoundModule;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;
import ca.samanthaireland.lightning.engine.ext.module.ModuleIdentifier;
import ca.samanthaireland.lightning.engine.ext.module.ModuleResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Manages modules from external sources (e.g., JAR files).
 *
 * <p>Provides functionality to scan for modules, install new modules,
 * and resolve modules by name.
 */
public interface ModuleManager extends ModuleResolver {

    /**
     * Scan for available modules.
     *
     * <p>This method scans the configured location for module implementations
     * and registers them for later resolution.
     *
     * @throws IOException if the scan fails
     */
    void reloadInstalled() throws IOException;

    /**
     * Install a module from a file.
     *
     * <p>This method installs the module from the specified file,
     * making it available for resolution.
     *
     * @param moduleFile the path to the module file to install
     * @throws IOException if the installation fails
     */
    void installModule(Path moduleFile) throws IOException;

    /**
     * Install a module from a factory class.
     *
     * <p>This method instantiates the factory class and registers the module
     * for later resolution. Useful for programmatic module registration
     * without requiring JAR files.
     *
     * @param moduleFactory the module factory class to install
     */
    void installModule(Class<? extends ModuleFactory> moduleFactory);

    /**
     * Clear all caches and reset the scanned state.
     *
     * <p>Useful for hot-reloading modules.
     */
    void reset();

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
     * Uninstall a module by name.
     *
     * <p>This removes the module from the cache and optionally deletes
     * the associated JAR file from the scan directory.
     *
     * @param moduleName the name of the module to uninstall
     * @return true if the module was found and uninstalled
     */
    boolean uninstallModule(String moduleName);

    /**
     * Get the module factory by name.
     *
     * <p>This method returns the factory instance for a module, which can be used
     * to install the same module type in another container.
     *
     * @param moduleName the name of the module
     * @return the module factory, or null if not found
     */
    ModuleFactory getFactory(String moduleName);

    /**
     * Register a compound module.
     *
     * <p>This method registers a compound module that aggregates other modules.
     * The compound module's component modules must already be registered.
     *
     * @param module the compound module to register
     */
    void registerCompoundModule(CompoundModule module);

    /**
     * Resolve a module by identifier (name + version requirement).
     *
     * <p>Returns the module only if its version is compatible with the required version.
     *
     * @param identifier the module identifier with version requirement
     * @return the resolved module if found and compatible, empty otherwise
     */
    Optional<EngineModule> getModule(ModuleIdentifier identifier);
}
