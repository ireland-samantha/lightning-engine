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
import ca.samanthaireland.lightning.engine.core.match.Match;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.MatchRequest;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.MatchResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static ca.samanthaireland.lightning.engine.quarkus.api.rest.ContainerResourceSupport.getContainerOrThrow;

/**
 * REST resource for match management within a container.
 *
 * <p>Handles match CRUD operations scoped to a specific container.
 */
@Path("/api/containers/{containerId}/matches")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContainerMatchResource {

    @Inject
    ContainerManager containerManager;

    /**
     * Get all matches in a container.
     */
    @GET
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public List<MatchResponse> getMatches(@PathParam("containerId") long containerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        return container.matches().all().stream()
                .map(MatchResponse::from)
                .toList();
    }

    /**
     * Create a match in a container.
     */
    @POST
    @RolesAllowed("admin")
    public Response createMatch(
            @PathParam("containerId") long containerId,
            MatchRequest request) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        // Default null lists to empty lists
        List<String> modules = request.enabledModuleNames() != null
                ? request.enabledModuleNames()
                : List.of();
        List<String> ais = request.enabledAINames() != null
                ? request.enabledAINames()
                : List.of();
        Match match = new Match(
                request.id(),
                containerId,
                modules,
                ais
        );
        Match created = container.matches().create(match);
        return Response.status(Response.Status.CREATED)
                .entity(MatchResponse.from(created))
                .build();
    }

    /**
     * Get a specific match in a container.
     */
    @GET
    @Path("/{matchId}")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getMatch(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        return container.matches().get(matchId)
                .map(m -> Response.ok(MatchResponse.from(m)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * Delete a match from a container.
     */
    @DELETE
    @Path("/{matchId}")
    @RolesAllowed("admin")
    public Response deleteMatch(
            @PathParam("containerId") long containerId,
            @PathParam("matchId") long matchId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        container.matches().delete(matchId);
        return Response.noContent().build();
    }
}
