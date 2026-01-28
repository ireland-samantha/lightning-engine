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
import java.util.List;

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
import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.SnapshotResponse;

/**
 * WebSocket endpoint for streaming container-scoped match snapshots.
 *
 * <p>Clients connect to /ws/containers/{containerId}/matches/{matchId}/snapshot
 * and receive periodic snapshot updates for the specified match within the container.
 */
@WebSocket(path = "/ws/containers/{containerId}/matches/{matchId}/snapshot")
public class SnapshotWebSocket {
    private static final Logger log = LoggerFactory.getLogger(SnapshotWebSocket.class);

    @Inject
    ContainerManager containerManager;

    @Inject
    WebSocketConnection connection;

    @Inject
    WebSocketAuthenticator authenticator;

    @Inject
    WebSocketMetrics metrics;

    @ConfigProperty(name = "simulation.snapshot.broadcast-interval-ms", defaultValue = "100")
    long broadcastIntervalMs;

    @OnOpen
    public Multi<SnapshotResponse> onOpen(
            @PathParam String containerId,
            @PathParam String matchId) {
        // Use shared authenticator with view roles (allows view_only users)
        WebSocketAuthenticator.AuthResult authResult =
                authenticator.authenticate(connection, WebSocketAuthenticator.VIEW_ROLES);

        if (authResult instanceof WebSocketAuthenticator.AuthResult.Failure failure) {
            metrics.authFailure();
            log.warn("Snapshot WebSocket auth failed: {}", failure.message());
            return Multi.createFrom().item(SnapshotResponse.error(failure.message()));
        }

        WebSocketAuthenticator.AuthResult.Success success =
                (WebSocketAuthenticator.AuthResult.Success) authResult;

        long cId = Long.parseLong(containerId);
        long mId = Long.parseLong(matchId);
        metrics.connectionOpened();
        log.debug("Snapshot WebSocket opened for container {} match {} by user '{}'",
                cId, mId, success.subject());

        return Multi.createFrom().ticks().every(Duration.ofMillis(broadcastIntervalMs))
                .map(tick -> createSnapshotResponse(cId, mId));
    }

    @OnTextMessage
    public SnapshotResponse onMessage(
            String message,
            @PathParam String containerId,
            @PathParam String matchId) {
        long cId = Long.parseLong(containerId);
        long mId = Long.parseLong(matchId);
        return createSnapshotResponse(cId, mId);
    }

    @OnClose
    public void onClose(
            @PathParam String containerId,
            @PathParam String matchId) {
        metrics.connectionClosed();
        log.debug("Snapshot WebSocket closed for container {} match {}", containerId, matchId);
    }

    private SnapshotResponse createSnapshotResponse(long containerId, long matchId) {
        return containerManager.getContainer(containerId)
                .filter(container -> container.snapshots() != null)
                .map(container -> {
                    Snapshot snapshot = container.snapshots().forMatch(matchId);
                    return SnapshotResponse.from(
                            matchId,
                            container.ticks().current(),
                            snapshot
                    );
                })
                .orElse(new SnapshotResponse(matchId, 0, List.of()));
    }
}
