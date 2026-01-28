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

package ca.samanthaireland.lightning.engine.quarkus.api.persistence;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SnapshotHistoryService")
class SnapshotHistoryServiceTest {

    @Mock
    private SnapshotHistoryRepository repository;

    private SnapshotHistoryService service;

    private static final long CONTAINER_ID = 1L;
    private static final long MATCH_ID = 42L;

    @BeforeEach
    void setUp() {
        service = new SnapshotHistoryService(repository);
    }

    private SnapshotDocument createSnapshot(long containerId, long matchId, long tick) {
        return new SnapshotDocument(
                new ObjectId(),
                containerId,
                matchId,
                tick,
                Instant.now(),
                Map.of("TestModule", Map.of("COMPONENT", List.of(1.0f)))
        );
    }

    @Nested
    @DisplayName("container-scoped operations")
    class ContainerScopedOperations {

        @Test
        @DisplayName("getSnapshot should delegate to repository with container ID")
        void getSnapshotDelegatesToRepository() {
            SnapshotDocument expected = createSnapshot(CONTAINER_ID, MATCH_ID, 10);
            when(repository.findByContainerAndMatchIdAndTick(CONTAINER_ID, MATCH_ID, 10))
                    .thenReturn(Optional.of(expected));

            Optional<SnapshotDocument> result = service.getSnapshot(CONTAINER_ID, MATCH_ID, 10);

            assertThat(result).isPresent().contains(expected);
            verify(repository).findByContainerAndMatchIdAndTick(CONTAINER_ID, MATCH_ID, 10);
        }

        @Test
        @DisplayName("getSnapshotsInRange should delegate with tick range")
        void getSnapshotsInRangeDelegatesToRepository() {
            List<SnapshotDocument> expected = List.of(
                    createSnapshot(CONTAINER_ID, MATCH_ID, 5),
                    createSnapshot(CONTAINER_ID, MATCH_ID, 6),
                    createSnapshot(CONTAINER_ID, MATCH_ID, 7)
            );
            when(repository.findByContainerAndMatchIdAndTickBetween(CONTAINER_ID, MATCH_ID, 5, 10, 100))
                    .thenReturn(expected);

            List<SnapshotDocument> result = service.getSnapshotsInRange(CONTAINER_ID, MATCH_ID, 5, 10, 100);

            assertThat(result).isEqualTo(expected);
            verify(repository).findByContainerAndMatchIdAndTickBetween(CONTAINER_ID, MATCH_ID, 5, 10, 100);
        }

        @Test
        @DisplayName("getLatestSnapshots should delegate with limit")
        void getLatestSnapshotsDelegatesToRepository() {
            List<SnapshotDocument> expected = List.of(
                    createSnapshot(CONTAINER_ID, MATCH_ID, 10),
                    createSnapshot(CONTAINER_ID, MATCH_ID, 9)
            );
            when(repository.findLatestByContainerAndMatchId(CONTAINER_ID, MATCH_ID, 5))
                    .thenReturn(expected);

            List<SnapshotDocument> result = service.getLatestSnapshots(CONTAINER_ID, MATCH_ID, 5);

            assertThat(result).isEqualTo(expected);
            verify(repository).findLatestByContainerAndMatchId(CONTAINER_ID, MATCH_ID, 5);
        }

        @Test
        @DisplayName("countSnapshots should delegate to repository")
        void countSnapshotsDelegatesToRepository() {
            when(repository.countByContainerAndMatchId(CONTAINER_ID, MATCH_ID)).thenReturn(100L);

            long count = service.countSnapshots(CONTAINER_ID, MATCH_ID);

            assertThat(count).isEqualTo(100L);
            verify(repository).countByContainerAndMatchId(CONTAINER_ID, MATCH_ID);
        }

        @Test
        @DisplayName("deleteSnapshots should delegate and return count")
        void deleteSnapshotsDelegatesToRepository() {
            when(repository.deleteByContainerAndMatchId(CONTAINER_ID, MATCH_ID)).thenReturn(50L);

            long deleted = service.deleteSnapshots(CONTAINER_ID, MATCH_ID);

            assertThat(deleted).isEqualTo(50L);
            verify(repository).deleteByContainerAndMatchId(CONTAINER_ID, MATCH_ID);
        }
    }

    @Nested
    @DisplayName("summary operations")
    class SummaryOperations {

        @Test
        @DisplayName("getContainerSummary should aggregate data from repository")
        void getContainerSummaryAggregatesData() {
            when(repository.findDistinctMatchIdsByContainerId(CONTAINER_ID))
                    .thenReturn(List.of(1L, 2L, 3L));
            when(repository.countByContainerId(CONTAINER_ID)).thenReturn(150L);

            SnapshotHistoryService.ContainerHistorySummary summary = service.getContainerSummary(CONTAINER_ID);

            assertThat(summary.containerId()).isEqualTo(CONTAINER_ID);
            assertThat(summary.totalSnapshots()).isEqualTo(150L);
            assertThat(summary.matchCount()).isEqualTo(3);
            assertThat(summary.matchIds()).containsExactly(1L, 2L, 3L);
        }

        @Test
        @DisplayName("getMatchSummary should return summary with first and last tick")
        void getMatchSummaryReturnsCorrectData() {
            SnapshotDocument first = createSnapshot(CONTAINER_ID, MATCH_ID, 1);
            SnapshotDocument last = createSnapshot(CONTAINER_ID, MATCH_ID, 100);

            when(repository.countByContainerAndMatchId(CONTAINER_ID, MATCH_ID)).thenReturn(100L);
            when(repository.findFirstByContainerAndMatchId(CONTAINER_ID, MATCH_ID))
                    .thenReturn(Optional.of(first));
            when(repository.findLastByContainerAndMatchId(CONTAINER_ID, MATCH_ID))
                    .thenReturn(Optional.of(last));

            SnapshotHistoryService.MatchHistorySummary summary = service.getMatchSummary(CONTAINER_ID, MATCH_ID);

            assertThat(summary.matchId()).isEqualTo(MATCH_ID);
            assertThat(summary.snapshotCount()).isEqualTo(100L);
            assertThat(summary.firstTick()).isEqualTo(1L);
            assertThat(summary.lastTick()).isEqualTo(100L);
            assertThat(summary.firstTimestamp()).isNotNull();
            assertThat(summary.lastTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("getMatchSummary should handle empty history")
        void getMatchSummaryHandlesEmptyHistory() {
            when(repository.countByContainerAndMatchId(CONTAINER_ID, MATCH_ID)).thenReturn(0L);
            when(repository.findFirstByContainerAndMatchId(CONTAINER_ID, MATCH_ID))
                    .thenReturn(Optional.empty());
            when(repository.findLastByContainerAndMatchId(CONTAINER_ID, MATCH_ID))
                    .thenReturn(Optional.empty());

            SnapshotHistoryService.MatchHistorySummary summary = service.getMatchSummary(CONTAINER_ID, MATCH_ID);

            assertThat(summary.matchId()).isEqualTo(MATCH_ID);
            assertThat(summary.snapshotCount()).isEqualTo(0L);
            assertThat(summary.firstTick()).isNull();
            assertThat(summary.lastTick()).isNull();
        }
    }

    @Nested
    @DisplayName("legacy operations (backward compatibility)")
    class LegacyOperations {

        @Test
        @DisplayName("legacy getSnapshot should work without container ID")
        void legacyGetSnapshotWorks() {
            SnapshotDocument expected = createSnapshot(0, MATCH_ID, 10);
            when(repository.findByMatchIdAndTick(MATCH_ID, 10)).thenReturn(Optional.of(expected));

            Optional<SnapshotDocument> result = service.getSnapshot(MATCH_ID, 10);

            assertThat(result).isPresent();
            verify(repository).findByMatchIdAndTick(MATCH_ID, 10);
        }

        @Test
        @DisplayName("legacy getLatestSnapshots should work without container ID")
        void legacyGetLatestSnapshotsWorks() {
            List<SnapshotDocument> expected = List.of(createSnapshot(0, MATCH_ID, 10));
            when(repository.findLatestByMatchId(MATCH_ID, 5)).thenReturn(expected);

            List<SnapshotDocument> result = service.getLatestSnapshots(MATCH_ID, 5);

            assertThat(result).isEqualTo(expected);
            verify(repository).findLatestByMatchId(MATCH_ID, 5);
        }

        @Test
        @DisplayName("getSummary should return overall summary")
        void getSummaryReturnsOverallData() {
            when(repository.findDistinctMatchIds()).thenReturn(List.of(1L, 2L));
            when(repository.countAll()).thenReturn(1000L);

            SnapshotHistoryService.HistorySummary summary = service.getSummary();

            assertThat(summary.totalSnapshots()).isEqualTo(1000L);
            assertThat(summary.matchCount()).isEqualTo(2);
            assertThat(summary.matchIds()).containsExactly(1L, 2L);
        }
    }
}
