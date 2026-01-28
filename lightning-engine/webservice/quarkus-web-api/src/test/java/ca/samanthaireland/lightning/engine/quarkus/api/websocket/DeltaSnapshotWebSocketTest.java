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

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.smallrye.jwt.build.Jwt;

import ca.samanthaireland.lightning.engine.core.container.ContainerConfig;
import ca.samanthaireland.lightning.engine.core.container.ContainerManager;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the DeltaSnapshotWebSocket endpoint.
 *
 * <p>Tests verify that the delta WebSocket endpoint:
 * <ul>
 *   <li>Accepts connections</li>
 *   <li>Broadcasts delta snapshots with compression metrics</li>
 *   <li>Responds to reset commands</li>
 *   <li>Tracks changes between ticks</li>
 * </ul>
 */
@QuarkusTest
class DeltaSnapshotWebSocketTest {

    @TestHTTPResource("/")
    URI baseUri;

    @Inject
    BasicWebSocketConnector connector;

    @Inject
    ContainerManager containerManager;

    private ExecutionContainer container;
    private long containerId;
    private long matchId;

    @BeforeEach
    void setUp() {
        // Create and start a container for testing
        container = containerManager.createContainer(ContainerConfig.builder("delta-ws-test").build());
        containerId = container.getId();
        container.lifecycle().start();

        // Create a match
        var match = container.matches().create(
                new ca.samanthaireland.lightning.engine.core.match.Match(0, containerId, java.util.List.of(), java.util.List.of()));
        matchId = match.id();
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.lifecycle().stop();
            containerManager.deleteContainer(containerId);
        }
    }

    private URI getDeltaWsUri(String token) {
        String wsPath = String.format("/ws/containers/%d/matches/%d/delta?token=%s", containerId, matchId, token);
        return URI.create(baseUri.toString().replace("http://", "ws://") + wsPath.substring(1));
    }

    private String generateViewToken() {
        return Jwt.issuer("https://lightningfirefly.com")
                .subject("test-viewer")
                .groups(Set.of("view_only"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .sign();
    }

    @Test
    @Timeout(30)
    void shouldConnectAndReceiveDeltaSnapshot() throws Exception {
        LinkedBlockingDeque<String> messages = new LinkedBlockingDeque<>();
        String token = generateViewToken();

        WebSocketClientConnection connection = connector
                .baseUri(getDeltaWsUri(token))
                .onTextMessage((c, m) -> messages.add(m))
                .connectAndAwait();

        try {
            // Wait for at least one message (broadcast happens every 100ms by default)
            String message = messages.poll(10, TimeUnit.SECONDS);

            assertThat(message).isNotNull();
            assertThat(message).contains("matchId");
            assertThat(message).contains("fromTick");
            assertThat(message).contains("toTick");
            assertThat(message).contains("compressionRatio");
        } finally {
            connection.closeAndAwait();
        }
    }

    @Test
    @Timeout(30)
    void shouldReceiveDeltaOnMessage() throws Exception {
        LinkedBlockingDeque<String> messages = new LinkedBlockingDeque<>();
        String token = generateViewToken();

        WebSocketClientConnection connection = connector
                .baseUri(getDeltaWsUri(token))
                .onTextMessage((c, m) -> messages.add(m))
                .connectAndAwait();

        try {
            // Clear any initial broadcast messages
            messages.poll(1, TimeUnit.SECONDS);
            messages.clear();

            // Send a refresh request
            connection.sendTextAndAwait("refresh");

            // Should receive a response
            String message = messages.poll(5, TimeUnit.SECONDS);

            assertThat(message).isNotNull();
            assertThat(message).contains("matchId");
            assertThat(message).contains("fromTick");
        } finally {
            connection.closeAndAwait();
        }
    }

    @Test
    @Timeout(30)
    void shouldResetDeltaStateOnResetCommand() throws Exception {
        LinkedBlockingDeque<String> messages = new LinkedBlockingDeque<>();
        String token = generateViewToken();

        WebSocketClientConnection connection = connector
                .baseUri(getDeltaWsUri(token))
                .onTextMessage((c, m) -> messages.add(m))
                .connectAndAwait();

        try {
            // Wait for initial message to establish baseline
            String initial = messages.poll(10, TimeUnit.SECONDS);
            assertThat(initial).isNotNull();

            // Clear the queue
            messages.clear();

            // Send reset command - this should cause next delta to be from empty state
            connection.sendTextAndAwait("reset");

            // Get the response after reset
            String afterReset = messages.poll(5, TimeUnit.SECONDS);

            assertThat(afterReset).isNotNull();
            assertThat(afterReset).contains("matchId");
            // After reset, fromTick should be 0 (delta from empty state)
            assertThat(afterReset).contains("\"fromTick\":0");
        } finally {
            connection.closeAndAwait();
        }
    }

    @Test
    @Timeout(30)
    void shouldReceiveMultipleDeltaSnapshots() throws Exception {
        LinkedBlockingDeque<String> messages = new LinkedBlockingDeque<>();
        String token = generateViewToken();

        WebSocketClientConnection connection = connector
                .baseUri(getDeltaWsUri(token))
                .onTextMessage((c, m) -> messages.add(m))
                .connectAndAwait();

        try {
            // Wait for multiple broadcast messages
            String message1 = messages.poll(10, TimeUnit.SECONDS);
            String message2 = messages.poll(2, TimeUnit.SECONDS);

            assertThat(message1).isNotNull();
            assertThat(message2).isNotNull();

            // Both should be valid delta responses
            assertThat(message1).contains("compressionRatio");
            assertThat(message2).contains("compressionRatio");
        } finally {
            connection.closeAndAwait();
        }
    }

    @Test
    @Timeout(30)
    void deltaSnapshotShouldContainChangeMetrics() throws Exception {
        LinkedBlockingDeque<String> messages = new LinkedBlockingDeque<>();
        String token = generateViewToken();

        WebSocketClientConnection connection = connector
                .baseUri(getDeltaWsUri(token))
                .onTextMessage((c, m) -> messages.add(m))
                .connectAndAwait();

        try {
            String message = messages.poll(10, TimeUnit.SECONDS);

            assertThat(message).isNotNull();
            // Verify all expected delta fields are present
            assertThat(message).contains("\"matchId\":");
            assertThat(message).contains("\"fromTick\":");
            assertThat(message).contains("\"toTick\":");
            assertThat(message).contains("\"changedComponents\":");
            assertThat(message).contains("\"addedEntities\":");
            assertThat(message).contains("\"removedEntities\":");
            assertThat(message).contains("\"changeCount\":");
            assertThat(message).contains("\"compressionRatio\":");
        } finally {
            connection.closeAndAwait();
        }
    }
}
