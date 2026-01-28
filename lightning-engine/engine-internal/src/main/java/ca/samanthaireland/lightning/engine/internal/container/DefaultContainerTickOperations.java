/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.internal.container;

import ca.samanthaireland.lightning.engine.core.container.ContainerTickOperations;

import java.util.concurrent.TimeoutException;

/**
 * Default implementation of ContainerTickOperations.
 * Works directly with InMemoryExecutionContainer to avoid interface method dependencies.
 */
public final class DefaultContainerTickOperations implements ContainerTickOperations {

    private final InMemoryExecutionContainer container;

    public DefaultContainerTickOperations(InMemoryExecutionContainer container) {
        this.container = container;
    }

    @Override
    public ContainerTickOperations advance() {
        container.advanceTickInternal();
        return this;
    }

    @Override
    public ContainerTickOperations advanceBy(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("count must be at least 1");
        }
        for (int i = 0; i < count; i++) {
            container.advanceTickInternal();
        }
        return this;
    }

    @Override
    public ContainerTickOperations play(long intervalMs) {
        if (intervalMs < 1) {
            throw new IllegalArgumentException("intervalMs must be at least 1");
        }
        container.startAutoAdvanceInternal(intervalMs);
        return this;
    }

    @Override
    public ContainerTickOperations stop() {
        container.stopAutoAdvanceInternal();
        return this;
    }

    @Override
    public ContainerTickOperations waitForTick(long targetTick) throws InterruptedException {
        if (!container.isAutoAdvancingInternal()) {
            throw new IllegalStateException("Cannot wait for tick when auto-advance is not active");
        }
        while (container.getCurrentTickInternal() < targetTick) {
            Thread.sleep(1);
        }
        return this;
    }

    @Override
    public ContainerTickOperations waitForTick(long targetTick, long timeoutMs)
            throws InterruptedException, TimeoutException {
        long startTime = System.currentTimeMillis();
        while (container.getCurrentTickInternal() < targetTick) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw new TimeoutException(
                        "Timeout waiting for tick " + targetTick + ", current: " + container.getCurrentTickInternal());
            }
            Thread.sleep(1);
        }
        return this;
    }

    @Override
    public long current() {
        return container.getCurrentTickInternal();
    }

    @Override
    public boolean isPlaying() {
        return container.isAutoAdvancingInternal();
    }

    @Override
    public long interval() {
        return container.getAutoAdvanceIntervalInternal();
    }
}
