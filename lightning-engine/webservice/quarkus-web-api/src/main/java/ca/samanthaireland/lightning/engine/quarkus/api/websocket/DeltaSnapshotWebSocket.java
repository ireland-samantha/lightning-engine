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

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Multi;

import ca.samanthaireland.lightning.engine.core.container.ContainerManager;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.core.snapshot.DeltaCompressionService;
import ca.samanthaireland.lightning.engine.core.snapshot.DeltaSnapshot;
import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.DeltaSnapshotResponse;

/**
 * WebSocket endpoint for streaming container-scoped delta snapshots.
 *
 * <p>Clients connect to /ws/containers/{containerId}/matches/{matchId}/delta
 * and receive periodic delta updates. Each message contains only the changes
 * since the last update.
 *
 * <p>The first message after connection will be a full snapshot (delta from empty state).
 * Subsequent messages will be deltas from the previous snapshot.
 */
@WebSocket(path = "/ws/containers/{containerId}/matches/{matchId}/delta")
public class DeltaSnapshotWebSocket {
    private static final Logger log = LoggerFactory.getLogger(DeltaSnapshotWebSocket.class);

    @Inject
    ContainerManager containerManager;

    @Inject
    DeltaCompressionService deltaCompressionService;

    @Inject
    WebSocketAuthenticator authenticator;

    @Inject
    WebSocketMetrics metrics;

    @ConfigProperty(name = "simulation.snapshot.broadcast-interval-ms", defaultValue = "100")
    long broadcastIntervalMs;

    // Track the last snapshot sent to each connection
    private final Map<String, SnapshotState> connectionStates = new ConcurrentHashMap<>();

    @OnOpen
    public Multi<DeltaSnapshotResponse> onOpen(
            WebSocketConnection connection,
            @PathParam String containerId,
            @PathParam String matchId) {
        // Use shared authenticator with view roles (allows view_only users)
        WebSocketAuthenticator.AuthResult authResult =
                authenticator.authenticate(connection, WebSocketAuthenticator.VIEW_ROLES);

        if (authResult instanceof WebSocketAuthenticator.AuthResult.Failure failure) {
            metrics.authFailure();
            log.warn("Delta WebSocket auth failed: {}", failure.message());
            return Multi.createFrom().item(DeltaSnapshotResponse.error(failure.message()));
        }

        WebSocketAuthenticator.AuthResult.Success success =
                (WebSocketAuthenticator.AuthResult.Success) authResult;

        long cId = Long.parseLong(containerId);
        long mId = Long.parseLong(matchId);
        String connectionId = connection.id();

        metrics.connectionOpened();
        log.debug("Delta WebSocket opened for container {} match {} by user '{}' with connection {}",
                cId, mId, success.subject(), connectionId);

        // Initialize state for this connection
        connectionStates.put(connectionId, new SnapshotState(null, -1));

        return Multi.createFrom().ticks().every(Duration.ofMillis(broadcastIntervalMs))
                .map(tick -> createDeltaResponse(cId, mId, connectionId));
    }

    @OnClose
    public void onClose(
            WebSocketConnection connection,
            @PathParam String containerId,
            @PathParam String matchId) {
        String connectionId = connection.id();
        connectionStates.remove(connectionId);
        metrics.connectionClosed();
        log.debug("Delta WebSocket closed for container {} match {} with connection {}", containerId, matchId, connectionId);
    }

    @OnTextMessage
    public DeltaSnapshotResponse onMessage(
            String message,
            WebSocketConnection connection,
            @PathParam String containerId,
            @PathParam String matchId) {
        long cId = Long.parseLong(containerId);
        long mId = Long.parseLong(matchId);
        String connectionId = connection.id();

        // Handle special commands
        if ("reset".equalsIgnoreCase(message.trim())) {
            // Reset to send full snapshot on next tick
            connectionStates.put(connectionId, new SnapshotState(null, -1));
            log.debug("Reset delta state for connection {}", connectionId);
        }

        // Return current delta immediately
        return createDeltaResponse(cId, mId, connectionId);
    }

    private DeltaSnapshotResponse createDeltaResponse(long containerId, long matchId, String connectionId) {
        return containerManager.getContainer(containerId)
                .filter(container -> container.snapshots() != null)
                .map(container -> computeDeltaForConnection(container, matchId, connectionId))
                .orElse(new DeltaSnapshotResponse(matchId, 0, 0, Map.of(), Set.of(), Set.of(), 0, 1.0));
    }

    private DeltaSnapshotResponse computeDeltaForConnection(ExecutionContainer container, long matchId, String connectionId) {
        long currentTick = container.ticks().current();
        Snapshot currentSnapshot = container.snapshots().forMatch(matchId);

        SnapshotState state = connectionStates.get(connectionId);
        if (state == null) {
            state = new SnapshotState(null, -1);
            connectionStates.put(connectionId, state);
        }

        DeltaSnapshot delta;
        long fromTick;

        if (state.lastSnapshot == null) {
            // First message - send delta from empty state (effectively full snapshot)
            fromTick = 0;
            Snapshot emptySnapshot = Snapshot.empty();
            delta = deltaCompressionService.computeDelta(matchId, fromTick, emptySnapshot, currentTick, currentSnapshot);
        } else if (state.lastTick == currentTick) {
            // No change since last update
            delta = new DeltaSnapshot(matchId, currentTick, currentTick, Map.of(), Set.of(), Set.of());
            fromTick = currentTick;
        } else {
            // Normal delta from last snapshot
            fromTick = state.lastTick;
            delta = deltaCompressionService.computeDelta(matchId, fromTick, state.lastSnapshot, currentTick, currentSnapshot);
        }

        // Update state for next comparison
        connectionStates.put(connectionId, new SnapshotState(currentSnapshot, currentTick));

        return new DeltaSnapshotResponse(
                delta.matchId(),
                delta.fromTick(),
                delta.toTick(),
                delta.changedComponents(),
                delta.addedEntities(),
                delta.removedEntities(),
                delta.changeCount(),
                calculateCompressionRatio(state.lastSnapshot, currentSnapshot, delta)
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

    /**
     * Tracks the last snapshot state for a connection.
     */
    private record SnapshotState(Snapshot lastSnapshot, long lastTick) {}
}
