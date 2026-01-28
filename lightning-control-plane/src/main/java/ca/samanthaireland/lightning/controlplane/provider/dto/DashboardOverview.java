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

import ca.samanthaireland.lightning.controlplane.autoscaler.model.ScalingAction;

import java.time.Instant;

/**
 * Complete dashboard overview with all cluster metrics.
 */
public record DashboardOverview(
        // Cluster health
        ClusterHealth clusterHealth,

        // Node summary
        NodeSummary nodes,

        // Match summary
        MatchSummary matches,

        // Capacity summary
        CapacitySummary capacity,

        // Autoscaler summary
        AutoscalerSummary autoscaler,

        // Timestamp
        Instant generatedAt
) {

    /**
     * Creates a new dashboard overview.
     */
    public static DashboardOverview create(
            ClusterHealth clusterHealth,
            NodeSummary nodes,
            MatchSummary matches,
            CapacitySummary capacity,
            AutoscalerSummary autoscaler
    ) {
        return new DashboardOverview(
                clusterHealth,
                nodes,
                matches,
                capacity,
                autoscaler,
                Instant.now()
        );
    }

    /**
     * Cluster health status.
     */
    public record ClusterHealth(
            String status,  // HEALTHY, DEGRADED, CRITICAL
            int healthyNodes,
            int totalNodes,
            double saturation
    ) {
        public static ClusterHealth healthy(int healthyNodes, int totalNodes, double saturation) {
            String status = determineStatus(healthyNodes, totalNodes, saturation);
            return new ClusterHealth(status, healthyNodes, totalNodes, saturation);
        }

        private static String determineStatus(int healthy, int total, double saturation) {
            if (total == 0 || healthy == 0) return "CRITICAL";
            if (saturation > 0.9) return "CRITICAL";
            if (healthy < total || saturation > 0.8) return "DEGRADED";
            return "HEALTHY";
        }
    }

    /**
     * Node statistics summary.
     */
    public record NodeSummary(
            int total,
            int healthy,
            int draining,
            int offline
    ) {
    }

    /**
     * Match statistics summary.
     */
    public record MatchSummary(
            int total,
            int running,
            int finished,
            int totalPlayers
    ) {
    }

    /**
     * Capacity utilization summary.
     */
    public record CapacitySummary(
            int totalCapacity,
            int usedCapacity,
            int availableCapacity,
            double utilizationPercent
    ) {
    }

    /**
     * Autoscaler status summary.
     */
    public record AutoscalerSummary(
            boolean enabled,
            ScalingAction recommendedAction,
            int recommendedNodeDelta,
            boolean inCooldown
    ) {
    }
}
