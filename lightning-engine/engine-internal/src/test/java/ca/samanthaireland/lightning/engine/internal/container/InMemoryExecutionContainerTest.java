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


package ca.samanthaireland.lightning.engine.internal.container;

import ca.samanthaireland.lightning.engine.core.match.Match;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.core.container.ContainerConfig;
import ca.samanthaireland.lightning.engine.core.container.ContainerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InMemoryExecutionContainer")
class InMemoryExecutionContainerTest {

    private InMemoryExecutionContainer container;

    @BeforeEach
    void setUp() {
        container = new InMemoryExecutionContainer(1L, ContainerConfig.withDefaults("test-container"));
    }

    @Nested
    @DisplayName("identity")
    class Identity {

        @Test
        @DisplayName("should return correct ID")
        void shouldReturnCorrectId() {
            assertThat(container.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should return correct name from config")
        void shouldReturnCorrectName() {
            assertThat(container.getName()).isEqualTo("test-container");
        }

        @Test
        @DisplayName("should return config")
        void shouldReturnConfig() {
            ContainerConfig config = container.getConfig();
            assertThat(config).isNotNull();
            assertThat(config.name()).isEqualTo("test-container");
        }
    }

    @Nested
    @DisplayName("lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("should start in CREATED status")
        void shouldStartInCreatedStatus() {
            assertThat(container.getStatus()).isEqualTo(ContainerStatus.CREATED);
        }

        @Test
        @DisplayName("should transition to RUNNING when started")
        void shouldTransitionToRunning() {
            container.lifecycle().start();
            assertThat(container.getStatus()).isEqualTo(ContainerStatus.RUNNING);
        }

        @Test
        @DisplayName("should transition to PAUSED when paused")
        void shouldTransitionToPaused() {
            container.lifecycle().start();
            container.lifecycle().pause();
            assertThat(container.getStatus()).isEqualTo(ContainerStatus.PAUSED);
        }

        @Test
        @DisplayName("should resume from PAUSED to RUNNING")
        void shouldResumeToRunning() {
            container.lifecycle().start();
            container.lifecycle().pause();
            container.lifecycle().resume();
            assertThat(container.getStatus()).isEqualTo(ContainerStatus.RUNNING);
        }

        @Test
        @DisplayName("should transition to STOPPED when stopped")
        void shouldTransitionToStopped() {
            container.lifecycle().start();
            container.lifecycle().stop();
            assertThat(container.getStatus()).isEqualTo(ContainerStatus.STOPPED);
        }

        @Test
        @DisplayName("should throw when starting non-CREATED container")
        void shouldThrowWhenStartingNonCreatedContainer() {
            container.lifecycle().start();
            assertThatThrownBy(() -> container.lifecycle().start())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should throw when pausing non-RUNNING container")
        void shouldThrowWhenPausingNonRunningContainer() {
            assertThatThrownBy(() -> container.lifecycle().pause())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should throw when resuming non-PAUSED container")
        void shouldThrowWhenResumingNonPausedContainer() {
            container.lifecycle().start();
            assertThatThrownBy(() -> container.lifecycle().resume())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("tick control")
    class TickControl {

        @BeforeEach
        void startContainer() {
            container.lifecycle().start();
        }

        @Test
        @DisplayName("should start at tick 0")
        void shouldStartAtTickZero() {
            assertThat(container.ticks().current()).isZero();
        }

        @Test
        @DisplayName("should advance tick by one")
        void shouldAdvanceTickByOne() {
            container.ticks().advance();
            assertThat(container.ticks().current()).isEqualTo(1);
        }

        @Test
        @DisplayName("should advance tick multiple times")
        void shouldAdvanceTickMultipleTimes() {
            container.ticks().advance().advance().advance();
            assertThat(container.ticks().current()).isEqualTo(3);
        }

        @Test
        @DisplayName("should throw when advancing tick on non-running container")
        void shouldThrowWhenAdvancingOnNonRunning() {
            container.lifecycle().pause();
            assertThatThrownBy(() -> container.ticks().advance())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should not be auto-advancing initially")
        void shouldNotBeAutoAdvancingInitially() {
            assertThat(container.ticks().isPlaying()).isFalse();
            assertThat(container.ticks().interval()).isZero();
        }

        @Test
        @DisplayName("should start auto-advance with specified interval")
        void shouldStartAutoAdvance() throws InterruptedException {
            container.ticks().play(50);
            assertThat(container.ticks().isPlaying()).isTrue();
            assertThat(container.ticks().interval()).isEqualTo(50);

            Thread.sleep(150); // Wait for a few ticks
            assertThat(container.ticks().current()).isGreaterThan(0);

            container.ticks().stop();
        }

        @Test
        @DisplayName("should stop auto-advance")
        void shouldStopAutoAdvance() {
            container.ticks().play(50);
            container.ticks().stop();

            assertThat(container.ticks().isPlaying()).isFalse();
            assertThat(container.ticks().interval()).isZero();
        }
    }

    @Nested
    @DisplayName("match management")
    class MatchManagement {

        @BeforeEach
        void startContainer() {
            container.lifecycle().start();
        }

        @Test
        @DisplayName("should create match with container ID")
        void shouldCreateMatchWithContainerId() {
            Match match = new Match(100L, List.of(), List.of());
            Match created = container.matches().create(match);

            assertThat(created.id()).isEqualTo(100L);
            assertThat(created.containerId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should retrieve created match")
        void shouldRetrieveCreatedMatch() {
            Match match = new Match(100L, List.of(), List.of());
            container.matches().create(match);

            Optional<Match> found = container.matches().get(100L);

            assertThat(found).isPresent();
            assertThat(found.get().id()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should return empty for non-existent match")
        void shouldReturnEmptyForNonExistentMatch() {
            Optional<Match> found = container.matches().get(999L);
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should list all matches")
        void shouldListAllMatches() {
            container.matches().create(new Match(1L, List.of(), List.of()));
            container.matches().create(new Match(2L, List.of(), List.of()));
            container.matches().create(new Match(3L, List.of(), List.of()));

            List<Match> all = container.matches().all();

            assertThat(all).hasSize(3);
            assertThat(all).extracting(Match::id).containsExactlyInAnyOrder(1L, 2L, 3L);
        }

        @Test
        @DisplayName("should delete match")
        void shouldDeleteMatch() {
            container.matches().create(new Match(100L, List.of(), List.of()));
            container.matches().delete(100L);

            assertThat(container.matches().get(100L)).isEmpty();
        }

        @Test
        @DisplayName("should throw when creating match on non-running container")
        void shouldThrowWhenCreatingMatchOnNonRunning() {
            container.lifecycle().pause();
            Match match = new Match(100L, List.of(), List.of());

            assertThatThrownBy(() -> container.matches().create(match))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should return empty list when match service not initialized")
        void shouldReturnEmptyWhenMatchServiceNotInitialized() {
            InMemoryExecutionContainer uninitializedContainer =
                    new InMemoryExecutionContainer(2L, ContainerConfig.withDefaults("unstarted"));

            assertThat(uninitializedContainer.matches().all()).isEmpty();
            assertThat(uninitializedContainer.matches().get(1L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("module management")
    class ModuleManagement {

        @BeforeEach
        void startContainer() {
            container.lifecycle().start();
        }

        @Test
        @DisplayName("should return empty commands list when no modules loaded")
        void shouldReturnEmptyCommandsListWhenNoModulesLoaded() {
            var commands = container.getAvailableCommands();
            assertThat(commands).isNotNull();
        }
    }

    @Nested
    @DisplayName("command execution")
    class CommandExecution {

        @BeforeEach
        void startContainer() {
            container.lifecycle().start();
        }

        @Test
        @DisplayName("should throw when enqueueing unknown command")
        void shouldThrowWhenEnqueueingUnknownCommand() {
            assertThatThrownBy(() -> container.commands().named("unknown-command").execute())
                    .hasMessageContaining("Command not found");
        }

        @Test
        @DisplayName("should throw when enqueueing on non-running container")
        void shouldThrowWhenEnqueueingOnNonRunning() {
            container.lifecycle().pause();

            assertThatThrownBy(() -> container.commands().named("any-command").execute())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("AI management")
    class AIManagement {

        @Test
        @DisplayName("should return non-null ai() before container started with empty list")
        void shouldReturnNonNullAiBeforeStartedWithEmptyList() {
            // AI operations are always created, but return empty list before start
            assertThat(container.ai()).isNotNull();
            assertThat(container.ai().available()).isEmpty();
        }

        @Test
        @DisplayName("should return non-null ai() after container started")
        void shouldReturnNonNullAiAfterStarted() {
            container.lifecycle().start();
            assertThat(container.ai()).isNotNull();
        }

        @Test
        @DisplayName("should return empty available list when no AI installed")
        void shouldReturnEmptyAvailableList() {
            container.lifecycle().start();
            assertThat(container.ai().available()).isEmpty();
        }
    }

    @Nested
    @DisplayName("resource management")
    class ResourceManagement {

        @Test
        @DisplayName("should return non-null resources() before container started with empty list")
        void shouldReturnNonNullResourcesBeforeStartedWithEmptyList() {
            // Resource operations are always created, but return empty list before start
            assertThat(container.resources()).isNotNull();
            assertThat(container.resources().all()).isEmpty();
        }

        @Test
        @DisplayName("should return non-null resources() after container started")
        void shouldReturnNonNullResourcesAfterStarted() {
            container.lifecycle().start();
            assertThat(container.resources()).isNotNull();
        }

        @Test
        @DisplayName("should return empty resource list when no resources uploaded")
        void shouldReturnEmptyResourceList() {
            container.lifecycle().start();
            assertThat(container.resources().all()).isEmpty();
        }
    }
}
