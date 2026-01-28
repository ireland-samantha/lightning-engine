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

package ca.samanthaireland.lightning.controlplane.cluster.service;

import ca.samanthaireland.lightning.controlplane.cluster.model.ClusterStatus;
import ca.samanthaireland.lightning.controlplane.node.model.Node;
import ca.samanthaireland.lightning.controlplane.node.model.NodeStatus;
import ca.samanthaireland.lightning.controlplane.node.service.NodeRegistryService;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of ClusterService.
 *
 * <p>This is a pure domain implementation with no framework dependencies.
 * Dependencies are provided via constructor injection.
 */
public class ClusterServiceImpl implements ClusterService {

    private final NodeRegistryService nodeRegistryService;

    /**
     * Creates a new ClusterServiceImpl.
     *
     * @param nodeRegistryService the node registry service
     */
    public ClusterServiceImpl(NodeRegistryService nodeRegistryService) {
        this.nodeRegistryService = nodeRegistryService;
    }

    @Override
    public List<Node> getAllNodes() {
        return nodeRegistryService.findAll();
    }

    @Override
    public Optional<Node> getNode(String nodeId) {
        return nodeRegistryService.findById(nodeId);
    }

    @Override
    public ClusterStatus getClusterStatus() {
        List<Node> nodes = nodeRegistryService.findAll();

        int totalNodes = nodes.size();
        int healthyNodes = 0;
        int drainingNodes = 0;
        int totalContainers = 0;
        int totalMatches = 0;
        int totalCapacity = 0;
        int availableCapacity = 0;

        for (Node node : nodes) {
            if (node.status() == NodeStatus.HEALTHY) {
                healthyNodes++;
                availableCapacity += node.availableCapacity();
            } else if (node.status() == NodeStatus.DRAINING) {
                drainingNodes++;
            }

            totalContainers += node.metrics().containerCount();
            totalMatches += node.metrics().matchCount();
            totalCapacity += node.capacity().maxContainers();
        }

        return new ClusterStatus(
                totalNodes,
                healthyNodes,
                drainingNodes,
                totalContainers,
                totalMatches,
                totalCapacity,
                availableCapacity
        );
    }
}
