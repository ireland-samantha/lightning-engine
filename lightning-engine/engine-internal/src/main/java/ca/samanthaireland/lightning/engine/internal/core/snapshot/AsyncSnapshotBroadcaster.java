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

package ca.samanthaireland.lightning.engine.internal.core.snapshot;

import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;
import ca.samanthaireland.lightning.engine.core.snapshot.SnapshotDelta;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

/**
 * Asynchronous broadcaster for snapshot data to WebSocket clients.
 *
 * <p>This class handles non-blocking broadcast of snapshot data to connected
 * clients, ensuring the game loop is never blocked by slow network operations.
 *
 * <p><b>Features:</b>
 * <ul>
 *   <li>Thread pool for parallel broadcast to multiple matches</li>
 *   <li>Named threads for debugging and monitoring</li>
 *   <li>Watchdog for detecting stuck broadcast threads</li>
 *   <li>Graceful shutdown with timeout</li>
 *   <li>Queue saturation monitoring</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * AsyncSnapshotBroadcaster broadcaster = new AsyncSnapshotBroadcaster();
 *
 * // Broadcast snapshot asynchronously
 * broadcaster.broadcastAsync(matchId, snapshot, (id, s) -> {
 *     webSocket.sendToClients(id, s);
 * }).thenRun(() -> log.debug("Broadcast complete for match {}", matchId));
 *
 * // Broadcast delta asynchronously
 * broadcaster.broadcastDeltaAsync(matchId, delta, (id, d) -> {
 *     webSocket.sendDeltaToClients(id, d);
 * });
 *
 * // Shutdown when container stops
 * broadcaster.shutdown();
 * }</pre>
 */
@Slf4j
public class AsyncSnapshotBroadcaster {

    /**
     * Default thread pool size (based on available processors).
     */
    public static final int DEFAULT_POOL_SIZE = Math.max(2, Runtime.getRuntime().availableProcessors());

    /**
     * Default queue size before saturation warnings.
     */
    public static final int DEFAULT_QUEUE_WARNING_THRESHOLD = 100;

    /**
     * Default watchdog check interval in seconds.
     */
    public static final int DEFAULT_WATCHDOG_INTERVAL_SECONDS = 30;

    private final ExecutorService broadcastExecutor;
    private final ScheduledExecutorService watchdogExecutor;
    private final int queueWarningThreshold;

    // Metrics
    private final AtomicLong totalBroadcasts = new AtomicLong();
    private final AtomicLong successfulBroadcasts = new AtomicLong();
    private final AtomicLong failedBroadcasts = new AtomicLong();
    private final AtomicLong droppedBroadcasts = new AtomicLong();

    private volatile boolean shutdown = false;

    /**
     * Creates an AsyncSnapshotBroadcaster with default configuration.
     */
    public AsyncSnapshotBroadcaster() {
        this(DEFAULT_POOL_SIZE, DEFAULT_QUEUE_WARNING_THRESHOLD, DEFAULT_WATCHDOG_INTERVAL_SECONDS);
    }

    /**
     * Creates an AsyncSnapshotBroadcaster with custom configuration.
     *
     * @param poolSize               the thread pool size
     * @param queueWarningThreshold  queue size before warnings
     * @param watchdogIntervalSeconds watchdog check interval in seconds
     */
    public AsyncSnapshotBroadcaster(int poolSize, int queueWarningThreshold, int watchdogIntervalSeconds) {
        this.queueWarningThreshold = queueWarningThreshold;

        // Create thread pool with named threads for debugging
        AtomicInteger threadCounter = new AtomicInteger();
        this.broadcastExecutor = new ThreadPoolExecutor(
                poolSize,
                poolSize,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = new Thread(r, "snapshot-broadcast-" + threadCounter.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
        );

        // Create watchdog executor
        this.watchdogExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "snapshot-broadcast-watchdog");
            t.setDaemon(true);
            return t;
        });

        // Schedule watchdog checks
        watchdogExecutor.scheduleAtFixedRate(
                this::checkThreadHealth,
                watchdogIntervalSeconds,
                watchdogIntervalSeconds,
                TimeUnit.SECONDS
        );

        log.info("AsyncSnapshotBroadcaster initialized with {} threads", poolSize);
    }

    /**
     * Broadcasts a snapshot asynchronously.
     *
     * @param matchId   the match ID
     * @param snapshot  the snapshot to broadcast
     * @param broadcaster the broadcast function that sends to clients
     * @return a CompletableFuture that completes when broadcast is done
     */
    public CompletableFuture<Void> broadcastAsync(
            long matchId,
            Snapshot snapshot,
            BiConsumer<Long, Snapshot> broadcaster) {

        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(broadcaster, "broadcaster must not be null");

        if (shutdown) {
            droppedBroadcasts.incrementAndGet();
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Broadcaster is shutdown"));
        }

        totalBroadcasts.incrementAndGet();

        return CompletableFuture.runAsync(() -> {
            try {
                broadcaster.accept(matchId, snapshot);
                successfulBroadcasts.incrementAndGet();
            } catch (Exception e) {
                failedBroadcasts.incrementAndGet();
                log.error("Failed to broadcast snapshot for match {}: {}", matchId, e.getMessage(), e);
                throw e;
            }
        }, broadcastExecutor);
    }

    /**
     * Broadcasts a snapshot delta asynchronously.
     *
     * @param matchId   the match ID
     * @param delta     the delta to broadcast
     * @param broadcaster the broadcast function that sends to clients
     * @return a CompletableFuture that completes when broadcast is done
     */
    public CompletableFuture<Void> broadcastDeltaAsync(
            long matchId,
            SnapshotDelta delta,
            BiConsumer<Long, SnapshotDelta> broadcaster) {

        Objects.requireNonNull(delta, "delta must not be null");
        Objects.requireNonNull(broadcaster, "broadcaster must not be null");

        if (shutdown) {
            droppedBroadcasts.incrementAndGet();
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Broadcaster is shutdown"));
        }

        totalBroadcasts.incrementAndGet();

        return CompletableFuture.runAsync(() -> {
            try {
                broadcaster.accept(matchId, delta);
                successfulBroadcasts.incrementAndGet();
            } catch (Exception e) {
                failedBroadcasts.incrementAndGet();
                log.error("Failed to broadcast delta for match {}: {}", matchId, e.getMessage(), e);
                throw e;
            }
        }, broadcastExecutor);
    }

    /**
     * Checks thread pool health and logs warnings if saturated.
     */
    private void checkThreadHealth() {
        if (shutdown) {
            return;
        }

        if (broadcastExecutor instanceof ThreadPoolExecutor tpe) {
            int activeCount = tpe.getActiveCount();
            int poolSize = tpe.getPoolSize();
            int queueSize = tpe.getQueue().size();

            if (activeCount == poolSize && queueSize > queueWarningThreshold) {
                log.warn("Broadcast thread pool saturated! active={}/{}, queue={}",
                        activeCount, poolSize, queueSize);
            } else if (log.isDebugEnabled()) {
                log.debug("Broadcast thread pool health: active={}/{}, queue={}",
                        activeCount, poolSize, queueSize);
            }
        }
    }

    /**
     * Shuts down the broadcaster gracefully.
     *
     * <p>Waits up to 5 seconds for pending broadcasts to complete.
     */
    public void shutdown() {
        shutdown(5, TimeUnit.SECONDS);
    }

    /**
     * Shuts down the broadcaster with a custom timeout.
     *
     * @param timeout the timeout value
     * @param unit    the timeout unit
     */
    public void shutdown(long timeout, TimeUnit unit) {
        if (shutdown) {
            return;
        }

        shutdown = true;
        log.info("Shutting down AsyncSnapshotBroadcaster...");

        watchdogExecutor.shutdown();
        broadcastExecutor.shutdown();

        try {
            if (!broadcastExecutor.awaitTermination(timeout, unit)) {
                log.warn("Broadcast executor did not terminate in time, forcing shutdown");
                broadcastExecutor.shutdownNow();
            }
            if (!watchdogExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                watchdogExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            broadcastExecutor.shutdownNow();
            watchdogExecutor.shutdownNow();
        }

        log.info("AsyncSnapshotBroadcaster shutdown complete. Stats: total={}, success={}, failed={}, dropped={}",
                totalBroadcasts.get(), successfulBroadcasts.get(),
                failedBroadcasts.get(), droppedBroadcasts.get());
    }

    /**
     * Returns true if the broadcaster is shutdown.
     *
     * @return true if shutdown
     */
    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * Returns the total number of broadcast attempts.
     *
     * @return total broadcasts
     */
    public long getTotalBroadcasts() {
        return totalBroadcasts.get();
    }

    /**
     * Returns the number of successful broadcasts.
     *
     * @return successful broadcasts
     */
    public long getSuccessfulBroadcasts() {
        return successfulBroadcasts.get();
    }

    /**
     * Returns the number of failed broadcasts.
     *
     * @return failed broadcasts
     */
    public long getFailedBroadcasts() {
        return failedBroadcasts.get();
    }

    /**
     * Returns the number of dropped broadcasts (after shutdown).
     *
     * @return dropped broadcasts
     */
    public long getDroppedBroadcasts() {
        return droppedBroadcasts.get();
    }

    /**
     * Returns the current queue size.
     *
     * @return queue size, or 0 if not a ThreadPoolExecutor
     */
    public int getQueueSize() {
        if (broadcastExecutor instanceof ThreadPoolExecutor tpe) {
            return tpe.getQueue().size();
        }
        return 0;
    }

    /**
     * Returns the current active thread count.
     *
     * @return active thread count, or 0 if not a ThreadPoolExecutor
     */
    public int getActiveThreadCount() {
        if (broadcastExecutor instanceof ThreadPoolExecutor tpe) {
            return tpe.getActiveCount();
        }
        return 0;
    }
}
