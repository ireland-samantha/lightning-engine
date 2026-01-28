/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.internal.container;

import ca.samanthaireland.lightning.engine.core.command.CommandPayload;
import ca.samanthaireland.lightning.engine.core.container.ContainerCommandOperations;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default implementation of ContainerCommandOperations.
 * Works directly with InMemoryExecutionContainer for command execution.
 */
public final class DefaultContainerCommandOperations implements ContainerCommandOperations {

    private final InMemoryExecutionContainer container;

    public DefaultContainerCommandOperations(InMemoryExecutionContainer container) {
        this.container = container;
    }

    @Override
    public CommandBuilder named(String commandName) {
        return new DefaultCommandBuilder(this, commandName);
    }

    @Override
    public List<ExecutionContainer.CommandInfo> available() {
        return container.getAvailableCommands();
    }

    @Override
    public boolean has(String commandName) {
        return container.getAvailableCommands().stream()
                .anyMatch(cmd -> cmd.name().equals(commandName));
    }

    @Override
    public List<String> names() {
        return container.getAvailableCommands().stream()
                .map(ExecutionContainer.CommandInfo::name)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExecutionContainer.CommandInfo> fromModule(String moduleName) {
        return container.getAvailableCommands().stream()
                .filter(cmd -> moduleName.equals(cmd.module()))
                .collect(Collectors.toList());
    }

    /**
     * Default implementation of CommandBuilder.
     */
    private static final class DefaultCommandBuilder implements CommandBuilder {

        private final DefaultContainerCommandOperations operations;
        private final String commandName;
        private Long matchId;
        private CommandPayload payload;
        private final Map<String, Object> params = new HashMap<>();

        DefaultCommandBuilder(DefaultContainerCommandOperations operations, String commandName) {
            this.operations = operations;
            this.commandName = commandName;
        }

        @Override
        public CommandBuilder forMatch(long matchId) {
            this.matchId = matchId;
            return this;
        }

        @Override
        public CommandBuilder withPayload(CommandPayload payload) {
            this.payload = payload;
            return this;
        }

        @Override
        public CommandBuilder withParams(Map<String, Object> parameters) {
            this.params.putAll(parameters);
            return this;
        }

        @Override
        public CommandBuilder param(String name, Object value) {
            this.params.put(name, value);
            return this;
        }

        @Override
        public ContainerCommandOperations execute() {
            CommandPayload effectivePayload = buildPayload();

            if (matchId != null) {
                operations.container.enqueueCommandInternal(matchId, commandName, effectivePayload);
            } else {
                operations.container.enqueueCommandInternal(commandName, effectivePayload);
            }

            return operations;
        }

        private CommandPayload buildPayload() {
            if (payload != null) {
                return payload;
            }
            if (!params.isEmpty()) {
                return new MapCommandPayload(params);
            }
            return new MapCommandPayload(Map.of());
        }
    }

    /**
     * Simple CommandPayload implementation wrapping a Map.
     */
    private record MapCommandPayload(Map<String, Object> data) implements CommandPayload {
        @Override
        public Map<String, Object> getPayload() {
            return data;
        }
    }
}
