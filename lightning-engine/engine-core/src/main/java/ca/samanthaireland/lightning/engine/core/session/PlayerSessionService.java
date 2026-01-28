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

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for PlayerSession business operations.
 *
 * <p>Provides business-level operations with validation and domain logic.
 * Uses {@link PlayerSessionRepository} for persistence.
 *
 * <p>SOLID compliance:
 * <ul>
 *   <li>SRP: Handles business logic only, delegates persistence to repository</li>
 *   <li>ISP: Separate from PlayerSessionRepository with distinct responsibilities</li>
 *   <li>DIP: Depends on PlayerSessionRepository abstraction</li>
 * </ul>
 */
public interface PlayerSessionService {

    /**
     * Create a new session for a player joining a match.
     *
     * <p>Validates that both player and match exist before creating the session.
     * If the player has an existing DISCONNECTED session, this will reactivate it.
     *
     * @param playerId the player ID
     * @param matchId the match ID
     * @return the created or reactivated session
     * @throws ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException if player or match doesn't exist
     * @throws ca.samanthaireland.lightning.engine.core.exception.ConflictException if player already has an active session
     */
    PlayerSession createSession(long playerId, long matchId);

    /**
     * Create a new session for a player joining a match, skipping match validation.
     *
     * <p>This method is intended for container-scoped sessions where the match
     * existence has already been validated by the container. Only validates
     * that the player exists.
     *
     * <p>If the player has an existing DISCONNECTED session, this will reactivate it.
     *
     * @param playerId the player ID
     * @param matchId the match ID
     * @return the created or reactivated session
     * @throws ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException if player doesn't exist
     * @throws ca.samanthaireland.lightning.engine.core.exception.ConflictException if player already has an active session
     */
    PlayerSession createSessionForContainer(long playerId, long matchId);

    /**
     * Reconnect a player to an existing session.
     *
     * <p>The session must be in DISCONNECTED status. This will transition
     * it back to ACTIVE status.
     *
     * @param playerId the player ID
     * @param matchId the match ID
     * @return the reconnected session
     * @throws ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException if no session exists
     * @throws IllegalStateException if session cannot be reconnected (not DISCONNECTED)
     */
    PlayerSession reconnect(long playerId, long matchId);

    /**
     * Disconnect a player's session.
     *
     * <p>The session will transition to DISCONNECTED status, allowing
     * reconnection within the timeout period.
     *
     * @param playerId the player ID
     * @param matchId the match ID
     * @throws ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException if no session exists
     */
    void disconnect(long playerId, long matchId);

    /**
     * Abandon a player's session.
     *
     * <p>The session will transition to ABANDONED status and cannot be
     * reconnected. The player must create a new session to rejoin.
     *
     * @param playerId the player ID
     * @param matchId the match ID
     * @throws ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException if no session exists
     */
    void abandon(long playerId, long matchId);

    /**
     * Expire stale sessions.
     *
     * <p>Transitions DISCONNECTED sessions that have exceeded the timeout
     * to EXPIRED status. Should be called periodically by a background task.
     *
     * @param timeout the duration after which disconnected sessions expire
     * @return the number of sessions that were expired
     */
    int expireStaleSessions(Duration timeout);

    /**
     * Find the active session for a player in a match.
     *
     * @param playerId the player ID
     * @param matchId the match ID
     * @return an Optional containing the active session if found
     */
    Optional<PlayerSession> findActiveSession(long playerId, long matchId);

    /**
     * Find any session for a player in a match (any status).
     *
     * @param playerId the player ID
     * @param matchId the match ID
     * @return an Optional containing the session if found
     */
    Optional<PlayerSession> findSession(long playerId, long matchId);

    /**
     * Get all sessions for a match (all statuses).
     *
     * @param matchId the match ID
     * @return list of sessions for the match
     */
    List<PlayerSession> findMatchSessions(long matchId);

    /**
     * Get active sessions for a match.
     *
     * @param matchId the match ID
     * @return list of active sessions for the match
     */
    List<PlayerSession> findActiveMatchSessions(long matchId);

    /**
     * Check if a player can reconnect to a match.
     *
     * <p>Returns true if the player has a session in DISCONNECTED status.
     *
     * @param playerId the player ID
     * @param matchId the match ID
     * @return true if reconnection is possible
     */
    boolean canReconnect(long playerId, long matchId);

    /**
     * Get all sessions across all matches.
     *
     * @return list of all sessions
     */
    List<PlayerSession> findAllSessions();

    /**
     * Record activity for a session.
     *
     * <p>Updates the lastActivityAt timestamp. Used to track active sessions
     * and detect stale connections.
     *
     * @param playerId the player ID
     * @param matchId the match ID
     */
    void recordActivity(long playerId, long matchId);
}
