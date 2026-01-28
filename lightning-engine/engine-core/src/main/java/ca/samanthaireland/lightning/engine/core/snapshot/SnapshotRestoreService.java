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

import java.util.List;

/**
 * Service interface for restoring match state from persisted snapshots.
 *
 * <p>This service allows restoration of ECS state after server restart
 * by loading snapshot data from MongoDB and reconstructing entities.
 *
 * <p>SOLID compliance:
 * <ul>
 *   <li>SRP: Only handles snapshot restoration</li>
 *   <li>ISP: Focused interface for restore operations</li>
 * </ul>
 */
public interface SnapshotRestoreService {

    /**
     * Restore a match from its latest persisted snapshot.
     *
     * @param matchId the match ID to restore
     * @return result containing restoration details
     */
    RestoreResult restoreMatch(long matchId);

    /**
     * Restore a match to a specific tick.
     *
     * @param matchId the match ID to restore
     * @param tick the specific tick to restore to
     * @return result containing restoration details
     */
    RestoreResult restoreMatch(long matchId, long tick);

    /**
     * Restore all matches that have persisted snapshots.
     *
     * <p>Useful for server startup recovery.
     *
     * @return list of results for each match restoration attempt
     */
    List<RestoreResult> restoreAllMatches();

    /**
     * Check if a match can be restored.
     *
     * <p>Returns true if there are persisted snapshots available for the match.
     *
     * @param matchId the match ID to check
     * @return true if restoration is possible
     */
    boolean canRestore(long matchId);

    /**
     * Result of a match restoration attempt.
     */
    record RestoreResult(
        long matchId,
        long restoredTick,
        int entityCount,
        boolean success,
        String message
    ) {
        /**
         * Create a successful restoration result.
         */
        public static RestoreResult success(long matchId, long tick, int entityCount) {
            return new RestoreResult(matchId, tick, entityCount, true, "Restored successfully");
        }

        /**
         * Create a failed restoration result.
         */
        public static RestoreResult failure(long matchId, String reason) {
            return new RestoreResult(matchId, -1, 0, false, reason);
        }
    }
}
