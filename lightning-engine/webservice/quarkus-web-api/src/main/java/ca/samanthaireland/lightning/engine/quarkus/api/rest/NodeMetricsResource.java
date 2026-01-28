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

import ca.samanthaireland.lightning.engine.quarkus.api.controlplane.dto.NodeMetricsDto;
import ca.samanthaireland.lightning.engine.quarkus.api.controlplane.service.NodeRegistrationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * REST resource for node metrics.
 * Exposes the current node's metrics for monitoring and debugging.
 */
@Path("/api/node")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NodeMetricsResource {

    private final NodeRegistrationService nodeRegistrationService;

    @Inject
    public NodeMetricsResource(NodeRegistrationService nodeRegistrationService) {
        this.nodeRegistrationService = nodeRegistrationService;
    }

    /**
     * Get current metrics for this node.
     */
    @GET
    @Path("/metrics")
    public NodeMetricsDto getMetrics() {
        return nodeRegistrationService.getCurrentMetrics();
    }

    /**
     * Get information about this node's control plane registration status.
     */
    @GET
    @Path("/status")
    public NodeStatusResponse getStatus() {
        return new NodeStatusResponse(
                nodeRegistrationService.getNodeId(),
                nodeRegistrationService.isRegistered()
        );
    }

    /**
     * Response containing node status information.
     */
    public record NodeStatusResponse(String nodeId, boolean registered) {
    }
}
