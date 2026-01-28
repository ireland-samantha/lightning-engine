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

package ca.samanthaireland.lightning.controlplane.match.repository;

import ca.samanthaireland.lightning.controlplane.match.model.MatchRegistryEntry;
import ca.samanthaireland.lightning.controlplane.match.model.MatchStatus;
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
 * Contract tests for {@link MatchRegistry} interface.
 *
 * <p>These tests verify the expected behavior of any MatchRegistry implementation.
 * They use an in-memory implementation to test the interface contract without
 * requiring external dependencies like Redis.
 */
@DisplayName("MatchRegistry Contract Tests")
class MatchRegistryContractTest {

    private MatchRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InMemoryMatchRegistry();
    }

    @Test
    @DisplayName("save() should persist a new match")
    void save_shouldPersistNewMatch() {
        MatchRegistryEntry entry = createTestEntry("match-1", "node-1");

        registry.save(entry);

        Optional<MatchRegistryEntry> found = registry.findById("match-1");
        assertThat(found).isPresent();
        assertThat(found.get().matchId()).isEqualTo("match-1");
    }

    @Test
    @DisplayName("save() should update an existing match")
    void save_shouldUpdateExistingMatch() {
        MatchRegistryEntry original = createTestEntry("match-1", "node-1");
        registry.save(original);

        MatchRegistryEntry updated = original.finished();
        registry.save(updated);

        Optional<MatchRegistryEntry> found = registry.findById("match-1");
        assertThat(found).isPresent();
        assertThat(found.get().status()).isEqualTo(MatchStatus.FINISHED);
    }

    @Test
    @DisplayName("findById() should return empty for non-existent match")
    void findById_shouldReturnEmptyForNonExistent() {
        Optional<MatchRegistryEntry> found = registry.findById("non-existent");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByNodeId() should return matches for a node")
    void findByNodeId_shouldReturnMatchesForNode() {
        registry.save(createTestEntry("match-1", "node-1"));
        registry.save(createTestEntry("match-2", "node-1"));
        registry.save(createTestEntry("match-3", "node-2"));

        var node1Matches = registry.findByNodeId("node-1");
        var node2Matches = registry.findByNodeId("node-2");

        assertThat(node1Matches).hasSize(2);
        assertThat(node2Matches).hasSize(1);
    }

    @Test
    @DisplayName("findByStatus() should filter matches by status")
    void findByStatus_shouldFilterByStatus() {
        registry.save(createTestEntry("match-1", "node-1").running());
        registry.save(createTestEntry("match-2", "node-1")); // CREATING
        registry.save(createTestEntry("match-3", "node-2").running());

        var runningMatches = registry.findByStatus(MatchStatus.RUNNING);
        var creatingMatches = registry.findByStatus(MatchStatus.CREATING);

        assertThat(runningMatches).hasSize(2);
        assertThat(creatingMatches).hasSize(1);
    }

    @Test
    @DisplayName("findAll() should return all matches")
    void findAll_shouldReturnAllMatches() {
        registry.save(createTestEntry("match-1", "node-1"));
        registry.save(createTestEntry("match-2", "node-2"));
        registry.save(createTestEntry("match-3", "node-3"));

        var matches = registry.findAll();

        assertThat(matches).hasSize(3);
    }

    @Test
    @DisplayName("deleteById() should remove a match")
    void deleteById_shouldRemoveMatch() {
        registry.save(createTestEntry("match-1", "node-1"));

        registry.deleteById("match-1");

        assertThat(registry.findById("match-1")).isEmpty();
    }

    @Test
    @DisplayName("deleteByNodeId() should remove all matches for a node")
    void deleteByNodeId_shouldRemoveAllMatchesForNode() {
        registry.save(createTestEntry("match-1", "node-1"));
        registry.save(createTestEntry("match-2", "node-1"));
        registry.save(createTestEntry("match-3", "node-2"));

        registry.deleteByNodeId("node-1");

        assertThat(registry.findByNodeId("node-1")).isEmpty();
        assertThat(registry.findByNodeId("node-2")).hasSize(1);
    }

    @Test
    @DisplayName("existsById() should return true for existing match")
    void existsById_shouldReturnTrueForExisting() {
        registry.save(createTestEntry("match-1", "node-1"));

        assertThat(registry.existsById("match-1")).isTrue();
    }

    @Test
    @DisplayName("existsById() should return false for non-existent match")
    void existsById_shouldReturnFalseForNonExistent() {
        assertThat(registry.existsById("non-existent")).isFalse();
    }

    @Test
    @DisplayName("countActive() should return count of CREATING and RUNNING matches")
    void countActive_shouldReturnCorrectCount() {
        registry.save(createTestEntry("match-1", "node-1").running());
        registry.save(createTestEntry("match-2", "node-1")); // CREATING
        registry.save(createTestEntry("match-3", "node-2").finished());

        assertThat(registry.countActive()).isEqualTo(2);
    }

    @Test
    @DisplayName("countActiveByNodeId() should return active matches for a node")
    void countActiveByNodeId_shouldReturnCorrectCount() {
        registry.save(createTestEntry("match-1", "node-1").running());
        registry.save(createTestEntry("match-2", "node-1").finished());
        registry.save(createTestEntry("match-3", "node-2").running());

        assertThat(registry.countActiveByNodeId("node-1")).isEqualTo(1);
        assertThat(registry.countActiveByNodeId("node-2")).isEqualTo(1);
        assertThat(registry.countActiveByNodeId("node-3")).isEqualTo(0);
    }

    private MatchRegistryEntry createTestEntry(String matchId, String nodeId) {
        return MatchRegistryEntry.creating(
                matchId,
                nodeId,
                1L,
                List.of("EntityModule", "HealthModule"),
                "http://localhost:8080"
        );
    }

    /**
     * In-memory implementation for contract testing.
     */
    private static class InMemoryMatchRegistry implements MatchRegistry {
        private final Map<String, MatchRegistryEntry> matches = new HashMap<>();

        @Override
        public MatchRegistryEntry save(MatchRegistryEntry entry) {
            matches.put(entry.matchId(), entry);
            return entry;
        }

        @Override
        public Optional<MatchRegistryEntry> findById(String matchId) {
            return Optional.ofNullable(matches.get(matchId));
        }

        @Override
        public List<MatchRegistryEntry> findAll() {
            return new ArrayList<>(matches.values());
        }

        @Override
        public List<MatchRegistryEntry> findByNodeId(String nodeId) {
            return matches.values().stream()
                    .filter(m -> m.nodeId().equals(nodeId))
                    .toList();
        }

        @Override
        public List<MatchRegistryEntry> findByStatus(MatchStatus status) {
            return matches.values().stream()
                    .filter(m -> m.status() == status)
                    .toList();
        }

        @Override
        public void deleteById(String matchId) {
            matches.remove(matchId);
        }

        @Override
        public void deleteByNodeId(String nodeId) {
            matches.entrySet().removeIf(e -> e.getValue().nodeId().equals(nodeId));
        }

        @Override
        public boolean existsById(String matchId) {
            return matches.containsKey(matchId);
        }

        @Override
        public long countActive() {
            return matches.values().stream()
                    .filter(m -> m.status() == MatchStatus.CREATING || m.status() == MatchStatus.RUNNING)
                    .count();
        }

        @Override
        public long countActiveByNodeId(String nodeId) {
            return matches.values().stream()
                    .filter(m -> m.nodeId().equals(nodeId))
                    .filter(m -> m.status() == MatchStatus.CREATING || m.status() == MatchStatus.RUNNING)
                    .count();
        }
    }
}
