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


package ca.samanthaireland.lightning.engine.internal;

import ca.samanthaireland.lightning.engine.core.system.EngineSystem;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleResolver;
import ca.samanthaireland.lightning.engine.internal.core.command.CommandQueueExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link GameLoop}.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Constructor validation (SOLID compliance)</li>
 *   <li>Tick execution flow</li>
 *   <li>Error handling during system execution</li>
 *   <li>Cache invalidation</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class GameLoopTest {

    @Mock
    private ModuleResolver moduleResolver;

    @Mock
    private CommandQueueExecutor commandQueueExecutor;

    @Mock
    private EngineModule module;

    @Mock
    private EngineSystem system1;

    @Mock
    private EngineSystem system2;

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("should reject null moduleResolver")
        void shouldRejectNullModuleResolver() {
            assertThatThrownBy(() -> new GameLoop(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("moduleResolver must not be null");
        }

        @Test
        @DisplayName("should reject null moduleResolver with executor")
        void shouldRejectNullModuleResolverWithExecutor() {
            assertThatThrownBy(() -> new GameLoop(null, commandQueueExecutor))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("moduleResolver must not be null");
        }

        @Test
        @DisplayName("should accept null commandQueueExecutor")
        void shouldAcceptNullCommandQueueExecutor() {
            GameLoop loop = new GameLoop(moduleResolver, null);
            assertThat(loop).isNotNull();
        }

        @Test
        @DisplayName("should reject non-positive maxCommandsPerTick")
        void shouldRejectNonPositiveMaxCommands() {
            assertThatThrownBy(() -> new GameLoop(moduleResolver, commandQueueExecutor, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxCommandsPerTick must be positive");

            assertThatThrownBy(() -> new GameLoop(moduleResolver, commandQueueExecutor, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxCommandsPerTick must be positive");
        }

        @Test
        @DisplayName("should accept positive maxCommandsPerTick")
        void shouldAcceptPositiveMaxCommands() {
            GameLoop loop = new GameLoop(moduleResolver, commandQueueExecutor, 100);
            assertThat(loop).isNotNull();
        }
    }

    @Nested
    @DisplayName("Tick execution")
    class TickExecution {

        private GameLoop gameLoop;

        @BeforeEach
        void setUp() {
            when(moduleResolver.resolveAllModules()).thenReturn(List.of(module));
            when(module.createSystems()).thenReturn(List.of(system1, system2));
            gameLoop = new GameLoop(moduleResolver, commandQueueExecutor);
        }

        @Test
        @DisplayName("should execute commands during tick")
        void shouldExecuteCommands() {
            gameLoop.advanceTick(1);

            verify(commandQueueExecutor).executeCommands(10000); // default max
        }

        @Test
        @DisplayName("should execute all systems during tick")
        void shouldExecuteAllSystems() {
            gameLoop.advanceTick(1);

            verify(system1).updateEntities();
            verify(system2).updateEntities();
        }

        @Test
        @DisplayName("should use custom maxCommandsPerTick")
        void shouldUseCustomMaxCommands() {
            GameLoop customLoop = new GameLoop(moduleResolver, commandQueueExecutor, 500);

            customLoop.advanceTick(1);

            verify(commandQueueExecutor).executeCommands(500);
        }

        @Test
        @DisplayName("should skip command execution when executor is null")
        void shouldSkipCommandsWhenExecutorNull() {
            GameLoop loopWithoutExecutor = new GameLoop(moduleResolver);

            loopWithoutExecutor.advanceTick(1);

            verifyNoInteractions(commandQueueExecutor);
            verify(system1).updateEntities();
            verify(system2).updateEntities();
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        private GameLoop gameLoop;

        @BeforeEach
        void setUp() {
            when(moduleResolver.resolveAllModules()).thenReturn(List.of(module));
            gameLoop = new GameLoop(moduleResolver, commandQueueExecutor);
        }

        @Test
        @DisplayName("should continue executing systems after one throws exception")
        void shouldContinueAfterSystemException() {
            when(module.createSystems()).thenReturn(List.of(system1, system2));
            doThrow(new RuntimeException("System 1 failed")).when(system1).updateEntities();

            // Should not throw
            gameLoop.advanceTick(1);

            // Both systems should have been called
            verify(system1).updateEntities();
            verify(system2).updateEntities();
        }

        @Test
        @DisplayName("should handle all systems throwing exceptions")
        void shouldHandleAllSystemsFailing() {
            when(module.createSystems()).thenReturn(List.of(system1, system2));
            doThrow(new RuntimeException("System 1 failed")).when(system1).updateEntities();
            doThrow(new RuntimeException("System 2 failed")).when(system2).updateEntities();

            // Should not throw
            gameLoop.advanceTick(1);

            verify(system1).updateEntities();
            verify(system2).updateEntities();
        }
    }

    @Nested
    @DisplayName("Cache behavior")
    class CacheBehavior {

        @Test
        @DisplayName("should cache systems list")
        void shouldCacheSystemsList() {
            when(moduleResolver.resolveAllModules()).thenReturn(List.of(module));
            when(module.createSystems()).thenReturn(List.of(system1));

            GameLoop gameLoop = new GameLoop(moduleResolver, commandQueueExecutor);

            gameLoop.advanceTick(1);
            gameLoop.advanceTick(2);
            gameLoop.advanceTick(3);

            // Module resolver should only be called once (caching)
            verify(moduleResolver, times(1)).resolveAllModules();
            // System should be called each tick
            verify(system1, times(3)).updateEntities();
        }

        @Test
        @DisplayName("should rebuild cache after invalidation")
        void shouldRebuildCacheAfterInvalidation() {
            when(moduleResolver.resolveAllModules()).thenReturn(List.of(module));
            when(module.createSystems()).thenReturn(List.of(system1));

            GameLoop gameLoop = new GameLoop(moduleResolver, commandQueueExecutor);

            gameLoop.advanceTick(1);
            gameLoop.invalidateCache();
            gameLoop.advanceTick(2);

            // Module resolver should be called twice (before and after invalidation)
            verify(moduleResolver, times(2)).resolveAllModules();
        }
    }

    @Nested
    @DisplayName("Integration scenarios")
    class IntegrationScenarios {

        @Test
        @DisplayName("should track system execution count")
        void shouldTrackSystemExecutionCount() {
            AtomicInteger executionCount = new AtomicInteger(0);
            EngineSystem trackingSystem = () -> executionCount.incrementAndGet();

            when(moduleResolver.resolveAllModules()).thenReturn(List.of(module));
            when(module.createSystems()).thenReturn(List.of(trackingSystem));

            GameLoop gameLoop = new GameLoop(moduleResolver);

            for (int i = 0; i < 100; i++) {
                gameLoop.advanceTick(i);
            }

            assertThat(executionCount.get()).isEqualTo(100);
        }

        @Test
        @DisplayName("should handle empty module list")
        void shouldHandleEmptyModuleList() {
            when(moduleResolver.resolveAllModules()).thenReturn(List.of());

            GameLoop gameLoop = new GameLoop(moduleResolver);

            // Should not throw
            gameLoop.advanceTick(1);
        }

        @Test
        @DisplayName("should handle module with null systems")
        void shouldHandleModuleWithNullSystems() {
            when(moduleResolver.resolveAllModules()).thenReturn(List.of(module));
            when(module.createSystems()).thenReturn(null);

            GameLoop gameLoop = new GameLoop(moduleResolver);

            // Should not throw
            gameLoop.advanceTick(1);
        }
    }
}
