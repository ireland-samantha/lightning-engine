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
import ca.samanthaireland.lightning.engine.core.snapshot.SnapshotRestoreService;
import ca.samanthaireland.lightning.engine.core.snapshot.SnapshotRestoreService.RestoreResult;
import ca.samanthaireland.lightning.engine.quarkus.api.config.RestoreConfig;
import ca.samanthaireland.lightning.engine.quarkus.api.persistence.SnapshotPersistenceConfig;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

import static ca.samanthaireland.lightning.engine.quarkus.api.rest.ContainerResourceSupport.getContainerOrThrow;

/**
 * REST resource for state restoration from persisted snapshots.
 *
 * <p>Handles restoring match state from MongoDB persisted snapshots.
 */
@Path("/api/containers/{containerId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContainerRestoreResource {

    @Inject
    ContainerManager containerManager;

    @Inject
    SnapshotPersistenceConfig persistenceConfig;

    @Inject
    SnapshotRestoreService restoreService;

    @Inject
    RestoreConfig restoreConfig;

    /**
     * Restore a match from its persisted snapshot.
     */
    @POST
    @Path("/matches/{matchId}/restore")
    @RolesAllowed("admin")
    public Response restoreMatch(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId,
            @QueryParam("tick") @DefaultValue("-1") long tick) {

        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        if (!persistenceConfig.enabled()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Snapshot persistence is not enabled"))
                    .build();
        }

        if (!restoreConfig.enabled()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Snapshot restoration is not enabled"))
                    .build();
        }

        RestoreResult result = restoreService.restoreMatch(matchId, tick);

        if (result.success()) {
            return Response.ok(result).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(result)
                    .build();
        }
    }

    /**
     * Restore all matches in a container from their persisted snapshots.
     */
    @POST
    @Path("/restore/all")
    @RolesAllowed("admin")
    public Response restoreAllMatches(@PathParam("containerId") long containerId) {
        getContainerOrThrow(containerManager, containerId); // Validate container exists

        if (!persistenceConfig.enabled()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Snapshot persistence is not enabled"))
                    .build();
        }

        if (!restoreConfig.enabled()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Snapshot restoration is not enabled"))
                    .build();
        }

        List<RestoreResult> results = restoreService.restoreAllMatches();

        long successCount = results.stream().filter(RestoreResult::success).count();
        long failedCount = results.stream().filter(r -> !r.success()).count();

        return Response.ok(Map.of(
                "total", results.size(),
                "success", successCount,
                "failed", failedCount,
                "results", results
        )).build();
    }

    /**
     * Check if a match can be restored.
     */
    @GET
    @Path("/matches/{matchId}/restore/available")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response canRestoreMatch(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId) {

        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        if (!persistenceConfig.enabled()) {
            return Response.ok(Map.of(
                    "matchId", matchId,
                    "canRestore", false,
                    "reason", "Snapshot persistence is not enabled"
            )).build();
        }

        boolean canRestore = restoreService.canRestore(matchId);
        return Response.ok(Map.of(
                "matchId", matchId,
                "canRestore", canRestore
        )).build();
    }

    /**
     * Get the current restoration configuration.
     */
    @GET
    @Path("/restore/config")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getRestoreConfig(@PathParam("containerId") long containerId) {
        getContainerOrThrow(containerManager, containerId); // Validate container exists

        return Response.ok(Map.of(
                "persistenceEnabled", persistenceConfig.enabled(),
                "restoreEnabled", restoreConfig.enabled(),
                "autoRestoreOnStartup", restoreConfig.autoRestoreOnStartup()
        )).build();
    }
}
