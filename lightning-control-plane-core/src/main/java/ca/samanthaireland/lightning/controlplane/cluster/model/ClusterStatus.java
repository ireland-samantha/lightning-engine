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

package ca.samanthaireland.lightning.controlplane.cluster.model;

/**
 * Domain model representing the overall cluster status.
 *
 * @param totalNodes        total number of registered nodes
 * @param healthyNodes      number of nodes in HEALTHY status
 * @param drainingNodes     number of nodes in DRAINING status
 * @param totalContainers   total containers across all nodes
 * @param totalMatches      total matches across all nodes
 * @param totalCapacity     total container capacity across all nodes
 * @param availableCapacity available container slots across healthy nodes
 */
public record ClusterStatus(
        int totalNodes,
        int healthyNodes,
        int drainingNodes,
        int totalContainers,
        int totalMatches,
        int totalCapacity,
        int availableCapacity
) {

    /**
     * Calculates the current saturation of the cluster.
     *
     * @return saturation as a decimal between 0.0 and 1.0
     */
    public double saturation() {
        if (totalCapacity == 0) {
            return 1.0;
        }
        return (double) (totalCapacity - availableCapacity) / totalCapacity;
    }

    /**
     * Returns true if all registered nodes are healthy.
     *
     * @return true if cluster is fully healthy
     */
    public boolean isFullyHealthy() {
        return totalNodes > 0 && healthyNodes == totalNodes;
    }
}
