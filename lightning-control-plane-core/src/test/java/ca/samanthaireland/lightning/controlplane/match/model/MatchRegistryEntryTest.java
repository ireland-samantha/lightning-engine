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

package ca.samanthaireland.lightning.controlplane.match.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class MatchRegistryEntryTest {

    @Test
    void creating_setsStatusToCreating() {
        // Act
        MatchRegistryEntry entry = MatchRegistryEntry.creating(
                "match-1", "node-1", 42L, List.of("entity-module"), "http://localhost:8080"
        );

        // Assert
        assertThat(entry.status()).isEqualTo(MatchStatus.CREATING);
        assertThat(entry.playerCount()).isEqualTo(0);
        assertThat(entry.createdAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void creating_buildsCorrectWebsocketUrl_http() {
        // Act
        MatchRegistryEntry entry = MatchRegistryEntry.creating(
                "match-1", "node-1", 42L, List.of("entity-module"), "http://localhost:8080"
        );

        // Assert
        assertThat(entry.websocketUrl()).isEqualTo("ws://localhost:8080/ws/containers/42/matches/match-1/snapshots");
    }

    @Test
    void creating_buildsCorrectWebsocketUrl_https() {
        // Act
        MatchRegistryEntry entry = MatchRegistryEntry.creating(
                "match-1", "node-1", 42L, List.of("entity-module"), "https://example.com"
        );

        // Assert
        assertThat(entry.websocketUrl()).isEqualTo("wss://example.com/ws/containers/42/matches/match-1/snapshots");
    }

    @Test
    void running_changesStatusToRunning() {
        // Arrange
        MatchRegistryEntry entry = MatchRegistryEntry.creating(
                "match-1", "node-1", 42L, List.of("entity-module"), "http://localhost:8080"
        );

        // Act
        MatchRegistryEntry running = entry.running();

        // Assert
        assertThat(running.status()).isEqualTo(MatchStatus.RUNNING);
        assertThat(running.matchId()).isEqualTo("match-1"); // Other fields preserved
        assertThat(running.createdAt()).isEqualTo(entry.createdAt());
    }

    @Test
    void finished_changesStatusToFinished() {
        // Arrange
        MatchRegistryEntry entry = MatchRegistryEntry.creating(
                "match-1", "node-1", 42L, List.of("entity-module"), "http://localhost:8080"
        ).running();

        // Act
        MatchRegistryEntry finished = entry.finished();

        // Assert
        assertThat(finished.status()).isEqualTo(MatchStatus.FINISHED);
    }

    @Test
    void error_changesStatusToError() {
        // Arrange
        MatchRegistryEntry entry = MatchRegistryEntry.creating(
                "match-1", "node-1", 42L, List.of("entity-module"), "http://localhost:8080"
        );

        // Act
        MatchRegistryEntry error = entry.error();

        // Assert
        assertThat(error.status()).isEqualTo(MatchStatus.ERROR);
    }

    @Test
    void withPlayerCount_updatesPlayerCount() {
        // Arrange
        MatchRegistryEntry entry = MatchRegistryEntry.creating(
                "match-1", "node-1", 42L, List.of("entity-module"), "http://localhost:8080"
        ).running();

        // Act
        MatchRegistryEntry updated = entry.withPlayerCount(5);

        // Assert
        assertThat(updated.playerCount()).isEqualTo(5);
        assertThat(updated.status()).isEqualTo(MatchStatus.RUNNING); // Status preserved
    }

    @Test
    void constructor_nullMatchId_throwsException() {
        // Act & Assert
        assertThatThrownBy(() -> new MatchRegistryEntry(
                null, "node-1", 42L, MatchStatus.RUNNING, Instant.now(),
                List.of("module"), "http://localhost:8080", "ws://localhost:8080/ws", 0
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_blankMatchId_throwsException() {
        // Act & Assert
        assertThatThrownBy(() -> new MatchRegistryEntry(
                "  ", "node-1", 42L, MatchStatus.RUNNING, Instant.now(),
                List.of("module"), "http://localhost:8080", "ws://localhost:8080/ws", 0
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("matchId cannot be blank");
    }

    @Test
    void constructor_negativePlayerCount_throwsException() {
        // Act & Assert
        assertThatThrownBy(() -> new MatchRegistryEntry(
                "match-1", "node-1", 42L, MatchStatus.RUNNING, Instant.now(),
                List.of("module"), "http://localhost:8080", "ws://localhost:8080/ws", -1
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("playerCount cannot be negative");
    }

    @Test
    void constructor_nullNodeId_throwsException() {
        // Act & Assert
        assertThatThrownBy(() -> new MatchRegistryEntry(
                "match-1", null, 42L, MatchStatus.RUNNING, Instant.now(),
                List.of("module"), "http://localhost:8080", "ws://localhost:8080/ws", 0
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_nullStatus_throwsException() {
        // Act & Assert
        assertThatThrownBy(() -> new MatchRegistryEntry(
                "match-1", "node-1", 42L, null, Instant.now(),
                List.of("module"), "http://localhost:8080", "ws://localhost:8080/ws", 0
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_nullModuleNames_throwsException() {
        // Act & Assert
        assertThatThrownBy(() -> new MatchRegistryEntry(
                "match-1", "node-1", 42L, MatchStatus.RUNNING, Instant.now(),
                null, "http://localhost:8080", "ws://localhost:8080/ws", 0
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void statusTransitions_preserveOtherFields() {
        // Arrange
        MatchRegistryEntry original = MatchRegistryEntry.creating(
                "match-1", "node-1", 42L, List.of("module-a", "module-b"), "http://localhost:8080"
        );

        // Act - chain transitions
        MatchRegistryEntry running = original.running().withPlayerCount(5);
        MatchRegistryEntry finished = running.finished();

        // Assert - all fields preserved through transitions
        assertThat(finished.matchId()).isEqualTo("match-1");
        assertThat(finished.nodeId()).isEqualTo("node-1");
        assertThat(finished.containerId()).isEqualTo(42L);
        assertThat(finished.moduleNames()).containsExactly("module-a", "module-b");
        assertThat(finished.advertiseAddress()).isEqualTo("http://localhost:8080");
        assertThat(finished.playerCount()).isEqualTo(5);
        assertThat(finished.createdAt()).isEqualTo(original.createdAt());
    }
}
