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

package ca.samanthaireland.lightning.controlplane.node.model;

/**
 * Current metrics from a Lightning Engine node.
 *
 * @param containerCount current number of containers
 * @param matchCount     current number of active matches
 * @param cpuUsage       CPU usage percentage (0.0 - 1.0)
 * @param memoryUsedMb   memory usage in megabytes
 * @param memoryMaxMb    maximum memory in megabytes
 */
public record NodeMetrics(
        int containerCount,
        int matchCount,
        double cpuUsage,
        long memoryUsedMb,
        long memoryMaxMb
) {

    public NodeMetrics {
        if (containerCount < 0) {
            throw new IllegalArgumentException("containerCount cannot be negative");
        }
        if (matchCount < 0) {
            throw new IllegalArgumentException("matchCount cannot be negative");
        }
        if (cpuUsage < 0.0 || cpuUsage > 1.0) {
            throw new IllegalArgumentException("cpuUsage must be between 0.0 and 1.0");
        }
        if (memoryUsedMb < 0) {
            throw new IllegalArgumentException("memoryUsedMb cannot be negative");
        }
        if (memoryMaxMb < 0) {
            throw new IllegalArgumentException("memoryMaxMb cannot be negative");
        }
    }

    /**
     * Creates empty metrics for a newly registered node.
     *
     * @return metrics with zero values
     */
    public static NodeMetrics empty() {
        return new NodeMetrics(0, 0, 0.0, 0, 0);
    }
}
