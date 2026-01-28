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

import ca.samanthaireland.lightning.controlplane.node.model.NodeMetrics;

/**
 * DTO for node metrics information.
 *
 * @param containerCount current number of containers
 * @param matchCount     current number of active matches
 * @param cpuUsage       CPU usage percentage (0.0 - 1.0)
 * @param memoryUsedMb   memory usage in megabytes
 * @param memoryMaxMb    maximum memory in megabytes
 */
public record NodeMetricsDto(
        int containerCount,
        int matchCount,
        double cpuUsage,
        long memoryUsedMb,
        long memoryMaxMb
) {

    /**
     * Creates a DTO from a domain model.
     *
     * @param metrics the domain model
     * @return the DTO
     */
    public static NodeMetricsDto from(NodeMetrics metrics) {
        return new NodeMetricsDto(
                metrics.containerCount(),
                metrics.matchCount(),
                metrics.cpuUsage(),
                metrics.memoryUsedMb(),
                metrics.memoryMaxMb()
        );
    }

    /**
     * Converts this DTO to a domain model.
     *
     * @return the domain model
     */
    public NodeMetrics toModel() {
        return new NodeMetrics(
                containerCount,
                matchCount,
                cpuUsage,
                memoryUsedMb,
                memoryMaxMb
        );
    }
}
