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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CommandExecutorFromResolver}.
 */
@DisplayName("CommandExecutorFromResolver")
@ExtendWith(MockitoExtension.class)
class CommandExecutorFromResolverTest {

    @Mock
    private CommandResolver mockResolver;

    @Mock
    private EngineCommand mockCommand;

    @Mock
    private CommandPayload mockPayload;

    private CommandExecutorFromResolver executor;

    @BeforeEach
    void setUp() {
        executor = new CommandExecutorFromResolver(mockResolver);
    }

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should accept CommandResolver dependency")
        void shouldAcceptCommandResolverDependency() {
            CommandExecutorFromResolver exec = new CommandExecutorFromResolver(mockResolver);

            assertThat(exec).isNotNull();
        }
    }

    @Nested
    @DisplayName("executeCommand")
    class ExecuteCommand {

        @Test
        @DisplayName("should execute command when resolver finds it")
        void shouldExecuteCommandWhenResolverFindsIt() {
            when(mockResolver.resolveByName("testCmd")).thenReturn(mockCommand);

            executor.executeCommand("testCmd", mockPayload);

            verify(mockCommand).executeCommand(mockPayload);
        }

        @Test
        @DisplayName("should not throw when command not found")
        void shouldNotThrowWhenCommandNotFound() {
            when(mockResolver.resolveByName("unknown")).thenReturn(null);

            assertThatCode(() -> executor.executeCommand("unknown", mockPayload))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should not execute anything when command not found")
        void shouldNotExecuteAnythingWhenCommandNotFound() {
            when(mockResolver.resolveByName("unknown")).thenReturn(null);

            executor.executeCommand("unknown", mockPayload);

            verify(mockResolver).resolveByName("unknown");
            verifyNoMoreInteractions(mockResolver);
        }

        @Test
        @DisplayName("should catch exceptions from command execution")
        void shouldCatchExceptionsFromCommandExecution() {
            when(mockResolver.resolveByName("failingCmd")).thenReturn(mockCommand);
            doThrow(new RuntimeException("Command failed")).when(mockCommand).executeCommand(any());

            assertThatCode(() -> executor.executeCommand("failingCmd", mockPayload))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should pass payload to resolved command")
        void shouldPassPayloadToResolvedCommand() {
            CommandPayload specificPayload = mock(CommandPayload.class);
            when(mockResolver.resolveByName("cmd")).thenReturn(mockCommand);

            executor.executeCommand("cmd", specificPayload);

            verify(mockCommand).executeCommand(specificPayload);
        }

        @Test
        @DisplayName("should resolve command by name from resolver")
        void shouldResolveCommandByNameFromResolver() {
            when(mockResolver.resolveByName("myCommand")).thenReturn(mockCommand);

            executor.executeCommand("myCommand", mockPayload);

            verify(mockResolver).resolveByName("myCommand");
        }

        @Test
        @DisplayName("should handle null payload")
        void shouldHandleNullPayload() {
            when(mockResolver.resolveByName("cmd")).thenReturn(mockCommand);

            executor.executeCommand("cmd", null);

            verify(mockCommand).executeCommand(null);
        }
    }
}
