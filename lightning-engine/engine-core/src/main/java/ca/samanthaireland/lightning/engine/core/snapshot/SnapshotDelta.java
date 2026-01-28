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

package ca.samanthaireland.lightning.engine.core.snapshot;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the delta (difference) between two snapshots.
 *
 * <p>A snapshot delta contains only the changes that occurred between a base tick
 * and the current tick. This enables efficient network transmission by sending
 * only changed data instead of full snapshots.
 *
 * <p><b>Delta Format:</b>
 * <pre>{@code
 * {
 *   "baseTick": 42,
 *   "currentTick": 43,
 *   "modules": [
 *     {
 *       "name": "EntityModule",
 *       "version": "1.0",
 *       "components": [
 *         {
 *           "name": "POSITION_X",
 *           "changes": [
 *             {"entityIndex": 5, "entityId": 1001, "oldValue": 10.0, "newValue": 15.0}
 *           ]
 *         }
 *       ]
 *     }
 *   ]
 * }
 * }</pre>
 *
 * <p><b>Client-Side Reconstruction:</b>
 * <pre>{@code
 * // JavaScript client example
 * class SnapshotState {
 *     applyDelta(delta) {
 *         for (const moduleDelta of delta.modules) {
 *             for (const componentDelta of moduleDelta.components) {
 *                 for (const change of componentDelta.changes) {
 *                     if (change.newValue !== null) {
 *                         this.data[moduleDelta.name][componentDelta.name][change.entityIndex] = change.newValue;
 *                     }
 *                 }
 *             }
 *         }
 *     }
 * }
 * }</pre>
 *
 * @param baseTick    the tick this delta is relative to (the "from" snapshot)
 * @param currentTick the current tick (the "to" snapshot)
 * @param modules     list of module deltas containing changes
 * @param full        true if this delta represents a full snapshot (no previous state)
 */
public record SnapshotDelta(long baseTick, long currentTick, List<ModuleDelta> modules, boolean full) {

    /**
     * Creates a new SnapshotDelta.
     *
     * @param baseTick    the base tick
     * @param currentTick the current tick
     * @param modules     the module deltas
     * @param full        whether this is a full snapshot delta
     * @throws NullPointerException if modules is null
     */
    public SnapshotDelta {
        Objects.requireNonNull(modules, "Modules cannot be null");
        modules = List.copyOf(modules);
    }

    /**
     * Creates an incremental delta between two ticks.
     *
     * @param baseTick    the base tick
     * @param currentTick the current tick
     * @param modules     the module deltas
     * @return a new incremental SnapshotDelta
     */
    public static SnapshotDelta incremental(long baseTick, long currentTick, List<ModuleDelta> modules) {
        return new SnapshotDelta(baseTick, currentTick, modules, false);
    }

    /**
     * Creates a full snapshot delta (no base state).
     *
     * <p>A full delta is sent when there's no previous state to compare against,
     * typically on initial connection or after a reconnect.
     *
     * @param currentTick the current tick
     * @param modules     the module deltas representing full state
     * @return a new full SnapshotDelta
     */
    public static SnapshotDelta full(long currentTick, List<ModuleDelta> modules) {
        return new SnapshotDelta(0, currentTick, modules, true);
    }

    /**
     * Creates an empty delta (no changes).
     *
     * @param baseTick    the base tick
     * @param currentTick the current tick
     * @return an empty SnapshotDelta
     */
    public static SnapshotDelta empty(long baseTick, long currentTick) {
        return new SnapshotDelta(baseTick, currentTick, List.of(), false);
    }

    /**
     * Returns true if this delta has no changes.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return modules.isEmpty() || modules.stream().allMatch(ModuleDelta::isEmpty);
    }

    /**
     * Returns true if this delta represents a full snapshot.
     *
     * @return true if full snapshot
     */
    public boolean isFull() {
        return full;
    }

    /**
     * Returns true if this is an incremental delta.
     *
     * @return true if incremental
     */
    public boolean isIncremental() {
        return !full;
    }

    /**
     * Returns the number of module deltas.
     *
     * @return the module count
     */
    public int moduleCount() {
        return modules.size();
    }

    /**
     * Returns the total number of entity changes across all modules and components.
     *
     * @return total change count
     */
    public int totalChangeCount() {
        return modules.stream()
                .mapToInt(ModuleDelta::totalChangeCount)
                .sum();
    }

    /**
     * Returns the module delta with the given name, if present.
     *
     * @param moduleName the module name
     * @return the module delta, or empty if not found
     */
    public Optional<ModuleDelta> module(String moduleName) {
        return modules.stream()
                .filter(m -> m.name().equals(moduleName))
                .findFirst();
    }

    /**
     * Returns true if this delta contains a module with the given name.
     *
     * @param moduleName the module name
     * @return true if the module delta exists
     */
    public boolean hasModule(String moduleName) {
        return modules.stream().anyMatch(m -> m.name().equals(moduleName));
    }

    /**
     * Returns all module names in this delta.
     *
     * @return list of module names
     */
    public List<String> moduleNames() {
        return modules.stream()
                .map(ModuleDelta::name)
                .toList();
    }
}
