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

package ca.samanthaireland.lightning.engine.api.resource.adapter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import ca.samanthaireland.api.proto.CommandProtos;

/**
 * WebSocket client for sending commands to a container.
 *
 * <p>Connects to the container-scoped command WebSocket endpoint and sends
 * Protocol Buffer commands. Provides both synchronous and asynchronous APIs.
 *
 * <p>Example usage:
 * <pre>{@code
 * CommandWebSocketClient client = CommandWebSocketClient.connect("http://localhost:8080", containerId, token);
 *
 * // Send a spawn command
 * client.spawn(new SpawnParams(matchId, playerId, entityType, posX, posY));
 *
 * // Send attach rigid body command
 * client.attachRigidBody(new RigidBodyParams(matchId, playerId, entityId, mass, posX, posY, velX, velY));
 *
 * // Close when done
 * client.close();
 * }</pre>
 */
public class CommandWebSocketClient implements AutoCloseable {

    // Configuration constants
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(10);
    private static final int RESPONSE_POLL_ATTEMPTS = 100;
    private static final Duration RESPONSE_POLL_INTERVAL = Duration.ofMillis(100);

    private final WebSocket webSocket;
    private final ConcurrentLinkedQueue<CommandProtos.CommandResponse> responses = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicReference<Throwable> lastError = new AtomicReference<>();
    private final CountDownLatch connectionLatch = new CountDownLatch(1);
    private final Object sendLock = new Object();

    private CommandWebSocketClient(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    // ==================== Connection Factory Methods ====================

    /**
     * Connect to a container's command WebSocket with default timeout.
     *
     * @param baseUrl the base URL (http://host:port)
     * @param containerId the container ID
     * @param token JWT token for authentication
     * @return a connected client
     * @throws IOException if connection fails
     */
    public static CommandWebSocketClient connect(String baseUrl, long containerId, String token) throws IOException {
        return connect(baseUrl, containerId, token, DEFAULT_TIMEOUT);
    }

    /**
     * Connect to a container's command WebSocket with custom timeout.
     *
     * @param baseUrl the base URL (http://host:port)
     * @param containerId the container ID
     * @param token JWT token for authentication
     * @param timeout connection timeout
     * @return a connected client
     * @throws IOException if connection fails
     */
    public static CommandWebSocketClient connect(String baseUrl, long containerId, String token, Duration timeout) throws IOException {
        validateToken(token);
        String wsUrl = buildWebSocketUrl(baseUrl, containerId, token);
        return establishConnection(wsUrl, timeout);
    }

    private static void validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is required for command WebSocket connection");
        }
    }

    private static String buildWebSocketUrl(String baseUrl, long containerId, String token) {
        String wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://");
        if (!wsUrl.endsWith("/")) {
            wsUrl += "/";
        }
        return wsUrl + "containers/" + containerId + "/commands?token=" + token;
    }

    private static CommandWebSocketClient establishConnection(String wsUrl, Duration timeout) throws IOException {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();

        CommandWebSocketClient[] clientRef = new CommandWebSocketClient[1];
        WebSocketMessageHandler handler = new WebSocketMessageHandler(clientRef);

        try {
            WebSocket webSocket = httpClient.newWebSocketBuilder()
                    .connectTimeout(timeout)
                    .buildAsync(URI.create(wsUrl), handler)
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);

            CommandWebSocketClient client = new CommandWebSocketClient(webSocket);
            clientRef[0] = client;

            waitForConnection(client, timeout);
            return client;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to connect to command WebSocket", e);
        }
    }

    private static void waitForConnection(CommandWebSocketClient client, Duration timeout) throws IOException {
        try {
            if (!client.connectionLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IOException("Connection timed out");
            }
            if (client.lastError.get() != null) {
                throw new IOException("Connection failed", client.lastError.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Connection interrupted", e);
        }
    }

    // ==================== Parameter Objects ====================

    /**
     * Parameters for spawn command.
     */
    public record SpawnParams(long matchId, long playerId, long entityType, long positionX, long positionY) {
        public SpawnParams {
            if (matchId < 0) throw new IllegalArgumentException("matchId must be non-negative");
            if (playerId < 0) throw new IllegalArgumentException("playerId must be non-negative");
        }
    }

    /**
     * Parameters for attachRigidBody command.
     */
    public record RigidBodyParams(
            long matchId,
            long playerId,
            long entityId,
            long mass,
            long positionX,
            long positionY,
            long velocityX,
            long velocityY
    ) {
        public RigidBodyParams {
            if (matchId < 0) throw new IllegalArgumentException("matchId must be non-negative");
            if (playerId < 0) throw new IllegalArgumentException("playerId must be non-negative");
            if (entityId < 0) throw new IllegalArgumentException("entityId must be non-negative");
            if (mass <= 0) throw new IllegalArgumentException("mass must be positive");
        }
    }

    /**
     * Parameters for attachSprite command.
     */
    public record SpriteParams(
            long matchId,
            long playerId,
            long entityId,
            long resourceId,
            long width,
            long height,
            boolean visible
    ) {
        public SpriteParams {
            if (matchId < 0) throw new IllegalArgumentException("matchId must be non-negative");
            if (playerId < 0) throw new IllegalArgumentException("playerId must be non-negative");
            if (entityId < 0) throw new IllegalArgumentException("entityId must be non-negative");
            if (width <= 0) throw new IllegalArgumentException("width must be positive");
            if (height <= 0) throw new IllegalArgumentException("height must be positive");
        }
    }

    // ==================== Command Methods ====================

    /**
     * Send a spawn command using parameter object.
     */
    public void spawn(SpawnParams params) throws IOException {
        CommandProtos.CommandRequest request = CommandProtos.CommandRequest.newBuilder()
                .setCommandName("spawn")
                .setMatchId(params.matchId())
                .setPlayerId(params.playerId())
                .setSpawn(CommandProtos.SpawnPayload.newBuilder()
                        .setEntityType(params.entityType())
                        .setPositionX(params.positionX())
                        .setPositionY(params.positionY())
                        .build())
                .build();
        sendAndWait(request);
    }

    /**
     * Send a spawn command (legacy method for backward compatibility).
     */
    public void spawn(long matchId, long playerId, long entityType, long posX, long posY) throws IOException {
        spawn(new SpawnParams(matchId, playerId, entityType, posX, posY));
    }

    /**
     * Send an attachRigidBody command using parameter object.
     */
    public void attachRigidBody(RigidBodyParams params) throws IOException {
        CommandProtos.CommandRequest request = CommandProtos.CommandRequest.newBuilder()
                .setCommandName("attachRigidBody")
                .setMatchId(params.matchId())
                .setPlayerId(params.playerId())
                .setAttachRigidBody(CommandProtos.AttachRigidBodyPayload.newBuilder()
                        .setEntityId(params.entityId())
                        .setMass(params.mass())
                        .setPositionX(params.positionX())
                        .setPositionY(params.positionY())
                        .setVelocityX(params.velocityX())
                        .setVelocityY(params.velocityY())
                        .build())
                .build();
        sendAndWait(request);
    }

    /**
     * Send an attachRigidBody command (legacy method for backward compatibility).
     */
    public void attachRigidBody(long matchId, long playerId, long entityId, long mass,
                                long posX, long posY, long velX, long velY) throws IOException {
        attachRigidBody(new RigidBodyParams(matchId, playerId, entityId, mass, posX, posY, velX, velY));
    }

    /**
     * Send an attachSprite command using parameter object.
     */
    public void attachSprite(SpriteParams params) throws IOException {
        CommandProtos.CommandRequest request = CommandProtos.CommandRequest.newBuilder()
                .setCommandName("attachSprite")
                .setMatchId(params.matchId())
                .setPlayerId(params.playerId())
                .setAttachSprite(CommandProtos.AttachSpritePayload.newBuilder()
                        .setEntityId(params.entityId())
                        .setResourceId(params.resourceId())
                        .setWidth(params.width())
                        .setHeight(params.height())
                        .setVisible(params.visible())
                        .build())
                .build();
        sendAndWait(request);
    }

    /**
     * Send an attachSprite command (legacy method for backward compatibility).
     */
    public void attachSprite(long matchId, long playerId, long entityId, long resourceId,
                             long width, long height, boolean visible) throws IOException {
        attachSprite(new SpriteParams(matchId, playerId, entityId, resourceId, width, height, visible));
    }

    /**
     * Send a custom command with raw parameters.
     */
    public void sendCommand(String commandName, long matchId, long playerId) throws IOException {
        if (commandName == null || commandName.isBlank()) {
            throw new IllegalArgumentException("commandName is required");
        }
        CommandProtos.CommandRequest request = CommandProtos.CommandRequest.newBuilder()
                .setCommandName(commandName)
                .setMatchId(matchId)
                .setPlayerId(playerId)
                .build();
        sendAndWait(request);
    }

    /**
     * Send raw protobuf request.
     */
    public CommandProtos.CommandResponse send(CommandProtos.CommandRequest request) throws IOException {
        return sendAndWait(request);
    }

    /**
     * Send a command asynchronously without waiting for response.
     */
    public CompletableFuture<Void> sendAsync(CommandProtos.CommandRequest request) {
        if (!connected.get()) {
            return CompletableFuture.failedFuture(new IOException("Not connected"));
        }

        byte[] bytes = request.toByteArray();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        return webSocket.sendBinary(buffer, true).thenAccept(ws -> {});
    }

    /**
     * Send a spawn command without waiting for response (fire-and-forget).
     *
     * <p>Use this for bulk operations where waiting for each response is too slow.
     * The caller is responsible for verifying commands succeeded via snapshots.
     */
    public void spawnFireAndForget(long matchId, long playerId, long entityType, long posX, long posY) {
        CommandProtos.CommandRequest request = CommandProtos.CommandRequest.newBuilder()
                .setCommandName("spawn")
                .setMatchId(matchId)
                .setPlayerId(playerId)
                .setSpawn(CommandProtos.SpawnPayload.newBuilder()
                        .setEntityType(entityType)
                        .setPositionX(posX)
                        .setPositionY(posY)
                        .build())
                .build();
        sendFireAndForget(request);
    }

    /**
     * Send a command without waiting for response (fire-and-forget).
     *
     * <p>Commands are serialized to avoid WebSocket "Send pending" errors.
     * Use this for bulk operations where waiting for each response is too slow.
     */
    public void sendFireAndForget(CommandProtos.CommandRequest request) {
        if (!connected.get()) {
            throw new IllegalStateException("Not connected");
        }

        byte[] bytes = request.toByteArray();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        synchronized (sendLock) {
            try {
                webSocket.sendBinary(buffer, true).get(SEND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                throw new RuntimeException("Failed to send command", e);
            }
        }
    }

    /**
     * Send an attachRigidBody command without waiting for response (fire-and-forget).
     */
    public void attachRigidBodyFireAndForget(RigidBodyParams params) {
        CommandProtos.CommandRequest request = CommandProtos.CommandRequest.newBuilder()
                .setCommandName("attachRigidBody")
                .setMatchId(params.matchId())
                .setPlayerId(params.playerId())
                .setAttachRigidBody(CommandProtos.AttachRigidBodyPayload.newBuilder()
                        .setEntityId(params.entityId())
                        .setMass(params.mass())
                        .setPositionX(params.positionX())
                        .setPositionY(params.positionY())
                        .setVelocityX(params.velocityX())
                        .setVelocityY(params.velocityY())
                        .build())
                .build();
        sendFireAndForget(request);
    }

    /**
     * Send an attachSprite command without waiting for response (fire-and-forget).
     */
    public void attachSpriteFireAndForget(SpriteParams params) {
        CommandProtos.CommandRequest request = CommandProtos.CommandRequest.newBuilder()
                .setCommandName("attachSprite")
                .setMatchId(params.matchId())
                .setPlayerId(params.playerId())
                .setAttachSprite(CommandProtos.AttachSpritePayload.newBuilder()
                        .setEntityId(params.entityId())
                        .setResourceId(params.resourceId())
                        .setWidth(params.width())
                        .setHeight(params.height())
                        .setVisible(params.visible())
                        .build())
                .build();
        sendFireAndForget(request);
    }

    // ==================== Connection State ====================

    /**
     * Check if connected.
     */
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public void close() {
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "closing").join();
        }
        connected.set(false);
    }

    // ==================== Private Helpers ====================

    private CommandProtos.CommandResponse sendAndWait(CommandProtos.CommandRequest request) throws IOException {
        if (!connected.get()) {
            throw new IOException("Not connected");
        }

        byte[] bytes = request.toByteArray();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        // Synchronize sends to prevent "Send pending" errors from concurrent WebSocket usage
        synchronized (sendLock) {
            try {
                webSocket.sendBinary(buffer, true).get(SEND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                return pollForResponse();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while sending command", e);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException("Failed to send command", e);
            }
        }
    }

    private CommandProtos.CommandResponse pollForResponse() throws IOException, InterruptedException {
        for (int i = 0; i < RESPONSE_POLL_ATTEMPTS; i++) {
            CommandProtos.CommandResponse response = responses.poll();
            if (response != null) {
                if (response.getStatus() == CommandProtos.CommandResponse.Status.ERROR) {
                    throw new IOException("Command failed: " + response.getMessage());
                }
                return response;
            }
            Thread.sleep(RESPONSE_POLL_INTERVAL.toMillis());
        }
        throw new IOException("Timeout waiting for command response");
    }

    // ==================== Inner Classes ====================

    /**
     * WebSocket message handler - extracted from anonymous class for testability.
     */
    private static class WebSocketMessageHandler implements WebSocket.Listener {
        private final CommandWebSocketClient[] clientRef;
        private final ByteBufferAccumulator accumulator = new ByteBufferAccumulator();

        WebSocketMessageHandler(CommandWebSocketClient[] clientRef) {
            this.clientRef = clientRef;
        }

        @Override
        public void onOpen(WebSocket ws) {
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            // Handle JSON text messages (connection response from server)
            String message = data.toString();
            if (message.contains("\"status\":\"ACCEPTED\"") || message.contains("\"ACCEPTED\"")) {
                CommandWebSocketClient client = clientRef[0];
                if (client != null && !client.connected.get()) {
                    client.connected.set(true);
                    client.connectionLatch.countDown();
                }
            } else if (message.contains("\"status\":\"ERROR\"") || message.contains("\"ERROR\"")) {
                CommandWebSocketClient client = clientRef[0];
                if (client != null) {
                    client.lastError.set(new IOException("Server error: " + message));
                    client.connectionLatch.countDown();
                }
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
            accumulator.append(data);
            if (last) {
                processCompleteBinaryMessage();
            }
            ws.request(1);
            return null;
        }

        private void processCompleteBinaryMessage() {
            try {
                byte[] bytes = accumulator.getAndClear();
                CommandProtos.CommandResponse response = CommandProtos.CommandResponse.parseFrom(bytes);
                handleResponse(response);
            } catch (Exception e) {
                handleError(e);
            }
        }

        private void handleResponse(CommandProtos.CommandResponse response) {
            CommandWebSocketClient client = clientRef[0];
            if (client != null) {
                client.responses.add(response);
                if (!client.connected.get() &&
                        response.getStatus() == CommandProtos.CommandResponse.Status.ACCEPTED) {
                    client.connected.set(true);
                    client.connectionLatch.countDown();
                }
            }
        }

        private void handleError(Throwable error) {
            CommandWebSocketClient client = clientRef[0];
            if (client != null) {
                client.lastError.set(error);
            }
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            CommandWebSocketClient client = clientRef[0];
            if (client != null) {
                client.connected.set(false);
                client.connectionLatch.countDown();
            }
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            CommandWebSocketClient client = clientRef[0];
            if (client != null) {
                client.lastError.set(error);
                client.connectionLatch.countDown();
            }
        }
    }

    /**
     * Accumulates fragmented binary WebSocket messages.
     */
    private static class ByteBufferAccumulator {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        void append(ByteBuffer data) {
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            buffer.write(bytes, 0, bytes.length);
        }

        byte[] getAndClear() {
            byte[] result = buffer.toByteArray();
            buffer.reset();
            return result;
        }
    }
}
