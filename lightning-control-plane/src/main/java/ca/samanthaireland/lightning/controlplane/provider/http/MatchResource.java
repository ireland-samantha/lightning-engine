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

package ca.samanthaireland.lightning.controlplane.provider.http;

import ca.samanthaireland.lightning.controlplane.provider.dto.CreateMatchRequest;
import ca.samanthaireland.lightning.controlplane.provider.dto.MatchResponse;
import ca.samanthaireland.lightning.controlplane.match.model.MatchRegistryEntry;
import ca.samanthaireland.lightning.controlplane.match.model.MatchStatus;
import ca.samanthaireland.lightning.controlplane.match.service.MatchRoutingService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

/**
 * REST resource for match routing operations.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/matches/create - Create a new match (scheduler picks node)</li>
 *   <li>GET /api/matches/{matchId} - Get match details with connection info</li>
 *   <li>GET /api/matches - List all matches</li>
 *   <li>DELETE /api/matches/{matchId} - Delete a match</li>
 *   <li>POST /api/matches/{matchId}/finish - Mark match as finished</li>
 * </ul>
 */
@Path("/api/matches")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MatchResource {
    private static final Logger log = LoggerFactory.getLogger(MatchResource.class);

    private final MatchRoutingService matchRoutingService;

    @Inject
    public MatchResource(MatchRoutingService matchRoutingService) {
        this.matchRoutingService = matchRoutingService;
    }

    /**
     * Creates a new match in the cluster.
     * The scheduler selects the best available node based on load and capacity.
     *
     * @param request the match creation request
     * @return 201 Created with match details and connection info
     */
    @POST
    @Path("/create")
    public Response create(@Valid CreateMatchRequest request) {
        log.info("Create match request: modules={}, preferredNode={}",
                request.moduleNames(), request.preferredNodeId());

        MatchRegistryEntry entry = matchRoutingService.createMatch(
                request.moduleNames(),
                request.preferredNodeId()
        );

        log.info("Match created: matchId={}, nodeId={}, websocketUrl={}",
                entry.matchId(), entry.nodeId(), entry.websocketUrl());

        return Response.created(URI.create("/api/matches/" + entry.matchId()))
                .entity(MatchResponse.from(entry))
                .build();
    }

    /**
     * Gets match details by ID, including connection information.
     *
     * @param matchId the cluster-unique match ID
     * @return the match details
     */
    @GET
    @Path("/{matchId}")
    public MatchResponse getById(@PathParam("matchId") String matchId) {
        MatchRegistryEntry entry = matchRoutingService.findById(matchId)
                .orElseThrow(() -> new ca.samanthaireland.lightning.controlplane.match.exception.MatchNotFoundException(matchId));

        return MatchResponse.from(entry);
    }

    /**
     * Lists all matches in the cluster.
     *
     * @param status optional status filter
     * @return list of all matches
     */
    @GET
    public List<MatchResponse> list(@QueryParam("status") MatchStatus status) {
        List<MatchRegistryEntry> entries;

        if (status != null) {
            entries = matchRoutingService.findByStatus(status);
        } else {
            entries = matchRoutingService.findAll();
        }

        return entries.stream()
                .map(MatchResponse::from)
                .toList();
    }

    /**
     * Deletes a match from the cluster.
     * This will also delete the match from the hosting node.
     *
     * @param matchId the match ID to delete
     * @return 204 No Content on success
     */
    @DELETE
    @Path("/{matchId}")
    public Response delete(@PathParam("matchId") String matchId) {
        log.info("Delete match request: matchId={}", matchId);

        matchRoutingService.deleteMatch(matchId);

        return Response.noContent().build();
    }

    /**
     * Marks a match as finished.
     * Finished matches remain in the registry but are no longer active.
     *
     * @param matchId the match ID to finish
     * @return the updated match
     */
    @POST
    @Path("/{matchId}/finish")
    public MatchResponse finish(@PathParam("matchId") String matchId) {
        log.info("Finish match request: matchId={}", matchId);

        matchRoutingService.finishMatch(matchId);

        return matchRoutingService.findById(matchId)
                .map(MatchResponse::from)
                .orElseThrow(() -> new ca.samanthaireland.lightning.controlplane.match.exception.MatchNotFoundException(matchId));
    }

    /**
     * Updates the player count for a match.
     *
     * @param matchId     the match ID
     * @param playerCount the new player count
     * @return the updated match
     */
    @PUT
    @Path("/{matchId}/players")
    public MatchResponse updatePlayerCount(
            @PathParam("matchId") String matchId,
            @QueryParam("count") int playerCount
    ) {
        log.debug("Update player count: matchId={}, count={}", matchId, playerCount);

        matchRoutingService.updatePlayerCount(matchId, playerCount);

        return matchRoutingService.findById(matchId)
                .map(MatchResponse::from)
                .orElseThrow(() -> new ca.samanthaireland.lightning.controlplane.match.exception.MatchNotFoundException(matchId));
    }
}
