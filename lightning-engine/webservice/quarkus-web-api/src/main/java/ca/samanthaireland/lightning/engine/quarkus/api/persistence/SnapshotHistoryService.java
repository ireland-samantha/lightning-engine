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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for querying historical snapshots.
 *
 * <p>This service provides a high-level API for snapshot history operations,
 * delegating to the {@link SnapshotHistoryRepository} for actual storage access.
 */
public class SnapshotHistoryService {
    private static final Logger log = LoggerFactory.getLogger(SnapshotHistoryService.class);

    private final SnapshotHistoryRepository repository;

    public SnapshotHistoryService(SnapshotHistoryRepository repository) {
        this.repository = repository;
    }

    // =========================================================================
    // CONTAINER-SCOPED METHODS (preferred for container isolation)
    // =========================================================================

    /**
     * Get a snapshot for a container/match at a specific tick.
     *
     * @param containerId the container ID
     * @param matchId the match ID
     * @param tick the tick number
     * @return the snapshot if found
     */
    public Optional<SnapshotDocument> getSnapshot(long containerId, long matchId, long tick) {
        return repository.findByContainerAndMatchIdAndTick(containerId, matchId, tick);
    }

    /**
     * Get snapshots for a container/match within a tick range.
     *
     * @param containerId the container ID
     * @param matchId the match ID
     * @param fromTick the starting tick (inclusive)
     * @param toTick the ending tick (inclusive)
     * @param limit maximum number of snapshots to return
     * @return list of snapshots ordered by tick
     */
    public List<SnapshotDocument> getSnapshotsInRange(long containerId, long matchId, long fromTick, long toTick, int limit) {
        return repository.findByContainerAndMatchIdAndTickBetween(containerId, matchId, fromTick, toTick, limit);
    }

    /**
     * Get the latest N snapshots for a container/match.
     *
     * @param containerId the container ID
     * @param matchId the match ID
     * @param limit maximum number of snapshots to return
     * @return list of snapshots ordered by tick descending
     */
    public List<SnapshotDocument> getLatestSnapshots(long containerId, long matchId, int limit) {
        return repository.findLatestByContainerAndMatchId(containerId, matchId, limit);
    }

    /**
     * Get the first snapshot for a container/match.
     *
     * @param containerId the container ID
     * @param matchId the match ID
     * @return the first snapshot if any exist
     */
    public Optional<SnapshotDocument> getFirstSnapshot(long containerId, long matchId) {
        return repository.findFirstByContainerAndMatchId(containerId, matchId);
    }

    /**
     * Get the last snapshot for a container/match.
     *
     * @param containerId the container ID
     * @param matchId the match ID
     * @return the last snapshot if any exist
     */
    public Optional<SnapshotDocument> getLastSnapshot(long containerId, long matchId) {
        return repository.findLastByContainerAndMatchId(containerId, matchId);
    }

    /**
     * Count snapshots for a container/match.
     *
     * @param containerId the container ID
     * @param matchId the match ID
     * @return the number of snapshots
     */
    public long countSnapshots(long containerId, long matchId) {
        return repository.countByContainerAndMatchId(containerId, matchId);
    }

    /**
     * Count all snapshots for a container.
     *
     * @param containerId the container ID
     * @return the number of snapshots
     */
    public long countContainerSnapshots(long containerId) {
        return repository.countByContainerId(containerId);
    }

    /**
     * Get all unique match IDs that have snapshots in a container.
     *
     * @param containerId the container ID
     * @return list of match IDs
     */
    public List<Long> getMatchIds(long containerId) {
        return repository.findDistinctMatchIdsByContainerId(containerId);
    }

    /**
     * Delete all snapshots for a container/match.
     *
     * @param containerId the container ID
     * @param matchId the match ID
     * @return the number of deleted snapshots
     */
    public long deleteSnapshots(long containerId, long matchId) {
        long deleted = repository.deleteByContainerAndMatchId(containerId, matchId);
        log.info("Deleted {} snapshots for container {} match {}", deleted, containerId, matchId);
        return deleted;
    }

    /**
     * Delete snapshots older than a given tick.
     *
     * @param containerId the container ID
     * @param matchId the match ID
     * @param olderThanTick delete snapshots with tick < this value
     * @return the number of deleted snapshots
     */
    public long deleteSnapshotsOlderThan(long containerId, long matchId, long olderThanTick) {
        long deleted = repository.deleteByContainerAndMatchIdAndTickLessThan(containerId, matchId, olderThanTick);
        log.info("Deleted {} snapshots for container {} match {} older than tick {}", deleted, containerId, matchId, olderThanTick);
        return deleted;
    }

    /**
     * Get summary info about a container's stored history.
     *
     * @param containerId the container ID
     * @return summary containing counts and match info
     */
    public ContainerHistorySummary getContainerSummary(long containerId) {
        List<Long> matchIds = getMatchIds(containerId);
        long totalSnapshots = countContainerSnapshots(containerId);
        return new ContainerHistorySummary(containerId, totalSnapshots, matchIds.size(), matchIds);
    }

    /**
     * Get summary info about a specific match's history within a container.
     *
     * @param containerId the container ID
     * @param matchId the match ID
     * @return summary for the match
     */
    public MatchHistorySummary getMatchSummary(long containerId, long matchId) {
        long count = countSnapshots(containerId, matchId);
        Optional<SnapshotDocument> first = getFirstSnapshot(containerId, matchId);
        Optional<SnapshotDocument> last = getLastSnapshot(containerId, matchId);

        return new MatchHistorySummary(
                matchId,
                count,
                first.map(SnapshotDocument::tick).orElse(null),
                last.map(SnapshotDocument::tick).orElse(null),
                first.map(SnapshotDocument::timestamp).orElse(null),
                last.map(SnapshotDocument::timestamp).orElse(null)
        );
    }

    // =========================================================================
    // LEGACY METHODS (for backward compatibility, query across all containers)
    // =========================================================================

    /**
     * Get a snapshot for a match at a specific tick.
     *
     * @param matchId the match ID
     * @param tick the tick number
     * @return the snapshot if found
     */
    public Optional<SnapshotDocument> getSnapshot(long matchId, long tick) {
        return repository.findByMatchIdAndTick(matchId, tick);
    }

    /**
     * Get snapshots for a match within a tick range.
     *
     * @param matchId the match ID
     * @param fromTick the starting tick (inclusive)
     * @param toTick the ending tick (inclusive)
     * @param limit maximum number of snapshots to return
     * @return list of snapshots ordered by tick
     */
    public List<SnapshotDocument> getSnapshotsInRange(long matchId, long fromTick, long toTick, int limit) {
        return repository.findByMatchIdAndTickBetween(matchId, fromTick, toTick, limit);
    }

    /**
     * Get snapshots for a match within a time range.
     *
     * @param matchId the match ID
     * @param from the start timestamp (inclusive)
     * @param to the end timestamp (inclusive)
     * @param limit maximum number of snapshots to return
     * @return list of snapshots ordered by timestamp
     */
    public List<SnapshotDocument> getSnapshotsInTimeRange(long matchId, Instant from, Instant to, int limit) {
        return repository.findByMatchIdAndTimestampBetween(matchId, from, to, limit);
    }

    /**
     * Get the latest N snapshots for a match.
     *
     * @param matchId the match ID
     * @param limit maximum number of snapshots to return
     * @return list of snapshots ordered by tick descending
     */
    public List<SnapshotDocument> getLatestSnapshots(long matchId, int limit) {
        return repository.findLatestByMatchId(matchId, limit);
    }

    /**
     * Get the first snapshot for a match.
     *
     * @param matchId the match ID
     * @return the first snapshot if any exist
     */
    public Optional<SnapshotDocument> getFirstSnapshot(long matchId) {
        return repository.findFirstByMatchId(matchId);
    }

    /**
     * Get the last snapshot for a match.
     *
     * @param matchId the match ID
     * @return the last snapshot if any exist
     */
    public Optional<SnapshotDocument> getLastSnapshot(long matchId) {
        return repository.findLastByMatchId(matchId);
    }

    /**
     * Count snapshots for a match.
     *
     * @param matchId the match ID
     * @return the number of snapshots
     */
    public long countSnapshots(long matchId) {
        return repository.countByMatchId(matchId);
    }

    /**
     * Count all snapshots in the database.
     *
     * @return the total number of snapshots
     */
    public long countAllSnapshots() {
        return repository.countAll();
    }

    /**
     * Get all unique match IDs that have snapshots.
     *
     * @return list of match IDs
     */
    public List<Long> getMatchIds() {
        return repository.findDistinctMatchIds();
    }

    /**
     * Delete all snapshots for a match.
     *
     * @param matchId the match ID
     * @return the number of deleted snapshots
     */
    public long deleteSnapshots(long matchId) {
        long deleted = repository.deleteByMatchId(matchId);
        log.info("Deleted {} snapshots for match {}", deleted, matchId);
        return deleted;
    }

    /**
     * Delete snapshots older than a given tick.
     *
     * @param matchId the match ID
     * @param olderThanTick delete snapshots with tick < this value
     * @return the number of deleted snapshots
     */
    public long deleteSnapshotsOlderThan(long matchId, long olderThanTick) {
        long deleted = repository.deleteByMatchIdAndTickLessThan(matchId, olderThanTick);
        log.info("Deleted {} snapshots for match {} older than tick {}", deleted, matchId, olderThanTick);
        return deleted;
    }

    /**
     * Get summary info about stored history.
     *
     * @return summary containing counts and match info
     */
    public HistorySummary getSummary() {
        List<Long> matchIds = getMatchIds();
        long totalSnapshots = countAllSnapshots();
        return new HistorySummary(totalSnapshots, matchIds.size(), matchIds);
    }

    /**
     * Get summary info about a specific match's history.
     *
     * @param matchId the match ID
     * @return summary for the match
     */
    public MatchHistorySummary getMatchSummary(long matchId) {
        long count = countSnapshots(matchId);
        Optional<SnapshotDocument> first = getFirstSnapshot(matchId);
        Optional<SnapshotDocument> last = getLastSnapshot(matchId);

        return new MatchHistorySummary(
                matchId,
                count,
                first.map(SnapshotDocument::tick).orElse(null),
                last.map(SnapshotDocument::tick).orElse(null),
                first.map(SnapshotDocument::timestamp).orElse(null),
                last.map(SnapshotDocument::timestamp).orElse(null)
        );
    }

    /**
     * Overall history summary.
     */
    public record HistorySummary(
            long totalSnapshots,
            int matchCount,
            List<Long> matchIds
    ) {}

    /**
     * Container-specific history summary.
     */
    public record ContainerHistorySummary(
            long containerId,
            long totalSnapshots,
            int matchCount,
            List<Long> matchIds
    ) {}

    /**
     * Match-specific history summary.
     */
    public record MatchHistorySummary(
            long matchId,
            long snapshotCount,
            Long firstTick,
            Long lastTick,
            Instant firstTimestamp,
            Instant lastTimestamp
    ) {}
}
