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


package ca.samanthaireland.lightning.engine.internal.core.snapshot;

import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;
import ca.samanthaireland.lightning.engine.core.snapshot.SnapshotHistory.TickedSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InMemorySnapshotHistoryTest {

    private InMemorySnapshotHistory history;

    @BeforeEach
    void setUp() {
        history = new InMemorySnapshotHistory(5); // Small limit for testing
    }

    @Test
    void recordSnapshot_storesSnapshot() {
        // Given
        Snapshot snapshot = createSnapshot(1L, 100.0f);

        // When
        history.recordSnapshot(1L, 10L, snapshot);

        // Then
        assertThat(history.getSnapshot(1L, 10L)).isPresent();
        assertThat(history.getSnapshotCount(1L)).isEqualTo(1);
    }

    @Test
    void getSnapshot_nonExistentTick_returnsEmpty() {
        // Given
        history.recordSnapshot(1L, 10L, createSnapshot(1L, 100.0f));

        // When/Then
        assertThat(history.getSnapshot(1L, 5L)).isEmpty();
        assertThat(history.getSnapshot(1L, 15L)).isEmpty();
    }

    @Test
    void getSnapshot_nonExistentMatch_returnsEmpty() {
        // Given
        history.recordSnapshot(1L, 10L, createSnapshot(1L, 100.0f));

        // When/Then
        assertThat(history.getSnapshot(2L, 10L)).isEmpty();
    }

    @Test
    void getLatestSnapshot_returnsLatest() {
        // Given
        history.recordSnapshot(1L, 10L, createSnapshot(1L, 100.0f));
        history.recordSnapshot(1L, 20L, createSnapshot(1L, 200.0f));
        history.recordSnapshot(1L, 15L, createSnapshot(1L, 150.0f));

        // When
        Optional<TickedSnapshot> latest = history.getLatestSnapshot(1L);

        // Then
        assertThat(latest).isPresent();
        assertThat(latest.get().tick()).isEqualTo(20L);
    }

    @Test
    void getOldestSnapshot_returnsOldest() {
        // Given
        history.recordSnapshot(1L, 20L, createSnapshot(1L, 200.0f));
        history.recordSnapshot(1L, 10L, createSnapshot(1L, 100.0f));
        history.recordSnapshot(1L, 15L, createSnapshot(1L, 150.0f));

        // When
        Optional<TickedSnapshot> oldest = history.getOldestSnapshot(1L);

        // Then
        assertThat(oldest).isPresent();
        assertThat(oldest.get().tick()).isEqualTo(10L);
    }

    @Test
    void recordSnapshot_evictsOldWhenOverLimit() {
        // Given - history has limit of 5
        for (int i = 1; i <= 7; i++) {
            history.recordSnapshot(1L, i, createSnapshot(1L, i * 10.0f));
        }

        // Then - only last 5 should remain
        assertThat(history.getSnapshotCount(1L)).isEqualTo(5);
        assertThat(history.getSnapshot(1L, 1L)).isEmpty(); // Evicted
        assertThat(history.getSnapshot(1L, 2L)).isEmpty(); // Evicted
        assertThat(history.getSnapshot(1L, 3L)).isPresent(); // Still there
        assertThat(history.getSnapshot(1L, 7L)).isPresent(); // Still there
    }

    @Test
    void clearHistory_removesAllForMatch() {
        // Given
        history.recordSnapshot(1L, 10L, createSnapshot(1L, 100.0f));
        history.recordSnapshot(1L, 20L, createSnapshot(1L, 200.0f));
        history.recordSnapshot(2L, 10L, createSnapshot(2L, 100.0f));

        // When
        history.clearHistory(1L);

        // Then
        assertThat(history.getSnapshotCount(1L)).isZero();
        assertThat(history.getSnapshot(1L, 10L)).isEmpty();
        assertThat(history.getSnapshot(2L, 10L)).isPresent(); // Unaffected
    }

    @Test
    void getLatestSnapshot_emptyHistory_returnsEmpty() {
        assertThat(history.getLatestSnapshot(1L)).isEmpty();
    }

    @Test
    void getOldestSnapshot_emptyHistory_returnsEmpty() {
        assertThat(history.getOldestSnapshot(1L)).isEmpty();
    }

    @Test
    void getAvailableTicks_returnsAllStoredTicks() {
        // Given
        history.recordSnapshot(1L, 10L, createSnapshot(1L, 100.0f));
        history.recordSnapshot(1L, 20L, createSnapshot(1L, 200.0f));
        history.recordSnapshot(1L, 15L, createSnapshot(1L, 150.0f));

        // When
        Set<Long> ticks = history.getAvailableTicks(1L);

        // Then
        assertThat(ticks).containsExactlyInAnyOrder(10L, 15L, 20L);
    }

    @Test
    void isolatesMatchHistories() {
        // Given
        history.recordSnapshot(1L, 10L, createSnapshot(1L, 100.0f));
        history.recordSnapshot(2L, 10L, createSnapshot(2L, 200.0f));

        // When
        Snapshot match1Snapshot = history.getSnapshot(1L, 10L).orElseThrow();
        Snapshot match2Snapshot = history.getSnapshot(2L, 10L).orElseThrow();

        // Then - verify they are different snapshots
        float match1Value = match1Snapshot.toLegacyFormat().get("TestModule").get("POSITION_X").get(0);
        float match2Value = match2Snapshot.toLegacyFormat().get("TestModule").get("POSITION_X").get(0);

        assertThat(match1Value).isEqualTo(100.0f);
        assertThat(match2Value).isEqualTo(200.0f);
    }

    @Test
    void defaultConstructor_usesDefaultLimit() {
        InMemorySnapshotHistory defaultHistory = new InMemorySnapshotHistory();
        assertThat(defaultHistory.getMaxSnapshotsPerMatch()).isEqualTo(100);
    }

    private Snapshot createSnapshot(long entityId, float positionX) {
        return Snapshot.fromLegacyFormat(Map.of(
                "TestModule", Map.of(
                        "ENTITY_ID", List.of((float) entityId),
                        "POSITION_X", List.of(positionX)
                )
        ));
    }
}
