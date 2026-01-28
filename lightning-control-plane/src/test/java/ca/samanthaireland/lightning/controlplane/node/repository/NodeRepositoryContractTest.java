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

package ca.samanthaireland.lightning.controlplane.node.repository;

import ca.samanthaireland.lightning.controlplane.node.model.Node;
import ca.samanthaireland.lightning.controlplane.node.model.NodeCapacity;
import ca.samanthaireland.lightning.controlplane.node.model.NodeMetrics;
import ca.samanthaireland.lightning.controlplane.node.model.NodeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for {@link NodeRepository} interface.
 *
 * <p>These tests verify the expected behavior of any NodeRepository implementation.
 * They use an in-memory implementation to test the interface contract without
 * requiring external dependencies like Redis.
 */
@DisplayName("NodeRepository Contract Tests")
class NodeRepositoryContractTest {

    private NodeRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryNodeRepository();
    }

    @Test
    @DisplayName("save() should persist a new node")
    void save_shouldPersistNewNode() {
        Node node = createTestNode("node-1");

        repository.save(node, 60);

        Optional<Node> found = repository.findById("node-1");
        assertThat(found).isPresent();
        assertThat(found.get().nodeId()).isEqualTo("node-1");
    }

    @Test
    @DisplayName("save() should update an existing node")
    void save_shouldUpdateExistingNode() {
        Node original = createTestNode("node-1");
        repository.save(original, 60);

        Node updated = original.drain();
        repository.save(updated, 60);

        Optional<Node> found = repository.findById("node-1");
        assertThat(found).isPresent();
        assertThat(found.get().status()).isEqualTo(NodeStatus.DRAINING);
    }

    @Test
    @DisplayName("findById() should return empty for non-existent node")
    void findById_shouldReturnEmptyForNonExistent() {
        Optional<Node> found = repository.findById("non-existent");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findAll() should return all saved nodes")
    void findAll_shouldReturnAllNodes() {
        repository.save(createTestNode("node-1"), 60);
        repository.save(createTestNode("node-2"), 60);
        repository.save(createTestNode("node-3"), 60);

        var nodes = repository.findAll();

        assertThat(nodes).hasSize(3);
        assertThat(nodes).extracting(Node::nodeId)
                .containsExactlyInAnyOrder("node-1", "node-2", "node-3");
    }

    @Test
    @DisplayName("deleteById() should remove a node")
    void deleteById_shouldRemoveNode() {
        repository.save(createTestNode("node-1"), 60);

        repository.deleteById("node-1");

        assertThat(repository.findById("node-1")).isEmpty();
    }

    @Test
    @DisplayName("deleteById() should be idempotent for non-existent node")
    void deleteById_shouldBeIdempotentForNonExistent() {
        // Should not throw
        repository.deleteById("non-existent");
    }

    @Test
    @DisplayName("existsById() should return true for existing node")
    void existsById_shouldReturnTrueForExisting() {
        repository.save(createTestNode("node-1"), 60);

        assertThat(repository.existsById("node-1")).isTrue();
    }

    @Test
    @DisplayName("existsById() should return false for non-existent node")
    void existsById_shouldReturnFalseForNonExistent() {
        assertThat(repository.existsById("non-existent")).isFalse();
    }

    private Node createTestNode(String nodeId) {
        return new Node(
                nodeId,
                "http://localhost:8080",
                NodeStatus.HEALTHY,
                new NodeCapacity(100),
                NodeMetrics.empty(),
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * In-memory implementation for contract testing.
     */
    private static class InMemoryNodeRepository implements NodeRepository {
        private final Map<String, Node> nodes = new HashMap<>();

        @Override
        public Node save(Node node, int ttlSeconds) {
            nodes.put(node.nodeId(), node);
            return node;
        }

        @Override
        public Optional<Node> findById(String nodeId) {
            return Optional.ofNullable(nodes.get(nodeId));
        }

        @Override
        public List<Node> findAll() {
            return new ArrayList<>(nodes.values());
        }

        @Override
        public void deleteById(String nodeId) {
            nodes.remove(nodeId);
        }

        @Override
        public boolean existsById(String nodeId) {
            return nodes.containsKey(nodeId);
        }
    }
}
