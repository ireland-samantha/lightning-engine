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

package ca.samanthaireland.lightning.controlplane.provider.dto;

import ca.samanthaireland.lightning.controlplane.node.model.Node;
import ca.samanthaireland.lightning.controlplane.node.model.NodeStatus;

import java.time.Instant;

/**
 * Response containing node information.
 *
 * @param nodeId           unique identifier for this node
 * @param advertiseAddress URL where this node can be reached
 * @param status           current status (HEALTHY or DRAINING)
 * @param capacity         capacity limits for this node
 * @param metrics          current metrics from the node
 * @param registeredAt     when the node first registered
 * @param lastHeartbeat    when the node last sent a heartbeat
 * @param availableCapacity number of available container slots
 */
public record NodeResponse(
        String nodeId,
        String advertiseAddress,
        NodeStatus status,
        NodeCapacityDto capacity,
        NodeMetricsDto metrics,
        Instant registeredAt,
        Instant lastHeartbeat,
        int availableCapacity
) {

    /**
     * Creates a response from a domain model.
     *
     * @param node the domain model
     * @return the response DTO
     */
    public static NodeResponse from(Node node) {
        return new NodeResponse(
                node.nodeId(),
                node.advertiseAddress(),
                node.status(),
                NodeCapacityDto.from(node.capacity()),
                NodeMetricsDto.from(node.metrics()),
                node.registeredAt(),
                node.lastHeartbeat(),
                node.availableCapacity()
        );
    }
}
