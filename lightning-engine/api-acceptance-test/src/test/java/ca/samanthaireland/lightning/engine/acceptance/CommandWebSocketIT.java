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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
 * Acceptance test for Command WebSocket functionality.
 *
 * <p>Tests the container-scoped WebSocket endpoint for submitting commands,
 * including authentication, command execution, and rate limiting.
 */
@Slf4j
@Tag("acceptance")
@Tag("testcontainers")
@DisplayName("Command WebSocket Acceptance Test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Timeout(value = 2, unit = TimeUnit.MINUTES)
class CommandWebSocketIT {

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
    @DisplayName("WebSocket connection with subprotocol authentication succeeds")
    void testSubprotocolAuthentication() throws Exception {
        setupMatchAndContainer();

        String wsUrl = baseUrl.replace("http://", "ws://") +
                "/containers/" + containerId + "/commands";
        log.info("Connecting to Command WebSocket with subprotocol auth: {}", wsUrl);

        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        HttpClient httpClient = HttpClient.newHttpClient();

        // Use subprotocol authentication (Bearer.{token})
        WebSocket webSocket = httpClient.newWebSocketBuilder()
                .subprotocols("Bearer." + authToken)
                .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                    private final StringBuilder messageBuffer = new StringBuilder();

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        messageBuffer.append(data);
                        if (last) {
                            String message = messageBuffer.toString();
                            log.info("Received WebSocket response: {}", message);
                            if (!responseFuture.isDone()) {
                                responseFuture.complete(message);
                            }
                        }
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        log.error("WebSocket error", error);
                        responseFuture.completeExceptionally(error);
                    }
                })
                .join();

        try {
            String response = responseFuture.get(10, TimeUnit.SECONDS);
            log.info("Connection response: {}", response);

            assertThat(response).contains("\"status\":\"ACCEPTED\"");
            assertThat(response).contains("Connected to container " + containerId);

            log.info("Subprotocol authentication test PASSED");
        } finally {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
        }
    }

    @Test
    @Order(2)
    @DisplayName("WebSocket connection with query parameter authentication succeeds (legacy)")
    void testQueryParameterAuthentication() throws Exception {
        setupMatchAndContainer();

        String wsUrl = baseUrl.replace("http://", "ws://") +
                "/containers/" + containerId + "/commands?token=" + authToken;
        log.info("Connecting to Command WebSocket with query param auth: {}", wsUrl);

        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        HttpClient httpClient = HttpClient.newHttpClient();

        WebSocket webSocket = httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                    private final StringBuilder messageBuffer = new StringBuilder();

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        messageBuffer.append(data);
                        if (last) {
                            String message = messageBuffer.toString();
                            log.info("Received WebSocket response: {}", message);
                            if (!responseFuture.isDone()) {
                                responseFuture.complete(message);
                            }
                        }
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        log.error("WebSocket error", error);
                        responseFuture.completeExceptionally(error);
                    }
                })
                .join();

        try {
            String response = responseFuture.get(10, TimeUnit.SECONDS);

            assertThat(response).contains("\"status\":\"ACCEPTED\"");
            assertThat(response).contains("Connected to container " + containerId);

            log.info("Query parameter authentication test PASSED");
        } finally {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
        }
    }

    @Test
    @Order(3)
    @DisplayName("WebSocket connection without authentication fails")
    void testAuthenticationRequired() throws Exception {
        setupMatchAndContainer();

        String wsUrl = baseUrl.replace("http://", "ws://") +
                "/containers/" + containerId + "/commands";
        log.info("Connecting to Command WebSocket without auth: {}", wsUrl);

        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        HttpClient httpClient = HttpClient.newHttpClient();

        WebSocket webSocket = httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                    private final StringBuilder messageBuffer = new StringBuilder();

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        messageBuffer.append(data);
                        if (last) {
                            String message = messageBuffer.toString();
                            log.info("Received WebSocket response: {}", message);
                            if (!responseFuture.isDone()) {
                                responseFuture.complete(message);
                            }
                        }
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        log.error("WebSocket error", error);
                        responseFuture.completeExceptionally(error);
                    }
                })
                .join();

        try {
            String response = responseFuture.get(10, TimeUnit.SECONDS);

            assertThat(response).contains("\"status\":\"ERROR\"");
            assertThat(response).containsIgnoringCase("authentication");

            log.info("Authentication required test PASSED");
        } finally {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
        }
    }

    @Test
    @Order(4)
    @DisplayName("Spawn command via WebSocket creates entity")
    void testSpawnCommandViaWebSocket() throws Exception {
        setupMatchAndContainer();

        // Get initial entity count
        int initialCount = getEntityCount();
        log.info("Initial entity count: {}", initialCount);

        // Connect to Command WebSocket
        String wsUrl = baseUrl.replace("http://", "ws://") +
                "/containers/" + containerId + "/commands";
        log.info("Connecting to Command WebSocket: {}", wsUrl);

        List<String> responses = new ArrayList<>();
        CompletableFuture<String> connectionFuture = new CompletableFuture<>();
        HttpClient httpClient = HttpClient.newHttpClient();

        WebSocket webSocket = httpClient.newWebSocketBuilder()
                .subprotocols("Bearer." + authToken)
                .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                    private final StringBuilder messageBuffer = new StringBuilder();

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        messageBuffer.append(data);
                        if (last) {
                            String message = messageBuffer.toString();
                            log.info("Received: {}", message);
                            responses.add(message);
                            messageBuffer.setLength(0);
                            if (!connectionFuture.isDone()) {
                                connectionFuture.complete(message);
                            }
                        }
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        log.error("WebSocket error", error);
                        connectionFuture.completeExceptionally(error);
                    }
                })
                .join();

        try {
            // Wait for connection response
            String connectionResponse = connectionFuture.get(10, TimeUnit.SECONDS);
            assertThat(connectionResponse).contains("\"status\":\"ACCEPTED\"");

            // Send spawn command as JSON
            String spawnCommand = """
                    {
                        "commandName": "spawn",
                        "matchId": %d,
                        "playerId": 1,
                        "spawn": {
                            "entityType": 100,
                            "positionX": 0,
                            "positionY": 0
                        }
                    }
                    """.formatted(matchId);

            log.info("Sending spawn command: {}", spawnCommand);
            webSocket.sendText(spawnCommand, true).join();

            // Wait for response
            Thread.sleep(500);

            // Tick the container to process the command
            container.tick();

            // Wait for entity to appear
            Thread.sleep(500);

            // Verify entity was created
            int finalCount = getEntityCount();
            log.info("Final entity count: {}", finalCount);

            assertThat(finalCount).isGreaterThan(initialCount);
            log.info("Spawn command via WebSocket test PASSED");

        } finally {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
        }
    }

    @Test
    @Order(5)
    @DisplayName("Multiple commands via WebSocket are processed")
    void testMultipleCommandsViaWebSocket() throws Exception {
        setupMatchAndContainer();

        int initialCount = getEntityCount();
        log.info("Initial entity count: {}", initialCount);

        String wsUrl = baseUrl.replace("http://", "ws://") +
                "/containers/" + containerId + "/commands";

        List<String> responses = new ArrayList<>();
        CompletableFuture<String> connectionFuture = new CompletableFuture<>();
        HttpClient httpClient = HttpClient.newHttpClient();

        WebSocket webSocket = httpClient.newWebSocketBuilder()
                .subprotocols("Bearer." + authToken)
                .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                    private final StringBuilder messageBuffer = new StringBuilder();

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        messageBuffer.append(data);
                        if (last) {
                            String message = messageBuffer.toString();
                            log.info("Received: {}", message);
                            responses.add(message);
                            messageBuffer.setLength(0);
                            if (!connectionFuture.isDone()) {
                                connectionFuture.complete(message);
                            }
                        }
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        log.error("WebSocket error", error);
                        connectionFuture.completeExceptionally(error);
                    }
                })
                .join();

        try {
            // Wait for connection
            connectionFuture.get(10, TimeUnit.SECONDS);

            // Send multiple spawn commands
            int commandCount = 3;
            for (int i = 0; i < commandCount; i++) {
                String spawnCommand = """
                        {
                            "commandName": "spawn",
                            "matchId": %d,
                            "playerId": 1,
                            "spawn": {
                                "entityType": %d,
                                "positionX": %d,
                                "positionY": %d
                            }
                        }
                        """.formatted(matchId, 100 + i, i * 10, i * 10);
                webSocket.sendText(spawnCommand, true).join();
            }

            // Wait for commands to be processed
            Thread.sleep(500);
            container.tick();
            Thread.sleep(500);

            // Verify all entities were created
            int finalCount = getEntityCount();
            log.info("Final entity count: {} (expected: {} + {})", finalCount, initialCount, commandCount);

            assertThat(finalCount).isGreaterThanOrEqualTo(initialCount + commandCount);
            log.info("Multiple commands test PASSED");

        } finally {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
        }
    }

    @Test
    @Order(6)
    @DisplayName("Invalid command returns error response")
    void testInvalidCommandReturnsError() throws Exception {
        setupMatchAndContainer();

        String wsUrl = baseUrl.replace("http://", "ws://") +
                "/containers/" + containerId + "/commands";

        List<String> responses = new ArrayList<>();
        CompletableFuture<String> connectionFuture = new CompletableFuture<>();
        CompletableFuture<String> errorResponseFuture = new CompletableFuture<>();
        HttpClient httpClient = HttpClient.newHttpClient();

        WebSocket webSocket = httpClient.newWebSocketBuilder()
                .subprotocols("Bearer." + authToken)
                .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                    private final StringBuilder messageBuffer = new StringBuilder();
                    private boolean connectedReceived = false;

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        messageBuffer.append(data);
                        if (last) {
                            String message = messageBuffer.toString();
                            log.info("Received: {}", message);
                            responses.add(message);
                            messageBuffer.setLength(0);
                            if (!connectedReceived) {
                                connectedReceived = true;
                                connectionFuture.complete(message);
                            } else {
                                errorResponseFuture.complete(message);
                            }
                        }
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        log.error("WebSocket error", error);
                        connectionFuture.completeExceptionally(error);
                        errorResponseFuture.completeExceptionally(error);
                    }
                })
                .join();

        try {
            // Wait for connection
            connectionFuture.get(10, TimeUnit.SECONDS);

            // Send invalid JSON
            String invalidCommand = "not valid json";
            webSocket.sendText(invalidCommand, true).join();

            // Wait for error response
            String errorResponse = errorResponseFuture.get(10, TimeUnit.SECONDS);

            assertThat(errorResponse).contains("\"status\":\"ERROR\"");
            assertThat(errorResponse).containsIgnoringCase("parse");

            log.info("Invalid command error test PASSED");

        } finally {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
        }
    }

    // ========== Helper Methods ==========

    private void setupMatchAndContainer() throws Exception {
        ContainerAdapter.ContainerResponse containerResponse = client.containers()
                .create()
                .name("cmd-ws-test-" + System.currentTimeMillis())
                .withModules(REQUIRED_MODULES.toArray(new String[0]))
                .execute();
        containerId = containerResponse.id();
        container = client.container(containerId);
        container.play(60);

        EntitySpawner.waitForContainerRunning(client, containerId);

        var match = container.createMatch(REQUIRED_MODULES);
        matchId = match.id();
        log.info("Created container {} and match {}", containerId, matchId);
    }

    private int getEntityCount() {
        try {
            var snapshotOpt = container.getSnapshot(matchId);
            if (snapshotOpt.isPresent()) {
                return client.parseSnapshot(snapshotOpt.get().data()).entityIds().size();
            }
        } catch (Exception e) {
            log.warn("Failed to get entity count: {}", e.getMessage());
        }
        return 0;
    }
}
