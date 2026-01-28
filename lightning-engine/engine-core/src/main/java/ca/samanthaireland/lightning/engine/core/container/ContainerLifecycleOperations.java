/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.core.container;

/**
 * Fluent API for container lifecycle operations.
 *
 * <p>Provides chainable methods for starting, stopping, pausing, and resuming containers.
 *
 * <p>Example usage:
 * <pre>{@code
 * container.lifecycle()
 *     .start()
 *     .thenPlay(16);  // Start auto-advancing at 60 FPS
 *
 * container.lifecycle()
 *     .pause()
 *     .resume();
 *
 * ContainerStatus status = container.lifecycle().status();
 * }</pre>
 */
public interface ContainerLifecycleOperations {

    /**
     * Starts the container. Initializes the classloader, loads modules,
     * and prepares for tick execution.
     *
     * @return this for fluent chaining
     * @throws IllegalStateException if the container is not in CREATED state
     */
    ContainerLifecycleOperations start();

    /**
     * Stops the container. Halts tick execution and releases resources.
     * The container cannot be restarted after stopping.
     *
     * @return this for fluent chaining
     * @throws IllegalStateException if the container is already stopped
     */
    ContainerLifecycleOperations stop();

    /**
     * Pauses tick execution. The container remains in memory and can be resumed.
     *
     * @return this for fluent chaining
     * @throws IllegalStateException if the container is not running
     */
    ContainerLifecycleOperations pause();

    /**
     * Resumes tick execution after a pause.
     *
     * @return this for fluent chaining
     * @throws IllegalStateException if the container is not paused
     */
    ContainerLifecycleOperations resume();

    /**
     * Starts the container and immediately begins auto-advancing ticks.
     * Convenience method combining start() + thenPlay().
     *
     * @param intervalMs the interval between ticks in milliseconds
     * @return this for fluent chaining
     * @throws IllegalStateException if the container is not in CREATED state
     */
    ContainerLifecycleOperations startAndPlay(long intervalMs);

    /**
     * After starting, begins auto-advancing ticks at the specified interval.
     *
     * @param intervalMs the interval between ticks in milliseconds
     * @return this for fluent chaining
     * @throws IllegalStateException if the container is not running
     */
    ContainerLifecycleOperations thenPlay(long intervalMs);

    /**
     * Stops auto-advancing but keeps the container running.
     *
     * @return this for fluent chaining
     */
    ContainerLifecycleOperations stopAutoAdvance();

    /**
     * Returns the current lifecycle status of the container.
     *
     * @return the current status
     */
    ContainerStatus status();

    /**
     * Checks if the container is in the running state.
     *
     * @return true if running
     */
    boolean isRunning();

    /**
     * Checks if the container is paused.
     *
     * @return true if paused
     */
    boolean isPaused();

    /**
     * Checks if the container is stopped.
     *
     * @return true if stopped
     */
    boolean isStopped();
}
