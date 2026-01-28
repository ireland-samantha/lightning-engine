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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.smallrye.jwt.build.Jwt;
import io.vertx.core.buffer.Buffer;

import ca.samanthaireland.api.proto.CommandProtos;
import ca.samanthaireland.lightning.engine.core.container.ContainerConfig;
import ca.samanthaireland.lightning.engine.core.container.ContainerManager;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.core.match.Match;

/**
 * Integration tests for the ContainerCommandWebSocket endpoint.
 *
 * <p>Tests verify that the WebSocket endpoint:
 * <ul>
 *   <li>Accepts connections to valid containers with proper authentication</li>
 *   <li>Processes JSON and protobuf command messages</li>
 *   <li>Returns appropriate responses</li>
 *   <li>Enforces role-based access control</li>
 * </ul>
 */
@QuarkusTest
class ContainerCommandWebSocketTest {

    @TestHTTPResource("/")
    URI baseUri;

    @Inject
    BasicWebSocketConnector connector;

    @Inject
    ContainerManager containerManager;

    @Inject
    ObjectMapper objectMapper;

    private ExecutionContainer container;
    private long containerId;
    private long matchId;

    @BeforeEach
    void setUp() {
        // Create and start a container for testing
        container = containerManager.createContainer(ContainerConfig.builder("cmd-ws-test").build());
        containerId = container.getId();
        container.lifecycle().start();

        // Create a match (no modules needed for WebSocket tests)
        Match match = container.matches().create(
                new Match(0, containerId, List.of(), List.of()));
        matchId = match.id();
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.lifecycle().stop();
            containerManager.deleteContainer(containerId);
        }
    }

    private URI getCommandWsUri(long containerId, String token) {
        String wsPath = String.format("/containers/%d/commands?token=%s", containerId, token);
        return URI.create(baseUri.toString().replace("http://", "ws://") + wsPath.substring(1));
    }

    private URI getCommandWsUriWithoutToken(long containerId) {
        String wsPath = String.format("/containers/%d/commands", containerId);
        return URI.create(baseUri.toString().replace("http://", "ws://") + wsPath.substring(1));
    }

    private String generateAdminToken() {
        return Jwt.issuer("https://lightningfirefly.com")
                .subject("test-admin")
                .groups(Set.of("admin"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .sign();
    }

    private String generateUserToken() {
        return Jwt.issuer("https://lightningfirefly.com")
                .subject("test-user")
                .groups(Set.of("user"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .sign();
    }

    @Test
    @Timeout(30)
    void shouldConnectAndReceiveJsonConnectedResponse() throws Exception {
        LinkedBlockingDeque<String> textMessages = new LinkedBlockingDeque<>();
        String token = generateAdminToken();

        WebSocketClientConnection connection = connector
                .baseUri(getCommandWsUri(containerId, token))
                .onTextMessage((c, m) -> textMessages.add(m))
                .connectAndAwait();

        try {
            // Wait for connection response (JSON text)
            String message = textMessages.poll(5, TimeUnit.SECONDS);
            assertThat(message).isNotNull();

            JsonNode response = objectMapper.readTree(message);
            assertThat(response.get("status").asText()).isEqualTo("ACCEPTED");
            assertThat(response.get("message").asText()).contains("Connected to container");
        } finally {
            connection.closeAndAwait();
        }
    }

    @Test
    @Timeout(30)
    void shouldProcessCommandViaProtobuf() throws Exception {
        LinkedBlockingDeque<String> textMessages = new LinkedBlockingDeque<>();
        LinkedBlockingDeque<Buffer> binaryMessages = new LinkedBlockingDeque<>();
        String token = generateAdminToken();

        WebSocketClientConnection connection = connector
                .baseUri(getCommandWsUri(containerId, token))
                .onTextMessage((c, m) -> textMessages.add(m))
                .onBinaryMessage((c, m) -> binaryMessages.add(m))
                .connectAndAwait();

        try {
            // Wait for connection response (JSON text)
            String connMsg = textMessages.poll(5, TimeUnit.SECONDS);
            assertThat(connMsg).isNotNull();

            // Send a command via protobuf
            CommandProtos.CommandRequest request = CommandProtos.CommandRequest.newBuilder()
                    .setCommandName("test-command")
                    .setMatchId(matchId)
                    .setPlayerId(1)
                    .setGeneric(CommandProtos.GenericPayload.newBuilder()
                            .putStringParams("action", "test")
                            .putLongParams("value", 42)
                            .build())
                    .build();

            connection.sendBinaryAndAwait(Buffer.buffer(request.toByteArray()));

            // Wait for protobuf response - verifies WebSocket can receive and respond to binary messages
            Buffer responseBuffer = binaryMessages.poll(5, TimeUnit.SECONDS);
            assertThat(responseBuffer).isNotNull();

            CommandProtos.CommandResponse response = CommandProtos.CommandResponse.parseFrom(responseBuffer.getBytes());
            // Verify response is properly formatted (status may be ERROR if command doesn't exist)
            assertThat(response.getStatus()).isNotNull();
            assertThat(response.getMessage()).isNotEmpty();
        } finally {
            connection.closeAndAwait();
        }
    }

    @Test
    @Timeout(30)
    void shouldProcessCommandViaJson() throws Exception {
        LinkedBlockingDeque<String> textMessages = new LinkedBlockingDeque<>();
        String token = generateAdminToken();

        WebSocketClientConnection connection = connector
                .baseUri(getCommandWsUri(containerId, token))
                .onTextMessage((c, m) -> textMessages.add(m))
                .connectAndAwait();

        try {
            // Wait for connection response
            String connMsg = textMessages.poll(5, TimeUnit.SECONDS);
            assertThat(connMsg).isNotNull();

            // Send a command via JSON
            String jsonCommand = """
                {
                    "commandName": "test-command",
                    "matchId": %d,
                    "playerId": 1,
                    "generic": {
                        "stringParams": {"action": "test"},
                        "longParams": {"value": 42}
                    }
                }
                """.formatted(matchId);

            connection.sendTextAndAwait(jsonCommand);

            // Wait for JSON response - verifies WebSocket can receive and respond to text messages
            String responseText = textMessages.poll(5, TimeUnit.SECONDS);
            assertThat(responseText).isNotNull();

            JsonNode response = objectMapper.readTree(responseText);
            // Verify response is properly formatted (status may be ERROR if command doesn't exist)
            assertThat(response.has("status")).isTrue();
            assertThat(response.has("message")).isTrue();
        } finally {
            connection.closeAndAwait();
        }
    }

    @Test
    @Timeout(30)
    void shouldRejectInvalidContainerId() throws Exception {
        LinkedBlockingDeque<String> textMessages = new LinkedBlockingDeque<>();
        String token = generateAdminToken();

        WebSocketClientConnection connection = connector
                .baseUri(getCommandWsUri(999999, token)) // Non-existent container
                .onTextMessage((c, m) -> textMessages.add(m))
                .connectAndAwait();

        try {
            // Wait for connection response
            String message = textMessages.poll(5, TimeUnit.SECONDS);
            assertThat(message).isNotNull();

            JsonNode response = objectMapper.readTree(message);
            assertThat(response.get("status").asText()).isEqualTo("ERROR");
            assertThat(response.get("message").asText()).contains("Container not found");
        } finally {
            connection.closeAndAwait();
        }
    }

    @Test
    @Timeout(30)
    void shouldHandleMultipleCommands() throws Exception {
        LinkedBlockingDeque<String> textMessages = new LinkedBlockingDeque<>();
        LinkedBlockingDeque<Buffer> binaryMessages = new LinkedBlockingDeque<>();
        String token = generateAdminToken();

        WebSocketClientConnection connection = connector
                .baseUri(getCommandWsUri(containerId, token))
                .onTextMessage((c, m) -> textMessages.add(m))
                .onBinaryMessage((c, m) -> binaryMessages.add(m))
                .connectAndAwait();

        try {
            // Wait for connection response
            textMessages.poll(5, TimeUnit.SECONDS);

            // Send multiple commands
            for (int i = 0; i < 3; i++) {
                CommandProtos.CommandRequest request = CommandProtos.CommandRequest.newBuilder()
                        .setCommandName("test-command-" + i)
                        .setMatchId(matchId)
                        .setPlayerId(1)
                        .setGeneric(CommandProtos.GenericPayload.newBuilder()
                                .putStringParams("action", "test")
                                .putLongParams("index", i)
                                .build())
                        .build();

                connection.sendBinaryAndAwait(Buffer.buffer(request.toByteArray()));
            }

            // All should receive responses (verifies WebSocket handles multiple messages)
            for (int i = 0; i < 3; i++) {
                Buffer responseBuffer = binaryMessages.poll(5, TimeUnit.SECONDS);
                assertThat(responseBuffer).isNotNull();

                CommandProtos.CommandResponse response = CommandProtos.CommandResponse.parseFrom(responseBuffer.getBytes());
                // Verify response is properly formatted
                assertThat(response.getStatus()).isNotNull();
            }
        } finally {
            connection.closeAndAwait();
        }
    }

    @Test
    @Timeout(30)
    void shouldRejectConnectionWithoutToken() throws Exception {
        LinkedBlockingDeque<String> textMessages = new LinkedBlockingDeque<>();

        WebSocketClientConnection connection = connector
                .baseUri(getCommandWsUriWithoutToken(containerId))
                .onTextMessage((c, m) -> textMessages.add(m))
                .connectAndAwait();

        try {
            String message = textMessages.poll(5, TimeUnit.SECONDS);
            assertThat(message).isNotNull();

            JsonNode response = objectMapper.readTree(message);
            assertThat(response.get("status").asText()).isEqualTo("ERROR");
            assertThat(response.get("message").asText()).containsIgnoringCase("token");
        } finally {
            connection.closeAndAwait();
        }
    }

    @Test
    @Timeout(30)
    void shouldRejectConnectionWithInsufficientRole() throws Exception {
        LinkedBlockingDeque<String> textMessages = new LinkedBlockingDeque<>();
        String userToken = generateUserToken(); // User role, not admin or command_manager

        WebSocketClientConnection connection = connector
                .baseUri(getCommandWsUri(containerId, userToken))
                .onTextMessage((c, m) -> textMessages.add(m))
                .connectAndAwait();

        try {
            String message = textMessages.poll(5, TimeUnit.SECONDS);
            assertThat(message).isNotNull();

            JsonNode response = objectMapper.readTree(message);
            assertThat(response.get("status").asText()).isEqualTo("ERROR");
            assertThat(response.get("message").asText()).containsIgnoringCase("authorization failed");
        } finally {
            connection.closeAndAwait();
        }
    }

    // Note: Subprotocol authentication is supported by the backend (WebSocketAuthenticator extracts
    // tokens from Sec-WebSocket-Protocol header), but Quarkus websockets-next doesn't automatically
    // echo back subprotocols during handshake, so this test is skipped for now.
    // The frontend can still use subprotocol auth by setting the header manually.
}
