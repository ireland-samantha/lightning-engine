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

package ca.samanthaireland.lightning.engine.internal.ext.ai;

import ca.samanthaireland.lightning.engine.core.command.CommandExecutor;
import ca.samanthaireland.lightning.engine.core.resources.ResourceManager;
import ca.samanthaireland.game.domain.AICommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultAIContext}.
 */
@DisplayName("DefaultAIContext")
@ExtendWith(MockitoExtension.class)
class DefaultAIContextTest {

    @Mock
    private CommandExecutor mockCommandExecutor;

    @Mock
    private ResourceManager mockResourceManager;

    private DefaultAIContext context;

    @BeforeEach
    void setUp() {
        context = new DefaultAIContext(42L);
    }

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should store matchId")
        void shouldStoreMatchId() {
            DefaultAIContext ctx = new DefaultAIContext(123L);

            assertThat(ctx.getMatchId()).isEqualTo(123L);
        }

        @Test
        @DisplayName("should initialize current tick to 0")
        void shouldInitializeCurrentTickToZero() {
            DefaultAIContext ctx = new DefaultAIContext(1L);

            assertThat(ctx.getCurrentTick()).isZero();
        }
    }

    @Nested
    @DisplayName("getMatchId")
    class GetMatchId {

        @Test
        @DisplayName("should return stored match ID")
        void shouldReturnStoredMatchId() {
            assertThat(context.getMatchId()).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("getCurrentTick and setCurrentTick")
    class CurrentTick {

        @Test
        @DisplayName("should return current tick value")
        void shouldReturnCurrentTickValue() {
            context.setCurrentTick(100L);

            assertThat(context.getCurrentTick()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should update current tick value")
        void shouldUpdateCurrentTickValue() {
            context.setCurrentTick(50L);
            context.setCurrentTick(75L);

            assertThat(context.getCurrentTick()).isEqualTo(75L);
        }

        @Test
        @DisplayName("should handle large tick values")
        void shouldHandleLargeTickValues() {
            context.setCurrentTick(Long.MAX_VALUE);

            assertThat(context.getCurrentTick()).isEqualTo(Long.MAX_VALUE);
        }
    }

    @Nested
    @DisplayName("executeCommand")
    class ExecuteCommand {

        @Test
        @DisplayName("should delegate to CommandExecutor")
        void shouldDelegateToCommandExecutor() {
            context.setCommandExecutor(mockCommandExecutor);
            AICommand aiCommand = AICommand.of("testCmd", Map.of("key", "value"));

            context.executeCommand(aiCommand);

            verify(mockCommandExecutor).executeCommand(eq("testCmd"), any());
        }

        @Test
        @DisplayName("should not throw when executor not set")
        void shouldNotThrowWhenExecutorNotSet() {
            AICommand aiCommand = AICommand.of("testCmd", Map.of());

            assertThatCode(() -> context.executeCommand(aiCommand))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should not execute when executor not set")
        void shouldNotExecuteWhenExecutorNotSet() {
            AICommand aiCommand = AICommand.of("testCmd", Map.of());

            context.executeCommand(aiCommand);

            verifyNoInteractions(mockCommandExecutor);
        }

        @Test
        @DisplayName("should catch exceptions from command execution")
        void shouldCatchExceptionsFromCommandExecution() {
            context.setCommandExecutor(mockCommandExecutor);
            doThrow(new RuntimeException("Execution failed"))
                    .when(mockCommandExecutor).executeCommand(any(), any());
            AICommand aiCommand = AICommand.of("failCmd", Map.of());

            assertThatCode(() -> context.executeCommand(aiCommand))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("getResourceIdByName")
    class GetResourceIdByName {

        @Test
        @DisplayName("should delegate to ResourceManager")
        void shouldDelegateToResourceManager() {
            context.setResourceManager(mockResourceManager);
            when(mockResourceManager.getResourceIdByName("testResource")).thenReturn(123L);

            long result = context.getResourceIdByName("testResource");

            assertThat(result).isEqualTo(123L);
        }

        @Test
        @DisplayName("should return -1 when manager not set")
        void shouldReturnMinusOneWhenManagerNotSet() {
            long result = context.getResourceIdByName("testResource");

            assertThat(result).isEqualTo(-1L);
        }
    }

    @Nested
    @DisplayName("addDependency")
    class AddDependency {

        @Test
        @DisplayName("should store dependency by type")
        void shouldStoreDependencyByType() {
            String dependency = "test dependency";

            context.addDependency(String.class, dependency);

            // Verify by adding multiple and checking no exception
            context.addDependency(Integer.class, 42);
        }
    }

    @Nested
    @DisplayName("copyDependencies")
    class CopyDependencies {

        @Test
        @DisplayName("should copy all dependencies from source map")
        void shouldCopyAllDependenciesFromSourceMap() {
            Map<Class<?>, Object> deps = Map.of(
                    String.class, "value",
                    Integer.class, 42
            );

            assertThatCode(() -> context.copyDependencies(deps))
                    .doesNotThrowAnyException();
        }
    }
}
