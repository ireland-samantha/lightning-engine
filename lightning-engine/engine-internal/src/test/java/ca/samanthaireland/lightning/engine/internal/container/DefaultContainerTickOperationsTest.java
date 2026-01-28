/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.internal.container;

import ca.samanthaireland.lightning.engine.core.container.ContainerConfig;
import ca.samanthaireland.lightning.engine.core.container.ContainerStatus;
import ca.samanthaireland.lightning.engine.core.container.ContainerTickOperations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DefaultContainerTickOperations")
class DefaultContainerTickOperationsTest {

    private InMemoryExecutionContainer container;
    private ContainerTickOperations ticks;

    @BeforeEach
    void setUp() {
        container = new InMemoryExecutionContainer(1L, ContainerConfig.withDefaults("test-container"));
        container.lifecycle().start();
        ticks = container.ticks();
    }

    @AfterEach
    void tearDown() {
        if (container.ticks().isPlaying()) {
            container.ticks().stop();
        }
        if (container.getStatus() != ContainerStatus.STOPPED) {
            container.lifecycle().stop();
        }
    }

    @Nested
    @DisplayName("fluent chaining")
    class FluentChaining {

        @Test
        @DisplayName("advance returns same instance for chaining")
        void advanceReturnsChainableInstance() {
            ContainerTickOperations result = ticks.advance();
            assertThat(result).isSameAs(ticks);
        }

        @Test
        @DisplayName("advanceBy returns same instance for chaining")
        void advanceByReturnsChainableInstance() {
            ContainerTickOperations result = ticks.advanceBy(5);
            assertThat(result).isSameAs(ticks);
        }

        @Test
        @DisplayName("play returns same instance for chaining")
        void playReturnsChainableInstance() {
            ContainerTickOperations result = ticks.play(100);
            assertThat(result).isSameAs(ticks);
            ticks.stop();
        }

        @Test
        @DisplayName("stop returns same instance for chaining")
        void stopReturnsChainableInstance() {
            ticks.play(100);
            ContainerTickOperations result = ticks.stop();
            assertThat(result).isSameAs(ticks);
        }
    }

    @Nested
    @DisplayName("manual tick advancement")
    class ManualTickAdvancement {

        @Test
        @DisplayName("advance increments tick by one")
        void advanceIncrementsByOne() {
            assertThat(ticks.current()).isZero();

            ticks.advance();

            assertThat(ticks.current()).isEqualTo(1);
        }

        @Test
        @DisplayName("multiple advances increment correctly")
        void multipleAdvancesWork() {
            ticks.advance().advance().advance();

            assertThat(ticks.current()).isEqualTo(3);
        }

        @Test
        @DisplayName("advanceBy increments by specified count")
        void advanceByIncrementsCorrectly() {
            ticks.advanceBy(10);

            assertThat(ticks.current()).isEqualTo(10);
        }

        @Test
        @DisplayName("advanceBy with count 1 works")
        void advanceByWithOneWorks() {
            ticks.advanceBy(1);

            assertThat(ticks.current()).isEqualTo(1);
        }

        @Test
        @DisplayName("advanceBy throws for count less than 1")
        void advanceByThrowsForInvalidCount() {
            assertThatThrownBy(() -> ticks.advanceBy(0))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> ticks.advanceBy(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("auto-advance control")
    class AutoAdvanceControl {

        @Test
        @DisplayName("play starts auto-advance with specified interval")
        void playStartsAutoAdvance() throws InterruptedException {
            ticks.play(50);

            assertThat(ticks.isPlaying()).isTrue();
            assertThat(ticks.interval()).isEqualTo(50);

            // Wait for some ticks
            Thread.sleep(200);
            assertThat(ticks.current()).isGreaterThan(0);
        }

        @Test
        @DisplayName("stop stops auto-advance")
        void stopStopsAutoAdvance() {
            ticks.play(50);
            ticks.stop();

            assertThat(ticks.isPlaying()).isFalse();
            assertThat(ticks.interval()).isZero();
        }

        @Test
        @DisplayName("isPlaying returns correct state")
        void isPlayingReturnsCorrectState() {
            assertThat(ticks.isPlaying()).isFalse();

            ticks.play(50);
            assertThat(ticks.isPlaying()).isTrue();

            ticks.stop();
            assertThat(ticks.isPlaying()).isFalse();
        }

        @Test
        @DisplayName("interval returns current interval")
        void intervalReturnsCurrentInterval() {
            assertThat(ticks.interval()).isZero();

            ticks.play(100);
            assertThat(ticks.interval()).isEqualTo(100);

            ticks.stop();
            assertThat(ticks.interval()).isZero();
        }

        @Test
        @DisplayName("play throws for interval less than 1")
        void playThrowsForInvalidInterval() {
            assertThatThrownBy(() -> ticks.play(0))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> ticks.play(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("waitForTick")
    class WaitForTick {

        @Test
        @DisplayName("waitForTick blocks until target tick reached")
        void waitForTickBlocksUntilReached() throws InterruptedException {
            ticks.play(10); // Fast ticks

            ticks.waitForTick(5);

            assertThat(ticks.current()).isGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("waitForTick returns immediately if already past target")
        void waitForTickReturnsIfAlreadyPast() throws InterruptedException {
            ticks.advanceBy(10);
            ticks.play(100);

            long before = System.currentTimeMillis();
            ticks.waitForTick(5);
            long elapsed = System.currentTimeMillis() - before;

            assertThat(elapsed).isLessThan(50); // Should be nearly instant
        }

        @Test
        @DisplayName("waitForTick throws when auto-advance not active")
        void waitForTickThrowsWhenNotPlaying() {
            assertThatThrownBy(() -> ticks.waitForTick(10))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("waitForTick with timeout succeeds when tick reached")
        void waitForTickWithTimeoutSucceeds() throws InterruptedException, TimeoutException {
            ticks.play(10);

            ticks.waitForTick(5, 5000);

            assertThat(ticks.current()).isGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("waitForTick with timeout throws when timeout exceeded")
        void waitForTickWithTimeoutThrows() {
            ticks.play(1000); // Very slow ticks (1 per second)

            assertThatThrownBy(() -> ticks.waitForTick(100, 50))
                    .isInstanceOf(TimeoutException.class);
        }
    }

    @Nested
    @DisplayName("current tick")
    class CurrentTick {

        @Test
        @DisplayName("current returns 0 initially")
        void currentReturnsZeroInitially() {
            assertThat(ticks.current()).isZero();
        }

        @Test
        @DisplayName("current updates after advance")
        void currentUpdatesAfterAdvance() {
            ticks.advance();
            assertThat(ticks.current()).isEqualTo(1);

            ticks.advanceBy(5);
            assertThat(ticks.current()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("complex chains")
    class ComplexChains {

        @Test
        @DisplayName("advance-play-stop chain works")
        void advancePlayStopChainWorks() throws InterruptedException {
            ticks
                    .advance()
                    .advance()
                    .play(50);

            Thread.sleep(100);
            long tickAfterPlay = ticks.current();

            ticks.stop();

            assertThat(tickAfterPlay).isGreaterThan(2);
            assertThat(ticks.isPlaying()).isFalse();
        }

        @Test
        @DisplayName("advanceBy chain works")
        void advanceByChainWorks() {
            ticks
                    .advanceBy(5)
                    .advanceBy(10)
                    .advanceBy(3);

            assertThat(ticks.current()).isEqualTo(18);
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandling {

        @Test
        @DisplayName("advance throws when container not running")
        void advanceThrowsWhenNotRunning() {
            container.lifecycle().pause();

            assertThatThrownBy(() -> ticks.advance())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("advanceBy throws when container not running")
        void advanceByThrowsWhenNotRunning() {
            container.lifecycle().pause();

            assertThatThrownBy(() -> ticks.advanceBy(5))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("play throws when container not running")
        void playThrowsWhenNotRunning() {
            container.lifecycle().pause();

            assertThatThrownBy(() -> ticks.play(50))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
