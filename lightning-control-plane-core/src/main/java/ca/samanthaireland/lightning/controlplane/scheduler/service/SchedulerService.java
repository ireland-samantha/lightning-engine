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

import java.util.List;

/**
 * Service for scheduling matches to nodes in the cluster.
 */
public interface SchedulerService {

    /**
     * Selects a node to host a new match.
     * Uses a least-loaded selection algorithm that considers:
     * <ul>
     *   <li>Node health status (must be HEALTHY)</li>
     *   <li>Available capacity</li>
     *   <li>Current load (container and match count)</li>
     * </ul>
     *
     * @param requiredModules   modules required for the match (reserved for future use)
     * @param preferredNodeId   optional preferred node ID
     * @return the selected node
     * @throws ca.samanthaireland.lightning.controlplane.scheduler.exception.NoAvailableNodesException if no healthy nodes exist
     * @throws ca.samanthaireland.lightning.controlplane.scheduler.exception.NoCapableNodesException if no nodes have capacity
     */
    Node selectNodeForMatch(List<String> requiredModules, String preferredNodeId);

    /**
     * Calculates the saturation percentage for a node.
     * Saturation = activeContainers / maxContainers
     *
     * @param node the node
     * @return saturation as a value between 0.0 and 1.0
     */
    double calculateSaturation(Node node);

    /**
     * Returns the overall cluster saturation.
     *
     * @return cluster saturation as a value between 0.0 and 1.0
     */
    double getClusterSaturation();
}
