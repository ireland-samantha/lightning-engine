/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.core.container;

import ca.samanthaireland.lightning.engine.core.session.PlayerSession;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Fluent API for container-scoped session operations.
 *
 * <p>Sessions are isolated per container. A session created in one container
 * is not visible to other containers.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create a session (player joins match)
 * PlayerSession session = container.sessions().create(playerId, matchId);
 *
 * // Get all sessions for a match
 * List<PlayerSession> sessions = container.sessions().forMatch(matchId);
 *
 * // Disconnect a player
 * container.sessions().disconnect(playerId, matchId);
 * }</pre>
 */
public interface ContainerSessionOperations {

    /**
     * Create a new session for a player joining a match.
     *
     * <p>If the player has an existing DISCONNECTED session, this will reactivate it.
     *
     * @param playerId the player ID
     * @param matchId the match ID
     * @return the created or reactivated session
     * @throws ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException if player doesn't exist in this container
     * @throws ca.samanthaireland.lightning.engine.core.exception.ConflictException if player already has an active session
     */
    PlayerSession create(long playerId, long matchId);

    /**
     * Reconnect a player to an existing session.
     *
     * <p>The session must be in DISCONNECTED status.
     *
     * @param playerId the player ID
     * @param matchId the match ID
     * @return the reconnected session
     * @throws ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException if no session exists
     * @throws IllegalStateException if session cannot be reconnected
     */
    PlayerSession reconnect(long playerId, long matchId);

    /**
     * Disconnect a player's session.
     *
     * <p>The session will transition to DISCONNECTED status.
     *
     * @param playerId the player ID
     * @param matchId the match ID
     */
    void disconnect(long playerId, long matchId);

    /**
     * Abandon a player's session.
     *
     * <p>The session will transition to ABANDONED status and cannot be reconnected.
     *
     * @param playerId the player ID
     * @param matchId the match ID
     */
    void abandon(long playerId, long matchId);

    /**
     * Expire stale sessions that have been disconnected for too long.
     *
     * @param timeout the duration after which disconnected sessions expire
     * @return the number of sessions that were expired
     */
    int expireStale(Duration timeout);

    /**
     * Find the active session for a player in a match.
     *
     * @param playerId the player ID
     * @param matchId the match ID
     * @return an Optional containing the active session if found
     */
    Optional<PlayerSession> findActive(long playerId, long matchId);

    /**
     * Find any session for a player in a match (any status).
     *
     * @param playerId the player ID
     * @param matchId the match ID
     * @return an Optional containing the session if found
     */
    Optional<PlayerSession> find(long playerId, long matchId);

    /**
     * Get all sessions for a match (all statuses).
     *
     * @param matchId the match ID
     * @return list of sessions for the match
     */
    List<PlayerSession> forMatch(long matchId);

    /**
     * Get active sessions for a match.
     *
     * @param matchId the match ID
     * @return list of active sessions for the match
     */
    List<PlayerSession> activeForMatch(long matchId);

    /**
     * Get all sessions in this container.
     *
     * @return list of all sessions
     */
    List<PlayerSession> all();

    /**
     * Check if a player can reconnect to a match.
     *
     * @param playerId the player ID
     * @param matchId the match ID
     * @return true if reconnection is possible
     */
    boolean canReconnect(long playerId, long matchId);

    /**
     * Record activity for a session.
     *
     * @param playerId the player ID
     * @param matchId the match ID
     */
    void recordActivity(long playerId, long matchId);

    /**
     * Get the count of sessions in this container.
     *
     * @return the number of sessions
     */
    int count();
}
