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

import ca.samanthaireland.lightning.controlplane.config.AutoscalerConfiguration;
import ca.samanthaireland.lightning.controlplane.autoscaler.model.ScalingAction;
import ca.samanthaireland.lightning.controlplane.autoscaler.model.ScalingRecommendation;
import ca.samanthaireland.lightning.controlplane.autoscaler.service.AutoscalerService;
import ca.samanthaireland.lightning.controlplane.cluster.model.ClusterStatus;
import ca.samanthaireland.lightning.controlplane.cluster.service.ClusterService;
import ca.samanthaireland.lightning.controlplane.provider.dto.DashboardOverview;
import ca.samanthaireland.lightning.controlplane.provider.dto.PagedResponse;
import ca.samanthaireland.lightning.controlplane.provider.dto.MatchResponse;
import ca.samanthaireland.lightning.controlplane.match.model.MatchRegistryEntry;
import ca.samanthaireland.lightning.controlplane.match.model.MatchStatus;
import ca.samanthaireland.lightning.controlplane.match.service.MatchRoutingService;
import ca.samanthaireland.lightning.controlplane.provider.dto.NodeResponse;
import ca.samanthaireland.lightning.controlplane.node.model.Node;
import ca.samanthaireland.lightning.controlplane.node.model.NodeCapacity;
import ca.samanthaireland.lightning.controlplane.node.model.NodeStatus;
import ca.samanthaireland.lightning.controlplane.scheduler.service.SchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private ClusterService clusterService;

    @Mock
    private MatchRoutingService matchRoutingService;

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private AutoscalerService autoscalerService;

    @Mock
    private AutoscalerConfiguration autoscalerConfig;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(
                clusterService,
                matchRoutingService,
                schedulerService,
                autoscalerService,
                autoscalerConfig
        );
    }

    @Test
    void getOverview_returnsCompleteOverview() {
        // Arrange
        ClusterStatus clusterStatus = new ClusterStatus(
                3, 2, 1, 10, 15, 300, 200
        );
        when(clusterService.getClusterStatus()).thenReturn(clusterStatus);
        when(schedulerService.getClusterSaturation()).thenReturn(0.5);

        List<MatchRegistryEntry> matches = List.of(
                createMatch("m1", MatchStatus.RUNNING, 5),
                createMatch("m2", MatchStatus.FINISHED, 0),
                createMatch("m3", MatchStatus.RUNNING, 3)
        );
        when(matchRoutingService.findAll()).thenReturn(matches);

        when(autoscalerConfig.enabled()).thenReturn(true);
        when(autoscalerService.getRecommendation()).thenReturn(
                ScalingRecommendation.none(2, 0.5, "Within range")
        );
        when(autoscalerService.isInCooldown()).thenReturn(false);

        // Act
        DashboardOverview overview = dashboardService.getOverview();

        // Assert
        assertThat(overview.clusterHealth().status()).isEqualTo("DEGRADED"); // 2/3 healthy
        assertThat(overview.clusterHealth().healthyNodes()).isEqualTo(2);
        assertThat(overview.clusterHealth().totalNodes()).isEqualTo(3);

        assertThat(overview.nodes().total()).isEqualTo(3);
        assertThat(overview.nodes().healthy()).isEqualTo(2);
        assertThat(overview.nodes().draining()).isEqualTo(1);

        assertThat(overview.matches().total()).isEqualTo(3);
        assertThat(overview.matches().running()).isEqualTo(2);
        assertThat(overview.matches().finished()).isEqualTo(1);
        assertThat(overview.matches().totalPlayers()).isEqualTo(8);

        assertThat(overview.capacity().totalCapacity()).isEqualTo(300);
        assertThat(overview.capacity().availableCapacity()).isEqualTo(200);

        assertThat(overview.autoscaler().enabled()).isTrue();
        assertThat(overview.autoscaler().recommendedAction()).isEqualTo(ScalingAction.NONE);
        assertThat(overview.autoscaler().inCooldown()).isFalse();

        assertThat(overview.generatedAt()).isNotNull();
    }

    @Test
    void getOverview_noNodes_returnsCriticalStatus() {
        // Arrange
        ClusterStatus clusterStatus = new ClusterStatus(0, 0, 0, 0, 0, 0, 0);
        when(clusterService.getClusterStatus()).thenReturn(clusterStatus);
        when(schedulerService.getClusterSaturation()).thenReturn(1.0);
        when(matchRoutingService.findAll()).thenReturn(List.of());
        when(autoscalerConfig.enabled()).thenReturn(true);
        when(autoscalerService.getRecommendation()).thenReturn(
                ScalingRecommendation.scaleUp(0, 1, 1.0, 0.0, "No nodes")
        );
        when(autoscalerService.isInCooldown()).thenReturn(false);

        // Act
        DashboardOverview overview = dashboardService.getOverview();

        // Assert
        assertThat(overview.clusterHealth().status()).isEqualTo("CRITICAL");
    }

    @Test
    void getNodes_noFilter_returnsAllNodes() {
        // Arrange
        List<Node> nodes = List.of(
                Node.register("node-1", "http://localhost:8080", new NodeCapacity(100)),
                Node.register("node-2", "http://localhost:8081", new NodeCapacity(100)).drain()
        );
        when(clusterService.getAllNodes()).thenReturn(nodes);

        // Act
        PagedResponse<NodeResponse> result = dashboardService.getNodes(0, 10, null);

        // Assert
        assertThat(result.totalItems()).isEqualTo(2);
        assertThat(result.items()).hasSize(2);
    }

    @Test
    void getNodes_withStatusFilter_returnsFilteredNodes() {
        // Arrange
        List<Node> nodes = List.of(
                Node.register("node-1", "http://localhost:8080", new NodeCapacity(100)),
                Node.register("node-2", "http://localhost:8081", new NodeCapacity(100)).drain()
        );
        when(clusterService.getAllNodes()).thenReturn(nodes);

        // Act
        PagedResponse<NodeResponse> result = dashboardService.getNodes(0, 10, NodeStatus.HEALTHY);

        // Assert
        assertThat(result.totalItems()).isEqualTo(1);
        assertThat(result.items().getFirst().status()).isEqualTo(NodeStatus.HEALTHY);
    }

    @Test
    void getMatches_noFilter_returnsAllMatches() {
        // Arrange
        List<MatchRegistryEntry> matches = List.of(
                createMatch("m1", MatchStatus.RUNNING, 5),
                createMatch("m2", MatchStatus.FINISHED, 0)
        );
        when(matchRoutingService.findAll()).thenReturn(matches);

        // Act
        PagedResponse<MatchResponse> result = dashboardService.getMatches(0, 10, null, null);

        // Assert
        assertThat(result.totalItems()).isEqualTo(2);
    }

    @Test
    void getMatches_withStatusFilter_returnsFilteredMatches() {
        // Arrange
        List<MatchRegistryEntry> matches = List.of(
                createMatch("m1", MatchStatus.RUNNING, 5)
        );
        when(matchRoutingService.findByStatus(MatchStatus.RUNNING)).thenReturn(matches);

        // Act
        PagedResponse<MatchResponse> result = dashboardService.getMatches(0, 10, MatchStatus.RUNNING, null);

        // Assert
        assertThat(result.totalItems()).isEqualTo(1);
        assertThat(result.items().getFirst().status()).isEqualTo(MatchStatus.RUNNING);
    }

    @Test
    void getMatches_withNodeFilter_returnsFilteredMatches() {
        // Arrange
        List<MatchRegistryEntry> matches = List.of(
                createMatchForNode("m1", "node-1"),
                createMatchForNode("m2", "node-2")
        );
        when(matchRoutingService.findAll()).thenReturn(matches);

        // Act
        PagedResponse<MatchResponse> result = dashboardService.getMatches(0, 10, null, "node-1");

        // Assert
        assertThat(result.totalItems()).isEqualTo(1);
        assertThat(result.items().getFirst().nodeId()).isEqualTo("node-1");
    }

    @Test
    void getNodes_pagination_worksCorrectly() {
        // Arrange
        List<Node> nodes = List.of(
                Node.register("node-1", "http://localhost:8080", new NodeCapacity(100)),
                Node.register("node-2", "http://localhost:8081", new NodeCapacity(100)),
                Node.register("node-3", "http://localhost:8082", new NodeCapacity(100))
        );
        when(clusterService.getAllNodes()).thenReturn(nodes);

        // Act
        PagedResponse<NodeResponse> page1 = dashboardService.getNodes(0, 2, null);
        PagedResponse<NodeResponse> page2 = dashboardService.getNodes(1, 2, null);

        // Assert
        assertThat(page1.items()).hasSize(2);
        assertThat(page1.hasNext()).isTrue();
        assertThat(page1.hasPrevious()).isFalse();

        assertThat(page2.items()).hasSize(1);
        assertThat(page2.hasNext()).isFalse();
        assertThat(page2.hasPrevious()).isTrue();
    }

    private MatchRegistryEntry createMatch(String id, MatchStatus status, int playerCount) {
        MatchRegistryEntry entry = MatchRegistryEntry.creating(id, "node-1", 1L, List.of("module"), "http://localhost:8080");
        entry = switch (status) {
            case RUNNING -> entry.running();
            case FINISHED -> entry.running().finished();
            case ERROR -> entry.error();
            case CREATING -> entry;
        };
        return entry.withPlayerCount(playerCount);
    }

    private MatchRegistryEntry createMatchForNode(String id, String nodeId) {
        return MatchRegistryEntry.creating(id, nodeId, 1L, List.of("module"), "http://localhost:8080")
                .running();
    }
}
