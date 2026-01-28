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

package ca.samanthaireland.lightning.controlplane.node.service;

import ca.samanthaireland.lightning.controlplane.config.ControlPlaneConfiguration;
import ca.samanthaireland.lightning.controlplane.node.exception.NodeNotFoundException;
import ca.samanthaireland.lightning.controlplane.node.model.Node;
import ca.samanthaireland.lightning.controlplane.node.model.NodeCapacity;
import ca.samanthaireland.lightning.controlplane.node.model.NodeMetrics;
import ca.samanthaireland.lightning.controlplane.node.model.NodeStatus;
import ca.samanthaireland.lightning.controlplane.node.repository.NodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class NodeRegistryServiceImplTest {

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private ControlPlaneConfiguration config;

    private NodeRegistryServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(config.nodeTtlSeconds()).thenReturn(30);
        service = new NodeRegistryServiceImpl(nodeRepository, config);
    }

    @Test
    void register_newNode_createsNode() {
        // Arrange
        String nodeId = "node-1";
        String address = "http://localhost:8080";
        NodeCapacity capacity = new NodeCapacity(100);

        when(nodeRepository.findById(nodeId)).thenReturn(Optional.empty());
        when(nodeRepository.save(any(), eq(30))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Node result = service.register(nodeId, address, capacity);

        // Assert
        assertThat(result.nodeId()).isEqualTo(nodeId);
        assertThat(result.advertiseAddress()).isEqualTo(address);
        assertThat(result.status()).isEqualTo(NodeStatus.HEALTHY);
        assertThat(result.capacity()).isEqualTo(capacity);
        assertThat(result.metrics()).isEqualTo(NodeMetrics.empty());

        verify(nodeRepository).save(any(Node.class), eq(30));
    }

    @Test
    void register_existingNode_preservesData() {
        // Arrange
        String nodeId = "node-1";
        String newAddress = "http://localhost:8081";
        NodeCapacity newCapacity = new NodeCapacity(200);

        Node existingNode = Node.register(nodeId, "http://localhost:8080", new NodeCapacity(100))
                .withHeartbeat(new NodeMetrics(5, 10, 0.5, 256, 512));

        when(nodeRepository.findById(nodeId)).thenReturn(Optional.of(existingNode));
        when(nodeRepository.save(any(), eq(30))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Node result = service.register(nodeId, newAddress, newCapacity);

        // Assert
        assertThat(result.nodeId()).isEqualTo(nodeId);
        assertThat(result.advertiseAddress()).isEqualTo(newAddress);
        assertThat(result.status()).isEqualTo(NodeStatus.HEALTHY); // Preserved
        assertThat(result.capacity()).isEqualTo(newCapacity);
        assertThat(result.metrics().containerCount()).isEqualTo(5); // Preserved
        assertThat(result.registeredAt()).isEqualTo(existingNode.registeredAt()); // Preserved
    }

    @Test
    void heartbeat_existingNode_updatesMetrics() {
        // Arrange
        String nodeId = "node-1";
        Node existingNode = Node.register(nodeId, "http://localhost:8080", new NodeCapacity(100));
        NodeMetrics newMetrics = new NodeMetrics(3, 5, 0.3, 128, 512);

        when(nodeRepository.findById(nodeId)).thenReturn(Optional.of(existingNode));
        when(nodeRepository.save(any(), eq(30))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Node result = service.heartbeat(nodeId, newMetrics);

        // Assert
        assertThat(result.metrics()).isEqualTo(newMetrics);
        assertThat(result.lastHeartbeat()).isAfterOrEqualTo(existingNode.lastHeartbeat());

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        verify(nodeRepository).save(nodeCaptor.capture(), eq(30));
        assertThat(nodeCaptor.getValue().metrics()).isEqualTo(newMetrics);
    }

    @Test
    void heartbeat_nonExistentNode_throwsException() {
        // Arrange
        String nodeId = "node-unknown";
        when(nodeRepository.findById(nodeId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.heartbeat(nodeId, NodeMetrics.empty()))
                .isInstanceOf(NodeNotFoundException.class)
                .hasMessageContaining(nodeId);
    }

    @Test
    void drain_existingNode_setsStatusToDraining() {
        // Arrange
        String nodeId = "node-1";
        Node existingNode = Node.register(nodeId, "http://localhost:8080", new NodeCapacity(100));

        when(nodeRepository.findById(nodeId)).thenReturn(Optional.of(existingNode));
        when(nodeRepository.save(any(), eq(30))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Node result = service.drain(nodeId);

        // Assert
        assertThat(result.status()).isEqualTo(NodeStatus.DRAINING);

        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        verify(nodeRepository).save(nodeCaptor.capture(), eq(30));
        assertThat(nodeCaptor.getValue().status()).isEqualTo(NodeStatus.DRAINING);
    }

    @Test
    void drain_nonExistentNode_throwsException() {
        // Arrange
        String nodeId = "node-unknown";
        when(nodeRepository.findById(nodeId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.drain(nodeId))
                .isInstanceOf(NodeNotFoundException.class)
                .hasMessageContaining(nodeId);
    }

    @Test
    void deregister_existingNode_deletesNode() {
        // Arrange
        String nodeId = "node-1";
        when(nodeRepository.existsById(nodeId)).thenReturn(true);

        // Act
        service.deregister(nodeId);

        // Assert
        verify(nodeRepository).deleteById(nodeId);
    }

    @Test
    void deregister_nonExistentNode_throwsException() {
        // Arrange
        String nodeId = "node-unknown";
        when(nodeRepository.existsById(nodeId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> service.deregister(nodeId))
                .isInstanceOf(NodeNotFoundException.class)
                .hasMessageContaining(nodeId);
    }

    @Test
    void findAll_returnsAllNodes() {
        // Arrange
        List<Node> nodes = List.of(
                Node.register("node-1", "http://localhost:8080", new NodeCapacity(100)),
                Node.register("node-2", "http://localhost:8081", new NodeCapacity(100))
        );
        when(nodeRepository.findAll()).thenReturn(nodes);

        // Act
        List<Node> result = service.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Node::nodeId).containsExactly("node-1", "node-2");
    }
}
