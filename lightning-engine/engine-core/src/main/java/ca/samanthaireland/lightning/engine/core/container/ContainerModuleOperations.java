/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.core.container;

import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;

import java.io.IOException;
import java.util.List;

/**
 * Fluent API for module operations within an ExecutionContainer.
 *
 * <p>Provides chainable methods for installing, reloading, and querying modules.
 *
 * <p>Example usage:
 * <pre>{@code
 * container.modules()
 *     .install("path/to/module.jar")
 *     .install(CustomModuleFactory.class)
 *     .reload();
 *
 * List<String> available = container.modules().available();
 * }</pre>
 */
public interface ContainerModuleOperations {

    /**
     * Installs a module from a JAR file path.
     *
     * @param jarPath path to the module JAR file
     * @return this for fluent chaining
     */
    ContainerModuleOperations install(String jarPath);

    /**
     * Installs a module from a factory class.
     *
     * @param factoryClass the module factory class
     * @return this for fluent chaining
     */
    ContainerModuleOperations install(Class<? extends ModuleFactory> factoryClass);

    /**
     * Reloads all installed modules.
     *
     * @return this for fluent chaining
     * @throws IOException if module loading fails
     */
    ContainerModuleOperations reload() throws IOException;

    /**
     * Returns a list of all available module names.
     *
     * @return list of module names
     */
    List<String> available();

    /**
     * Checks if a module is available.
     *
     * @param moduleName the module name to check
     * @return true if the module is available
     */
    boolean has(String moduleName);
}
