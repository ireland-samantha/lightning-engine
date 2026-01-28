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

package ca.samanthaireland.lightning.controlplane.node.service;

import ca.samanthaireland.lightning.controlplane.config.ControlPlaneConfiguration;
import ca.samanthaireland.lightning.controlplane.node.exception.NodeNotFoundException;
import ca.samanthaireland.lightning.controlplane.node.model.Node;
import ca.samanthaireland.lightning.controlplane.node.model.NodeCapacity;
import ca.samanthaireland.lightning.controlplane.node.model.NodeMetrics;
import ca.samanthaireland.lightning.controlplane.node.repository.NodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of NodeRegistryService.
 *
 * <p>This is a pure domain implementation with no framework dependencies.
 * Dependencies are provided via constructor injection.
 */
public class NodeRegistryServiceImpl implements NodeRegistryService {
    private static final Logger log = LoggerFactory.getLogger(NodeRegistryServiceImpl.class);

    private final NodeRepository nodeRepository;
    private final ControlPlaneConfiguration config;

    /**
     * Creates a new NodeRegistryServiceImpl.
     *
     * @param nodeRepository the node repository
     * @param config         the control plane configuration
     */
    public NodeRegistryServiceImpl(NodeRepository nodeRepository, ControlPlaneConfiguration config) {
        this.nodeRepository = nodeRepository;
        this.config = config;
    }

    @Override
    public Node register(String nodeId, String advertiseAddress, NodeCapacity capacity) {
        // Check if node already exists (re-registration)
        Optional<Node> existing = nodeRepository.findById(nodeId);
        Node node;

        if (existing.isPresent()) {
            // Re-registration: preserve existing data but refresh TTL
            Node existingNode = existing.get();
            node = new Node(
                    nodeId,
                    advertiseAddress,
                    existingNode.status(),
                    capacity,
                    existingNode.metrics(),
                    existingNode.registeredAt(),
                    existingNode.lastHeartbeat()
            );
            log.info("Re-registering node {} at {}", nodeId, advertiseAddress);
        } else {
            // New registration
            node = Node.register(nodeId, advertiseAddress, capacity);
            log.info("Registered new node {} at {}", nodeId, advertiseAddress);
        }

        return nodeRepository.save(node, config.nodeTtlSeconds());
    }

    @Override
    public Node heartbeat(String nodeId, NodeMetrics metrics) {
        Node node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new NodeNotFoundException(nodeId));

        Node updated = node.withHeartbeat(metrics);
        nodeRepository.save(updated, config.nodeTtlSeconds());

        log.debug("Heartbeat from node {}: containers={}, matches={}",
                nodeId, metrics.containerCount(), metrics.matchCount());

        return updated;
    }

    @Override
    public Node drain(String nodeId) {
        Node node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new NodeNotFoundException(nodeId));

        Node drained = node.drain();
        nodeRepository.save(drained, config.nodeTtlSeconds());

        log.info("Node {} marked as draining", nodeId);

        return drained;
    }

    @Override
    public void deregister(String nodeId) {
        if (!nodeRepository.existsById(nodeId)) {
            throw new NodeNotFoundException(nodeId);
        }

        nodeRepository.deleteById(nodeId);
        log.info("Deregistered node {}", nodeId);
    }

    @Override
    public Optional<Node> findById(String nodeId) {
        return nodeRepository.findById(nodeId);
    }

    @Override
    public List<Node> findAll() {
        return nodeRepository.findAll();
    }
}
