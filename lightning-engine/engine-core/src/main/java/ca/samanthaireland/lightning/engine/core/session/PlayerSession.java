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

import java.time.Instant;

/**
 * Represents a player's session in a match.
 *
 * <p>A session tracks a player's connection state to a match, allowing
 * for reconnection after temporary disconnects while preserving entity
 * ownership and game state.
 *
 * <p>Session lifecycle:
 * <ul>
 *   <li>ACTIVE - Player is connected and participating</li>
 *   <li>DISCONNECTED - Temporary disconnect, can reconnect within timeout</li>
 *   <li>EXPIRED - Session timed out, cannot reconnect</li>
 *   <li>ABANDONED - Player explicitly left the match</li>
 * </ul>
 */
public record PlayerSession(
    long id,
    long playerId,
    long matchId,
    SessionStatus status,
    Instant connectedAt,
    Instant lastActivityAt,
    Instant disconnectedAt
) {

    /**
     * Check if this session can be reconnected.
     *
     * @return true if the session is in DISCONNECTED status
     */
    public boolean canReconnect() {
        return status == SessionStatus.DISCONNECTED;
    }

    /**
     * Check if this session is currently active.
     *
     * @return true if the session is in ACTIVE status
     */
    public boolean isActive() {
        return status == SessionStatus.ACTIVE;
    }

    /**
     * Create a new session with updated status.
     *
     * @param newStatus the new status
     * @return a new PlayerSession with the updated status
     */
    public PlayerSession withStatus(SessionStatus newStatus) {
        Instant newDisconnectedAt = newStatus == SessionStatus.DISCONNECTED
            ? Instant.now()
            : disconnectedAt;

        return new PlayerSession(
            id,
            playerId,
            matchId,
            newStatus,
            connectedAt,
            Instant.now(),
            newDisconnectedAt
        );
    }

    /**
     * Create a new session with updated activity timestamp.
     *
     * @return a new PlayerSession with current time as lastActivityAt
     */
    public PlayerSession withActivity() {
        return new PlayerSession(
            id,
            playerId,
            matchId,
            status,
            connectedAt,
            Instant.now(),
            disconnectedAt
        );
    }

    /**
     * Create a new active session for a player joining a match.
     *
     * @param playerId the player ID
     * @param matchId the match ID
     * @return a new active PlayerSession
     */
    public static PlayerSession create(long playerId, long matchId) {
        Instant now = Instant.now();
        return new PlayerSession(
            0,
            playerId,
            matchId,
            SessionStatus.ACTIVE,
            now,
            now,
            null
        );
    }
}
