/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.core.container;

/**
 * Fluent API for container tick operations.
 *
 * <p>Provides chainable methods for advancing simulation ticks and controlling auto-advance.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Manual tick control
 * long newTick = container.ticks()
 *     .advance()
 *     .advance()
 *     .advance()
 *     .current();
 *
 * // Advance multiple ticks
 * container.ticks().advanceBy(100);
 *
 * // Auto-advance control
 * container.ticks()
 *     .play(16)      // Start at 60 FPS
 *     .waitForTick(1000)  // Wait until tick 1000
 *     .stop();
 * }</pre>
 */
public interface ContainerTickOperations {

    /**
     * Advances the simulation by one tick.
     *
     * @return this for fluent chaining
     * @throws IllegalStateException if the container is not running
     */
    ContainerTickOperations advance();

    /**
     * Advances the simulation by the specified number of ticks.
     *
     * @param count the number of ticks to advance
     * @return this for fluent chaining
     * @throws IllegalStateException if the container is not running
     * @throws IllegalArgumentException if count is less than 1
     */
    ContainerTickOperations advanceBy(int count);

    /**
     * Starts auto-advancing ticks at the specified interval.
     *
     * @param intervalMs the interval between ticks in milliseconds
     * @return this for fluent chaining
     * @throws IllegalStateException if the container is not running
     * @throws IllegalArgumentException if intervalMs is less than 1
     */
    ContainerTickOperations play(long intervalMs);

    /**
     * Stops auto-advancing ticks.
     *
     * @return this for fluent chaining
     */
    ContainerTickOperations stop();

    /**
     * Blocks until the specified tick is reached.
     * Only works when auto-advancing is active.
     *
     * @param targetTick the tick to wait for
     * @return this for fluent chaining
     * @throws InterruptedException if the wait is interrupted
     * @throws IllegalStateException if auto-advance is not active
     */
    ContainerTickOperations waitForTick(long targetTick) throws InterruptedException;

    /**
     * Blocks until the specified tick is reached, with a timeout.
     *
     * @param targetTick the tick to wait for
     * @param timeoutMs maximum time to wait in milliseconds
     * @return this for fluent chaining
     * @throws InterruptedException if the wait is interrupted
     * @throws java.util.concurrent.TimeoutException if the timeout is exceeded
     */
    ContainerTickOperations waitForTick(long targetTick, long timeoutMs) throws InterruptedException, java.util.concurrent.TimeoutException;

    /**
     * Returns the current tick number.
     *
     * @return the current tick
     */
    long current();

    /**
     * Checks if auto-advance is currently active.
     *
     * @return true if auto-advancing
     */
    boolean isPlaying();

    /**
     * Returns the current auto-advance interval in milliseconds.
     *
     * @return the interval, or 0 if not auto-advancing
     */
    long interval();
}
