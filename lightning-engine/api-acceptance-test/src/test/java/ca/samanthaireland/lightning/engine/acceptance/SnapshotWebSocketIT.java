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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import ca.samanthaireland.lightning.engine.acceptance.fixture.EntitySpawner;
import ca.samanthaireland.lightning.engine.api.resource.adapter.AuthAdapter;
import ca.samanthaireland.lightning.engine.api.resource.adapter.ContainerAdapter;
import ca.samanthaireland.lightning.engine.api.resource.adapter.EngineClient;
import ca.samanthaireland.lightning.engine.api.resource.adapter.EngineClient.ContainerClient;
import lombok.extern.slf4j.Slf4j;

/**
 * Acceptance test for WebSocket snapshot streaming.
 *
 * <p>This test verifies that snapshots received via WebSocket contain
 * the expected component data after entities are created and modified.
 */
@Slf4j
@Tag("acceptance")
@Tag("testcontainers")
@DisplayName("Snapshot WebSocket Acceptance Test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Timeout(value = 2, unit = TimeUnit.MINUTES)
class SnapshotWebSocketIT {

    private static final List<String> REQUIRED_MODULES = List.of(
            "EntityModule", "GridMapModule", "RigidBodyModule", "RenderModule"
    );

    @Container
    static GenericContainer<?> backendContainer = TestContainers.backendContainer();

    private EngineClient client;
    private ContainerClient container;
    private long containerId = -1;
    private long matchId = -1;
    private String baseUrl;
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = TestContainers.getBaseUrl(backendContainer);
        log.info("Backend URL: {}", baseUrl);

        AuthAdapter auth = new AuthAdapter.HttpAuthAdapter(baseUrl);
        authToken = auth.login("admin", TestContainers.TEST_ADMIN_PASSWORD).token();
        log.info("Authenticated successfully");

        client = EngineClient.builder()
                .baseUrl(baseUrl)
                .withBearerToken(authToken)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (containerId > 0) {
            try {
                client.containers().stopContainer(containerId);
                client.containers().deleteContainer(containerId);
            } catch (Exception e) {
                log.warn("Failed to clean up container {}: {}", containerId, e.getMessage());
            }
        }
        containerId = -1;
        matchId = -1;
        container = null;
    }

    @Test
    @Order(1)
    @DisplayName("REST snapshot endpoint returns components after setVelocity")
    void testRestSnapshotReturnsComponents() throws Exception {
        setupMatchAndContainer();

        // Spawn entity
        long entityId = spawnEntity();
        log.info("Spawned entity: {}", entityId);

        // Attach rigid body with initial position
        container.forMatch(matchId).send("attachRigidBody", Map.of(
                "entityId", entityId,
                "positionX", 100.0f,
                "positionY", 200.0f,
                "mass", 1.0f
        ));
        // Wait for rigid body components to appear
        EntitySpawner.waitForComponent(client, container, matchId, "RigidBodyModule", "MASS");
        EntitySpawner.waitForComponent(client, container, matchId, "GridMapModule", "POSITION_X");

        // Set velocity
        container.forMatch(matchId).custom("setVelocity")
                .param("entityId", entityId)
                .param("velocityX", 50.0f)
                .param("velocityY", -30.0f)
                .param("velocityZ", 0.0f)
                .execute();
        container.tick();

        // Get snapshot via REST
        var snapshot = getSnapshot();
        log.info("REST snapshot modules: {}", snapshot.moduleNames());

        var rigidBody = snapshot.module("RigidBodyModule");
        log.info("RigidBodyModule components: {}", rigidBody.componentNames());

        // Verify components exist and have values
        assertThat(rigidBody.has("VELOCITY_X"))
                .as("RigidBodyModule should have VELOCITY_X component")
                .isTrue();

        List<Float> velocityX = rigidBody.component("VELOCITY_X");
        assertThat(velocityX)
                .as("VELOCITY_X should have at least one value")
                .isNotEmpty();

        float vx = rigidBody.first("VELOCITY_X", 0);
        log.info("REST snapshot VELOCITY_X: {}", vx);

        assertThat(Math.abs(vx)).isGreaterThan(40);
        log.info("REST snapshot test PASSED");
    }

    @Test
    @Order(2)
    @DisplayName("WebSocket snapshot returns components after setVelocity")
    void testWebSocketSnapshotReturnsComponents() throws Exception {
        setupMatchAndContainer();

        // Spawn entity
        long entityId = spawnEntity();
        log.info("Spawned entity: {}", entityId);

        // Attach rigid body
        container.forMatch(matchId).send("attachRigidBody", Map.of(
                "entityId", entityId,
                "positionX", 100.0f,
                "positionY", 200.0f,
                "mass", 1.0f
        ));
        // Wait for rigid body components to appear
        EntitySpawner.waitForComponent(client, container, matchId, "RigidBodyModule", "MASS");
        EntitySpawner.waitForComponent(client, container, matchId, "GridMapModule", "POSITION_X");

        // Set velocity
        container.forMatch(matchId).custom("setVelocity")
                .param("entityId", entityId)
                .param("velocityX", 50.0f)
                .param("velocityY", -30.0f)
                .param("velocityZ", 0.0f)
                .execute();
        container.tick();

        // Connect to WebSocket and receive snapshot (with JWT token for authentication)
        String wsUrl = baseUrl.replace("http://", "ws://") +
                "/ws/containers/" + containerId + "/matches/" + matchId + "/snapshot?token=" + authToken;
        log.info("Connecting to WebSocket: {}", wsUrl);

        CompletableFuture<String> snapshotFuture = new CompletableFuture<>();
        HttpClient httpClient = HttpClient.newHttpClient();

        WebSocket webSocket = httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                    private final StringBuilder messageBuffer = new StringBuilder();

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        messageBuffer.append(data);
                        if (last) {
                            String message = messageBuffer.toString();
                            log.info("Received WebSocket message: {}", message);
                            snapshotFuture.complete(message);
                        }
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        log.error("WebSocket error", error);
                        snapshotFuture.completeExceptionally(error);
                    }
                })
                .join();

        try {
            // Wait for snapshot message
            String snapshotJson = snapshotFuture.get(10, TimeUnit.SECONDS);
            log.info("WebSocket snapshot JSON: {}", snapshotJson);

            // Parse and validate the new snapshot structure with modules
            assertThat(snapshotJson).contains("\"modules\":");
            assertThat(snapshotJson).contains("RigidBodyModule");
            assertThat(snapshotJson).contains("VELOCITY_X");
            // Verify version information is present in the new format
            assertThat(snapshotJson).contains("\"version\":");

            // Verify the data is not empty
            assertThat(snapshotJson)
                    .as("WebSocket snapshot should contain actual component values, not empty arrays")
                    .doesNotContain("\"values\":[]");

            log.info("WebSocket snapshot test PASSED");
        } finally {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
        }
    }

    @Test
    @Order(3)
    @DisplayName("Delta WebSocket returns changed components")
    void testDeltaWebSocketReturnsComponents() throws Exception {
        setupMatchAndContainer();

        // Spawn entity
        long entityId = spawnEntity();
        log.info("Spawned entity: {}", entityId);

        // Attach rigid body
        container.forMatch(matchId).send("attachRigidBody", Map.of(
                "entityId", entityId,
                "positionX", 100.0f,
                "positionY", 200.0f,
                "mass", 1.0f
        ));
        // Wait for rigid body components to appear
        EntitySpawner.waitForComponent(client, container, matchId, "RigidBodyModule", "MASS");
        EntitySpawner.waitForComponent(client, container, matchId, "GridMapModule", "POSITION_X");

        // Set velocity
        container.forMatch(matchId).custom("setVelocity")
                .param("entityId", entityId)
                .param("velocityX", 50.0f)
                .param("velocityY", -30.0f)
                .param("velocityZ", 0.0f)
                .execute();
        container.tick();

        // Connect to Delta WebSocket and receive delta snapshot (with JWT token for authentication)
        String wsUrl = baseUrl.replace("http://", "ws://") +
                "/ws/containers/" + containerId + "/matches/" + matchId + "/delta?token=" + authToken;
        log.info("Connecting to Delta WebSocket: {}", wsUrl);

        CompletableFuture<String> deltaFuture = new CompletableFuture<>();
        HttpClient httpClient = HttpClient.newHttpClient();

        WebSocket webSocket = httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                    private final StringBuilder messageBuffer = new StringBuilder();

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        messageBuffer.append(data);
                        if (last) {
                            String message = messageBuffer.toString();
                            log.info("Received Delta WebSocket message: {}", message);
                            deltaFuture.complete(message);
                        }
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        log.error("Delta WebSocket error", error);
                        deltaFuture.completeExceptionally(error);
                    }
                })
                .join();

        try {
            // Wait for delta message
            String deltaJson = deltaFuture.get(10, TimeUnit.SECONDS);
            log.info("Delta WebSocket JSON: {}", deltaJson);

            // Parse and validate the delta response
            assertThat(deltaJson).contains("\"matchId\":");
            assertThat(deltaJson).contains("\"changedComponents\":");

            // First delta should include all added entities (full snapshot as delta from empty)
            // Verify it contains RigidBodyModule with velocity components
            assertThat(deltaJson).contains("RigidBodyModule");
            assertThat(deltaJson).contains("VELOCITY_X");

            log.info("Delta WebSocket test PASSED");
        } finally {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
        }
    }

    // ========== Helper Methods ==========

    private void setupMatchAndContainer() throws Exception {
        ContainerAdapter.ContainerResponse containerResponse = client.containers()
                .create()
                .name("snapshot-test-" + System.currentTimeMillis())
                .withModules(REQUIRED_MODULES.toArray(new String[0]))
                .execute();
        containerId = containerResponse.id();
        container = client.container(containerId);
        container.play(60);

        // Wait for container to be fully running
        EntitySpawner.waitForContainerRunning(client, containerId);

        var match = container.createMatch(REQUIRED_MODULES);
        matchId = match.id();
        log.info("Created container {} and match {}", containerId, matchId);
    }

    private long spawnEntity() throws IOException {
        return EntitySpawner.spawnEntity(client, container, matchId);
    }

    private EngineClient.Snapshot getSnapshot() throws IOException {
        var snapshotOpt = container.getSnapshot(matchId);
        if (snapshotOpt.isEmpty()) {
            throw new IllegalStateException("No snapshot available for match " + matchId);
        }
        return client.parseSnapshot(snapshotOpt.get().data());
    }
}
