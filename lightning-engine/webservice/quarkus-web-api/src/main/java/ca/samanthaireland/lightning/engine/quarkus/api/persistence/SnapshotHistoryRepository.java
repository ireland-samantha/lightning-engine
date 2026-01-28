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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for snapshot history persistence.
 *
 * <p>This abstraction allows for different storage backends (MongoDB, in-memory, etc.)
 * without changing the service layer.
 */
public interface SnapshotHistoryRepository {

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
    Optional<SnapshotDocument> findByContainerAndMatchIdAndTick(long containerId, long matchId, long tick);

    /**
     * Get snapshots for a container/match within a tick range.
     *
     * @param containerId the container ID
     * @param matchId the match ID
     * @param fromTick the starting tick (inclusive)
     * @param toTick the ending tick (inclusive)
     * @param limit maximum number of snapshots to return
     * @return list of snapshots ordered by tick ascending
     */
    List<SnapshotDocument> findByContainerAndMatchIdAndTickBetween(long containerId, long matchId, long fromTick, long toTick, int limit);

    /**
     * Get the latest N snapshots for a container/match.
     *
     * @param containerId the container ID
     * @param matchId the match ID
     * @param limit maximum number of snapshots to return
     * @return list of snapshots ordered by tick descending
     */
    List<SnapshotDocument> findLatestByContainerAndMatchId(long containerId, long matchId, int limit);

    /**
     * Get the first snapshot for a container/match (earliest tick).
     *
     * @param containerId the container ID
     * @param matchId the match ID
     * @return the first snapshot if any exist
     */
    Optional<SnapshotDocument> findFirstByContainerAndMatchId(long containerId, long matchId);

    /**
     * Get the last snapshot for a container/match (latest tick).
     *
     * @param containerId the container ID
     * @param matchId the match ID
     * @return the last snapshot if any exist
     */
    Optional<SnapshotDocument> findLastByContainerAndMatchId(long containerId, long matchId);

    /**
     * Count snapshots for a container/match.
     *
     * @param containerId the container ID
     * @param matchId the match ID
     * @return the number of snapshots
     */
    long countByContainerAndMatchId(long containerId, long matchId);

    /**
     * Count all snapshots for a container.
     *
     * @param containerId the container ID
     * @return the number of snapshots
     */
    long countByContainerId(long containerId);

    /**
     * Get all unique match IDs that have snapshots in a container.
     *
     * @param containerId the container ID
     * @return list of match IDs
     */
    List<Long> findDistinctMatchIdsByContainerId(long containerId);

    /**
     * Delete all snapshots for a container/match.
     *
     * @param containerId the container ID
     * @param matchId the match ID
     * @return the number of deleted snapshots
     */
    long deleteByContainerAndMatchId(long containerId, long matchId);

    /**
     * Delete snapshots with tick less than specified value.
     *
     * @param containerId the container ID
     * @param matchId the match ID
     * @param olderThanTick delete snapshots with tick < this value
     * @return the number of deleted snapshots
     */
    long deleteByContainerAndMatchIdAndTickLessThan(long containerId, long matchId, long olderThanTick);

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
    Optional<SnapshotDocument> findByMatchIdAndTick(long matchId, long tick);

    /**
     * Get snapshots for a match within a tick range.
     *
     * @param matchId the match ID
     * @param fromTick the starting tick (inclusive)
     * @param toTick the ending tick (inclusive)
     * @param limit maximum number of snapshots to return
     * @return list of snapshots ordered by tick ascending
     */
    List<SnapshotDocument> findByMatchIdAndTickBetween(long matchId, long fromTick, long toTick, int limit);

    /**
     * Get snapshots for a match within a time range.
     *
     * @param matchId the match ID
     * @param from the start timestamp (inclusive)
     * @param to the end timestamp (inclusive)
     * @param limit maximum number of snapshots to return
     * @return list of snapshots ordered by timestamp ascending
     */
    List<SnapshotDocument> findByMatchIdAndTimestampBetween(long matchId, Instant from, Instant to, int limit);

    /**
     * Get the latest N snapshots for a match.
     *
     * @param matchId the match ID
     * @param limit maximum number of snapshots to return
     * @return list of snapshots ordered by tick descending
     */
    List<SnapshotDocument> findLatestByMatchId(long matchId, int limit);

    /**
     * Get the first snapshot for a match (earliest tick).
     *
     * @param matchId the match ID
     * @return the first snapshot if any exist
     */
    Optional<SnapshotDocument> findFirstByMatchId(long matchId);

    /**
     * Get the last snapshot for a match (latest tick).
     *
     * @param matchId the match ID
     * @return the last snapshot if any exist
     */
    Optional<SnapshotDocument> findLastByMatchId(long matchId);

    /**
     * Count snapshots for a match.
     *
     * @param matchId the match ID
     * @return the number of snapshots
     */
    long countByMatchId(long matchId);

    /**
     * Count all snapshots in the repository.
     *
     * @return the total number of snapshots
     */
    long countAll();

    /**
     * Get all unique match IDs that have snapshots.
     *
     * @return list of match IDs
     */
    List<Long> findDistinctMatchIds();

    /**
     * Save a snapshot.
     *
     * @param snapshot the snapshot to save
     * @return the saved snapshot (with generated ID if new)
     */
    SnapshotDocument save(SnapshotDocument snapshot);

    /**
     * Delete all snapshots for a match.
     *
     * @param matchId the match ID
     * @return the number of deleted snapshots
     */
    long deleteByMatchId(long matchId);

    /**
     * Delete snapshots with tick less than specified value.
     *
     * @param matchId the match ID
     * @param olderThanTick delete snapshots with tick < this value
     * @return the number of deleted snapshots
     */
    long deleteByMatchIdAndTickLessThan(long matchId, long olderThanTick);
}
