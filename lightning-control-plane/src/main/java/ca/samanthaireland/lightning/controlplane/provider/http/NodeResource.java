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

import ca.samanthaireland.lightning.controlplane.config.ControlPlaneConfiguration;
import ca.samanthaireland.lightning.controlplane.provider.dto.HeartbeatRequest;
import ca.samanthaireland.lightning.controlplane.provider.dto.NodeRegistrationRequest;
import ca.samanthaireland.lightning.controlplane.provider.dto.NodeResponse;
import ca.samanthaireland.lightning.controlplane.node.exception.NodeAuthenticationException;
import ca.samanthaireland.lightning.controlplane.node.model.Node;
import ca.samanthaireland.lightning.controlplane.node.service.NodeRegistryService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * REST resource for node registration and heartbeat operations.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/nodes/register - Register a new node</li>
 *   <li>PUT /api/nodes/{nodeId}/heartbeat - Send heartbeat</li>
 *   <li>POST /api/nodes/{nodeId}/drain - Mark node as draining</li>
 *   <li>DELETE /api/nodes/{nodeId} - Deregister node</li>
 * </ul>
 */
@Path("/api/nodes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NodeResource {
    private static final Logger log = LoggerFactory.getLogger(NodeResource.class);
    private static final String AUTH_HEADER = "X-Control-Plane-Token";

    private final NodeRegistryService nodeRegistryService;
    private final ControlPlaneConfiguration config;

    @Inject
    public NodeResource(NodeRegistryService nodeRegistryService, ControlPlaneConfiguration config) {
        this.nodeRegistryService = nodeRegistryService;
        this.config = config;
    }

    /**
     * Register a new node or re-register an existing node.
     * This operation is idempotent.
     */
    @POST
    @Path("/register")
    public Response register(
            @HeaderParam(AUTH_HEADER) String authToken,
            @Valid NodeRegistrationRequest request
    ) {
        validateAuth(authToken);

        log.info("Node registration request: nodeId={}, address={}",
                request.nodeId(), request.advertiseAddress());

        Node node = nodeRegistryService.register(
                request.nodeId(),
                request.advertiseAddress(),
                request.capacity().toModel()
        );

        return Response.created(URI.create("/api/cluster/nodes/" + node.nodeId()))
                .entity(NodeResponse.from(node))
                .build();
    }

    /**
     * Process a heartbeat from a node, refreshing its TTL and updating metrics.
     */
    @PUT
    @Path("/{nodeId}/heartbeat")
    public Response heartbeat(
            @HeaderParam(AUTH_HEADER) String authToken,
            @PathParam("nodeId") String nodeId,
            @Valid HeartbeatRequest request
    ) {
        validateAuth(authToken);

        Node node = nodeRegistryService.heartbeat(nodeId, request.metrics().toModel());

        return Response.ok(NodeResponse.from(node)).build();
    }

    /**
     * Mark a node as draining. It will no longer accept new containers.
     */
    @POST
    @Path("/{nodeId}/drain")
    public Response drain(
            @HeaderParam(AUTH_HEADER) String authToken,
            @PathParam("nodeId") String nodeId
    ) {
        validateAuth(authToken);

        log.info("Drain request for node: {}", nodeId);

        Node node = nodeRegistryService.drain(nodeId);

        return Response.ok(NodeResponse.from(node)).build();
    }

    /**
     * Deregister a node from the cluster.
     */
    @DELETE
    @Path("/{nodeId}")
    public Response deregister(
            @HeaderParam(AUTH_HEADER) String authToken,
            @PathParam("nodeId") String nodeId
    ) {
        validateAuth(authToken);

        log.info("Deregister request for node: {}", nodeId);

        nodeRegistryService.deregister(nodeId);

        return Response.noContent().build();
    }

    /**
     * Validates the authentication token if authentication is required.
     */
    private void validateAuth(String providedToken) {
        if (!config.requireAuth()) {
            return;
        }

        String expectedToken = config.authToken().orElse(null);

        if (expectedToken == null || expectedToken.isBlank()) {
            throw new NodeAuthenticationException("Authentication required but no token configured on server");
        }

        if (providedToken == null || providedToken.isBlank()) {
            throw new NodeAuthenticationException("Missing authentication token");
        }

        if (!expectedToken.equals(providedToken)) {
            throw new NodeAuthenticationException("Invalid authentication token");
        }
    }
}
