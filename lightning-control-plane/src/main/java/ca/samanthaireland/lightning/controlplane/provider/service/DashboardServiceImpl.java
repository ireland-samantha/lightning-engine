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

package ca.samanthaireland.lightning.controlplane.provider.service;

import ca.samanthaireland.lightning.controlplane.autoscaler.model.ScalingRecommendation;
import ca.samanthaireland.lightning.controlplane.autoscaler.service.AutoscalerService;
import ca.samanthaireland.lightning.controlplane.config.AutoscalerConfiguration;
import ca.samanthaireland.lightning.controlplane.cluster.model.ClusterStatus;
import ca.samanthaireland.lightning.controlplane.cluster.service.ClusterService;
import ca.samanthaireland.lightning.controlplane.provider.dto.DashboardOverview;
import ca.samanthaireland.lightning.controlplane.provider.dto.DashboardOverview.*;
import ca.samanthaireland.lightning.controlplane.provider.dto.PagedResponse;
import ca.samanthaireland.lightning.controlplane.provider.dto.MatchResponse;
import ca.samanthaireland.lightning.controlplane.match.model.MatchRegistryEntry;
import ca.samanthaireland.lightning.controlplane.match.model.MatchStatus;
import ca.samanthaireland.lightning.controlplane.match.service.MatchRoutingService;
import ca.samanthaireland.lightning.controlplane.provider.dto.NodeResponse;
import ca.samanthaireland.lightning.controlplane.node.model.Node;
import ca.samanthaireland.lightning.controlplane.node.model.NodeStatus;
import ca.samanthaireland.lightning.controlplane.scheduler.service.SchedulerService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Implementation of DashboardService that aggregates data from various services.
 */
@ApplicationScoped
public class DashboardServiceImpl implements DashboardService {

    private final ClusterService clusterService;
    private final MatchRoutingService matchRoutingService;
    private final SchedulerService schedulerService;
    private final AutoscalerService autoscalerService;
    private final AutoscalerConfiguration autoscalerConfig;

    @Inject
    public DashboardServiceImpl(
            ClusterService clusterService,
            MatchRoutingService matchRoutingService,
            SchedulerService schedulerService,
            AutoscalerService autoscalerService,
            AutoscalerConfiguration autoscalerConfig
    ) {
        this.clusterService = clusterService;
        this.matchRoutingService = matchRoutingService;
        this.schedulerService = schedulerService;
        this.autoscalerService = autoscalerService;
        this.autoscalerConfig = autoscalerConfig;
    }

    @Override
    public DashboardOverview getOverview() {
        // Get cluster status
        ClusterStatus clusterStatus = clusterService.getClusterStatus();
        double saturation = schedulerService.getClusterSaturation();

        // Build cluster health
        ClusterHealth clusterHealth = ClusterHealth.healthy(
                clusterStatus.healthyNodes(),
                clusterStatus.totalNodes(),
                saturation
        );

        // Build node summary
        int offlineNodes = clusterStatus.totalNodes() - clusterStatus.healthyNodes() - clusterStatus.drainingNodes();
        NodeSummary nodeSummary = new NodeSummary(
                clusterStatus.totalNodes(),
                clusterStatus.healthyNodes(),
                clusterStatus.drainingNodes(),
                Math.max(0, offlineNodes)
        );

        // Build match summary
        List<MatchRegistryEntry> allMatches = matchRoutingService.findAll();
        int runningMatches = (int) allMatches.stream()
                .filter(m -> m.status() == MatchStatus.RUNNING)
                .count();
        int finishedMatches = (int) allMatches.stream()
                .filter(m -> m.status() == MatchStatus.FINISHED)
                .count();
        int totalPlayers = allMatches.stream()
                .mapToInt(MatchRegistryEntry::playerCount)
                .sum();

        MatchSummary matchSummary = new MatchSummary(
                allMatches.size(),
                runningMatches,
                finishedMatches,
                totalPlayers
        );

        // Build capacity summary
        int usedCapacity = clusterStatus.totalCapacity() - clusterStatus.availableCapacity();
        double utilization = clusterStatus.totalCapacity() > 0
                ? (double) usedCapacity / clusterStatus.totalCapacity() * 100
                : 0;

        CapacitySummary capacitySummary = new CapacitySummary(
                clusterStatus.totalCapacity(),
                usedCapacity,
                clusterStatus.availableCapacity(),
                utilization
        );

        // Build autoscaler summary
        ScalingRecommendation recommendation = autoscalerService.getRecommendation();
        AutoscalerSummary autoscalerSummary = new AutoscalerSummary(
                autoscalerConfig.enabled(),
                recommendation.action(),
                recommendation.nodeDelta(),
                autoscalerService.isInCooldown()
        );

        return DashboardOverview.create(
                clusterHealth,
                nodeSummary,
                matchSummary,
                capacitySummary,
                autoscalerSummary
        );
    }

    @Override
    public PagedResponse<NodeResponse> getNodes(int page, int pageSize, NodeStatus status) {
        List<Node> nodes = clusterService.getAllNodes();

        // Filter by status if provided
        if (status != null) {
            nodes = nodes.stream()
                    .filter(n -> n.status() == status)
                    .toList();
        }

        // Convert to DTOs
        List<NodeResponse> responses = nodes.stream()
                .map(NodeResponse::from)
                .toList();

        return PagedResponse.of(responses, page, pageSize);
    }

    @Override
    public PagedResponse<MatchResponse> getMatches(int page, int pageSize, MatchStatus status, String nodeId) {
        List<MatchRegistryEntry> matches;

        if (status != null) {
            matches = matchRoutingService.findByStatus(status);
        } else {
            matches = matchRoutingService.findAll();
        }

        // Filter by nodeId if provided
        if (nodeId != null && !nodeId.isBlank()) {
            matches = matches.stream()
                    .filter(m -> m.nodeId().equals(nodeId))
                    .toList();
        }

        // Convert to DTOs
        List<MatchResponse> responses = matches.stream()
                .map(MatchResponse::from)
                .toList();

        return PagedResponse.of(responses, page, pageSize);
    }
}
