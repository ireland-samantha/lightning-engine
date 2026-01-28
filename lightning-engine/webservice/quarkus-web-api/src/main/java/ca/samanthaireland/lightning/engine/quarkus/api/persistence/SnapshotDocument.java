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


package ca.samanthaireland.lightning.engine.quarkus.api.persistence;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * MongoDB document representation of a simulation snapshot.
 *
 * <p>This record captures the complete state of a match at a specific tick,
 * including all entity component data organized by module.
 *
 * @param id MongoDB document ID
 * @param containerId the container this snapshot belongs to
 * @param matchId the match this snapshot belongs to
 * @param tick the simulation tick number
 * @param timestamp when the snapshot was captured
 * @param data module-organized component data: moduleName -> componentName -> [values...]
 */
public record SnapshotDocument(
        @BsonId ObjectId id,
        long containerId,
        long matchId,
        long tick,
        Instant timestamp,
        Map<String, Map<String, List<Float>>> data
) {

    /**
     * Create a new snapshot document without an ID (for insertion).
     *
     * @param containerId the container ID
     * @param matchId the match ID
     * @param tick the tick number
     * @param timestamp when the snapshot was captured
     * @param data the component data
     * @return a new snapshot document
     */
    public static SnapshotDocument create(long containerId, long matchId, long tick, Instant timestamp,
                                          Map<String, Map<String, List<Float>>> data) {
        return new SnapshotDocument(null, containerId, matchId, tick, timestamp, data);
    }

    /**
     * Create a new snapshot document with the current timestamp.
     *
     * @param containerId the container ID
     * @param matchId the match ID
     * @param tick the tick number
     * @param data the component data
     * @return a new snapshot document
     */
    public static SnapshotDocument create(long containerId, long matchId, long tick,
                                          Map<String, Map<String, List<Float>>> data) {
        return create(containerId, matchId, tick, Instant.now(), data);
    }
}
