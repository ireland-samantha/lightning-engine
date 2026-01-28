/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.core.container;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Fluent API for AI operations within an ExecutionContainer.
 *
 * <p>Provides chainable methods for installing, uninstalling, and querying AI.
 *
 * <p>Example usage:
 * <pre>{@code
 * container.ai()
 *     .install(Path.of("path/to/ai.jar"))
 *     .reload();
 *
 * List<String> available = container.ai().available();
 * boolean hasTickCounter = container.ai().has("TickCounter");
 * }</pre>
 */
public interface ContainerAIOperations {

    /**
     * Installs AI from a JAR file path.
     *
     * @param jarPath path to the AI JAR file
     * @return this for fluent chaining
     * @throws IOException if installation fails
     */
    ContainerAIOperations install(Path jarPath) throws IOException;

    /**
     * Installs AI from a factory class name.
     *
     * @param factoryClassName the fully qualified AI factory class name
     * @return this for fluent chaining
     */
    ContainerAIOperations install(String factoryClassName);

    /**
     * Reloads all installed AI from disk.
     *
     * @return this for fluent chaining
     * @throws IOException if reloading fails
     */
    ContainerAIOperations reload() throws IOException;

    /**
     * Returns a list of all available AI names.
     *
     * @return list of AI names
     */
    List<String> available();

    /**
     * Checks if an AI is available.
     *
     * @param aiName the AI name to check
     * @return true if the AI is available
     */
    boolean has(String aiName);

    /**
     * Uninstalls an AI by name.
     *
     * @param aiName the AI name to uninstall
     * @return this for fluent chaining
     */
    ContainerAIOperations uninstall(String aiName);
}
