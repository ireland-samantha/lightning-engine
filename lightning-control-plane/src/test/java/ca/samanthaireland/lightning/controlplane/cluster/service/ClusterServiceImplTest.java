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
import ca.samanthaireland.lightning.controlplane.node.service.NodeRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClusterServiceImplTest {

    @Mock
    private NodeRegistryService nodeRegistryService;

    private ClusterServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClusterServiceImpl(nodeRegistryService);
    }

    @Test
    void getClusterStatus_emptyCluster_returnsZeros() {
        // Arrange
        when(nodeRegistryService.findAll()).thenReturn(List.of());

        // Act
        ClusterStatus result = service.getClusterStatus();

        // Assert
        assertThat(result.totalNodes()).isZero();
        assertThat(result.healthyNodes()).isZero();
        assertThat(result.drainingNodes()).isZero();
        assertThat(result.totalContainers()).isZero();
        assertThat(result.totalMatches()).isZero();
        assertThat(result.totalCapacity()).isZero();
        assertThat(result.availableCapacity()).isZero();
    }

    @Test
    void getClusterStatus_withHealthyNodes_calculatesCorrectly() {
        // Arrange
        Node node1 = Node.register("node-1", "http://node1:8080", new NodeCapacity(100))
                .withHeartbeat(new NodeMetrics(3, 5, 0.3, 256, 512));
        Node node2 = Node.register("node-2", "http://node2:8080", new NodeCapacity(50))
                .withHeartbeat(new NodeMetrics(2, 3, 0.2, 128, 256));

        when(nodeRegistryService.findAll()).thenReturn(List.of(node1, node2));

        // Act
        ClusterStatus result = service.getClusterStatus();

        // Assert
        assertThat(result.totalNodes()).isEqualTo(2);
        assertThat(result.healthyNodes()).isEqualTo(2);
        assertThat(result.drainingNodes()).isZero();
        assertThat(result.totalContainers()).isEqualTo(5); // 3 + 2
        assertThat(result.totalMatches()).isEqualTo(8); // 5 + 3
        assertThat(result.totalCapacity()).isEqualTo(150); // 100 + 50
        assertThat(result.availableCapacity()).isEqualTo(145); // (100-3) + (50-2)
    }

    @Test
    void getClusterStatus_withDrainingNode_excludesFromAvailable() {
        // Arrange
        Node healthyNode = Node.register("node-1", "http://node1:8080", new NodeCapacity(100))
                .withHeartbeat(new NodeMetrics(10, 20, 0.5, 256, 512));
        Node drainingNode = Node.register("node-2", "http://node2:8080", new NodeCapacity(100))
                .withHeartbeat(new NodeMetrics(5, 10, 0.3, 128, 256))
                .drain();

        when(nodeRegistryService.findAll()).thenReturn(List.of(healthyNode, drainingNode));

        // Act
        ClusterStatus result = service.getClusterStatus();

        // Assert
        assertThat(result.totalNodes()).isEqualTo(2);
        assertThat(result.healthyNodes()).isEqualTo(1);
        assertThat(result.drainingNodes()).isEqualTo(1);
        assertThat(result.totalContainers()).isEqualTo(15); // 10 + 5
        assertThat(result.totalMatches()).isEqualTo(30); // 20 + 10
        assertThat(result.totalCapacity()).isEqualTo(200); // 100 + 100
        assertThat(result.availableCapacity()).isEqualTo(90); // Only healthy node: 100 - 10
    }

    @Test
    void getNode_existingNode_returnsNode() {
        // Arrange
        Node node = Node.register("node-1", "http://node1:8080", new NodeCapacity(100));
        when(nodeRegistryService.findById("node-1")).thenReturn(Optional.of(node));

        // Act
        Optional<Node> result = service.getNode("node-1");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().nodeId()).isEqualTo("node-1");
    }

    @Test
    void getNode_nonExistentNode_returnsEmpty() {
        // Arrange
        when(nodeRegistryService.findById("node-unknown")).thenReturn(Optional.empty());

        // Act
        Optional<Node> result = service.getNode("node-unknown");

        // Assert
        assertThat(result).isEmpty();
    }
}
