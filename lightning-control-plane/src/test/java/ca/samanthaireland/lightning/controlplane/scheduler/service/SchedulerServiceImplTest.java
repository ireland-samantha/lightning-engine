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
import ca.samanthaireland.lightning.controlplane.node.model.NodeCapacity;
import ca.samanthaireland.lightning.controlplane.node.model.NodeMetrics;
import ca.samanthaireland.lightning.controlplane.node.model.NodeStatus;
import ca.samanthaireland.lightning.controlplane.node.service.NodeRegistryService;
import ca.samanthaireland.lightning.controlplane.scheduler.exception.NoAvailableNodesException;
import ca.samanthaireland.lightning.controlplane.scheduler.exception.NoCapableNodesException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceImplTest {

    @Mock
    private NodeRegistryService nodeRegistryService;

    private SchedulerServiceImpl scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SchedulerServiceImpl(nodeRegistryService);
    }

    @Test
    void selectNodeForMatch_withHealthyNodes_selectsLeastLoaded() {
        // Arrange
        Node heavyNode = createNodeWithContainers("node-1", 80, 100); // 80% loaded
        Node lightNode = createNodeWithContainers("node-2", 20, 100); // 20% loaded
        Node mediumNode = createNodeWithContainers("node-3", 50, 100); // 50% loaded

        when(nodeRegistryService.findAll()).thenReturn(List.of(heavyNode, lightNode, mediumNode));

        // Act
        Node selected = scheduler.selectNodeForMatch(List.of("test-module"), null);

        // Assert
        assertThat(selected.nodeId()).isEqualTo("node-2"); // Least loaded
    }

    @Test
    void selectNodeForMatch_withPreferredNode_selectsPreferred() {
        // Arrange
        Node heavyNode = createNodeWithContainers("node-1", 80, 100);
        Node lightNode = createNodeWithContainers("node-2", 20, 100);

        when(nodeRegistryService.findAll()).thenReturn(List.of(heavyNode, lightNode));

        // Act
        Node selected = scheduler.selectNodeForMatch(List.of("test-module"), "node-1");

        // Assert
        assertThat(selected.nodeId()).isEqualTo("node-1"); // Preferred even though heavier
    }

    @Test
    void selectNodeForMatch_preferredNodeNotAvailable_selectsLeastLoaded() {
        // Arrange
        Node heavyNode = createNodeWithContainers("node-1", 80, 100);
        Node lightNode = createNodeWithContainers("node-2", 20, 100);

        when(nodeRegistryService.findAll()).thenReturn(List.of(heavyNode, lightNode));

        // Act
        Node selected = scheduler.selectNodeForMatch(List.of("test-module"), "node-nonexistent");

        // Assert
        assertThat(selected.nodeId()).isEqualTo("node-2"); // Falls back to least loaded
    }

    @Test
    void selectNodeForMatch_noHealthyNodes_throwsNoAvailableNodesException() {
        // Arrange
        when(nodeRegistryService.findAll()).thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> scheduler.selectNodeForMatch(List.of("test-module"), null))
                .isInstanceOf(NoAvailableNodesException.class);
    }

    @Test
    void selectNodeForMatch_onlyDrainingNodes_throwsNoAvailableNodesException() {
        // Arrange
        Node drainingNode = Node.register("node-1", "http://localhost:8080", new NodeCapacity(100)).drain();

        when(nodeRegistryService.findAll()).thenReturn(List.of(drainingNode));

        // Act & Assert
        assertThatThrownBy(() -> scheduler.selectNodeForMatch(List.of("test-module"), null))
                .isInstanceOf(NoAvailableNodesException.class);
    }

    @Test
    void selectNodeForMatch_noCapacity_throwsNoCapableNodesException() {
        // Arrange - Node at max capacity
        Node fullNode = createNodeWithContainers("node-1", 100, 100);

        when(nodeRegistryService.findAll()).thenReturn(List.of(fullNode));

        // Act & Assert
        assertThatThrownBy(() -> scheduler.selectNodeForMatch(List.of("test-module"), null))
                .isInstanceOf(NoCapableNodesException.class)
                .hasMessageContaining("test-module");
    }

    @Test
    void selectNodeForMatch_preferredNodeFull_selectsLeastLoaded() {
        // Arrange
        Node fullPreferred = createNodeWithContainers("preferred", 100, 100);
        Node available = createNodeWithContainers("node-2", 50, 100);

        when(nodeRegistryService.findAll()).thenReturn(List.of(fullPreferred, available));

        // Act
        Node selected = scheduler.selectNodeForMatch(List.of("test-module"), "preferred");

        // Assert
        assertThat(selected.nodeId()).isEqualTo("node-2"); // Falls back since preferred is full
    }

    @Test
    void calculateSaturation_noContainers_returnsZero() {
        // Arrange
        Node emptyNode = createNodeWithContainers("node-1", 0, 100);

        // Act
        double saturation = scheduler.calculateSaturation(emptyNode);

        // Assert
        assertThat(saturation).isEqualTo(0.0);
    }

    @Test
    void calculateSaturation_halfFull_returnsHalf() {
        // Arrange
        Node halfFullNode = createNodeWithContainers("node-1", 50, 100);

        // Act
        double saturation = scheduler.calculateSaturation(halfFullNode);

        // Assert
        assertThat(saturation).isEqualTo(0.5);
    }

    @Test
    void calculateSaturation_fullCapacity_returnsOne() {
        // Arrange - Node at 100% capacity
        Node fullNode = createNodeWithContainers("node-1", 100, 100);

        // Act
        double saturation = scheduler.calculateSaturation(fullNode);

        // Assert
        assertThat(saturation).isEqualTo(1.0);
    }

    @Test
    void getClusterSaturation_noNodes_returnsOne() {
        // Arrange
        when(nodeRegistryService.findAll()).thenReturn(List.of());

        // Act
        double saturation = scheduler.getClusterSaturation();

        // Assert
        assertThat(saturation).isEqualTo(1.0);
    }

    @Test
    void getClusterSaturation_multipleNodes_returnsAverageSaturation() {
        // Arrange
        Node node1 = createNodeWithContainers("node-1", 60, 100); // 60%
        Node node2 = createNodeWithContainers("node-2", 40, 100); // 40%
        // Total: 100/200 = 50%

        when(nodeRegistryService.findAll()).thenReturn(List.of(node1, node2));

        // Act
        double saturation = scheduler.getClusterSaturation();

        // Assert
        assertThat(saturation).isEqualTo(0.5);
    }

    @Test
    void getClusterSaturation_ignoresDrainingNodes() {
        // Arrange
        Node healthy = createNodeWithContainers("node-1", 50, 100);
        Node draining = Node.register("node-2", "http://localhost:8081", new NodeCapacity(100))
                .withHeartbeat(new NodeMetrics(20, 0, 0, 0, 0))
                .drain();

        when(nodeRegistryService.findAll()).thenReturn(List.of(healthy, draining));

        // Act
        double saturation = scheduler.getClusterSaturation();

        // Assert
        assertThat(saturation).isEqualTo(0.5); // Only considers healthy node
    }

    private Node createNodeWithContainers(String nodeId, int containerCount, int maxContainers) {
        NodeMetrics metrics = new NodeMetrics(containerCount, 0, 0, 0, 0);
        return Node.register(nodeId, "http://localhost:8080", new NodeCapacity(maxContainers))
                .withHeartbeat(metrics);
    }
}
