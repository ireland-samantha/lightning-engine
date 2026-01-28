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

package ca.samanthaireland.lightning.engine.api.resource.adapter;

import ca.samanthaireland.lightning.engine.api.resource.adapter.dto.HistoryQueryParams;
import ca.samanthaireland.lightning.engine.api.resource.adapter.dto.HistorySnapshotDto;
import ca.samanthaireland.lightning.engine.api.resource.adapter.dto.MatchHistorySummaryDto;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Adapter interface for historical snapshot operations.
 *
 * <p>Handles retrieval of persisted snapshots from MongoDB history.
 */
public interface HistoryAdapter {

    /**
     * Get history summary for a match.
     *
     * @param matchId the match ID
     * @return the history summary
     */
    MatchHistorySummaryDto getMatchHistorySummary(long matchId) throws IOException;

    /**
     * Get historical snapshots for a match.
     *
     * @param matchId the match ID
     * @param params query parameters (tick range, limit)
     * @return list of historical snapshots
     */
    List<HistorySnapshotDto> getHistorySnapshots(long matchId, HistoryQueryParams params) throws IOException;

    /**
     * Get the latest historical snapshots for a match.
     *
     * @param matchId the match ID
     * @param limit maximum snapshots to return
     * @return list of latest snapshots (ordered by tick descending)
     */
    List<HistorySnapshotDto> getLatestHistorySnapshots(long matchId, int limit) throws IOException;

    /**
     * Get a specific historical snapshot by tick.
     *
     * @param matchId the match ID
     * @param tick the tick number
     * @return the snapshot if found
     */
    Optional<HistorySnapshotDto> getHistorySnapshotAtTick(long matchId, long tick) throws IOException;
}
