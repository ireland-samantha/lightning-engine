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

package ca.samanthaireland.lightning.engine.acceptance.fixture;

import ca.samanthaireland.lightning.engine.api.resource.adapter.CommandWebSocketClient;
import ca.samanthaireland.lightning.engine.api.resource.adapter.ContainerCommands;
import ca.samanthaireland.lightning.engine.api.resource.adapter.EngineClient;
import ca.samanthaireland.lightning.engine.api.resource.adapter.EngineClient.ContainerClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluent test fixture for Lightning Engine container operations.
 *
 * <p>Named TestEngineContainer to distinguish from Testcontainers library.
 *
 * <p>Usage:
 * <pre>{@code
 * TestEngineContainer container = TestEngineContainer.create(client)
 *     .withName("test-container")
 *     .withModules("EntityModule", "RigidBodyModule")
 *     .start();
 *
 * TestMatch match = container.createMatch()
 *     .withModules("EntityModule", "RigidBodyModule")
 *     .build();
 * }</pre>
 */
public class TestEngineContainer {

    private final EngineClient client;
    private final long containerId;
    private final String name;
    private final ContainerClient containerClient;
    private final List<TestMatch> matches = new ArrayList<>();
    private final List<Long> playerIds = new ArrayList<>();

    private TestEngineContainer(EngineClient client, long containerId, String name) {
        this.client = client;
        this.containerId = containerId;
        this.name = name;
        this.containerClient = client.container(containerId);
    }

    /**
     * Start creating a new test container.
     */
    public static Builder create(EngineClient client) {
        return new Builder(client);
    }

    /**
     * Get the container ID.
     */
    public long id() {
        return containerId;
    }

    /**
     * Get the container name.
     */
    public String name() {
        return name;
    }

    /**
     * Get the underlying container client.
     */
    public ContainerClient client() {
        return containerClient;
    }

    /**
     * Get the underlying EngineClient.
     */
    public EngineClient engineClient() {
        return client;
    }

    /**
     * Create a new match in this container.
     */
    public TestMatch.Builder createMatch() {
        return new TestMatch.Builder(this);
    }

    /**
     * Add a match to this container's tracked matches.
     */
    void addMatch(TestMatch match) {
        matches.add(match);
    }

    /**
     * Get all matches created in this container.
     */
    public List<TestMatch> matches() {
        return List.copyOf(matches);
    }

    /**
     * Create a player in this container.
     */
    public long createPlayer() {
        long playerId = containerClient.createPlayer();
        playerIds.add(playerId);
        return playerId;
    }

    /**
     * Get all player IDs created in this container.
     */
    public List<Long> playerIds() {
        return List.copyOf(playerIds);
    }

    /**
     * Advance the simulation by one tick.
     */
    public TestEngineContainer tick() {
        containerClient.tick();
        return this;
    }

    /**
     * Advance the simulation by multiple ticks.
     */
    public TestEngineContainer tick(int count) {
        for (int i = 0; i < count; i++) {
            containerClient.tick();
        }
        return this;
    }

    /**
     * Get a snapshot for a match.
     */
    public EngineClient.Snapshot snapshot(long matchId) {
        return containerClient.getSnapshot(matchId)
                .map(s -> client.parseSnapshot(s.data()))
                .orElseThrow(() -> new IllegalStateException("No snapshot for match " + matchId));
    }

    /**
     * Get a snapshot for a match.
     */
    public EngineClient.Snapshot snapshot(TestMatch match) {
        return snapshot(match.id());
    }

    /**
     * Clean up this container (stop and delete).
     */
    public void cleanup() {
        try {
            client.containers().stopContainer(containerId);
        } catch (Exception ignored) {}
        try {
            client.containers().deleteContainer(containerId);
        } catch (Exception ignored) {}
    }

    /**
     * Get a CommandWebSocketClient connected to this container.
     *
     * <p>The client should be closed when done to release resources.
     *
     * @param token JWT token for authentication (must have admin or command_manager role)
     * @return a connected CommandWebSocketClient
     * @throws IOException if connection fails
     */
    public CommandWebSocketClient commandWebSocket(String token) throws IOException {
        return CommandWebSocketClient.connect(client.baseUrl(), containerId, token);
    }

    /**
     * Get WebSocket-based match commands for a specific match.
     *
     * <p>Commands sent through this interface use WebSocket instead of HTTP.
     *
     * @param matchId the match ID
     * @param wsClient the WebSocket client (must be connected)
     * @return match commands interface using WebSocket
     */
    public ContainerCommands.MatchCommands forMatchWs(long matchId, CommandWebSocketClient wsClient) {
        return new ContainerCommands.WebSocketMatchCommands(wsClient, matchId);
    }

    /**
     * Get WebSocket-based match commands for a TestMatch.
     *
     * @param match the test match
     * @param wsClient the WebSocket client (must be connected)
     * @return match commands interface using WebSocket
     */
    public ContainerCommands.MatchCommands forMatchWs(TestMatch match, CommandWebSocketClient wsClient) {
        return forMatchWs(match.id(), wsClient);
    }

    /**
     * Wait for the container to reach RUNNING status.
     *
     * @param maxAttempts maximum number of polling attempts
     * @param pollIntervalMs interval between polls in milliseconds
     * @return this container for chaining
     * @throws IllegalStateException if container doesn't reach RUNNING status
     */
    public TestEngineContainer waitForRunning(int maxAttempts, long pollIntervalMs) {
        for (int i = 0; i < maxAttempts; i++) {
            try {
                var containerOpt = client.containers().getContainer(containerId);
                if (containerOpt.isPresent() && "RUNNING".equals(containerOpt.get().status())) {
                    return this;
                }
            } catch (Exception ignored) {
                // Container not ready yet
            }

            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Wait for container interrupted", e);
            }
        }
        throw new IllegalStateException("Container " + containerId + " did not reach RUNNING status after " + maxAttempts + " attempts");
    }

    /**
     * Wait for the container to reach RUNNING status with default settings.
     *
     * @return this container for chaining
     */
    public TestEngineContainer waitForRunning() {
        return waitForRunning(20, 100);
    }

    /**
     * Builder for creating test containers.
     */
    public static class Builder {
        private static final int DEFAULT_MAX_WAIT_ATTEMPTS = 20;
        private static final long DEFAULT_WAIT_INTERVAL_MS = 100;

        private final EngineClient client;
        private String name;
        private final List<String> modules = new ArrayList<>();

        Builder(EngineClient client) {
            this.client = client;
        }

        /**
         * Set the container name.
         */
        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Add modules to install in the container.
         */
        public Builder withModules(String... moduleNames) {
            modules.addAll(List.of(moduleNames));
            return this;
        }

        /**
         * Build and start the container, waiting for it to reach RUNNING status.
         */
        public TestEngineContainer start() {
            String containerName = name != null ? name : "test-" + System.currentTimeMillis();

            var builder = client.containers().create();
            builder.name(containerName);
            for (String module : modules) {
                builder.withModules(module);
            }

            try {
                var response = builder.execute();
                TestEngineContainer container = new TestEngineContainer(client, response.id(), containerName);
                // Start auto-advance to ensure container is running and processing commands
                container.containerClient.play(60);
                // Wait for container to reach RUNNING status before returning
                container.waitForRunning(DEFAULT_MAX_WAIT_ATTEMPTS, DEFAULT_WAIT_INTERVAL_MS);
                return container;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create container: " + containerName, e);
            }
        }
    }
}
