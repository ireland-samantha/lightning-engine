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

package ca.samanthaireland.lightning.controlplane.match.service;

import ca.samanthaireland.lightning.controlplane.match.model.MatchRegistryEntry;
import ca.samanthaireland.lightning.controlplane.match.model.MatchStatus;

import java.util.List;
import java.util.Optional;

/**
 * Service for routing match creation requests and managing the match registry.
 */
public interface MatchRoutingService {

    /**
     * Creates a new match in the cluster.
     * <ol>
     *   <li>Scheduler selects a node</li>
     *   <li>Creates a container on the node (if needed)</li>
     *   <li>Creates the match in the container</li>
     *   <li>Stores the registry entry</li>
     *   <li>Returns connection information</li>
     * </ol>
     *
     * @param moduleNames     modules required for the match
     * @param preferredNodeId optional preferred node ID
     * @return the created match entry with connection information
     */
    MatchRegistryEntry createMatch(List<String> moduleNames, String preferredNodeId);

    /**
     * Finds a match by its ID.
     *
     * @param matchId the match ID (in format "nodeId-containerId-matchId")
     * @return the match entry if found
     */
    Optional<MatchRegistryEntry> findById(String matchId);

    /**
     * Returns all matches.
     *
     * @return list of all matches
     */
    List<MatchRegistryEntry> findAll();

    /**
     * Finds matches by status.
     *
     * @param status the status to filter by
     * @return list of matches with that status
     */
    List<MatchRegistryEntry> findByStatus(MatchStatus status);

    /**
     * Deletes a match.
     *
     * @param matchId the match ID to delete
     */
    void deleteMatch(String matchId);

    /**
     * Updates the player count for a match.
     *
     * @param matchId     the match ID
     * @param playerCount the new player count
     */
    void updatePlayerCount(String matchId, int playerCount);

    /**
     * Marks a match as finished.
     *
     * @param matchId the match ID
     */
    void finishMatch(String matchId);
}
