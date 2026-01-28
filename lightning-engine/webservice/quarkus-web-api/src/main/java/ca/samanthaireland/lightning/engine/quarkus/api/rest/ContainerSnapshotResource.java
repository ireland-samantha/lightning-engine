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

package ca.samanthaireland.lightning.engine.quarkus.api.rest;

import ca.samanthaireland.lightning.engine.core.container.ContainerManager;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.core.snapshot.DeltaCompressionService;
import ca.samanthaireland.lightning.engine.core.snapshot.DeltaSnapshot;
import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;
import ca.samanthaireland.lightning.engine.core.snapshot.SnapshotHistory;
import ca.samanthaireland.lightning.engine.internal.core.snapshot.SnapshotProvider;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.DeltaSnapshotResponse;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.SnapshotResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ca.samanthaireland.lightning.engine.quarkus.api.rest.ContainerResourceSupport.getContainerOrThrow;

/**
 * REST resource for snapshot management within a container.
 *
 * <p>Handles snapshot retrieval, delta compression, recording, and in-memory history.
 */
@Path("/api/containers/{containerId}/matches/{matchId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContainerSnapshotResource {

    @Inject
    ContainerManager containerManager;

    @Inject
    SnapshotProvider snapshotProvider;

    @Inject
    SnapshotHistory snapshotHistory;

    @Inject
    DeltaCompressionService deltaCompressionService;

    /**
     * Get a snapshot for a match in a container.
     *
     * <p>Returns the current state of all entities in the match with their component data.
     * Optionally filter by player to get only entities owned by that player.
     */
    @GET
    @Path("/snapshot")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getMatchSnapshot(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId,
            @QueryParam("playerId") Long playerId) {

        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        // Get container-scoped snapshot
        if (container.snapshots() == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Container not started - snapshots unavailable"))
                    .build();
        }

        Snapshot snapshot = playerId != null
                ? container.snapshots().forMatchAndPlayer(matchId, playerId)
                : container.snapshots().forMatch(matchId);

        return Response.ok(SnapshotResponse.from(
                matchId,
                container.ticks().current(),
                snapshot
        )).build();
    }

    /**
     * Get a delta snapshot between two ticks for a match.
     *
     * <p>If toTick is not specified, returns delta from fromTick to current state.
     * If fromTick is not in history, returns 404.
     */
    @GET
    @Path("/snapshots/delta")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getDeltaSnapshot(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId,
            @QueryParam("fromTick") Long fromTick,
            @QueryParam("toTick") Long toTick) {

        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        // fromTick is required
        if (fromTick == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "fromTick query parameter is required"))
                    .build();
        }

        // Get the from snapshot from history
        Optional<Snapshot> fromSnapshotOpt = snapshotHistory.getSnapshot(matchId, fromTick);
        if (fromSnapshotOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error", "Snapshot not found for tick: " + fromTick,
                            "hint", "Use POST .../snapshots/record to record snapshots"))
                    .build();
        }

        // Determine the target tick and get/create the snapshot
        long targetTick;
        Snapshot toSnapshot;

        if (toTick != null) {
            targetTick = toTick;
            Optional<Snapshot> toSnapshotOpt = snapshotHistory.getSnapshot(matchId, toTick);
            if (toSnapshotOpt.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Snapshot not found for tick: " + toTick))
                        .build();
            }
            toSnapshot = toSnapshotOpt.get();
        } else {
            // Use current state
            targetTick = container.ticks().current();
            toSnapshot = snapshotProvider.createForMatch(matchId);
        }

        // Compute delta
        Snapshot fromSnapshot = fromSnapshotOpt.get();
        DeltaSnapshot delta = deltaCompressionService.computeDelta(
                matchId, fromTick, fromSnapshot, targetTick, toSnapshot);

        // Calculate compression ratio
        double compressionRatio = calculateCompressionRatio(toSnapshot, delta);

        DeltaSnapshotResponse response = new DeltaSnapshotResponse(
                delta.matchId(),
                delta.fromTick(),
                delta.toTick(),
                delta.changedComponents(),
                delta.addedEntities(),
                delta.removedEntities(),
                delta.changeCount(),
                compressionRatio
        );

        return Response.ok(response).build();
    }

    /**
     * Record the current snapshot for a match at the current tick.
     */
    @POST
    @Path("/snapshots/record")
    @RolesAllowed({"admin", "command_manager"})
    public Response recordSnapshot(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId) {

        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        long tick = container.ticks().current();
        Snapshot snapshot = snapshotProvider.createForMatch(matchId);
        snapshotHistory.recordSnapshot(matchId, tick, snapshot);

        return Response.ok(Map.of(
                "matchId", matchId,
                "tick", tick,
                "recorded", true,
                "historySize", snapshotHistory.getSnapshotCount(matchId)
        )).build();
    }

    /**
     * Get snapshot history info for a match.
     */
    @GET
    @Path("/snapshots/history-info")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getSnapshotHistoryInfo(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId) {

        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        var oldest = snapshotHistory.getOldestSnapshot(matchId);
        var latest = snapshotHistory.getLatestSnapshot(matchId);
        int count = snapshotHistory.getSnapshotCount(matchId);

        return Response.ok(Map.of(
                "matchId", matchId,
                "snapshotCount", count,
                "oldestTick", oldest.map(s -> s.tick()).orElse(-1L),
                "latestTick", latest.map(s -> s.tick()).orElse(-1L),
                "currentTick", container.ticks().current()
        )).build();
    }

    /**
     * Clear the snapshot history for a match.
     */
    @DELETE
    @Path("/snapshots/history")
    @RolesAllowed("admin")
    public Response clearSnapshotHistory(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId) {

        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        snapshotHistory.clearHistory(matchId);
        return Response.ok(Map.of(
                "matchId", matchId,
                "cleared", true
        )).build();
    }

    /**
     * Calculate the compression ratio of the delta vs full snapshot.
     * Lower values indicate better compression.
     */
    private double calculateCompressionRatio(Snapshot to, DeltaSnapshot delta) {
        // Estimate full snapshot size by counting all values
        int fullSnapshotSize = countValues(to);
        if (fullSnapshotSize == 0) {
            return 1.0;
        }

        // Delta size is the number of changed values plus entity changes
        int deltaSize = delta.changeCount()
                + (delta.addedEntities() != null ? delta.addedEntities().size() : 0)
                + (delta.removedEntities() != null ? delta.removedEntities().size() : 0);

        return (double) deltaSize / fullSnapshotSize;
    }

    private int countValues(Snapshot snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return 0;
        }
        return snapshot.modules().stream()
                .flatMap(moduleData -> moduleData.components().stream())
                .mapToInt(component -> component.values().size())
                .sum();
    }
}
