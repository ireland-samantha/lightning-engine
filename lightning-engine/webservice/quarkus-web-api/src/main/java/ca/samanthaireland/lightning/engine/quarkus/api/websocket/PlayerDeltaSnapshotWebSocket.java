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
import ca.samanthaireland.lightning.engine.core.snapshot.DeltaCompressionService;
import ca.samanthaireland.lightning.engine.core.snapshot.DeltaSnapshot;
import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.DeltaSnapshotResponse;
import io.quarkus.websockets.next.*;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for streaming container-scoped player-filtered delta snapshots.
 *
 * <p>Clients connect to /ws/containers/{containerId}/matches/{matchId}/players/{playerId}/delta
 * and receive periodic delta updates containing only changes to entities
 * owned by the specified player within the container.
 *
 * <p>The first message after connection will be a full snapshot (delta from empty state).
 * Subsequent messages will be deltas from the previous snapshot.
 *
 * <p>This enables efficient bandwidth usage by:
 * <ul>
 *   <li>Sending only player-owned entities</li>
 *   <li>Sending only changes between updates</li>
 * </ul>
 */
@WebSocket(path = "/ws/containers/{containerId}/matches/{matchId}/players/{playerId}/delta")
public class PlayerDeltaSnapshotWebSocket {
    private static final Logger log = LoggerFactory.getLogger(PlayerDeltaSnapshotWebSocket.class);

    @Inject
    ContainerManager containerManager;

    @Inject
    DeltaCompressionService deltaCompressionService;

    @ConfigProperty(name = "simulation.snapshot.broadcast-interval-ms", defaultValue = "100")
    long broadcastIntervalMs;

    private final Map<String, ConnectionState> connectionStates = new ConcurrentHashMap<>();

    @OnOpen
    public Multi<DeltaSnapshotResponse> onOpen(
            WebSocketConnection connection,
            @PathParam String containerId,
            @PathParam String matchId,
            @PathParam String playerId) {

        long cId = parseLong(containerId, "container ID");
        long mId = parseLong(matchId, "match ID");
        long pId = parseLong(playerId, "player ID");
        String connectionId = connection.id();

        log.info("Player {} connected to delta snapshot stream for container {} match {} (connection: {})",
                pId, cId, mId, connectionId);

        connectionStates.put(connectionId, ConnectionState.initial(cId, mId, pId));

        return Multi.createFrom()
                .ticks()
                .every(Duration.ofMillis(broadcastIntervalMs))
                .map(tick -> createDeltaResponse(connectionId));
    }

    @OnClose
    public void onClose(
            WebSocketConnection connection,
            @PathParam String containerId,
            @PathParam String matchId,
            @PathParam String playerId) {

        String connectionId = connection.id();
        connectionStates.remove(connectionId);

        log.info("Player {} disconnected from delta snapshot stream for container {} match {}",
                playerId, containerId, matchId);
    }

    @OnTextMessage
    public DeltaSnapshotResponse onMessage(
            String message,
            WebSocketConnection connection,
            @PathParam String containerId,
            @PathParam String matchId,
            @PathParam String playerId) {

        String connectionId = connection.id();
        long cId = parseLong(containerId, "container ID");
        long mId = parseLong(matchId, "match ID");
        long pId = parseLong(playerId, "player ID");

        if ("reset".equalsIgnoreCase(message.trim())) {
            connectionStates.put(connectionId, ConnectionState.initial(cId, mId, pId));
            log.debug("Reset delta state for player {} in container {} match {}", pId, cId, mId);
        }

        return createDeltaResponse(connectionId);
    }

    @OnError
    public void onError(
            WebSocketConnection connection,
            @PathParam String containerId,
            @PathParam String matchId,
            @PathParam String playerId,
            Throwable error) {

        log.error("Error in delta snapshot stream for player {} in container {} match {}: {}",
                playerId, containerId, matchId, error.getMessage());
    }

    private DeltaSnapshotResponse createDeltaResponse(String connectionId) {
        ConnectionState state = connectionStates.get(connectionId);
        if (state == null) {
            return createEmptyDelta();
        }

        return containerManager.getContainer(state.containerId())
                .filter(container -> container.snapshots() != null)
                .map(container -> computeDeltaForConnection(container, state, connectionId))
                .orElse(createEmptyDelta());
    }

    private DeltaSnapshotResponse computeDeltaForConnection(ExecutionContainer container, ConnectionState state, String connectionId) {
        long currentTick = container.ticks().current();
        Snapshot currentSnapshot = container.snapshots().forMatchAndPlayer(state.matchId(), state.playerId());

        DeltaSnapshot delta = computeDelta(state, currentSnapshot, currentTick);

        connectionStates.put(connectionId, state.withSnapshot(currentSnapshot, currentTick));

        return toDeltaResponse(delta, state.lastSnapshot(), currentSnapshot);
    }

    private DeltaSnapshot computeDelta(ConnectionState state, Snapshot currentSnapshot, long currentTick) {
        if (state.isFirstMessage()) {
            return deltaCompressionService.computeDelta(
                    state.matchId(),
                    0,
                    Snapshot.empty(),
                    currentTick,
                    currentSnapshot
            );
        }

        if (state.lastTick() == currentTick) {
            return createNoChangeDelta(state.matchId(), currentTick);
        }

        return deltaCompressionService.computeDelta(
                state.matchId(),
                state.lastTick(),
                state.lastSnapshot(),
                currentTick,
                currentSnapshot
        );
    }

    private DeltaSnapshot createNoChangeDelta(long matchId, long tick) {
        return new DeltaSnapshot(matchId, tick, tick, Map.of(), Set.of(), Set.of());
    }

    private DeltaSnapshotResponse createEmptyDelta() {
        return new DeltaSnapshotResponse(0, 0, 0, Map.of(), Set.of(), Set.of(), 0, 1.0);
    }

    private DeltaSnapshotResponse toDeltaResponse(
            DeltaSnapshot delta,
            Snapshot previousSnapshot,
            Snapshot currentSnapshot) {

        return new DeltaSnapshotResponse(
                delta.matchId(),
                delta.fromTick(),
                delta.toTick(),
                delta.changedComponents(),
                delta.addedEntities(),
                delta.removedEntities(),
                delta.changeCount(),
                calculateCompressionRatio(previousSnapshot, currentSnapshot, delta)
        );
    }

    private double calculateCompressionRatio(Snapshot from, Snapshot to, DeltaSnapshot delta) {
        if (to == null || to.isEmpty()) {
            return 1.0;
        }

        int fullSnapshotSize = to.modules().stream()
                .flatMap(moduleData -> moduleData.components().stream())
                .mapToInt(component -> component.values().size())
                .sum();

        if (fullSnapshotSize == 0) {
            return 1.0;
        }

        int deltaSize = delta.changeCount()
                + (delta.addedEntities() != null ? delta.addedEntities().size() : 0)
                + (delta.removedEntities() != null ? delta.removedEntities().size() : 0);

        return (double) deltaSize / fullSnapshotSize;
    }

    private long parseLong(String value, String fieldName) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + fieldName + ": " + value);
        }
    }

    /**
     * Tracks the connection state including container, player context, and last snapshot.
     */
    private record ConnectionState(
            long containerId,
            long matchId,
            long playerId,
            Snapshot lastSnapshot,
            long lastTick) {

        static ConnectionState initial(long containerId, long matchId, long playerId) {
            return new ConnectionState(containerId, matchId, playerId, null, -1);
        }

        boolean isFirstMessage() {
            return lastSnapshot == null;
        }

        ConnectionState withSnapshot(Snapshot snapshot, long tick) {
            return new ConnectionState(containerId, matchId, playerId, snapshot, tick);
        }
    }
}
