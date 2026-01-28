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

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.vertx.core.buffer.Buffer;

import ca.samanthaireland.api.proto.CommandProtos;
import ca.samanthaireland.lightning.engine.core.command.CommandPayload;
import ca.samanthaireland.lightning.engine.core.container.ContainerManager;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;

/**
 * Container-scoped WebSocket endpoint for submitting commands.
 *
 * <p>Clients connect to /containers/{containerId}/commands and send commands as
 * JSON text messages (for browsers) or Protocol Buffer binary messages (for native clients).
 * Commands are enqueued in the specified container's command queue for processing during the next tick.
 *
 * <p>Authentication is supported via:
 * <ul>
 *   <li>Subprotocol: Sec-WebSocket-Protocol: Bearer.{jwt_token} (recommended)</li>
 *   <li>Query parameter: ?token={jwt_token} (legacy)</li>
 * </ul>
 */
@WebSocket(path = "/containers/{containerId}/commands")
public class ContainerCommandWebSocket {
    private static final Logger log = LoggerFactory.getLogger(ContainerCommandWebSocket.class);

    @Inject
    ContainerManager containerManager;

    @Inject
    WebSocketConnection connection;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    WebSocketAuthenticator authenticator;

    @Inject
    WebSocketRateLimiter rateLimiter;

    @Inject
    CommandPayloadConverter payloadConverter;

    @Inject
    WebSocketMetrics metrics;

    @Inject
    WebSocketConnectionLimiter connectionLimiter;

    @Inject
    CommandTelemetry telemetry;

    /**
     * Handle WebSocket connection open.
     * Returns JSON text response for browser compatibility.
     */
    @OnOpen
    public String onOpen() {
        // Authenticate the connection
        WebSocketAuthenticator.AuthResult authResult = authenticator.authenticate(connection);
        if (authResult instanceof WebSocketAuthenticator.AuthResult.Failure failure) {
            metrics.authFailure();
            return buildJsonErrorResponse(failure.message());
        }

        WebSocketAuthenticator.AuthResult.Success authSuccess = (WebSocketAuthenticator.AuthResult.Success) authResult;

        // Validate container exists
        String containerIdStr = connection.pathParam("containerId");
        long containerId;
        try {
            containerId = Long.parseLong(containerIdStr);
        } catch (NumberFormatException e) {
            log.warn("Invalid container ID: {}", containerIdStr);
            return buildJsonErrorResponse("Invalid container ID: " + containerIdStr);
        }

        Optional<ExecutionContainer> containerOpt = containerManager.getContainer(containerId);
        if (containerOpt.isEmpty()) {
            log.warn("Container not found: {}", containerId);
            return buildJsonErrorResponse("Container not found: " + containerId);
        }

        // Check connection limits
        WebSocketConnectionLimiter.AcquireResult limitResult =
                connectionLimiter.tryAcquire(connection.id(), authSuccess.subject(), containerId);

        if (limitResult instanceof WebSocketConnectionLimiter.AcquireResult.UserLimitExceeded exceeded) {
            log.warn("User '{}' exceeded connection limit: {}/{}",
                    authSuccess.subject(), exceeded.currentCount(), exceeded.maxAllowed());
            return buildJsonErrorResponse("Connection limit exceeded: max " + exceeded.maxAllowed() + " connections per user");
        }

        if (limitResult instanceof WebSocketConnectionLimiter.AcquireResult.ContainerLimitExceeded exceeded) {
            log.warn("Container {} exceeded connection limit: {}/{}",
                    containerId, exceeded.currentCount(), exceeded.maxAllowed());
            return buildJsonErrorResponse("Container connection limit exceeded: max " + exceeded.maxAllowed() + " connections");
        }

        metrics.connectionOpened();
        log.info("Command WebSocket opened for container {} by user '{}'", containerId, authSuccess.subject());
        return buildJsonConnectedResponse(containerId);
    }

    /**
     * Handle WebSocket connection close.
     */
    @OnClose
    public void onClose() {
        // Clean up connection limiter slot
        connectionLimiter.release(connection.id());
        // Clean up rate limiter bucket to prevent memory leaks
        rateLimiter.removeConnection(connection.id());
        metrics.connectionClosed();
        log.debug("Command WebSocket closed, cleaned up resources for connection {}", connection.id());
    }

    /**
     * Handle JSON text messages (for browser clients).
     */
    @OnTextMessage
    public String onTextMessage(String message) {
        // Check rate limiting
        if (!rateLimiter.tryAcquire(connection.id())) {
            log.warn("Rate limit exceeded for connection {}", connection.id());
            metrics.commandRateLimited();
            return buildJsonErrorResponse("Rate limit exceeded: max " + rateLimiter.getMaxCommandsPerSecond() + " commands per second");
        }

        // Get container
        ExecutionContainer container;
        try {
            container = getContainer();
        } catch (IllegalArgumentException e) {
            return buildJsonErrorResponse(e.getMessage());
        }

        long startTime = System.nanoTime();
        String commandName = null;
        long containerId = getContainerId();

        try {
            CommandPayloadConverter.JsonCommandRequest request = objectMapper.readValue(
                    message, CommandPayloadConverter.JsonCommandRequest.class);

            commandName = request.commandName();
            CommandPayload payload = payloadConverter.convert(request);

            container.commands()
                    .named(commandName)
                    .withParams(payload.getPayload())
                    .execute();

            long duration = System.nanoTime() - startTime;
            log.debug("Command '{}' enqueued in container {} via JSON", commandName, containerId);
            metrics.commandProcessed();
            telemetry.recordCommand(commandName, containerId, duration);
            return buildJsonSuccessResponse(commandName);

        } catch (Exception e) {
            log.warn("Failed to parse JSON command for container {}: {}", containerId, e.getMessage());
            metrics.commandError();
            if (commandName != null) {
                telemetry.recordError(commandName, containerId);
            }
            return buildJsonErrorResponse("Failed to parse command: " + e.getMessage());
        }
    }

    /**
     * Handle Protocol Buffer binary messages (for native clients).
     */
    @OnBinaryMessage
    public Buffer onBinaryMessage(Buffer message) {
        // Check rate limiting
        if (!rateLimiter.tryAcquire(connection.id())) {
            log.warn("Rate limit exceeded for connection {}", connection.id());
            metrics.commandRateLimited();
            return buildProtobufErrorResponse("Rate limit exceeded: max " + rateLimiter.getMaxCommandsPerSecond() + " commands per second");
        }

        // Get container
        ExecutionContainer container;
        try {
            container = getContainer();
        } catch (IllegalArgumentException e) {
            return buildProtobufErrorResponse(e.getMessage());
        }

        long startTime = System.nanoTime();
        String commandName = null;
        long containerId = getContainerId();

        try {
            byte[] bytes = message.getBytes();
            CommandProtos.CommandRequest request = CommandProtos.CommandRequest.parseFrom(bytes);

            commandName = request.getCommandName();
            CommandPayload payload = payloadConverter.convert(request);

            container.commands()
                    .named(commandName)
                    .withParams(payload.getPayload())
                    .execute();

            long duration = System.nanoTime() - startTime;
            log.debug("Command '{}' enqueued in container {}", commandName, containerId);
            metrics.commandProcessed();
            telemetry.recordCommand(commandName, containerId, duration);

            CommandProtos.CommandResponse response = CommandProtos.CommandResponse.newBuilder()
                    .setStatus(CommandProtos.CommandResponse.Status.ACCEPTED)
                    .setCommandName(commandName)
                    .setMessage("Command accepted")
                    .build();

            return Buffer.buffer(response.toByteArray());

        } catch (Exception e) {
            log.warn("Failed to parse protobuf command for container {}: {}", containerId, e.getMessage());
            metrics.commandError();
            if (commandName != null) {
                telemetry.recordError(commandName, containerId);
            }
            return buildProtobufErrorResponse("Failed to parse command: " + e.getMessage());
        }
    }

    // ---- Helper methods ----

    private long getContainerId() {
        return Long.parseLong(connection.pathParam("containerId"));
    }

    private ExecutionContainer getContainer() {
        String containerIdStr = connection.pathParam("containerId");
        long containerId;
        try {
            containerId = Long.parseLong(containerIdStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid container ID: " + containerIdStr);
        }

        return containerManager.getContainer(containerId)
                .orElseThrow(() -> new IllegalArgumentException("Container not found: " + containerId));
    }

    private String buildJsonConnectedResponse(long containerId) {
        try {
            return objectMapper.writeValueAsString(new JsonCommandResponse(
                    "ACCEPTED", "", "Connected to container " + containerId));
        } catch (Exception e) {
            log.error("Failed to serialize connected response", e);
            return "{\"status\":\"ACCEPTED\",\"commandName\":\"\",\"message\":\"Connected\"}";
        }
    }

    private String buildJsonSuccessResponse(String commandName) {
        try {
            return objectMapper.writeValueAsString(new JsonCommandResponse("ACCEPTED", commandName, "Command accepted"));
        } catch (Exception e) {
            log.error("Failed to serialize success response", e);
            return "{\"status\":\"ACCEPTED\",\"commandName\":\"\",\"message\":\"Command accepted\"}";
        }
    }

    private String buildJsonErrorResponse(String message) {
        try {
            return objectMapper.writeValueAsString(new JsonCommandResponse("ERROR", "", message));
        } catch (Exception e) {
            log.error("Failed to serialize error response", e);
            return "{\"status\":\"ERROR\",\"commandName\":\"\",\"message\":\"An error occurred\"}";
        }
    }

    private Buffer buildProtobufErrorResponse(String message) {
        CommandProtos.CommandResponse response = CommandProtos.CommandResponse.newBuilder()
                .setStatus(CommandProtos.CommandResponse.Status.ERROR)
                .setMessage(message)
                .build();
        return Buffer.buffer(response.toByteArray());
    }

    /**
     * JSON command response DTO for proper serialization.
     */
    private record JsonCommandResponse(String status, String commandName, String message) {}
}
