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
import ca.samanthaireland.lightning.engine.core.container.ContainerPlayerOperations;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.core.match.Player;
import ca.samanthaireland.lightning.engine.core.session.PlayerSession;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.JoinMatchResponse;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.PlayerRequest;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.PlayerResponse;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.SessionResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static ca.samanthaireland.lightning.engine.quarkus.api.rest.ContainerResourceSupport.getContainerOrThrow;

/**
 * REST resource for player management within a container.
 *
 * <p>Handles player CRUD and player-match join/leave operations.
 */
@Path("/api/containers/{containerId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContainerPlayerResource {

    @Inject
    ContainerManager containerManager;

    // =========================================================================
    // PLAYER CRUD
    // =========================================================================

    /**
     * Create a player.
     */
    @POST
    @Path("/players")
    @RolesAllowed("admin")
    public Response createPlayer(
            @PathParam("containerId") long containerId,
            PlayerRequest request) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Auto-generate ID if not provided, or use specified ID
        Player player;
        if (request.id() != null && request.id() > 0) {
            player = container.players().create(request.id());
        } else {
            player = container.players().create();
        }

        return Response.status(Response.Status.CREATED)
                .entity(new PlayerResponse(player.id()))
                .build();
    }

    /**
     * Get a player by ID.
     */
    @GET
    @Path("/players/{playerId}")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getPlayer(
            @PathParam("containerId") long containerId,
            @PathParam("playerId") long playerId) {
        return getContainerOrThrow(containerManager, containerId).players().get(playerId)
                .map(player -> Response.ok(new PlayerResponse(player.id())).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * Get all players.
     */
    @GET
    @Path("/players")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public List<PlayerResponse> getAllPlayers(@PathParam("containerId") long containerId) {
        return getContainerOrThrow(containerManager, containerId).players().all().stream()
                .map(player -> new PlayerResponse(player.id()))
                .toList();
    }

    /**
     * Delete a player.
     */
    @DELETE
    @Path("/players/{playerId}")
    @RolesAllowed("admin")
    public Response deletePlayer(
            @PathParam("containerId") long containerId,
            @PathParam("playerId") long playerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        ContainerPlayerOperations players = container.players();

        if (players.get(playerId).isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        players.delete(playerId);
        return Response.noContent().build();
    }

    // =========================================================================
    // PLAYER-MATCH MANAGEMENT (using sessions)
    // =========================================================================

    /**
     * Join a player to a match in this container.
     * Creates a session for the player in the match.
     * Returns WebSocket and REST endpoint URLs for receiving player-scoped snapshots.
     */
    @POST
    @Path("/matches/{matchId}/players")
    @RolesAllowed("admin")
    public Response joinMatch(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId,
            JoinMatchRequest request) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        // Create a session for the player joining the match
        PlayerSession session = container.sessions().create(request.playerId(), matchId);
        JoinMatchResponse response = JoinMatchResponse.create(
                session.playerId(),
                session.matchId()
        );

        return Response.status(Response.Status.CREATED)
                .entity(response)
                .build();
    }

    /**
     * Get a player's participation in a match (via session).
     */
    @GET
    @Path("/matches/{matchId}/players/{playerId}")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getPlayerInMatch(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId,
            @PathParam("playerId") long playerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        // Use findActive to only return players currently "in" the match
        // (not abandoned or expired sessions)
        return container.sessions().findActive(playerId, matchId)
                .map(session -> Response.ok(SessionResponse.from(session)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * Get all players in a match (via sessions).
     */
    @GET
    @Path("/matches/{matchId}/players")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public List<SessionResponse> getPlayersInMatch(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        // Use activeForMatch to only return players currently "in" the match
        // (not abandoned or expired sessions)
        return container.sessions().activeForMatch(matchId).stream()
                .map(SessionResponse::from)
                .toList();
    }

    /**
     * Remove a player from a match (abandon session).
     */
    @DELETE
    @Path("/matches/{matchId}/players/{playerId}")
    @RolesAllowed("admin")
    public Response leaveMatch(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId,
            @PathParam("playerId") long playerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        // Verify match exists in this container
        container.matches().get(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " not found in container " + containerId));

        if (container.sessions().find(playerId, matchId).isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        container.sessions().abandon(playerId, matchId);
        return Response.noContent().build();
    }

    /**
     * Join match request DTO.
     */
    public record JoinMatchRequest(long playerId) {}
}
