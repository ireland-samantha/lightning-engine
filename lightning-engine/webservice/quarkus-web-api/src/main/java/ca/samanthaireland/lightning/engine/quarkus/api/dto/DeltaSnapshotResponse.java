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

import java.util.Map;
import java.util.Set;

/**
 * REST API response for delta snapshot requests.
 *
 * <p>Contains only the changes between two ticks, making it more efficient
 * for incremental updates than full snapshots.
 *
 * @param matchId the match this delta applies to
 * @param fromTick the base tick (starting point)
 * @param toTick the target tick (ending point)
 * @param changedComponents map of module -> component -> entityId -> new value
 * @param addedEntities entities that were added between fromTick and toTick
 * @param removedEntities entities that were removed between fromTick and toTick
 * @param changeCount total number of individual value changes
 * @param compressionRatio ratio of delta size to full snapshot size (lower is better)
 */
public record DeltaSnapshotResponse(
        long matchId,
        long fromTick,
        long toTick,
        Map<String, Map<String, Map<Long, Float>>> changedComponents,
        Set<Long> addedEntities,
        Set<Long> removedEntities,
        int changeCount,
        double compressionRatio,
        String error
) {
    /**
     * Constructor for successful responses (no error).
     */
    public DeltaSnapshotResponse(
            long matchId,
            long fromTick,
            long toTick,
            Map<String, Map<String, Map<Long, Float>>> changedComponents,
            Set<Long> addedEntities,
            Set<Long> removedEntities,
            int changeCount,
            double compressionRatio
    ) {
        this(matchId, fromTick, toTick, changedComponents, addedEntities, removedEntities, changeCount, compressionRatio, null);
    }

    /**
     * Create an error response.
     */
    public static DeltaSnapshotResponse error(String message) {
        return new DeltaSnapshotResponse(-1, 0, 0, Map.of(), Set.of(), Set.of(), 0, 1.0, message);
    }
}
