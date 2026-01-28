/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.internal.container;

import ca.samanthaireland.lightning.engine.core.container.ContainerConfig;
import ca.samanthaireland.lightning.engine.core.container.ContainerLifecycleOperations;
import ca.samanthaireland.lightning.engine.core.container.ContainerStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DefaultContainerLifecycleOperations")
class DefaultContainerLifecycleOperationsTest {

    private InMemoryExecutionContainer container;
    private ContainerLifecycleOperations lifecycle;

    @BeforeEach
    void setUp() {
        container = new InMemoryExecutionContainer(1L, ContainerConfig.withDefaults("test-container"));
        lifecycle = container.lifecycle();
    }

    @AfterEach
    void tearDown() {
        if (container.getStatus() != ContainerStatus.STOPPED) {
            container.lifecycle().stop();
        }
    }

    @Nested
    @DisplayName("fluent chaining")
    class FluentChaining {

        @Test
        @DisplayName("start returns same instance for chaining")
        void startReturnsChainableInstance() {
            ContainerLifecycleOperations result = lifecycle.start();
            assertThat(result).isSameAs(lifecycle);
        }

        @Test
        @DisplayName("stop returns same instance for chaining")
        void stopReturnsChainableInstance() {
            lifecycle.start();
            ContainerLifecycleOperations result = lifecycle.stop();
            assertThat(result).isSameAs(lifecycle);
        }

        @Test
        @DisplayName("pause returns same instance for chaining")
        void pauseReturnsChainableInstance() {
            lifecycle.start();
            ContainerLifecycleOperations result = lifecycle.pause();
            assertThat(result).isSameAs(lifecycle);
        }

        @Test
        @DisplayName("resume returns same instance for chaining")
        void resumeReturnsChainableInstance() {
            lifecycle.start();
            lifecycle.pause();
            ContainerLifecycleOperations result = lifecycle.resume();
            assertThat(result).isSameAs(lifecycle);
        }

        @Test
        @DisplayName("thenPlay returns same instance for chaining")
        void thenPlayReturnsChainableInstance() {
            lifecycle.start();
            ContainerLifecycleOperations result = lifecycle.thenPlay(100);
            assertThat(result).isSameAs(lifecycle);
            lifecycle.stopAutoAdvance();
        }

        @Test
        @DisplayName("stopAutoAdvance returns same instance for chaining")
        void stopAutoAdvanceReturnsChainableInstance() {
            lifecycle.start();
            lifecycle.thenPlay(100);
            ContainerLifecycleOperations result = lifecycle.stopAutoAdvance();
            assertThat(result).isSameAs(lifecycle);
        }

        @Test
        @DisplayName("startAndPlay returns same instance for chaining")
        void startAndPlayReturnsChainableInstance() {
            ContainerLifecycleOperations result = lifecycle.startAndPlay(100);
            assertThat(result).isSameAs(lifecycle);
            lifecycle.stopAutoAdvance();
        }
    }

    @Nested
    @DisplayName("lifecycle transitions")
    class LifecycleTransitions {

        @Test
        @DisplayName("start transitions to RUNNING")
        void startTransitionsToRunning() {
            lifecycle.start();
            assertThat(lifecycle.status()).isEqualTo(ContainerStatus.RUNNING);
            assertThat(lifecycle.isRunning()).isTrue();
        }

        @Test
        @DisplayName("stop transitions to STOPPED")
        void stopTransitionsToStopped() {
            lifecycle.start();
            lifecycle.stop();
            assertThat(lifecycle.status()).isEqualTo(ContainerStatus.STOPPED);
            assertThat(lifecycle.isStopped()).isTrue();
        }

        @Test
        @DisplayName("pause transitions to PAUSED")
        void pauseTransitionsToPaused() {
            lifecycle.start();
            lifecycle.pause();
            assertThat(lifecycle.status()).isEqualTo(ContainerStatus.PAUSED);
            assertThat(lifecycle.isPaused()).isTrue();
        }

        @Test
        @DisplayName("resume transitions from PAUSED to RUNNING")
        void resumeTransitionsToRunning() {
            lifecycle.start();
            lifecycle.pause();
            lifecycle.resume();
            assertThat(lifecycle.status()).isEqualTo(ContainerStatus.RUNNING);
            assertThat(lifecycle.isRunning()).isTrue();
        }
    }

    @Nested
    @DisplayName("convenience methods")
    class ConvenienceMethods {

        @Test
        @DisplayName("startAndPlay starts container and begins auto-advance")
        void startAndPlayStartsAndAutoAdvances() throws InterruptedException {
            lifecycle.startAndPlay(50);

            assertThat(lifecycle.isRunning()).isTrue();
            assertThat(container.ticks().isPlaying()).isTrue();
            assertThat(container.ticks().interval()).isEqualTo(50);

            // Wait for some ticks
            Thread.sleep(150);
            assertThat(container.ticks().current()).isGreaterThan(0);

            lifecycle.stopAutoAdvance();
        }

        @Test
        @DisplayName("thenPlay starts auto-advance on running container")
        void thenPlayStartsAutoAdvance() throws InterruptedException {
            lifecycle.start();
            lifecycle.thenPlay(50);

            assertThat(container.ticks().isPlaying()).isTrue();
            assertThat(container.ticks().interval()).isEqualTo(50);

            Thread.sleep(150);
            assertThat(container.ticks().current()).isGreaterThan(0);

            lifecycle.stopAutoAdvance();
        }

        @Test
        @DisplayName("stopAutoAdvance stops the auto-advance")
        void stopAutoAdvanceStopsIt() {
            lifecycle.startAndPlay(50);
            lifecycle.stopAutoAdvance();

            assertThat(container.ticks().isPlaying()).isFalse();
            assertThat(container.ticks().interval()).isZero();
        }
    }

    @Nested
    @DisplayName("status queries")
    class StatusQueries {

        @Test
        @DisplayName("isRunning returns true only when RUNNING")
        void isRunningReturnsCorrectValue() {
            assertThat(lifecycle.isRunning()).isFalse();

            lifecycle.start();
            assertThat(lifecycle.isRunning()).isTrue();

            lifecycle.pause();
            assertThat(lifecycle.isRunning()).isFalse();

            lifecycle.resume();
            assertThat(lifecycle.isRunning()).isTrue();

            lifecycle.stop();
            assertThat(lifecycle.isRunning()).isFalse();
        }

        @Test
        @DisplayName("isPaused returns true only when PAUSED")
        void isPausedReturnsCorrectValue() {
            assertThat(lifecycle.isPaused()).isFalse();

            lifecycle.start();
            assertThat(lifecycle.isPaused()).isFalse();

            lifecycle.pause();
            assertThat(lifecycle.isPaused()).isTrue();

            lifecycle.resume();
            assertThat(lifecycle.isPaused()).isFalse();
        }

        @Test
        @DisplayName("isStopped returns true only when STOPPED")
        void isStoppedReturnsCorrectValue() {
            assertThat(lifecycle.isStopped()).isFalse();

            lifecycle.start();
            assertThat(lifecycle.isStopped()).isFalse();

            lifecycle.stop();
            assertThat(lifecycle.isStopped()).isTrue();
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandling {

        @Test
        @DisplayName("start throws when already started")
        void startThrowsWhenAlreadyStarted() {
            lifecycle.start();
            assertThatThrownBy(() -> lifecycle.start())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("pause throws when not running")
        void pauseThrowsWhenNotRunning() {
            assertThatThrownBy(() -> lifecycle.pause())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("resume throws when not paused")
        void resumeThrowsWhenNotPaused() {
            lifecycle.start();
            assertThatThrownBy(() -> lifecycle.resume())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("thenPlay throws when not running")
        void thenPlayThrowsWhenNotRunning() {
            assertThatThrownBy(() -> lifecycle.thenPlay(50))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("complex chains")
    class ComplexChains {

        @Test
        @DisplayName("full lifecycle chain works correctly")
        void fullLifecycleChainWorks() {
            lifecycle
                    .start()
                    .pause()
                    .resume()
                    .stop();

            assertThat(lifecycle.isStopped()).isTrue();
        }

        @Test
        @DisplayName("start-play-stop chain works correctly")
        void startPlayStopChainWorks() throws InterruptedException {
            lifecycle
                    .startAndPlay(50)
                    .stopAutoAdvance()
                    .stop();

            assertThat(lifecycle.isStopped()).isTrue();
            assertThat(container.ticks().isPlaying()).isFalse();
        }
    }
}
