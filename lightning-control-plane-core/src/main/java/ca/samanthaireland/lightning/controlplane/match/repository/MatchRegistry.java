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

package ca.samanthaireland.lightning.controlplane.match.repository;

import ca.samanthaireland.lightning.controlplane.match.model.MatchRegistryEntry;
import ca.samanthaireland.lightning.controlplane.match.model.MatchStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository for storing and retrieving match registry entries.
 */
public interface MatchRegistry {

    /**
     * Saves a match registry entry.
     *
     * @param entry the entry to save
     * @return the saved entry
     */
    MatchRegistryEntry save(MatchRegistryEntry entry);

    /**
     * Finds a match by its ID.
     *
     * @param matchId the match ID
     * @return the entry if found
     */
    Optional<MatchRegistryEntry> findById(String matchId);

    /**
     * Returns all match entries.
     *
     * @return list of all entries
     */
    List<MatchRegistryEntry> findAll();

    /**
     * Finds all matches on a specific node.
     *
     * @param nodeId the node ID
     * @return list of matches on that node
     */
    List<MatchRegistryEntry> findByNodeId(String nodeId);

    /**
     * Finds all matches with a specific status.
     *
     * @param status the status to filter by
     * @return list of matches with that status
     */
    List<MatchRegistryEntry> findByStatus(MatchStatus status);

    /**
     * Deletes a match by its ID.
     *
     * @param matchId the match ID to delete
     */
    void deleteById(String matchId);

    /**
     * Deletes all matches for a node.
     *
     * @param nodeId the node ID
     */
    void deleteByNodeId(String nodeId);

    /**
     * Checks if a match exists.
     *
     * @param matchId the match ID
     * @return true if the match exists
     */
    boolean existsById(String matchId);

    /**
     * Counts all active matches (CREATING or RUNNING).
     *
     * @return count of active matches
     */
    long countActive();

    /**
     * Counts active matches on a specific node.
     *
     * @param nodeId the node ID
     * @return count of active matches on that node
     */
    long countActiveByNodeId(String nodeId);
}
