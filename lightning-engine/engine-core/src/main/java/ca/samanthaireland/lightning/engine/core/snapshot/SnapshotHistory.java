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


package ca.samanthaireland.lightning.engine.core.snapshot;

import java.util.Optional;

/**
 * Maintains a history of snapshots for delta computation.
 *
 * <p>The history allows looking up previous snapshots by tick number,
 * enabling delta computation between any two historical points.
 */
public interface SnapshotHistory {

    /**
     * Record a snapshot for a match at a specific tick.
     *
     * @param matchId the match ID
     * @param tick the tick number
     * @param snapshot the snapshot to record
     */
    void recordSnapshot(long matchId, long tick, Snapshot snapshot);

    /**
     * Get a historical snapshot for a match at a specific tick.
     *
     * @param matchId the match ID
     * @param tick the tick number
     * @return the snapshot if available, empty if not in history
     */
    Optional<Snapshot> getSnapshot(long matchId, long tick);

    /**
     * Get the latest recorded snapshot for a match.
     *
     * @param matchId the match ID
     * @return the latest snapshot if available
     */
    Optional<TickedSnapshot> getLatestSnapshot(long matchId);

    /**
     * Get the oldest recorded snapshot for a match.
     *
     * @param matchId the match ID
     * @return the oldest snapshot still in history
     */
    Optional<TickedSnapshot> getOldestSnapshot(long matchId);

    /**
     * Clear all history for a match.
     *
     * @param matchId the match ID
     */
    void clearHistory(long matchId);

    /**
     * Get the number of snapshots stored for a match.
     *
     * @param matchId the match ID
     * @return the count of stored snapshots
     */
    int getSnapshotCount(long matchId);

    /**
     * A snapshot with its associated tick number.
     */
    record TickedSnapshot(long tick, Snapshot snapshot) {}
}
