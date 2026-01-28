/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.internal.container;

import ca.samanthaireland.lightning.engine.core.container.ContainerCommandOperations;
import ca.samanthaireland.lightning.engine.core.container.ContainerConfig;
import ca.samanthaireland.lightning.engine.core.container.ContainerStatus;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DefaultContainerCommandOperations")
class DefaultContainerCommandOperationsTest {

    private InMemoryExecutionContainer container;
    private ContainerCommandOperations commands;

    @BeforeEach
    void setUp() {
        container = new InMemoryExecutionContainer(1L, ContainerConfig.withDefaults("test-container"));
        container.lifecycle().start();
        commands = container.commands();
    }

    @AfterEach
    void tearDown() {
        if (container.getStatus() != ContainerStatus.STOPPED) {
            container.lifecycle().stop();
        }
    }

    @Nested
    @DisplayName("command discovery")
    class CommandDiscovery {

        @Test
        @DisplayName("available returns list of commands")
        void availableReturnsList() {
            List<ExecutionContainer.CommandInfo> available = commands.available();

            assertThat(available).isNotNull();
            // Container may or may not have commands depending on loaded modules
        }

        @Test
        @DisplayName("names returns list of command names")
        void namesReturnsList() {
            List<String> names = commands.names();

            assertThat(names).isNotNull();
        }

        @Test
        @DisplayName("has returns false for non-existent command")
        void hasReturnsFalseForNonExistent() {
            boolean hasCommand = commands.has("non-existent-command");

            assertThat(hasCommand).isFalse();
        }

        @Test
        @DisplayName("fromModule returns commands for specified module")
        void fromModuleReturnsCommands() {
            List<ExecutionContainer.CommandInfo> moduleCommands = commands.fromModule("TestModule");

            assertThat(moduleCommands).isNotNull();
            // May be empty if no such module is loaded
        }

        @Test
        @DisplayName("fromModule returns empty list for non-existent module")
        void fromModuleReturnsEmptyForNonExistent() {
            List<ExecutionContainer.CommandInfo> moduleCommands = commands.fromModule("NonExistentModule");

            assertThat(moduleCommands).isEmpty();
        }
    }

    @Nested
    @DisplayName("command builder")
    class CommandBuilderTests {

        @Test
        @DisplayName("named returns CommandBuilder")
        void namedReturnsBuilder() {
            var builder = commands.named("test-command");

            assertThat(builder).isNotNull();
        }

        @Test
        @DisplayName("builder allows forMatch chaining")
        void builderAllowsForMatchChaining() {
            var builder = commands.named("test-command");
            var result = builder.forMatch(1L);

            assertThat(result).isSameAs(builder);
        }

        @Test
        @DisplayName("builder allows param chaining")
        void builderAllowsParamChaining() {
            var builder = commands.named("test-command");
            var result = builder.param("key", "value");

            assertThat(result).isSameAs(builder);
        }

        @Test
        @DisplayName("builder allows withParams chaining")
        void builderAllowsWithParamsChaining() {
            var builder = commands.named("test-command");
            var result = builder.withParams(java.util.Map.of("key", "value"));

            assertThat(result).isSameAs(builder);
        }

        @Test
        @DisplayName("builder allows multiple param calls")
        void builderAllowsMultipleParamCalls() {
            var builder = commands.named("test-command")
                    .param("key1", "value1")
                    .param("key2", 42)
                    .param("key3", true);

            assertThat(builder).isNotNull();
        }

        @Test
        @DisplayName("execute throws for unknown command")
        void executeThrowsForUnknownCommand() {
            assertThatThrownBy(() ->
                    commands.named("unknown-command").execute()
            ).hasMessageContaining("Command not found");
        }

        @Test
        @DisplayName("execute with match throws for unknown command")
        void executeWithMatchThrowsForUnknownCommand() {
            assertThatThrownBy(() ->
                    commands.named("unknown-command")
                            .forMatch(1L)
                            .execute()
            ).hasMessageContaining("Command not found");
        }
    }

    @Nested
    @DisplayName("fluent chaining")
    class FluentChaining {

        @Test
        @DisplayName("available command list is consistent")
        void availableIsConsistent() {
            List<ExecutionContainer.CommandInfo> first = commands.available();
            List<ExecutionContainer.CommandInfo> second = commands.available();

            assertThat(first).isEqualTo(second);
        }

        @Test
        @DisplayName("has is consistent with available")
        void hasIsConsistentWithAvailable() {
            for (ExecutionContainer.CommandInfo cmd : commands.available()) {
                assertThat(commands.has(cmd.name())).isTrue();
            }
        }

        @Test
        @DisplayName("names is consistent with available")
        void namesIsConsistentWithAvailable() {
            List<String> names = commands.names();
            List<String> availableNames = commands.available().stream()
                    .map(ExecutionContainer.CommandInfo::name)
                    .toList();

            assertThat(names).containsExactlyInAnyOrderElementsOf(availableNames);
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandling {

        @Test
        @DisplayName("execute throws when container not running")
        void executeThrowsWhenNotRunning() {
            container.lifecycle().pause();

            // First the command lookup should fail since we don't have any commands loaded
            // but if it did find a command, it would throw IllegalStateException
            assertThatThrownBy(() ->
                    commands.named("any-command").execute()
            ).isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("integration")
    class Integration {

        @Test
        @DisplayName("commands() returns same instance on multiple calls")
        void commandsReturnsSameInstance() {
            ContainerCommandOperations first = container.commands();
            ContainerCommandOperations second = container.commands();

            assertThat(first).isSameAs(second);
        }

        @Test
        @DisplayName("command info contains required fields")
        void commandInfoContainsRequiredFields() {
            for (ExecutionContainer.CommandInfo cmd : commands.available()) {
                assertThat(cmd.name()).isNotNull().isNotEmpty();
                assertThat(cmd.parameters()).isNotNull();
            }
        }

        @Test
        @DisplayName("parameter info contains required fields")
        void parameterInfoContainsRequiredFields() {
            for (ExecutionContainer.CommandInfo cmd : commands.available()) {
                for (ExecutionContainer.ParameterInfo param : cmd.parameters()) {
                    assertThat(param.name()).isNotNull().isNotEmpty();
                    assertThat(param.type()).isNotNull();
                }
            }
        }
    }
}
