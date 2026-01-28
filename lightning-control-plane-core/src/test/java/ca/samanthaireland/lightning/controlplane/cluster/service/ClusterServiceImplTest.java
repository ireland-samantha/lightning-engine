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

package ca.samanthaireland.lightning.controlplane.cluster.service;

import ca.samanthaireland.lightning.controlplane.cluster.model.ClusterStatus;
import ca.samanthaireland.lightning.controlplane.node.model.Node;
import ca.samanthaireland.lightning.controlplane.node.model.NodeCapacity;
import ca.samanthaireland.lightning.controlplane.node.model.NodeMetrics;
import ca.samanthaireland.lightning.controlplane.node.model.NodeStatus;
import ca.samanthaireland.lightning.controlplane.node.service.NodeRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClusterServiceImpl}.
 *
 * <p>Tests verify correct delegation to NodeRegistryService and proper
 * aggregation of cluster status from individual nodes.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClusterServiceImpl")
class ClusterServiceImplTest {

    @Mock
    private NodeRegistryService nodeRegistryService;

    private ClusterServiceImpl clusterService;

    @BeforeEach
    void setUp() {
        clusterService = new ClusterServiceImpl(nodeRegistryService);
    }

    @Nested
    @DisplayName("getAllNodes")
    class GetAllNodes {

        @Test
        @DisplayName("should delegate to nodeRegistryService")
        void shouldDelegateToNodeRegistryService() {
            // Arrange
            Node node1 = createHealthyNode("node-1", 50, 10);
            Node node2 = createHealthyNode("node-2", 30, 5);
            List<Node> nodes = List.of(node1, node2);
            when(nodeRegistryService.findAll()).thenReturn(nodes);

            // Act
            List<Node> result = clusterService.getAllNodes();

            // Assert
            assertThat(result).containsExactly(node1, node2);
            verify(nodeRegistryService).findAll();
        }

        @Test
        @DisplayName("should return empty list when no nodes")
        void shouldReturnEmptyListWhenNoNodes() {
            // Arrange
            when(nodeRegistryService.findAll()).thenReturn(List.of());

            // Act
            List<Node> result = clusterService.getAllNodes();

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getNode")
    class GetNode {

        @Test
        @DisplayName("should return node when found")
        void shouldReturnNodeWhenFound() {
            // Arrange
            Node node = createHealthyNode("node-1", 50, 10);
            when(nodeRegistryService.findById("node-1")).thenReturn(Optional.of(node));

            // Act
            Optional<Node> result = clusterService.getNode("node-1");

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(node);
            verify(nodeRegistryService).findById("node-1");
        }

        @Test
        @DisplayName("should return empty when node not found")
        void shouldReturnEmptyWhenNodeNotFound() {
            // Arrange
            when(nodeRegistryService.findById("nonexistent")).thenReturn(Optional.empty());

            // Act
            Optional<Node> result = clusterService.getNode("nonexistent");

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getClusterStatus")
    class GetClusterStatus {

        @Test
        @DisplayName("should return correct status with no nodes")
        void shouldReturnCorrectStatusWithNoNodes() {
            // Arrange
            when(nodeRegistryService.findAll()).thenReturn(List.of());

            // Act
            ClusterStatus status = clusterService.getClusterStatus();

            // Assert
            assertThat(status.totalNodes()).isZero();
            assertThat(status.healthyNodes()).isZero();
            assertThat(status.drainingNodes()).isZero();
            assertThat(status.totalContainers()).isZero();
            assertThat(status.totalMatches()).isZero();
            assertThat(status.totalCapacity()).isZero();
            assertThat(status.availableCapacity()).isZero();
        }

        @Test
        @DisplayName("should aggregate metrics from multiple healthy nodes")
        void shouldAggregateMetricsFromMultipleHealthyNodes() {
            // Arrange
            // Node 1: 50 containers, 100 matches, capacity 100 (50 available)
            Node node1 = createHealthyNode("node-1", 50, 100);
            // Node 2: 30 containers, 60 matches, capacity 100 (70 available)
            Node node2 = createHealthyNode("node-2", 30, 60);
            when(nodeRegistryService.findAll()).thenReturn(List.of(node1, node2));

            // Act
            ClusterStatus status = clusterService.getClusterStatus();

            // Assert
            assertThat(status.totalNodes()).isEqualTo(2);
            assertThat(status.healthyNodes()).isEqualTo(2);
            assertThat(status.drainingNodes()).isZero();
            assertThat(status.totalContainers()).isEqualTo(80); // 50 + 30
            assertThat(status.totalMatches()).isEqualTo(160); // 100 + 60
            assertThat(status.totalCapacity()).isEqualTo(200); // 100 + 100
            assertThat(status.availableCapacity()).isEqualTo(120); // 50 + 70
        }

        @Test
        @DisplayName("should correctly count draining nodes")
        void shouldCorrectlyCountDrainingNodes() {
            // Arrange
            Node healthyNode = createHealthyNode("node-1", 50, 100);
            Node drainingNode = createDrainingNode("node-2", 30, 60);
            when(nodeRegistryService.findAll()).thenReturn(List.of(healthyNode, drainingNode));

            // Act
            ClusterStatus status = clusterService.getClusterStatus();

            // Assert
            assertThat(status.totalNodes()).isEqualTo(2);
            assertThat(status.healthyNodes()).isEqualTo(1);
            assertThat(status.drainingNodes()).isEqualTo(1);
        }

        @Test
        @DisplayName("should not count draining node capacity as available")
        void shouldNotCountDrainingNodeCapacityAsAvailable() {
            // Arrange
            // Healthy node: 50/100 used, 50 available
            Node healthyNode = createHealthyNode("node-1", 50, 100);
            // Draining node: 30/100 used, but 0 available (draining)
            Node drainingNode = createDrainingNode("node-2", 30, 60);
            when(nodeRegistryService.findAll()).thenReturn(List.of(healthyNode, drainingNode));

            // Act
            ClusterStatus status = clusterService.getClusterStatus();

            // Assert
            assertThat(status.totalCapacity()).isEqualTo(200); // Both nodes count toward total
            assertThat(status.availableCapacity()).isEqualTo(50); // Only healthy node contributes
        }

        @Test
        @DisplayName("should aggregate containers and matches from all nodes including draining")
        void shouldAggregateContainersAndMatchesFromAllNodes() {
            // Arrange
            Node healthyNode = createHealthyNode("node-1", 50, 100);
            Node drainingNode = createDrainingNode("node-2", 30, 60);
            when(nodeRegistryService.findAll()).thenReturn(List.of(healthyNode, drainingNode));

            // Act
            ClusterStatus status = clusterService.getClusterStatus();

            // Assert
            // Containers and matches are counted from all nodes
            assertThat(status.totalContainers()).isEqualTo(80); // 50 + 30
            assertThat(status.totalMatches()).isEqualTo(160); // 100 + 60
        }

        @Test
        @DisplayName("should handle single node cluster")
        void shouldHandleSingleNodeCluster() {
            // Arrange
            Node node = createHealthyNode("node-1", 25, 50);
            when(nodeRegistryService.findAll()).thenReturn(List.of(node));

            // Act
            ClusterStatus status = clusterService.getClusterStatus();

            // Assert
            assertThat(status.totalNodes()).isEqualTo(1);
            assertThat(status.healthyNodes()).isEqualTo(1);
            assertThat(status.drainingNodes()).isZero();
            assertThat(status.totalContainers()).isEqualTo(25);
            assertThat(status.totalMatches()).isEqualTo(50);
            assertThat(status.totalCapacity()).isEqualTo(100);
            assertThat(status.availableCapacity()).isEqualTo(75);
        }

        @Test
        @DisplayName("should handle all nodes draining")
        void shouldHandleAllNodesDraining() {
            // Arrange
            Node draining1 = createDrainingNode("node-1", 50, 100);
            Node draining2 = createDrainingNode("node-2", 30, 60);
            when(nodeRegistryService.findAll()).thenReturn(List.of(draining1, draining2));

            // Act
            ClusterStatus status = clusterService.getClusterStatus();

            // Assert
            assertThat(status.totalNodes()).isEqualTo(2);
            assertThat(status.healthyNodes()).isZero();
            assertThat(status.drainingNodes()).isEqualTo(2);
            assertThat(status.availableCapacity()).isZero();
        }
    }

    // Helper methods for creating test nodes

    private Node createHealthyNode(String nodeId, int containerCount, int matchCount) {
        return Node.register(nodeId, "http://" + nodeId + ":8080", new NodeCapacity(100))
                .withHeartbeat(new NodeMetrics(containerCount, matchCount, 0.5, 256, 512));
    }

    private Node createDrainingNode(String nodeId, int containerCount, int matchCount) {
        return createHealthyNode(nodeId, containerCount, matchCount).drain();
    }
}
