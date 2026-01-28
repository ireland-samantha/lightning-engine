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


package ca.samanthaireland.lightning.engine.quarkus.api.dto;

/**
 * Response returned when a player joins a match.
 *
 * <p>Includes WebSocket endpoint URLs for receiving player-scoped snapshots and errors.
 *
 * @param playerId the player ID
 * @param matchId the match ID
 * @param snapshotWebSocketUrl WebSocket URL for full player-scoped snapshots
 * @param deltaSnapshotWebSocketUrl WebSocket URL for delta player-scoped snapshots
 * @param errorWebSocketUrl WebSocket URL for receiving game errors
 * @param restSnapshotUrl REST URL for fetching player-scoped snapshot
 */
public record JoinMatchResponse(
        long playerId,
        long matchId,
        String snapshotWebSocketUrl,
        String deltaSnapshotWebSocketUrl,
        String errorWebSocketUrl,
        String restSnapshotUrl
) {

    /**
     * Creates a JoinMatchResponse with generated endpoint URLs.
     *
     * @param playerId the player ID
     * @param matchId the match ID
     * @return the response with endpoint URLs
     */
    public static JoinMatchResponse create(long playerId, long matchId) {
        return new JoinMatchResponse(
                playerId,
                matchId,
                buildSnapshotWebSocketUrl(matchId, playerId),
                buildDeltaSnapshotWebSocketUrl(matchId, playerId),
                buildErrorWebSocketUrl(matchId, playerId),
                buildRestSnapshotUrl(matchId, playerId)
        );
    }

    private static String buildSnapshotWebSocketUrl(long matchId, long playerId) {
        return String.format("/ws/matches/%d/players/%d/snapshot", matchId, playerId);
    }

    private static String buildDeltaSnapshotWebSocketUrl(long matchId, long playerId) {
        return String.format("/ws/matches/%d/players/%d/snapshot/delta", matchId, playerId);
    }

    private static String buildErrorWebSocketUrl(long matchId, long playerId) {
        return String.format("/ws/matches/%d/players/%d/errors", matchId, playerId);
    }

    private static String buildRestSnapshotUrl(long matchId, long playerId) {
        return String.format("/api/snapshots/match/%d/player/%d", matchId, playerId);
    }
}
