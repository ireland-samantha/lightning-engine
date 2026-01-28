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

package ca.samanthaireland.lightning.controlplane.scheduler.service;

import ca.samanthaireland.lightning.controlplane.node.model.Node;
import ca.samanthaireland.lightning.controlplane.node.model.NodeStatus;
import ca.samanthaireland.lightning.controlplane.node.service.NodeRegistryService;
import ca.samanthaireland.lightning.controlplane.scheduler.exception.NoAvailableNodesException;
import ca.samanthaireland.lightning.controlplane.scheduler.exception.NoCapableNodesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of SchedulerService using a least-loaded selection algorithm.
 *
 * <p>This is a pure domain implementation with no framework dependencies.
 * Dependencies are provided via constructor injection.
 */
public class SchedulerServiceImpl implements SchedulerService {
    private static final Logger log = LoggerFactory.getLogger(SchedulerServiceImpl.class);

    private final NodeRegistryService nodeRegistryService;

    /**
     * Creates a new SchedulerServiceImpl.
     *
     * @param nodeRegistryService the node registry service
     */
    public SchedulerServiceImpl(NodeRegistryService nodeRegistryService) {
        this.nodeRegistryService = nodeRegistryService;
    }

    @Override
    public Node selectNodeForMatch(List<String> requiredModules, String preferredNodeId) {
        List<Node> healthyNodes = nodeRegistryService.findAll().stream()
                .filter(n -> n.status() == NodeStatus.HEALTHY)
                .toList();

        if (healthyNodes.isEmpty()) {
            log.warn("No healthy nodes available for scheduling");
            throw new NoAvailableNodesException();
        }

        // Filter nodes that have capacity
        List<Node> capableNodes = healthyNodes.stream()
                .filter(Node::canAcceptContainers)
                .toList();

        if (capableNodes.isEmpty()) {
            log.warn("No nodes with available capacity. Healthy nodes: {}", healthyNodes.size());
            throw new NoCapableNodesException(requiredModules);
        }

        // If preferred node is specified and available, use it
        if (preferredNodeId != null && !preferredNodeId.isBlank()) {
            Optional<Node> preferred = capableNodes.stream()
                    .filter(n -> n.nodeId().equals(preferredNodeId))
                    .findFirst();

            if (preferred.isPresent()) {
                log.debug("Using preferred node {} for match", preferredNodeId);
                return preferred.get();
            } else {
                log.debug("Preferred node {} not available, using least-loaded", preferredNodeId);
            }
        }

        // Select least-loaded node (lowest saturation)
        Node selected = capableNodes.stream()
                .min(Comparator.comparingDouble(this::calculateSaturation))
                .orElseThrow(() -> new NoCapableNodesException(requiredModules));

        log.debug("Selected node {} with saturation {:.2f}%",
                selected.nodeId(), calculateSaturation(selected) * 100);

        return selected;
    }

    @Override
    public double calculateSaturation(Node node) {
        int maxContainers = node.capacity().maxContainers();
        if (maxContainers == 0) {
            return 1.0; // Fully saturated if max is 0
        }
        int activeContainers = node.metrics().containerCount();
        return (double) activeContainers / maxContainers;
    }

    @Override
    public double getClusterSaturation() {
        List<Node> healthyNodes = nodeRegistryService.findAll().stream()
                .filter(n -> n.status() == NodeStatus.HEALTHY)
                .toList();

        if (healthyNodes.isEmpty()) {
            return 1.0; // Fully saturated if no nodes
        }

        int totalCapacity = healthyNodes.stream()
                .mapToInt(n -> n.capacity().maxContainers())
                .sum();

        if (totalCapacity == 0) {
            return 1.0;
        }

        int totalActive = healthyNodes.stream()
                .mapToInt(n -> n.metrics().containerCount())
                .sum();

        return (double) totalActive / totalCapacity;
    }
}
