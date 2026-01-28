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


package ca.samanthaireland.lightning.engine.core.session;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PlayerSession persistence operations.
 *
 * <p>Follows the Repository pattern - provides pure CRUD operations
 * without business logic. Business rules belong in {@link PlayerSessionService}.
 *
 * <p>SOLID compliance:
 * <ul>
 *   <li>SRP: Only handles data persistence, no business logic</li>
 *   <li>ISP: Separate from PlayerSessionService with distinct responsibilities</li>
 * </ul>
 */
public interface PlayerSessionRepository {

    /**
     * Save a player session to the repository.
     *
     * <p>If the session has id = 0, a new ID will be generated.
     * Otherwise, the existing session will be updated.
     *
     * @param session the session to save (must not be null)
     * @return the saved session with generated ID if new
     */
    PlayerSession save(PlayerSession session);

    /**
     * Find a session by its ID.
     *
     * @param id the session ID
     * @return an Optional containing the session if found
     */
    Optional<PlayerSession> findById(long id);

    /**
     * Find a session by player ID and match ID.
     *
     * <p>A player can have at most one session per match.
     *
     * @param playerId the player ID
     * @param matchId the match ID
     * @return an Optional containing the session if found
     */
    Optional<PlayerSession> findByPlayerAndMatch(long playerId, long matchId);

    /**
     * Find all sessions for a given match.
     *
     * @param matchId the match ID
     * @return list of sessions for the match (never null)
     */
    List<PlayerSession> findByMatchId(long matchId);

    /**
     * Find all sessions for a given player.
     *
     * @param playerId the player ID
     * @return list of sessions for the player (never null)
     */
    List<PlayerSession> findByPlayerId(long playerId);

    /**
     * Find all sessions with a specific status.
     *
     * @param status the session status
     * @return list of sessions with the given status (never null)
     */
    List<PlayerSession> findByStatus(SessionStatus status);

    /**
     * Find all sessions for a match with a specific status.
     *
     * @param matchId the match ID
     * @param status the session status
     * @return list of matching sessions (never null)
     */
    List<PlayerSession> findByMatchIdAndStatus(long matchId, SessionStatus status);

    /**
     * Delete a session by its ID.
     *
     * @param id the session ID
     */
    void deleteById(long id);

    /**
     * Delete all sessions for a match.
     *
     * <p>Used when a match is deleted.
     *
     * @param matchId the match ID
     */
    void deleteByMatchId(long matchId);

    /**
     * Check if a session exists for a player in a match.
     *
     * @param playerId the player ID
     * @param matchId the match ID
     * @return true if a session exists
     */
    boolean existsByPlayerAndMatch(long playerId, long matchId);

    /**
     * Count the total number of sessions.
     *
     * @return the total count
     */
    long count();

    /**
     * Find all sessions across all matches.
     *
     * @return list of all sessions (never null)
     */
    List<PlayerSession> findAll();
}
