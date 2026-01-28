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

package ca.samanthaireland.lightning.controlplane.provider.dto;

import ca.samanthaireland.lightning.controlplane.match.model.MatchRegistryEntry;
import ca.samanthaireland.lightning.controlplane.match.model.MatchStatus;

import java.time.Instant;
import java.util.List;

/**
 * Response containing match information and connection details.
 *
 * @param matchId          unique identifier for the match
 * @param nodeId           ID of the node hosting this match
 * @param containerId      ID of the container on the node
 * @param status           current status of the match
 * @param createdAt        when the match was created
 * @param moduleNames      modules enabled for this match
 * @param advertiseAddress HTTP address of the hosting node
 * @param websocketUrl     full WebSocket URL for client connections
 * @param playerCount      current number of connected players
 */
public record MatchResponse(
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

    /**
     * Creates a response from a registry entry.
     *
     * @param entry the registry entry
     * @return the response DTO
     */
    public static MatchResponse from(MatchRegistryEntry entry) {
        return new MatchResponse(
                entry.matchId(),
                entry.nodeId(),
                entry.containerId(),
                entry.status(),
                entry.createdAt(),
                entry.moduleNames(),
                entry.advertiseAddress(),
                entry.websocketUrl(),
                entry.playerCount()
        );
    }
}
