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

package ca.samanthaireland.lightning.engine.quarkus.api.rest;

import ca.samanthaireland.lightning.engine.core.container.ContainerManager;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.quarkus.api.persistence.SnapshotDocument;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared support utilities for container REST resources.
 *
 * <p>Provides common helper methods used across multiple container-related
 * REST resource classes.
 */
public final class ContainerResourceSupport {

    private ContainerResourceSupport() {
        // Utility class
    }

    /**
     * Get a container by ID or throw a 404 exception.
     *
     * @param containerManager the container manager
     * @param containerId the container ID
     * @return the execution container
     * @throws EntityNotFoundException if container not found
     */
    public static ExecutionContainer getContainerOrThrow(ContainerManager containerManager, long containerId) {
        return containerManager.getContainer(containerId)
                .orElseThrow(() -> new EntityNotFoundException("Container not found: " + containerId));
    }

    /**
     * Build a response indicating snapshot persistence is not enabled.
     *
     * @return a 503 Service Unavailable response
     */
    public static Response persistenceNotEnabled() {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(Map.of(
                        "error", "Snapshot persistence is not enabled",
                        "hint", "Set snapshot.persistence.enabled=true in configuration"))
                .build();
    }

    /**
     * Convert a SnapshotDocument to a DTO map for JSON serialization.
     *
     * @param doc the snapshot document
     * @return the DTO map
     */
    public static Map<String, Object> toHistoryDto(SnapshotDocument doc) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("containerId", doc.containerId());
        result.put("matchId", doc.matchId());
        result.put("tick", doc.tick());
        result.put("timestamp", doc.timestamp() != null ? doc.timestamp().toString() : null);
        result.put("data", doc.data());
        return result;
    }
}
