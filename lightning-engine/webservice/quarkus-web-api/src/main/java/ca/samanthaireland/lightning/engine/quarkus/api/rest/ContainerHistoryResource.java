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
import ca.samanthaireland.lightning.engine.quarkus.api.persistence.SnapshotDocument;
import ca.samanthaireland.lightning.engine.quarkus.api.persistence.SnapshotHistoryService;
import ca.samanthaireland.lightning.engine.quarkus.api.persistence.SnapshotPersistenceConfig;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ca.samanthaireland.lightning.engine.quarkus.api.rest.ContainerResourceSupport.*;

/**
 * REST resource for MongoDB persisted snapshot history.
 *
 * <p>Handles retrieval and deletion of persisted snapshots from MongoDB.
 */
@Path("/api/containers/{containerId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContainerHistoryResource {

    @Inject
    ContainerManager containerManager;

    @Inject
    SnapshotHistoryService historyService;

    @Inject
    SnapshotPersistenceConfig persistenceConfig;

    /**
     * Get container-scoped MongoDB history summary.
     */
    @GET
    @Path("/history")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getHistorySummary(@PathParam("containerId") long containerId) {
        getContainerOrThrow(containerManager, containerId); // Validate container exists

        if (!persistenceConfig.enabled()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of(
                            "error", "Snapshot persistence is not enabled",
                            "hint", "Set snapshot.persistence.enabled=true in configuration"))
                    .build();
        }

        SnapshotHistoryService.ContainerHistorySummary summary = historyService.getContainerSummary(containerId);
        return Response.ok(Map.of(
                "containerId", containerId,
                "totalSnapshots", summary.totalSnapshots(),
                "matchCount", summary.matchCount(),
                "matchIds", summary.matchIds(),
                "database", persistenceConfig.database(),
                "collection", persistenceConfig.collection(),
                "tickInterval", persistenceConfig.tickInterval()
        )).build();
    }

    /**
     * Get MongoDB history summary for a specific match in a container.
     */
    @GET
    @Path("/matches/{matchId}/history")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getMatchHistorySummary(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId) {

        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        if (!persistenceConfig.enabled()) {
            return persistenceNotEnabled();
        }

        SnapshotHistoryService.MatchHistorySummary summary = historyService.getMatchSummary(containerId, matchId);
        return Response.ok(Map.of(
                "containerId", containerId,
                "matchId", summary.matchId(),
                "snapshotCount", summary.snapshotCount(),
                "firstTick", summary.firstTick() != null ? summary.firstTick() : -1,
                "lastTick", summary.lastTick() != null ? summary.lastTick() : -1,
                "firstTimestamp", summary.firstTimestamp() != null ? summary.firstTimestamp().toString() : null,
                "lastTimestamp", summary.lastTimestamp() != null ? summary.lastTimestamp().toString() : null
        )).build();
    }

    /**
     * Get persisted snapshots for a match within a tick range.
     */
    @GET
    @Path("/matches/{matchId}/history/snapshots")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getHistorySnapshots(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId,
            @QueryParam("fromTick") @DefaultValue("0") long fromTick,
            @QueryParam("toTick") @DefaultValue("9223372036854775807") long toTick,
            @QueryParam("limit") @DefaultValue("100") int limit) {

        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        if (!persistenceConfig.enabled()) {
            return persistenceNotEnabled();
        }

        if (limit > 1000) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Limit cannot exceed 1000"))
                    .build();
        }

        List<SnapshotDocument> snapshots = historyService.getSnapshotsInRange(containerId, matchId, fromTick, toTick, limit);
        return Response.ok(Map.of(
                "containerId", containerId,
                "matchId", matchId,
                "fromTick", fromTick,
                "toTick", toTick,
                "count", snapshots.size(),
                "snapshots", snapshots.stream().map(ContainerResourceSupport::toHistoryDto).toList()
        )).build();
    }

    /**
     * Get the latest persisted snapshots for a match.
     */
    @GET
    @Path("/matches/{matchId}/history/snapshots/latest")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getLatestHistorySnapshots(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId,
            @QueryParam("limit") @DefaultValue("10") int limit) {

        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        if (!persistenceConfig.enabled()) {
            return persistenceNotEnabled();
        }

        if (limit > 100) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Limit cannot exceed 100"))
                    .build();
        }

        List<SnapshotDocument> snapshots = historyService.getLatestSnapshots(containerId, matchId, limit);
        return Response.ok(Map.of(
                "containerId", containerId,
                "matchId", matchId,
                "count", snapshots.size(),
                "snapshots", snapshots.stream().map(ContainerResourceSupport::toHistoryDto).toList()
        )).build();
    }

    /**
     * Get a specific persisted snapshot by tick.
     */
    @GET
    @Path("/matches/{matchId}/history/snapshots/{tick}")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getHistorySnapshot(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId,
            @PathParam("tick") long tick) {

        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        if (!persistenceConfig.enabled()) {
            return persistenceNotEnabled();
        }

        Optional<SnapshotDocument> snapshot = historyService.getSnapshot(containerId, matchId, tick);
        if (snapshot.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Snapshot not found for container " + containerId + " match " + matchId + " at tick " + tick))
                    .build();
        }

        return Response.ok(toHistoryDto(snapshot.get())).build();
    }

    /**
     * Delete all persisted snapshots for a match.
     */
    @DELETE
    @Path("/matches/{matchId}/history")
    @RolesAllowed("admin")
    public Response deleteMatchHistory(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId) {

        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        if (!persistenceConfig.enabled()) {
            return persistenceNotEnabled();
        }

        long deleted = historyService.deleteSnapshots(containerId, matchId);
        return Response.ok(Map.of(
                "containerId", containerId,
                "matchId", matchId,
                "deletedCount", deleted
        )).build();
    }

    /**
     * Delete persisted snapshots older than a specific tick.
     */
    @DELETE
    @Path("/matches/{matchId}/history/older-than/{tick}")
    @RolesAllowed("admin")
    public Response deleteOlderHistorySnapshots(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId,
            @PathParam("tick") long olderThanTick) {

        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        if (!persistenceConfig.enabled()) {
            return persistenceNotEnabled();
        }

        long deleted = historyService.deleteSnapshotsOlderThan(containerId, matchId, olderThanTick);
        return Response.ok(Map.of(
                "containerId", containerId,
                "matchId", matchId,
                "olderThanTick", olderThanTick,
                "deletedCount", deleted
        )).build();
    }
}
