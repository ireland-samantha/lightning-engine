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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Limits the number of WebSocket connections per user and per container.
 *
 * <p>Prevents resource exhaustion from:
 * <ul>
 *   <li>Single user opening too many connections</li>
 *   <li>Single container having too many concurrent clients</li>
 * </ul>
 */
@ApplicationScoped
public class WebSocketConnectionLimiter {
    private static final Logger log = LoggerFactory.getLogger(WebSocketConnectionLimiter.class);

    @ConfigProperty(name = "websocket.limits.max-connections-per-user", defaultValue = "10")
    int maxConnectionsPerUser;

    @ConfigProperty(name = "websocket.limits.max-connections-per-container", defaultValue = "100")
    int maxConnectionsPerContainer;

    // Track connections per user (by subject from JWT)
    private final ConcurrentHashMap<String, AtomicInteger> connectionsByUser = new ConcurrentHashMap<>();

    // Track connections per container
    private final ConcurrentHashMap<Long, AtomicInteger> connectionsByContainer = new ConcurrentHashMap<>();

    // Track which connections belong to which user/container for cleanup
    private final ConcurrentHashMap<String, ConnectionInfo> connectionRegistry = new ConcurrentHashMap<>();

    private record ConnectionInfo(String userId, long containerId) {}

    /**
     * Result of attempting to acquire a connection slot.
     */
    public sealed interface AcquireResult {
        record Success() implements AcquireResult {}
        record UserLimitExceeded(int currentCount, int maxAllowed) implements AcquireResult {}
        record ContainerLimitExceeded(int currentCount, int maxAllowed) implements AcquireResult {}
    }

    /**
     * Attempt to acquire a connection slot for a user on a container.
     *
     * @param connectionId unique connection identifier
     * @param userId user identifier (JWT subject)
     * @param containerId container being connected to
     * @return result indicating success or which limit was exceeded
     */
    public AcquireResult tryAcquire(String connectionId, String userId, long containerId) {
        // Check user limit
        AtomicInteger userConnections = connectionsByUser.computeIfAbsent(userId, k -> new AtomicInteger(0));
        int userCount = userConnections.get();
        if (userCount >= maxConnectionsPerUser) {
            log.warn("User '{}' exceeded max connections: {}/{}", userId, userCount, maxConnectionsPerUser);
            return new AcquireResult.UserLimitExceeded(userCount, maxConnectionsPerUser);
        }

        // Check container limit
        AtomicInteger containerConnections = connectionsByContainer.computeIfAbsent(containerId, k -> new AtomicInteger(0));
        int containerCount = containerConnections.get();
        if (containerCount >= maxConnectionsPerContainer) {
            log.warn("Container {} exceeded max connections: {}/{}", containerId, containerCount, maxConnectionsPerContainer);
            return new AcquireResult.ContainerLimitExceeded(containerCount, maxConnectionsPerContainer);
        }

        // Increment both counters atomically
        userConnections.incrementAndGet();
        containerConnections.incrementAndGet();

        // Register for cleanup
        connectionRegistry.put(connectionId, new ConnectionInfo(userId, containerId));

        log.debug("Connection acquired: user='{}' ({}/{}), container={} ({}/{})",
                userId, userConnections.get(), maxConnectionsPerUser,
                containerId, containerConnections.get(), maxConnectionsPerContainer);

        return new AcquireResult.Success();
    }

    /**
     * Release a connection slot when a connection is closed.
     *
     * @param connectionId the connection being closed
     */
    public void release(String connectionId) {
        ConnectionInfo info = connectionRegistry.remove(connectionId);
        if (info == null) {
            return; // Connection was never registered (e.g., failed auth)
        }

        AtomicInteger userConnections = connectionsByUser.get(info.userId);
        if (userConnections != null) {
            int remaining = userConnections.decrementAndGet();
            if (remaining <= 0) {
                connectionsByUser.remove(info.userId, userConnections);
            }
        }

        AtomicInteger containerConnections = connectionsByContainer.get(info.containerId);
        if (containerConnections != null) {
            int remaining = containerConnections.decrementAndGet();
            if (remaining <= 0) {
                connectionsByContainer.remove(info.containerId, containerConnections);
            }
        }

        log.debug("Connection released: user='{}', container={}", info.userId, info.containerId);
    }

    /**
     * Get current connection count for a user.
     */
    public int getUserConnectionCount(String userId) {
        AtomicInteger count = connectionsByUser.get(userId);
        return count != null ? count.get() : 0;
    }

    /**
     * Get current connection count for a container.
     */
    public int getContainerConnectionCount(long containerId) {
        AtomicInteger count = connectionsByContainer.get(containerId);
        return count != null ? count.get() : 0;
    }

    /**
     * Get total number of tracked connections.
     */
    public int getTotalConnectionCount() {
        return connectionRegistry.size();
    }

    /**
     * Get maximum connections allowed per user.
     */
    public int getMaxConnectionsPerUser() {
        return maxConnectionsPerUser;
    }

    /**
     * Get maximum connections allowed per container.
     */
    public int getMaxConnectionsPerContainer() {
        return maxConnectionsPerContainer;
    }
}
