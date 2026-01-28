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

package ca.samanthaireland.lightning.engine.quarkus.api.websocket;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Metrics for WebSocket command connections.
 *
 * <p>Provides observability into:
 * <ul>
 *   <li>Active connection count</li>
 *   <li>Total connections opened/closed</li>
 *   <li>Commands processed</li>
 *   <li>Commands rejected (rate limited, errors)</li>
 * </ul>
 *
 * <p>These metrics can be exposed via a REST endpoint or integrated with
 * external monitoring systems (Prometheus, Micrometer, etc.) as needed.
 */
@ApplicationScoped
public class WebSocketMetrics {
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final LongAdder connectionsOpened = new LongAdder();
    private final LongAdder connectionsClosed = new LongAdder();
    private final LongAdder commandsProcessed = new LongAdder();
    private final LongAdder commandsRateLimited = new LongAdder();
    private final LongAdder commandErrors = new LongAdder();
    private final LongAdder authFailures = new LongAdder();
    private final LongAdder totalCommandTimeNanos = new LongAdder();

    /**
     * Record a new connection opened.
     */
    public void connectionOpened() {
        activeConnections.incrementAndGet();
        connectionsOpened.increment();
    }

    /**
     * Record a connection closed.
     */
    public void connectionClosed() {
        activeConnections.decrementAndGet();
        connectionsClosed.increment();
    }

    /**
     * Record a command successfully processed.
     */
    public void commandProcessed() {
        commandsProcessed.increment();
    }

    /**
     * Record a command rejected due to rate limiting.
     */
    public void commandRateLimited() {
        commandsRateLimited.increment();
    }

    /**
     * Record a command processing error.
     */
    public void commandError() {
        commandErrors.increment();
    }

    /**
     * Record an authentication failure.
     */
    public void authFailure() {
        authFailures.increment();
    }

    /**
     * Execute a supplier while recording its execution time.
     *
     * @param supplier the operation to execute
     * @return the result of the operation
     */
    public <T> T recordTime(Supplier<T> supplier) {
        long start = System.nanoTime();
        try {
            return supplier.get();
        } finally {
            totalCommandTimeNanos.add(System.nanoTime() - start);
        }
    }

    /**
     * Get a snapshot of the current metrics.
     */
    public MetricsSnapshot getSnapshot() {
        return new MetricsSnapshot(
                activeConnections.get(),
                connectionsOpened.sum(),
                connectionsClosed.sum(),
                commandsProcessed.sum(),
                commandsRateLimited.sum(),
                commandErrors.sum(),
                authFailures.sum(),
                totalCommandTimeNanos.sum()
        );
    }

    /**
     * Get the current number of active connections.
     */
    public long getActiveConnectionCount() {
        return activeConnections.get();
    }

    /**
     * Immutable snapshot of metrics at a point in time.
     */
    public record MetricsSnapshot(
            long activeConnections,
            long connectionsOpened,
            long connectionsClosed,
            long commandsProcessed,
            long commandsRateLimited,
            long commandErrors,
            long authFailures,
            long totalCommandTimeNanos
    ) {
        /**
         * Get average command processing time in milliseconds.
         */
        public double averageCommandTimeMs() {
            if (commandsProcessed == 0) return 0.0;
            return (totalCommandTimeNanos / 1_000_000.0) / commandsProcessed;
        }
    }
}
