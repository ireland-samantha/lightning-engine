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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rate limiter for WebSocket command connections.
 *
 * <p>Uses a sliding window algorithm to limit the number of commands
 * per connection within a time window. Includes automatic cleanup of
 * expired buckets to prevent memory leaks.
 */
@ApplicationScoped
public class WebSocketRateLimiter {
    private static final Logger log = LoggerFactory.getLogger(WebSocketRateLimiter.class);

    @ConfigProperty(name = "websocket.ratelimit.max-commands-per-second", defaultValue = "100")
    int maxCommandsPerSecond;

    @ConfigProperty(name = "websocket.ratelimit.window-seconds", defaultValue = "1")
    int windowSeconds;

    @ConfigProperty(name = "websocket.ratelimit.cleanup-interval-seconds", defaultValue = "60")
    int cleanupIntervalSeconds;

    private final ConcurrentHashMap<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();
    private ScheduledExecutorService cleanupExecutor;

    private record RateLimitBucket(AtomicInteger count, Instant windowStart, Duration windowDuration) {
        boolean isExpired() {
            return Instant.now().isAfter(windowStart.plus(windowDuration));
        }

        boolean tryAcquire(int maxCommands) {
            if (isExpired()) {
                return false; // Bucket expired, will be replaced
            }
            return count.incrementAndGet() <= maxCommands;
        }
    }

    @PostConstruct
    void init() {
        // Schedule periodic cleanup of expired buckets to prevent memory leaks
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ws-ratelimit-cleanup");
            t.setDaemon(true);
            return t;
        });

        cleanupExecutor.scheduleAtFixedRate(
                this::cleanupExpiredBuckets,
                cleanupIntervalSeconds,
                cleanupIntervalSeconds,
                TimeUnit.SECONDS
        );

        log.info("WebSocket rate limiter initialized: max {} commands per {} second(s), cleanup every {} seconds",
                maxCommandsPerSecond, windowSeconds, cleanupIntervalSeconds);
    }

    @PreDestroy
    void shutdown() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Check if a command is allowed under rate limiting for the given connection.
     *
     * @param connectionId the WebSocket connection ID
     * @return true if the command is allowed, false if rate limited
     */
    public boolean tryAcquire(String connectionId) {
        Instant now = Instant.now();
        Duration windowDuration = Duration.ofSeconds(windowSeconds);

        RateLimitBucket bucket = buckets.compute(connectionId, (key, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new RateLimitBucket(new AtomicInteger(0), now, windowDuration);
            }
            return existing;
        });

        return bucket.tryAcquire(maxCommandsPerSecond);
    }

    /**
     * Remove the rate limit bucket for a disconnected connection.
     *
     * @param connectionId the WebSocket connection ID
     */
    public void removeConnection(String connectionId) {
        buckets.remove(connectionId);
    }

    /**
     * Get the maximum commands per second allowed.
     */
    public int getMaxCommandsPerSecond() {
        return maxCommandsPerSecond;
    }

    /**
     * Clean up expired buckets to prevent memory leaks from disconnected clients.
     */
    private void cleanupExpiredBuckets() {
        int removed = 0;
        for (var entry : buckets.entrySet()) {
            if (entry.getValue().isExpired()) {
                buckets.remove(entry.getKey(), entry.getValue());
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Cleaned up {} expired rate limit buckets", removed);
        }
    }

    /**
     * Get the current number of tracked connections (for monitoring).
     */
    public int getTrackedConnectionCount() {
        return buckets.size();
    }
}
