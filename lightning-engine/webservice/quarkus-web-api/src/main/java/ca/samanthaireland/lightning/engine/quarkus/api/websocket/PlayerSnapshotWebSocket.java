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

import ca.samanthaireland.lightning.engine.core.container.ContainerManager;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.SnapshotResponse;
import io.quarkus.websockets.next.*;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * WebSocket endpoint for streaming container-scoped player-filtered snapshots.
 *
 * <p>Clients connect to /ws/containers/{containerId}/matches/{matchId}/players/{playerId}/snapshot
 * and receive periodic snapshot updates containing only entities owned by
 * the specified player within the container.
 *
 * <p>This enables efficient client-side rendering by sending only the
 * data relevant to each connected player, reducing bandwidth and improving
 * security by not exposing other players' entity data.
 */
@WebSocket(path = "/ws/containers/{containerId}/matches/{matchId}/players/{playerId}/snapshot")
public class PlayerSnapshotWebSocket {
    private static final Logger log = LoggerFactory.getLogger(PlayerSnapshotWebSocket.class);

    @Inject
    ContainerManager containerManager;

    @ConfigProperty(name = "simulation.snapshot.broadcast-interval-ms", defaultValue = "100")
    long broadcastIntervalMs;

    @OnOpen
    public Multi<SnapshotResponse> onOpen(
            @PathParam String containerId,
            @PathParam String matchId,
            @PathParam String playerId) {

        long cId = parseLong(containerId, "container ID");
        long mId = parseLong(matchId, "match ID");
        long pId = parseLong(playerId, "player ID");

        log.info("Player {} connected to snapshot stream for container {} match {}", pId, cId, mId);

        return Multi.createFrom()
                .ticks()
                .every(Duration.ofMillis(broadcastIntervalMs))
                .map(tick -> createPlayerSnapshotResponse(cId, mId, pId));
    }

    @OnTextMessage
    public SnapshotResponse onMessage(
            String message,
            @PathParam String containerId,
            @PathParam String matchId,
            @PathParam String playerId) {

        long cId = parseLong(containerId, "container ID");
        long mId = parseLong(matchId, "match ID");
        long pId = parseLong(playerId, "player ID");

        log.debug("Received message from player {} in container {} match {}: {}", pId, cId, mId, message);

        return createPlayerSnapshotResponse(cId, mId, pId);
    }

    @OnClose
    public void onClose(
            @PathParam String containerId,
            @PathParam String matchId,
            @PathParam String playerId) {

        log.info("Player {} disconnected from snapshot stream for container {} match {}",
                playerId, containerId, matchId);
    }

    @OnError
    public void onError(
            @PathParam String containerId,
            @PathParam String matchId,
            @PathParam String playerId,
            Throwable error) {

        log.error("Error in snapshot stream for player {} in container {} match {}: {}",
                playerId, containerId, matchId, error.getMessage());
    }

    private SnapshotResponse createPlayerSnapshotResponse(long containerId, long matchId, long playerId) {
        return containerManager.getContainer(containerId)
                .filter(container -> container.snapshots() != null)
                .map(container -> {
                    Snapshot snapshot = container.snapshots().forMatchAndPlayer(matchId, playerId);
                    return SnapshotResponse.from(
                            matchId,
                            container.ticks().current(),
                            snapshot
                    );
                })
                .orElse(new SnapshotResponse(matchId, 0, List.of()));
    }

    private long parseLong(String value, String fieldName) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + fieldName + ": " + value);
        }
    }
}
