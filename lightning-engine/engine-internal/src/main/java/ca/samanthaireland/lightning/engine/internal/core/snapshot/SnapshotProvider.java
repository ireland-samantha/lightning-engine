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

import java.util.Optional;

/**
 * Provides snapshots of the simulation state.
 *
 * <p>Snapshots capture the current state of entities and their components,
 * optionally filtered by match or player.
 */
public interface SnapshotProvider {

    /**
     * Create a snapshot for the given match.
     *
     * @param matchId the match ID to create a snapshot for
     * @return the snapshot containing all entity component data for the match
     */
    Snapshot createForMatch(long matchId);

    /**
     * Create a snapshot for the given match filtered by player ownership.
     *
     * <p>Only entities owned by the specified player (those with OWNER_ID
     * matching the playerId) will be included in the snapshot.
     *
     * @param matchId the match ID to create a snapshot for
     * @param playerId the player ID to filter entities by ownership
     * @return the snapshot containing entity component data for entities owned by the player
     */
    Snapshot createForMatchAndPlayer(long matchId, long playerId);

    /**
     * Create a snapshot for the given match with optional player filtering.
     *
     * @param matchId the match ID to create a snapshot for
     * @param playerId optional player ID to filter entities by ownership
     * @return the snapshot containing entity component data, optionally filtered by player
     */
    default Snapshot createForMatch(long matchId, Optional<Long> playerId) {
        return playerId.map(pid -> createForMatchAndPlayer(matchId, pid))
                .orElseGet(() -> createForMatch(matchId));
    }

}
