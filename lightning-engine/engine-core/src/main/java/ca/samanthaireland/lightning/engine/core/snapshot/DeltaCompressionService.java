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

/**
 * Service for computing delta (difference) between snapshots.
 *
 * <p>Delta compression is useful for reducing network bandwidth when streaming
 * game state updates. Instead of sending the entire snapshot, only the changes
 * since the last known state are transmitted.
 */
public interface DeltaCompressionService {

    /**
     * Compute the delta between two snapshots.
     *
     * <p>The resulting delta contains:
     * <ul>
     *   <li>Changed components: values that differ between the two snapshots</li>
     *   <li>Added entities: entities present in 'to' but not in 'from'</li>
     *   <li>Removed entities: entities present in 'from' but not in 'to'</li>
     * </ul>
     *
     * @param matchId the match ID these snapshots belong to
     * @param fromTick the tick of the base snapshot
     * @param from the base snapshot (older state)
     * @param toTick the tick of the target snapshot
     * @param to the target snapshot (newer state)
     * @return a delta snapshot containing only the changes
     */
    DeltaSnapshot computeDelta(long matchId, long fromTick, Snapshot from, long toTick, Snapshot to);

    /**
     * Apply a delta to a base snapshot to produce a new snapshot.
     *
     * <p>This is the inverse operation of {@link #computeDelta}. Given a base
     * snapshot and a delta, produces the target snapshot.
     *
     * @param base the base snapshot to apply the delta to
     * @param delta the delta to apply
     * @return the resulting snapshot after applying the delta
     */
    Snapshot applyDelta(Snapshot base, DeltaSnapshot delta);
}
