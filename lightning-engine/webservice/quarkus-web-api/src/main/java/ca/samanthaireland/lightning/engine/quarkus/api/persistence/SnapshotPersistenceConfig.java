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

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for snapshot persistence to MongoDB.
 *
 * <p>When enabled, snapshots are persisted to MongoDB after each tick.
 * This allows for historical analysis and replay of simulation state.
 */
@ConfigMapping(prefix = "snapshot.persistence")
public interface SnapshotPersistenceConfig {

    /**
     * Whether snapshot persistence is enabled.
     *
     * @return true if snapshots should be persisted to MongoDB
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * The MongoDB database name for storing snapshots.
     *
     * @return the database name
     */
    @WithDefault("lightningfirefly")
    String database();

    /**
     * The MongoDB collection name for storing snapshots.
     *
     * @return the collection name
     */
    @WithDefault("snapshots")
    String collection();

    /**
     * Interval in ticks between persisted snapshots.
     *
     * <p>A value of 1 means every tick is persisted.
     * A value of 10 means every 10th tick is persisted.
     *
     * @return the persistence interval in ticks
     */
    @WithDefault("60")
    int tickInterval();
}
