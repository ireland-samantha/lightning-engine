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

import ca.samanthaireland.lightning.controlplane.cluster.model.ClusterStatus;
import ca.samanthaireland.lightning.controlplane.cluster.service.ClusterService;
import ca.samanthaireland.lightning.controlplane.provider.dto.NodeResponse;
import ca.samanthaireland.lightning.controlplane.node.exception.NodeNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * REST resource for cluster-wide operations.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET /api/cluster/nodes - List all nodes</li>
 *   <li>GET /api/cluster/nodes/{nodeId} - Get single node</li>
 *   <li>GET /api/cluster/status - Cluster health overview</li>
 * </ul>
 */
@Path("/api/cluster")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClusterResource {

    private final ClusterService clusterService;

    @Inject
    public ClusterResource(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    /**
     * List all nodes in the cluster.
     */
    @GET
    @Path("/nodes")
    public List<NodeResponse> getAllNodes() {
        return clusterService.getAllNodes().stream()
                .map(NodeResponse::from)
                .toList();
    }

    /**
     * Get a specific node by ID.
     */
    @GET
    @Path("/nodes/{nodeId}")
    public Response getNode(@PathParam("nodeId") String nodeId) {
        return clusterService.getNode(nodeId)
                .map(node -> Response.ok(NodeResponse.from(node)).build())
                .orElseThrow(() -> new NodeNotFoundException(nodeId));
    }

    /**
     * Get the overall cluster status.
     */
    @GET
    @Path("/status")
    public ClusterStatus getClusterStatus() {
        return clusterService.getClusterStatus();
    }
}
