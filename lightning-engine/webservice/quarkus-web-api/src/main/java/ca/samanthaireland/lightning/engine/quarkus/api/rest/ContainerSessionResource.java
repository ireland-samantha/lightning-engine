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
import ca.samanthaireland.lightning.engine.core.session.PlayerSession;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.SessionResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static ca.samanthaireland.lightning.engine.quarkus.api.rest.ContainerResourceSupport.getContainerOrThrow;

/**
 * REST resource for session management within a container.
 *
 * <p>Handles player session operations: connect, disconnect, reconnect, abandon.
 */
@Path("/api/containers/{containerId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContainerSessionResource {
    private static final Logger log = LoggerFactory.getLogger(ContainerSessionResource.class);

    @Inject
    ContainerManager containerManager;

    /**
     * Connect a player to a match in this container.
     * Creates or reactivates a session.
     */
    @POST
    @Path("/matches/{matchId}/sessions")
    @RolesAllowed({"admin", "command_manager"})
    public Response connectSession(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId,
            SessionConnectRequest request) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        // Create or reactivate session using container-scoped sessions
        PlayerSession session = container.sessions().create(request.playerId(), matchId);
        log.info("Created session for player {} in match {} (container {})", request.playerId(), matchId, containerId);

        return Response.status(Response.Status.CREATED)
                .entity(SessionResponse.from(session))
                .build();
    }

    /**
     * Get all sessions for a match in this container.
     */
    @GET
    @Path("/matches/{matchId}/sessions")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public List<SessionResponse> getMatchSessions(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        return container.sessions().forMatch(matchId).stream()
                .map(SessionResponse::from)
                .toList();
    }

    /**
     * Reconnect a player's session in a match.
     */
    @POST
    @Path("/matches/{matchId}/sessions/{playerId}/reconnect")
    @RolesAllowed({"admin", "command_manager"})
    public Response reconnectSession(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId,
            @PathParam("playerId") long playerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        PlayerSession session = container.sessions().reconnect(playerId, matchId);
        return Response.ok(SessionResponse.from(session)).build();
    }

    /**
     * Disconnect a player's session in a match.
     */
    @POST
    @Path("/matches/{matchId}/sessions/{playerId}/disconnect")
    @RolesAllowed({"admin", "command_manager"})
    public Response disconnectSession(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId,
            @PathParam("playerId") long playerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        container.sessions().disconnect(playerId, matchId);
        return Response.noContent().build();
    }

    /**
     * Abandon a player's session in a match.
     */
    @POST
    @Path("/matches/{matchId}/sessions/{playerId}/abandon")
    @RolesAllowed({"admin", "command_manager"})
    public Response abandonSession(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId,
            @PathParam("playerId") long playerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        container.sessions().abandon(playerId, matchId);
        return Response.noContent().build();
    }

    /**
     * Get a specific player's session in a match.
     */
    @GET
    @Path("/matches/{matchId}/sessions/{playerId}")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getSession(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId,
            @PathParam("playerId") long playerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        return container.sessions().find(playerId, matchId)
                .map(session -> Response.ok(SessionResponse.from(session)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * Check if a player can reconnect to a match.
     */
    @GET
    @Path("/matches/{matchId}/sessions/{playerId}/can-reconnect")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response canReconnect(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId,
            @PathParam("playerId") long playerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        boolean canReconnect = container.sessions().canReconnect(playerId, matchId);
        return Response.ok(Map.of("canReconnect", canReconnect)).build();
    }

    /**
     * Get active sessions for a match in this container.
     */
    @GET
    @Path("/matches/{matchId}/sessions/active")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public List<SessionResponse> getActiveMatchSessions(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        return container.sessions().activeForMatch(matchId).stream()
                .map(SessionResponse::from)
                .toList();
    }

    /**
     * Get all sessions across all matches in a container.
     */
    @GET
    @Path("/sessions")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public List<SessionResponse> getAllContainerSessions(@PathParam("containerId") long containerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Get all sessions in this container
        return container.sessions().all().stream()
                .map(SessionResponse::from)
                .toList();
    }

    /**
     * Session connect request DTO.
     */
    public record SessionConnectRequest(long playerId) {}
}
