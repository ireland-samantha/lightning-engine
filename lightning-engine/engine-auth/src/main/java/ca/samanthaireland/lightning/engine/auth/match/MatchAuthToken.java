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


package ca.samanthaireland.lightning.engine.auth.match;

import java.time.Instant;

/**
 * JWT token representing a player's authenticated session in a match.
 *
 * <p>Match tokens are issued when a player joins a match and grant
 * access to match-specific operations like submitting commands for
 * their entities.
 *
 * @param playerId the player's unique ID
 * @param matchId the match the player is authenticated for
 * @param playerName the player's display name
 * @param expiresAt when the token expires
 * @param jwtToken the raw JWT token string
 */
public record MatchAuthToken(
        long playerId,
        long matchId,
        String playerName,
        Instant expiresAt,
        String jwtToken
) {
    // JWT claim names
    public static final String CLAIM_PLAYER_ID = "player_id";
    public static final String CLAIM_MATCH_ID = "match_id";
    public static final String CLAIM_PLAYER_NAME = "player_name";

    /**
     * Check if this token is valid for the specified match.
     *
     * @param targetMatchId the match ID to check
     * @return true if this token is valid for the match
     */
    public boolean isValidForMatch(long targetMatchId) {
        return matchId == targetMatchId && !isExpired();
    }

    /**
     * Check if this token authorizes the specified player.
     *
     * @param targetPlayerId the player ID to check
     * @return true if this token authorizes the player
     */
    public boolean isForPlayer(long targetPlayerId) {
        return playerId == targetPlayerId;
    }

    /**
     * Check if the token has expired.
     *
     * @return true if the token has expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
