/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.internal.container;

import ca.samanthaireland.lightning.engine.core.container.ContainerLifecycleOperations;
import ca.samanthaireland.lightning.engine.core.container.ContainerStatus;

/**
 * Default implementation of ContainerLifecycleOperations.
 * Works directly with InMemoryExecutionContainer to avoid interface method dependencies.
 */
public final class DefaultContainerLifecycleOperations implements ContainerLifecycleOperations {

    private final InMemoryExecutionContainer container;

    public DefaultContainerLifecycleOperations(InMemoryExecutionContainer container) {
        this.container = container;
    }

    @Override
    public ContainerLifecycleOperations start() {
        container.startInternal();
        return this;
    }

    @Override
    public ContainerLifecycleOperations stop() {
        container.stopInternal();
        return this;
    }

    @Override
    public ContainerLifecycleOperations pause() {
        container.pauseInternal();
        return this;
    }

    @Override
    public ContainerLifecycleOperations resume() {
        container.resumeInternal();
        return this;
    }

    @Override
    public ContainerLifecycleOperations startAndPlay(long intervalMs) {
        container.startInternal();
        container.startAutoAdvanceInternal(intervalMs);
        return this;
    }

    @Override
    public ContainerLifecycleOperations thenPlay(long intervalMs) {
        container.startAutoAdvanceInternal(intervalMs);
        return this;
    }

    @Override
    public ContainerLifecycleOperations stopAutoAdvance() {
        container.stopAutoAdvanceInternal();
        return this;
    }

    @Override
    public ContainerStatus status() {
        return container.getStatus();
    }

    @Override
    public boolean isRunning() {
        return container.getStatus() == ContainerStatus.RUNNING;
    }

    @Override
    public boolean isPaused() {
        return container.getStatus() == ContainerStatus.PAUSED;
    }

    @Override
    public boolean isStopped() {
        return container.getStatus() == ContainerStatus.STOPPED;
    }
}
