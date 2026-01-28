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
import java.util.Map;
import java.util.Set;

/**
 * Represents a delta (difference) between two snapshots.
 *
 * <p>A delta snapshot contains only the changes between two points in time,
 * making it more efficient for network transmission when only a small portion
 * of the game state has changed.
 *
 * <p>Structure of changedComponents: moduleName -> componentName -> entityId -> newValue
 *
 * <pre>{@code
 * {
 *     "MovementModule": {
 *         "POSITION_X": {
 *             1: 150.0,  // Entity 1's POSITION_X changed to 150.0
 *             3: 275.0   // Entity 3's POSITION_X changed to 275.0
 *         },
 *         "VELOCITY_X": {
 *             2: 5.0     // Entity 2's VELOCITY_X changed to 5.0
 *         }
 *     }
 * }
 * }</pre>
 *
 * @param matchId the match this delta applies to
 * @param fromTick the starting tick (base snapshot tick)
 * @param toTick the ending tick (target snapshot tick)
 * @param changedComponents map of module -> component -> entityId -> new value for components that changed
 * @param addedEntities set of entity IDs that were added between fromTick and toTick
 * @param removedEntities set of entity IDs that were removed between fromTick and toTick
 */
public record DeltaSnapshot(
        long matchId,
        long fromTick,
        long toTick,
        Map<String, Map<String, Map<Long, Float>>> changedComponents,
        Set<Long> addedEntities,
        Set<Long> removedEntities
) {

    /**
     * Returns true if this delta contains no changes.
     *
     * @return true if the delta is empty (no changes between snapshots)
     */
    public boolean isEmpty() {
        return (changedComponents == null || changedComponents.isEmpty())
                && (addedEntities == null || addedEntities.isEmpty())
                && (removedEntities == null || removedEntities.isEmpty());
    }

    /**
     * Returns the total number of component value changes in this delta.
     *
     * @return the count of individual component value changes
     */
    public int changeCount() {
        if (changedComponents == null) {
            return 0;
        }
        return changedComponents.values().stream()
                .flatMap(moduleData -> moduleData.values().stream())
                .mapToInt(Map::size)
                .sum();
    }
}
