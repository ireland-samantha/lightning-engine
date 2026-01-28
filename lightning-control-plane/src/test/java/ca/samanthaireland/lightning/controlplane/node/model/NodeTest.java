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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NodeTest {

    @Test
    void register_createsHealthyNode() {
        // Act
        Node node = Node.register("node-1", "http://localhost:8080", new NodeCapacity(100));

        // Assert
        assertThat(node.nodeId()).isEqualTo("node-1");
        assertThat(node.advertiseAddress()).isEqualTo("http://localhost:8080");
        assertThat(node.status()).isEqualTo(NodeStatus.HEALTHY);
        assertThat(node.metrics()).isEqualTo(NodeMetrics.empty());
        assertThat(node.registeredAt()).isNotNull();
        assertThat(node.lastHeartbeat()).isNotNull();
    }

    @Test
    void withHeartbeat_updatesMetricsAndTimestamp() {
        // Arrange
        Node node = Node.register("node-1", "http://localhost:8080", new NodeCapacity(100));
        NodeMetrics newMetrics = new NodeMetrics(5, 10, 0.5, 256, 512);

        // Act
        Node updated = node.withHeartbeat(newMetrics);

        // Assert
        assertThat(updated.metrics()).isEqualTo(newMetrics);
        assertThat(updated.lastHeartbeat()).isAfterOrEqualTo(node.lastHeartbeat());
        assertThat(updated.registeredAt()).isEqualTo(node.registeredAt()); // Preserved
        assertThat(updated.status()).isEqualTo(node.status()); // Preserved
    }

    @Test
    void drain_setsStatusToDraining() {
        // Arrange
        Node node = Node.register("node-1", "http://localhost:8080", new NodeCapacity(100));

        // Act
        Node drained = node.drain();

        // Assert
        assertThat(drained.status()).isEqualTo(NodeStatus.DRAINING);
        assertThat(drained.nodeId()).isEqualTo(node.nodeId()); // Preserved
    }

    @Test
    void canAcceptContainers_healthyWithCapacity_returnsTrue() {
        // Arrange
        Node node = Node.register("node-1", "http://localhost:8080", new NodeCapacity(100))
                .withHeartbeat(new NodeMetrics(50, 100, 0.5, 256, 512));

        // Act & Assert
        assertThat(node.canAcceptContainers()).isTrue();
    }

    @Test
    void canAcceptContainers_healthyAtCapacity_returnsFalse() {
        // Arrange
        Node node = Node.register("node-1", "http://localhost:8080", new NodeCapacity(100))
                .withHeartbeat(new NodeMetrics(100, 200, 0.5, 256, 512));

        // Act & Assert
        assertThat(node.canAcceptContainers()).isFalse();
    }

    @Test
    void canAcceptContainers_draining_returnsFalse() {
        // Arrange
        Node node = Node.register("node-1", "http://localhost:8080", new NodeCapacity(100))
                .withHeartbeat(new NodeMetrics(10, 20, 0.1, 256, 512))
                .drain();

        // Act & Assert
        assertThat(node.canAcceptContainers()).isFalse();
    }

    @Test
    void availableCapacity_healthy_returnsRemainingSlots() {
        // Arrange
        Node node = Node.register("node-1", "http://localhost:8080", new NodeCapacity(100))
                .withHeartbeat(new NodeMetrics(30, 60, 0.3, 256, 512));

        // Act & Assert
        assertThat(node.availableCapacity()).isEqualTo(70);
    }

    @Test
    void availableCapacity_draining_returnsZero() {
        // Arrange
        Node node = Node.register("node-1", "http://localhost:8080", new NodeCapacity(100))
                .withHeartbeat(new NodeMetrics(30, 60, 0.3, 256, 512))
                .drain();

        // Act & Assert
        assertThat(node.availableCapacity()).isZero();
    }

    @Test
    void constructor_nullNodeId_throwsException() {
        assertThatThrownBy(() -> Node.register(null, "http://localhost:8080", new NodeCapacity(100)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_blankNodeId_throwsException() {
        assertThatThrownBy(() -> Node.register("  ", "http://localhost:8080", new NodeCapacity(100)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_blankAddress_throwsException() {
        assertThatThrownBy(() -> Node.register("node-1", "  ", new NodeCapacity(100)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
