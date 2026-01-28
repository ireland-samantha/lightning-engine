/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.dashboard.acceptance;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Factory for creating pre-configured test containers for Playwright tests.
 *
 * <p>Centralizes container configuration to ensure consistent security settings
 * and environment variables across all Playwright integration tests.
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
     * Creates a backend container configured for Playwright tests.
     *
     * <p>Uses a wait strategy that matches the full startup log message
     * including the "Listening on" portion.
     *
     * @return configured backend container (not started)
     */
    public static GenericContainer<?> backendContainer() {
        return new GenericContainer<>(DockerImageName.parse(BACKEND_IMAGE))
                .withExposedPorts(BACKEND_PORT)
                .withEnv("ADMIN_INITIAL_PASSWORD", TEST_ADMIN_PASSWORD)
                .withEnv("AUTH_JWT_SECRET", TEST_JWT_SECRET)
                .waitingFor(Wait.forLogMessage(".*started in.*Listening on.*", 1))
                .withStartupTimeout(DEFAULT_STARTUP_TIMEOUT);
    }

    /**
     * Constructs the base URL for accessing the backend from the host.
     *
     * @param container the running backend container
     * @return the base URL (e.g., "http://localhost:32768")
     */
    public static String getBaseUrl(GenericContainer<?> container) {
        return "http://localhost:" + container.getMappedPort(BACKEND_PORT);
    }
}
