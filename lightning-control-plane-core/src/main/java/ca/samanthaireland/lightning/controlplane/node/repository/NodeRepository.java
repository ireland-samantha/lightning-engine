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

package ca.samanthaireland.lightning.controlplane.node.repository;

import ca.samanthaireland.lightning.controlplane.node.model.Node;

import java.util.List;
import java.util.Optional;

/**
 * Repository for storing and retrieving node information.
 */
public interface NodeRepository {

    /**
     * Saves a node with TTL-based expiration.
     *
     * @param node       the node to save
     * @param ttlSeconds TTL in seconds for automatic expiration
     * @return the saved node
     */
    Node save(Node node, int ttlSeconds);

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

    /**
     * Deletes a node by its ID.
     *
     * @param nodeId the node ID to delete
     */
    void deleteById(String nodeId);

    /**
     * Checks if a node exists.
     *
     * @param nodeId the node ID
     * @return true if the node exists
     */
    boolean existsById(String nodeId);
}
