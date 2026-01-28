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

import ca.samanthaireland.lightning.engine.core.error.ErrorBroadcaster;
import ca.samanthaireland.lightning.engine.core.error.GameError;
import io.quarkus.websockets.next.*;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for streaming game errors to players.
 *
 * <p>Clients connect to /ws/matches/{matchId}/players/{playerId}/errors
 * and receive real-time error notifications relevant to their match/player.
 *
 * <p>Errors include:
 * <ul>
 *   <li>Command execution failures</li>
 *   <li>System execution errors</li>
 *   <li>General game errors</li>
 * </ul>
 */
@WebSocket(path = "/ws/matches/{matchId}/players/{playerId}/errors")
public class PlayerErrorWebSocket {
    private static final Logger log = LoggerFactory.getLogger(PlayerErrorWebSocket.class);

    @Inject
    ErrorBroadcaster errorBroadcaster;

    private final Map<String, String> connectionSubscriptions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(
            WebSocketConnection connection,
            @PathParam String matchId,
            @PathParam String playerId) {

        long mId = parseMatchId(matchId);
        long pId = parsePlayerId(playerId);
        String connectionId = connection.id();

        log.info("Player {} connected to error stream for match {} (connection: {})",
                pId, mId, connectionId);

        // Subscribe to errors for this player
        String subscriptionId = errorBroadcaster.subscribeToPlayer(mId, pId, error -> {
            try {
                connection.sendText(toJson(error)).subscribe().asCompletionStage();
            } catch (Exception e) {
                log.error("Failed to send error to connection {}: {}", connectionId, e.getMessage());
            }
        });

        connectionSubscriptions.put(connectionId, subscriptionId);
    }

    @OnClose
    public void onClose(
            WebSocketConnection connection,
            @PathParam String matchId,
            @PathParam String playerId) {

        String connectionId = connection.id();
        String subscriptionId = connectionSubscriptions.remove(connectionId);

        if (subscriptionId != null) {
            errorBroadcaster.unsubscribe(subscriptionId);
        }

        log.info("Player {} disconnected from error stream for match {}",
                parsePlayerId(playerId), parseMatchId(matchId));
    }

    @OnError
    public void onError(
            WebSocketConnection connection,
            @PathParam String matchId,
            @PathParam String playerId,
            Throwable error) {

        log.error("Error in error stream for player {} in match {}: {}",
                parsePlayerId(playerId), parseMatchId(matchId), error.getMessage());
    }

    private String toJson(GameError error) {
        return String.format(
                "{\"id\":\"%s\",\"timestamp\":\"%s\",\"matchId\":%d,\"playerId\":%d," +
                "\"type\":\"%s\",\"source\":\"%s\",\"message\":\"%s\",\"details\":\"%s\"}",
                error.id(),
                error.timestamp().toString(),
                error.matchId(),
                error.playerId(),
                error.type().name(),
                escapeJson(error.source()),
                escapeJson(error.message()),
                escapeJson(error.details() != null ? error.details() : "")
        );
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private long parseMatchId(String matchId) {
        try {
            return Long.parseLong(matchId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid match ID: " + matchId);
        }
    }

    private long parsePlayerId(String playerId) {
        try {
            return Long.parseLong(playerId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid player ID: " + playerId);
        }
    }
}
