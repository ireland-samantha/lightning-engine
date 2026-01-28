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

package ca.samanthaireland.lightning.controlplane.match.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Registry entry for a match in the control plane.
 * Maps a match to its hosting node and provides connection information.
 *
 * @param matchId       unique identifier for the match (from the Lightning Engine node)
 * @param nodeId        ID of the node hosting this match
 * @param containerId   ID of the container on the node
 * @param status        current status of the match
 * @param createdAt     when the match was created
 * @param moduleNames   modules enabled for this match
 * @param advertiseAddress HTTP address of the hosting node
 * @param websocketUrl  full WebSocket URL for client connections
 * @param playerCount   current number of connected players
 */
public record MatchRegistryEntry(
        String matchId,
        String nodeId,
        long containerId,
        MatchStatus status,
        Instant createdAt,
        List<String> moduleNames,
        String advertiseAddress,
        String websocketUrl,
        int playerCount
) {

    public MatchRegistryEntry {
        Objects.requireNonNull(matchId, "matchId cannot be null");
        Objects.requireNonNull(nodeId, "nodeId cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
        Objects.requireNonNull(moduleNames, "moduleNames cannot be null");
        Objects.requireNonNull(advertiseAddress, "advertiseAddress cannot be null");
        Objects.requireNonNull(websocketUrl, "websocketUrl cannot be null");

        if (matchId.isBlank()) {
            throw new IllegalArgumentException("matchId cannot be blank");
        }
        if (nodeId.isBlank()) {
            throw new IllegalArgumentException("nodeId cannot be blank");
        }
        if (playerCount < 0) {
            throw new IllegalArgumentException("playerCount cannot be negative");
        }
    }

    /**
     * Creates a new entry for a match being created.
     *
     * @param matchId          the match ID
     * @param nodeId           the node ID
     * @param containerId      the container ID
     * @param moduleNames      modules enabled for the match
     * @param advertiseAddress the node's advertise address
     * @return a new entry in CREATING status
     */
    public static MatchRegistryEntry creating(
            String matchId,
            String nodeId,
            long containerId,
            List<String> moduleNames,
            String advertiseAddress
    ) {
        String websocketUrl = buildWebsocketUrl(advertiseAddress, containerId, matchId);
        return new MatchRegistryEntry(
                matchId,
                nodeId,
                containerId,
                MatchStatus.CREATING,
                Instant.now(),
                moduleNames,
                advertiseAddress,
                websocketUrl,
                0
        );
    }

    /**
     * Creates a copy with RUNNING status.
     *
     * @return a new entry with RUNNING status
     */
    public MatchRegistryEntry running() {
        return new MatchRegistryEntry(
                matchId,
                nodeId,
                containerId,
                MatchStatus.RUNNING,
                createdAt,
                moduleNames,
                advertiseAddress,
                websocketUrl,
                playerCount
        );
    }

    /**
     * Creates a copy with updated player count.
     *
     * @param newPlayerCount the new player count
     * @return a new entry with updated player count
     */
    public MatchRegistryEntry withPlayerCount(int newPlayerCount) {
        return new MatchRegistryEntry(
                matchId,
                nodeId,
                containerId,
                status,
                createdAt,
                moduleNames,
                advertiseAddress,
                websocketUrl,
                newPlayerCount
        );
    }

    /**
     * Creates a copy with FINISHED status.
     *
     * @return a new entry with FINISHED status
     */
    public MatchRegistryEntry finished() {
        return new MatchRegistryEntry(
                matchId,
                nodeId,
                containerId,
                MatchStatus.FINISHED,
                createdAt,
                moduleNames,
                advertiseAddress,
                websocketUrl,
                playerCount
        );
    }

    /**
     * Creates a copy with ERROR status.
     *
     * @return a new entry with ERROR status
     */
    public MatchRegistryEntry error() {
        return new MatchRegistryEntry(
                matchId,
                nodeId,
                containerId,
                MatchStatus.ERROR,
                createdAt,
                moduleNames,
                advertiseAddress,
                websocketUrl,
                playerCount
        );
    }

    private static String buildWebsocketUrl(String advertiseAddress, long containerId, String matchId) {
        // Convert http:// to ws:// or https:// to wss://
        String wsAddress = advertiseAddress
                .replace("https://", "wss://")
                .replace("http://", "ws://");
        return wsAddress + "/ws/containers/" + containerId + "/matches/" + matchId + "/snapshots";
    }
}
