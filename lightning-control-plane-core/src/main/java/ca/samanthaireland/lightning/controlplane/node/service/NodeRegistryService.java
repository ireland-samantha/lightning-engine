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

import ca.samanthaireland.lightning.controlplane.node.model.Node;
import ca.samanthaireland.lightning.controlplane.node.model.NodeCapacity;
import ca.samanthaireland.lightning.controlplane.node.model.NodeMetrics;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing node registration and heartbeats.
 */
public interface NodeRegistryService {

    /**
     * Registers a new node or updates an existing node's registration.
     * This operation is idempotent - calling with the same nodeId will refresh the TTL.
     *
     * @param nodeId          unique identifier for the node
     * @param advertiseAddress URL where the node can be reached
     * @param capacity        capacity limits for the node
     * @return the registered node
     */
    Node register(String nodeId, String advertiseAddress, NodeCapacity capacity);

    /**
     * Processes a heartbeat from a node, refreshing its TTL and updating metrics.
     *
     * @param nodeId  the node ID
     * @param metrics current metrics from the node
     * @return the updated node
     * @throws ca.samanthaireland.lightning.controlplane.node.exception.NodeNotFoundException if node doesn't exist
     */
    Node heartbeat(String nodeId, NodeMetrics metrics);

    /**
     * Marks a node as draining. It will no longer accept new containers
     * but existing containers will continue running.
     *
     * @param nodeId the node ID
     * @return the updated node
     * @throws ca.samanthaireland.lightning.controlplane.node.exception.NodeNotFoundException if node doesn't exist
     */
    Node drain(String nodeId);

    /**
     * Deregisters a node from the cluster.
     *
     * @param nodeId the node ID
     * @throws ca.samanthaireland.lightning.controlplane.node.exception.NodeNotFoundException if node doesn't exist
     */
    void deregister(String nodeId);

    /**
     * Finds a node by its ID.
     *
     * @param nodeId the node ID
     * @return the node if found
     */
    Optional<Node> findById(String nodeId);

    /**
     * Returns all currently registered nodes.
     *
     * @return list of all nodes
     */
    List<Node> findAll();
}
