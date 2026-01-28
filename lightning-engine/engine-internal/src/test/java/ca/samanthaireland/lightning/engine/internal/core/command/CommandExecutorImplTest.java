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

package ca.samanthaireland.lightning.engine.internal.core.command;

import ca.samanthaireland.lightning.engine.core.command.CommandPayload;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link CommandExecutorImpl}.
 */
@DisplayName("CommandExecutorImpl")
@ExtendWith(MockitoExtension.class)
class CommandExecutorImplTest {

    @Mock
    private EngineCommand mockCommand;

    @Mock
    private CommandPayload mockPayload;

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should accept empty command map")
        void shouldAcceptEmptyCommandMap() {
            Map<String, EngineCommand> emptyMap = Map.of();

            CommandExecutorImpl executor = new CommandExecutorImpl(emptyMap);

            assertThat(executor).isNotNull();
        }

        @Test
        @DisplayName("should accept map with multiple commands")
        void shouldAcceptMapWithMultipleCommands() {
            EngineCommand cmd1 = mock(EngineCommand.class);
            EngineCommand cmd2 = mock(EngineCommand.class);
            Map<String, EngineCommand> commands = Map.of(
                    "cmd1", cmd1,
                    "cmd2", cmd2
            );

            CommandExecutorImpl executor = new CommandExecutorImpl(commands);

            assertThat(executor).isNotNull();
        }
    }

    @Nested
    @DisplayName("executeCommand")
    class ExecuteCommand {

        @Test
        @DisplayName("should execute command when found in map")
        void shouldExecuteCommandWhenFoundInMap() {
            Map<String, EngineCommand> commands = new HashMap<>();
            commands.put("testCommand", mockCommand);
            CommandExecutorImpl executor = new CommandExecutorImpl(commands);

            executor.executeCommand("testCommand", mockPayload);

            verify(mockCommand).executeCommand(mockPayload);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when command not found")
        void shouldThrowWhenCommandNotFound() {
            Map<String, EngineCommand> commands = Map.of();
            CommandExecutorImpl executor = new CommandExecutorImpl(commands);

            assertThatThrownBy(() -> executor.executeCommand("nonExistent", mockPayload))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Command not found: nonExistent");
        }

        @Test
        @DisplayName("should pass payload to command correctly")
        void shouldPassPayloadToCommandCorrectly() {
            CommandPayload specificPayload = mock(CommandPayload.class);
            Map<String, EngineCommand> commands = new HashMap<>();
            commands.put("cmd", mockCommand);
            CommandExecutorImpl executor = new CommandExecutorImpl(commands);

            executor.executeCommand("cmd", specificPayload);

            verify(mockCommand).executeCommand(specificPayload);
        }

        @Test
        @DisplayName("should pass null payload to command")
        void shouldPassNullPayloadToCommand() {
            Map<String, EngineCommand> commands = new HashMap<>();
            commands.put("cmd", mockCommand);
            CommandExecutorImpl executor = new CommandExecutorImpl(commands);

            executor.executeCommand("cmd", null);

            verify(mockCommand).executeCommand(null);
        }

        @Test
        @DisplayName("should handle multiple commands in map")
        void shouldHandleMultipleCommandsInMap() {
            EngineCommand cmd1 = mock(EngineCommand.class);
            EngineCommand cmd2 = mock(EngineCommand.class);
            Map<String, EngineCommand> commands = new HashMap<>();
            commands.put("first", cmd1);
            commands.put("second", cmd2);
            CommandExecutorImpl executor = new CommandExecutorImpl(commands);

            executor.executeCommand("first", mockPayload);
            executor.executeCommand("second", mockPayload);

            verify(cmd1).executeCommand(mockPayload);
            verify(cmd2).executeCommand(mockPayload);
        }
    }
}
