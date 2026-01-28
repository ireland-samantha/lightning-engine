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

package ca.samanthaireland.lightning.controlplane.client;

import ca.samanthaireland.lightning.controlplane.node.model.Node;

import java.util.List;

/**
 * Client for communicating with Lightning Engine nodes.
 *
 * <p>This is a pure domain abstraction with no framework dependencies.
 * Implementations may use any HTTP client library.
 */
public interface LightningNodeClient {

    /**
     * Creates a container on a node.
     *
     * @param node        the target node
     * @param moduleNames modules to install
     * @return the created container ID
     */
    long createContainer(Node node, List<String> moduleNames);

    /**
     * Creates a match on a node within an existing container.
     *
     * @param node        the target node
     * @param containerId the container ID
     * @param moduleNames modules enabled for the match
     * @return the created match ID
     */
    long createMatch(Node node, long containerId, List<String> moduleNames);

    /**
     * Deletes a match from a node.
     *
     * @param node        the target node
     * @param containerId the container ID
     * @param matchId     the match ID
     */
    void deleteMatch(Node node, long containerId, long matchId);

    /**
     * Deletes a container from a node.
     *
     * @param node        the target node
     * @param containerId the container ID
     */
    void deleteContainer(Node node, long containerId);

    /**
     * Checks if a node is reachable.
     *
     * @param node the node to check
     * @return true if the node responds to health check
     */
    boolean isReachable(Node node);
}
