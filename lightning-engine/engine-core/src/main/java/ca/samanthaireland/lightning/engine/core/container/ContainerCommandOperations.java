/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.core.container;

import ca.samanthaireland.lightning.engine.core.command.CommandPayload;

import java.util.List;

/**
 * Fluent API for container command operations.
 *
 * <p>Provides chainable methods for discovering and executing commands.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Execute a command
 * container.commands()
 *     .named("SpawnEntity")
 *     .forMatch(1)
 *     .withPayload(payload)
 *     .execute();
 *
 * // List available commands
 * List<CommandInfo> commands = container.commands().available();
 *
 * // Check if a command exists
 * boolean hasSpawn = container.commands().has("SpawnEntity");
 * }</pre>
 */
public interface ContainerCommandOperations {

    /**
     * Creates a command builder for the specified command name.
     *
     * @param commandName the name of the command
     * @return a command builder
     */
    CommandBuilder named(String commandName);

    /**
     * Returns metadata for all available commands.
     *
     * @return list of command metadata
     */
    List<ExecutionContainer.CommandInfo> available();

    /**
     * Checks if a command with the given name is available.
     *
     * @param commandName the command name to check
     * @return true if the command is available
     */
    boolean has(String commandName);

    /**
     * Returns the names of all available commands.
     *
     * @return list of command names
     */
    List<String> names();

    /**
     * Returns commands provided by the specified module.
     *
     * @param moduleName the module name
     * @return list of command metadata from that module
     */
    List<ExecutionContainer.CommandInfo> fromModule(String moduleName);

    /**
     * Fluent builder for command execution.
     */
    interface CommandBuilder {

        /**
         * Targets a specific match for this command.
         * If not called, the command is container-scoped.
         *
         * @param matchId the target match ID
         * @return this for fluent chaining
         */
        CommandBuilder forMatch(long matchId);

        /**
         * Sets the command payload.
         *
         * @param payload the command payload
         * @return this for fluent chaining
         */
        CommandBuilder withPayload(CommandPayload payload);

        /**
         * Sets the command payload from a map of parameters.
         *
         * @param parameters the parameter map
         * @return this for fluent chaining
         */
        CommandBuilder withParams(java.util.Map<String, Object> parameters);

        /**
         * Adds a single parameter to the payload.
         *
         * @param name the parameter name
         * @param value the parameter value
         * @return this for fluent chaining
         */
        CommandBuilder param(String name, Object value);

        /**
         * Executes the command.
         *
         * @return the command operations for further chaining
         * @throws IllegalStateException if the container is not running
         * @throws IllegalArgumentException if the command doesn't exist
         */
        ContainerCommandOperations execute();
    }
}
