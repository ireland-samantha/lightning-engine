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

import ca.samanthaireland.lightning.engine.core.container.ContainerStatus;
import ca.samanthaireland.lightning.engine.internal.GameLoop;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ContainerTickExecutor}.
 */
class ContainerTickExecutorTest {

    private ContainerTickExecutor executor;
    private ContainerStatus currentStatus;

    @BeforeEach
    void setUp() {
        currentStatus = ContainerStatus.RUNNING;
        executor = new ContainerTickExecutor(1L, "test-container", () -> currentStatus);
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdown(1, TimeUnit.SECONDS);
        }
    }

    // ========== Initial State Tests ==========

    @Test
    @DisplayName("New executor should have tick value of 0")
    void newExecutorShouldHaveTickZero() {
        assertThat(executor.getCurrentTick()).isEqualTo(0);
    }

    @Test
    @DisplayName("New executor should not be auto-advancing")
    void newExecutorShouldNotBeAutoAdvancing() {
        assertThat(executor.isAutoAdvancing()).isFalse();
    }

    @Test
    @DisplayName("New executor should have auto-advance interval of 0")
    void newExecutorShouldHaveZeroInterval() {
        assertThat(executor.getAutoAdvanceInterval()).isEqualTo(0);
    }

    // ========== Manual Tick Advancement Tests ==========

    @Test
    @DisplayName("advanceTick() should increment tick value")
    void advanceTickShouldIncrementTick() {
        long newTick = executor.advanceTick();

        assertThat(newTick).isEqualTo(1);
        assertThat(executor.getCurrentTick()).isEqualTo(1);
    }

    @Test
    @DisplayName("Multiple advanceTick() calls should increment correctly")
    void multipleAdvanceTickCallsShouldIncrement() {
        executor.advanceTick();
        executor.advanceTick();
        long thirdTick = executor.advanceTick();

        assertThat(thirdTick).isEqualTo(3);
        assertThat(executor.getCurrentTick()).isEqualTo(3);
    }

    @Test
    @DisplayName("advanceTick() should call gameLoop.advanceTick()")
    @Timeout(5)
    void advanceTickShouldCallGameLoop() throws InterruptedException {
        GameLoop mockGameLoop = mock(GameLoop.class);
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(mockGameLoop).advanceTick(anyLong());

        executor.setGameLoop(mockGameLoop);
        executor.advanceTick();

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        verify(mockGameLoop).advanceTick(1L);
    }

    @Test
    @DisplayName("advanceTick() without gameLoop should not throw")
    void advanceTickWithoutGameLoopShouldNotThrow() {
        // No gameLoop set - should not throw
        long tick = executor.advanceTick();
        assertThat(tick).isEqualTo(1);
    }

    // ========== Auto-Advance Tests ==========

    @Test
    @DisplayName("startAutoAdvance() should enable auto-advancing")
    void startAutoAdvanceShouldEnable() {
        executor.startAutoAdvance(100);

        assertThat(executor.isAutoAdvancing()).isTrue();
    }

    @Test
    @DisplayName("startAutoAdvance() should set interval")
    void startAutoAdvanceShouldSetInterval() {
        executor.startAutoAdvance(50);

        assertThat(executor.getAutoAdvanceInterval()).isEqualTo(50);
    }

    @Test
    @DisplayName("startAutoAdvance() with zero interval should throw")
    void startAutoAdvanceWithZeroIntervalShouldThrow() {
        assertThatThrownBy(() -> executor.startAutoAdvance(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("intervalMs must be positive");
    }

    @Test
    @DisplayName("startAutoAdvance() with negative interval should throw")
    void startAutoAdvanceWithNegativeIntervalShouldThrow() {
        assertThatThrownBy(() -> executor.startAutoAdvance(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("intervalMs must be positive");
    }

    @Test
    @DisplayName("startAutoAdvance() should advance ticks automatically")
    @Timeout(5)
    void startAutoAdvanceShouldAdvanceTicks() throws InterruptedException {
        AtomicLong tickCount = new AtomicLong(0);
        CountDownLatch latch = new CountDownLatch(3);

        GameLoop mockGameLoop = mock(GameLoop.class);
        doAnswer(invocation -> {
            tickCount.incrementAndGet();
            latch.countDown();
            return null;
        }).when(mockGameLoop).advanceTick(anyLong());

        executor.setGameLoop(mockGameLoop);
        executor.startAutoAdvance(50);

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(tickCount.get()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("startAutoAdvance() should only advance when status is RUNNING")
    @Timeout(5)
    void startAutoAdvanceShouldOnlyAdvanceWhenRunning() throws InterruptedException {
        currentStatus = ContainerStatus.PAUSED;

        GameLoop mockGameLoop = mock(GameLoop.class);
        executor.setGameLoop(mockGameLoop);

        executor.startAutoAdvance(50);

        // Wait a bit to ensure ticks would have happened if status check wasn't working
        Thread.sleep(200);

        // Should not have called advanceTick because status is PAUSED
        verify(mockGameLoop, never()).advanceTick(anyLong());
    }

    @Test
    @DisplayName("startAutoAdvance() should stop previous auto-advance")
    void startAutoAdvanceShouldStopPrevious() {
        executor.startAutoAdvance(100);
        assertThat(executor.getAutoAdvanceInterval()).isEqualTo(100);

        executor.startAutoAdvance(50);
        assertThat(executor.getAutoAdvanceInterval()).isEqualTo(50);
        assertThat(executor.isAutoAdvancing()).isTrue();
    }

    // ========== Stop Auto-Advance Tests ==========

    @Test
    @DisplayName("stopAutoAdvance() should disable auto-advancing")
    void stopAutoAdvanceShouldDisable() {
        executor.startAutoAdvance(100);
        executor.stopAutoAdvance();

        assertThat(executor.isAutoAdvancing()).isFalse();
    }

    @Test
    @DisplayName("stopAutoAdvance() should reset interval to 0")
    void stopAutoAdvanceShouldResetInterval() {
        executor.startAutoAdvance(100);
        executor.stopAutoAdvance();

        assertThat(executor.getAutoAdvanceInterval()).isEqualTo(0);
    }

    @Test
    @DisplayName("stopAutoAdvance() should stop tick advancement")
    @Timeout(5)
    void stopAutoAdvanceShouldStopTickAdvancement() throws InterruptedException {
        AtomicInteger callCount = new AtomicInteger(0);

        GameLoop mockGameLoop = mock(GameLoop.class);
        doAnswer(invocation -> {
            callCount.incrementAndGet();
            return null;
        }).when(mockGameLoop).advanceTick(anyLong());

        executor.setGameLoop(mockGameLoop);
        executor.startAutoAdvance(50);

        // Wait for a few ticks
        Thread.sleep(150);

        int countAtStop = callCount.get();
        executor.stopAutoAdvance();

        // Wait some more
        Thread.sleep(150);

        // Count should not have increased significantly after stop
        assertThat(callCount.get()).isLessThanOrEqualTo(countAtStop + 1);
    }

    @Test
    @DisplayName("stopAutoAdvance() when not running should not throw")
    void stopAutoAdvanceWhenNotRunningShouldNotThrow() {
        // Should not throw even if not auto-advancing
        executor.stopAutoAdvance();
        assertThat(executor.isAutoAdvancing()).isFalse();
    }

    @Test
    @DisplayName("Multiple stopAutoAdvance() calls should not throw")
    void multipleStopAutoAdvanceCallsShouldNotThrow() {
        executor.startAutoAdvance(100);
        executor.stopAutoAdvance();
        executor.stopAutoAdvance();
        executor.stopAutoAdvance();

        assertThat(executor.isAutoAdvancing()).isFalse();
    }

    // ========== Shutdown Tests ==========

    @Test
    @DisplayName("shutdown() should stop auto-advance")
    void shutdownShouldStopAutoAdvance() {
        executor.startAutoAdvance(100);
        executor.shutdown(1, TimeUnit.SECONDS);

        assertThat(executor.isAutoAdvancing()).isFalse();
    }

    @Test
    @DisplayName("shutdown() should return true on successful termination")
    void shutdownShouldReturnTrueOnSuccess() {
        boolean result = executor.shutdown(1, TimeUnit.SECONDS);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("shutdown() should return false on timeout")
    @Timeout(5)
    void shutdownShouldReturnFalseOnTimeout() throws InterruptedException {
        // Create a task that takes a long time
        GameLoop slowGameLoop = mock(GameLoop.class);
        CountDownLatch startLatch = new CountDownLatch(1);
        doAnswer(invocation -> {
            startLatch.countDown();
            Thread.sleep(5000); // Simulate long-running task
            return null;
        }).when(slowGameLoop).advanceTick(anyLong());

        executor.setGameLoop(slowGameLoop);
        executor.advanceTick();

        // Wait for the task to start
        startLatch.await(1, TimeUnit.SECONDS);

        // Now try to shutdown with a very short timeout
        boolean result = executor.shutdown(1, TimeUnit.MILLISECONDS);

        // Should return false because task is still running
        assertThat(result).isFalse();
    }

    // ========== Thread Safety Tests ==========

    @Test
    @DisplayName("Concurrent advanceTick() calls should be thread-safe")
    @Timeout(10)
    void concurrentAdvanceTickShouldBeThreadSafe() throws InterruptedException {
        int threadCount = 10;
        int ticksPerThread = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < ticksPerThread; j++) {
                        executor.advanceTick();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);

        assertThat(executor.getCurrentTick()).isEqualTo(threadCount * ticksPerThread);
    }

    // ========== setGameLoop() Tests ==========

    @Test
    @DisplayName("setGameLoop() should allow changing gameLoop")
    @Timeout(5)
    void setGameLoopShouldAllowChanging() throws InterruptedException {
        GameLoop firstLoop = mock(GameLoop.class);
        GameLoop secondLoop = mock(GameLoop.class);

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(secondLoop).advanceTick(anyLong());

        executor.setGameLoop(firstLoop);
        executor.setGameLoop(secondLoop);
        executor.advanceTick();

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        verify(secondLoop).advanceTick(1L);
        verify(firstLoop, never()).advanceTick(anyLong());
    }

    @Test
    @DisplayName("setGameLoop(null) should be allowed")
    void setGameLoopNullShouldBeAllowed() {
        GameLoop mockLoop = mock(GameLoop.class);
        executor.setGameLoop(mockLoop);
        executor.setGameLoop(null);
        executor.advanceTick(); // Should not throw

        assertThat(executor.getCurrentTick()).isEqualTo(1);
    }
}
