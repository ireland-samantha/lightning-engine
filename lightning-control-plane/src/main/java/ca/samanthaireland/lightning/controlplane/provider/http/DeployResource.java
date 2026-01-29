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

import ca.samanthaireland.lightning.controlplane.match.model.MatchRegistryEntry;
import ca.samanthaireland.lightning.controlplane.match.service.MatchRoutingService;
import ca.samanthaireland.lightning.controlplane.provider.dto.DeployRequest;
import ca.samanthaireland.lightning.controlplane.provider.dto.DeployResponse;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * REST resource for the deployment API (v1).
 * This is the primary CLI-facing endpoint for deploying games to the cluster.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/v1/deploy - Deploy a new game match</li>
 *   <li>GET /api/v1/deploy/{matchId} - Get deployment status</li>
 *   <li>DELETE /api/v1/deploy/{matchId} - Undeploy a match</li>
 * </ul>
 */
@Path("/api/v1/deploy")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeployResource {
    private static final Logger log = LoggerFactory.getLogger(DeployResource.class);

    private final MatchRoutingService matchRoutingService;

    @Inject
    public DeployResource(MatchRoutingService matchRoutingService) {
        this.matchRoutingService = matchRoutingService;
    }

    /**
     * Deploys a new game match to the cluster.
     * <p>
     * The deployment process:
     * <ol>
     *   <li>Selects the best available node based on load and capacity</li>
     *   <li>Creates a container on the selected node</li>
     *   <li>Starts the container (if autoStart is true)</li>
     *   <li>Creates a match with the specified modules</li>
     *   <li>Returns connection endpoints</li>
     * </ol>
     *
     * @param request the deployment request
     * @return 201 Created with deployment details and connection endpoints
     */
    @POST
    public Response deploy(@Valid DeployRequest request) {
        log.info("Deploy request: modules={}, preferredNode={}, autoStart={}",
                request.modules(), request.preferredNodeId(), request.isAutoStart());

        MatchRegistryEntry entry = matchRoutingService.createMatch(
                request.modules(),
                request.preferredNodeId()
        );

        DeployResponse response = DeployResponse.from(entry);

        log.info("Deployment successful: matchId={}, nodeId={}, websocket={}",
                entry.matchId(), entry.nodeId(), response.endpoints().websocket());

        return Response.created(URI.create("/api/v1/deploy/" + entry.matchId()))
                .entity(response)
                .build();
    }

    /**
     * Gets the status of a deployed match.
     *
     * @param matchId the match ID
     * @return the deployment status
     */
    @GET
    @Path("/{matchId}")
    public DeployResponse getStatus(@PathParam("matchId") String matchId) {
        log.debug("Get deployment status: matchId={}", matchId);

        MatchRegistryEntry entry = matchRoutingService.findById(matchId)
                .orElseThrow(() -> new ca.samanthaireland.lightning.controlplane.match.exception.MatchNotFoundException(matchId));

        return DeployResponse.from(entry);
    }

    /**
     * Undeploys a match from the cluster.
     * This stops the container and removes the match.
     *
     * @param matchId the match ID to undeploy
     * @return 204 No Content on success
     */
    @DELETE
    @Path("/{matchId}")
    public Response undeploy(@PathParam("matchId") String matchId) {
        log.info("Undeploy request: matchId={}", matchId);

        matchRoutingService.deleteMatch(matchId);

        return Response.noContent().build();
    }
}
