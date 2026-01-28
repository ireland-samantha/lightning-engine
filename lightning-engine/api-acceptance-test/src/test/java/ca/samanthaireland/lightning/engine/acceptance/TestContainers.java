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

package ca.samanthaireland.lightning.engine.acceptance;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Factory for creating pre-configured test containers.
 *
 * <p>Centralizes container configuration to ensure consistent security settings
 * and environment variables across all integration tests.
 */
public final class TestContainers {

    public static final String BACKEND_IMAGE = "samanthacireland/lightning-engine:latest";
    public static final int BACKEND_PORT = 8080;
    public static final String TEST_ADMIN_PASSWORD = "admin";
    public static final String TEST_JWT_SECRET = "test-jwt-secret-for-integration-tests";
    public static final Duration DEFAULT_STARTUP_TIMEOUT = Duration.ofMinutes(2);

    private TestContainers() {
        // Utility class
    }

    /**
     * Creates a standalone backend container with default configuration.
     *
     * <p>Use this for tests that don't need MongoDB persistence.
     *
     * @return configured backend container (not started)
     */
    public static GenericContainer<?> backendContainer() {
        return new GenericContainer<>(DockerImageName.parse(BACKEND_IMAGE))
                .withExposedPorts(BACKEND_PORT)
                .withEnv("ADMIN_INITIAL_PASSWORD", TEST_ADMIN_PASSWORD)
                .withEnv("AUTH_JWT_SECRET", TEST_JWT_SECRET)
                .withEnv("WEBSOCKET_RATELIMIT_MAX_COMMANDS_PER_SECOND", "50000") // High limit for performance tests
                .waitingFor(Wait.forLogMessage(".*started in.*\\n", 1)
                        .withStartupTimeout(DEFAULT_STARTUP_TIMEOUT));
    }

    /**
     * Creates a backend container configured for use with MongoDB.
     *
     * <p>The container will be configured to connect to MongoDB via the provided network.
     *
     * @param network the Docker network for container communication
     * @param mongoNetworkAlias the network alias of the MongoDB container
     * @return configured backend container (not started)
     */
    public static GenericContainer<?> backendContainerWithMongo(Network network, String mongoNetworkAlias) {
        return new GenericContainer<>(DockerImageName.parse(BACKEND_IMAGE))
                .withExposedPorts(BACKEND_PORT)
                .withNetwork(network)
                .withEnv("QUARKUS_MONGODB_CONNECTION_STRING", "mongodb://" + mongoNetworkAlias + ":27017")
                .withEnv("SNAPSHOT_PERSISTENCE_ENABLED", "true")
                .withEnv("SNAPSHOT_PERSISTENCE_DATABASE", "lightningfirefly")
                .withEnv("SNAPSHOT_PERSISTENCE_COLLECTION", "snapshots")
                .withEnv("SNAPSHOT_PERSISTENCE_TICK_INTERVAL", "1")
                .withEnv("ADMIN_INITIAL_PASSWORD", TEST_ADMIN_PASSWORD)
                .withEnv("AUTH_JWT_SECRET", TEST_JWT_SECRET)
                .waitingFor(Wait.forLogMessage(".*started in.*\\n", 1)
                        .withStartupTimeout(DEFAULT_STARTUP_TIMEOUT));
    }

    /**
     * Creates a MongoDB container configured for use with the backend.
     *
     * @param network the Docker network for container communication
     * @param networkAlias the network alias for this container
     * @return configured MongoDB container (not started)
     */
    public static MongoDBContainer mongoContainer(Network network, String networkAlias) {
        return new MongoDBContainer(DockerImageName.parse("mongo:7"))
                .withNetwork(network)
                .withNetworkAliases(networkAlias);
    }

    /**
     * Constructs the base URL for accessing the backend from the host.
     *
     * @param container the running backend container
     * @return the base URL (e.g., "http://localhost:32768")
     */
    public static String getBaseUrl(GenericContainer<?> container) {
        return String.format("http://%s:%d", container.getHost(), container.getMappedPort(BACKEND_PORT));
    }
}
