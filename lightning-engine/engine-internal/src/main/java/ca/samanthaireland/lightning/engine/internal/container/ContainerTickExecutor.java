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
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Handles tick execution and auto-advance for a container.
 *
 * <p>Extracted from InMemoryExecutionContainer to follow SRP.
 * Manages the scheduled executor for tick advancement and auto-play functionality.
 */
@Slf4j
public class ContainerTickExecutor {

    private final long containerId;
    private final String containerName;
    private final Supplier<ContainerStatus> statusSupplier;
    private final AtomicLong currentTick;
    private final AtomicLong autoAdvanceInterval;

    private final ScheduledExecutorService tickExecutor;
    private volatile ScheduledFuture<?> autoAdvanceTask;
    private volatile GameLoop gameLoop;

    /**
     * Creates a new tick executor for the specified container.
     *
     * @param containerId the container ID
     * @param containerName the container name (for logging)
     * @param statusSupplier supplier for current container status
     */
    public ContainerTickExecutor(long containerId, String containerName, Supplier<ContainerStatus> statusSupplier) {
        this.containerId = containerId;
        this.containerName = containerName;
        this.statusSupplier = statusSupplier;
        this.currentTick = new AtomicLong(0);
        this.autoAdvanceInterval = new AtomicLong(0);

        this.tickExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "container-" + containerId + "-tick");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Sets the game loop for this executor.
     *
     * @param gameLoop the game loop to use for tick execution
     */
    public void setGameLoop(GameLoop gameLoop) {
        this.gameLoop = gameLoop;
    }

    /**
     * Gets the current tick value.
     *
     * @return the current tick
     */
    public long getCurrentTick() {
        return currentTick.get();
    }

    /**
     * Advances the container by one tick.
     *
     * @return the new tick value
     */
    public long advanceTick() {
        long tick = currentTick.incrementAndGet();
        tickExecutor.execute(() -> {
            if (gameLoop != null) {
                gameLoop.advanceTick(tick);
            }
        });
        return tick;
    }

    /**
     * Starts auto-advancing at the specified interval.
     *
     * @param intervalMs the interval in milliseconds between ticks
     */
    public void startAutoAdvance(long intervalMs) {
        if (intervalMs <= 0) {
            throw new IllegalArgumentException("intervalMs must be positive");
        }

        stopAutoAdvance(); // Stop any existing auto-advance

        autoAdvanceInterval.set(intervalMs);
        autoAdvanceTask = tickExecutor.scheduleAtFixedRate(
                this::advanceTickSafe,
                0,
                intervalMs,
                TimeUnit.MILLISECONDS
        );

        log.info("Container {} '{}' auto-advance started at {} ms interval", containerId, containerName, intervalMs);
    }

    private void advanceTickSafe() {
        try {
            if (statusSupplier.get() == ContainerStatus.RUNNING) {
                advanceTick();
            }
        } catch (Exception e) {
            log.error("Error during auto-advance tick in container {}: {}", containerId, e.getMessage(), e);
        }
    }

    /**
     * Stops auto-advancing.
     */
    public void stopAutoAdvance() {
        if (autoAdvanceTask != null) {
            autoAdvanceTask.cancel(false);
            autoAdvanceTask = null;
            autoAdvanceInterval.set(0);
            log.debug("Container {} auto-advance stopped", containerId);
        }
    }

    /**
     * Checks if auto-advance is currently active.
     *
     * @return true if auto-advancing
     */
    public boolean isAutoAdvancing() {
        return autoAdvanceTask != null && !autoAdvanceTask.isCancelled();
    }

    /**
     * Gets the current auto-advance interval.
     *
     * @return the interval in milliseconds, or 0 if not auto-advancing
     */
    public long getAutoAdvanceInterval() {
        return autoAdvanceInterval.get();
    }

    /**
     * Shuts down the tick executor.
     *
     * @param timeout the maximum time to wait for termination
     * @param unit the time unit of the timeout
     * @return true if executor terminated, false if timeout elapsed
     */
    public boolean shutdown(long timeout, TimeUnit unit) {
        stopAutoAdvance();
        tickExecutor.shutdown();
        try {
            if (!tickExecutor.awaitTermination(timeout, unit)) {
                tickExecutor.shutdownNow();
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            tickExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
