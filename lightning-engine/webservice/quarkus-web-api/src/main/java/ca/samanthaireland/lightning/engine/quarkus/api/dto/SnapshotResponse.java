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

import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;

import java.util.List;

/**
 * REST API response for snapshot requests.
 *
 * <p>Provides a structured view of the ECS state organized by module, with each
 * module including its version and component data in columnar format.
 *
 * <pre>{@code
 * {
 *   "matchId": 1,
 *   "tick": 42,
 *   "modules": [
 *     {
 *       "name": "EntityModule",
 *       "version": "1.0.0",
 *       "components": [
 *         {"name": "ENTITY_ID", "values": [1.0, 2.0, 3.0]},
 *         {"name": "ENTITY_TYPE", "values": [100.0, 100.0, 200.0]}
 *       ]
 *     },
 *     {
 *       "name": "MovementModule",
 *       "version": "1.2.0",
 *       "components": [
 *         {"name": "POSITION_X", "values": [10.0, 20.0, 30.0]},
 *         {"name": "POSITION_Y", "values": [50.0, 60.0, 70.0]}
 *       ]
 *     }
 *   ]
 * }
 * }</pre>
 *
 * @param matchId the match ID this snapshot belongs to
 * @param tick    the simulation tick when this snapshot was taken
 * @param modules the module data with version information and components
 * @param error   error message if the request failed, null on success
 */
public record SnapshotResponse(
        long matchId,
        long tick,
        List<ModuleDataResponse> modules,
        String error
) {
    /**
     * Creates a successful snapshot response.
     *
     * @param matchId the match ID
     * @param tick    the simulation tick
     * @param modules the module data
     */
    public SnapshotResponse(long matchId, long tick, List<ModuleDataResponse> modules) {
        this(matchId, tick, modules, null);
    }

    /**
     * Creates an error response.
     *
     * @param message the error message
     * @return an error response with no snapshot data
     */
    public static SnapshotResponse error(String message) {
        return new SnapshotResponse(0, 0, List.of(), message);
    }

    /**
     * Creates a response from domain Snapshot.
     *
     * @param matchId  the match ID
     * @param tick     the simulation tick
     * @param snapshot the domain snapshot
     * @return the response DTO
     */
    public static SnapshotResponse from(long matchId, long tick, Snapshot snapshot) {
        List<ModuleDataResponse> modules = snapshot.modules().stream()
                .map(ModuleDataResponse::from)
                .toList();
        return new SnapshotResponse(matchId, tick, modules);
    }
}
