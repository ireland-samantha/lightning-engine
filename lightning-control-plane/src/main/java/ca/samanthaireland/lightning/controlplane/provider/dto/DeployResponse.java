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
 * Response from a deployment operation.
 * Contains all information needed for a client to connect to the deployed game.
 *
 * @param matchId          unique identifier for the deployed match
 * @param nodeId           ID of the node hosting this match
 * @param containerId      ID of the container on the node
 * @param status           current status of the deployment
 * @param createdAt        when the deployment was created
 * @param modules          modules enabled for this game
 * @param endpoints        connection endpoints for the deployed game
 */
public record DeployResponse(
        String matchId,
        String nodeId,
        long containerId,
        MatchStatus status,
        Instant createdAt,
        List<String> modules,
        Endpoints endpoints
) {

    /**
     * Connection endpoints for the deployed game.
     *
     * @param http      HTTP base URL for REST API access
     * @param websocket WebSocket URL for real-time snapshot streaming
     * @param commands  WebSocket URL for command submission
     */
    public record Endpoints(
            String http,
            String websocket,
            String commands
    ) {
        /**
         * Creates endpoints from a node's advertise address, container ID, and match ID.
         */
        public static Endpoints from(String advertiseAddress, long containerId, String matchId) {
            String wsAddress = advertiseAddress
                    .replace("https://", "wss://")
                    .replace("http://", "ws://");

            return new Endpoints(
                    advertiseAddress + "/api/containers/" + containerId,
                    wsAddress + "/ws/containers/" + containerId + "/snapshots/" + matchId,
                    wsAddress + "/containers/" + containerId + "/commands"
            );
        }
    }

    /**
     * Creates a response from a registry entry.
     *
     * @param entry the registry entry
     * @return the deployment response
     */
    public static DeployResponse from(MatchRegistryEntry entry) {
        return new DeployResponse(
                entry.matchId(),
                entry.nodeId(),
                entry.containerId(),
                entry.status(),
                entry.createdAt(),
                entry.moduleNames(),
                Endpoints.from(entry.advertiseAddress(), entry.containerId(), entry.matchId())
        );
    }
}
