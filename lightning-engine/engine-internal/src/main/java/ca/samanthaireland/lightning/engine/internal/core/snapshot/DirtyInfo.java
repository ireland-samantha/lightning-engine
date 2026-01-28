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

package ca.samanthaireland.lightning.engine.internal.core.snapshot;

import java.util.Set;

/**
 * Represents dirty state information for entities in a match.
 *
 * <p>Contains three disjoint sets of entity IDs:
 * <ul>
 *   <li>{@code modified} - Entities whose component values have changed</li>
 *   <li>{@code added} - Entities that were created since the last snapshot</li>
 *   <li>{@code removed} - Entities that were deleted since the last snapshot</li>
 * </ul>
 *
 * <p>This information is used by {@link CachingSnapshotProvider} to determine
 * whether to perform an incremental update or a full rebuild.
 *
 * @param modified entities whose component values have changed
 * @param added entities that were created since last snapshot
 * @param removed entities that were deleted since last snapshot
 */
public record DirtyInfo(Set<Long> modified, Set<Long> added, Set<Long> removed) {

    /**
     * Returns an empty DirtyInfo with no changes.
     *
     * @return empty dirty info
     */
    public static DirtyInfo empty() {
        return new DirtyInfo(Set.of(), Set.of(), Set.of());
    }

    /**
     * Checks if there are any changes (modified, added, or removed entities).
     *
     * @return true if any changes exist
     */
    public boolean hasChanges() {
        return !modified.isEmpty() || !added.isEmpty() || !removed.isEmpty();
    }

    /**
     * Returns the total number of changed entities.
     *
     * @return total count of modified + added + removed entities
     */
    public int totalChanges() {
        return modified.size() + added.size() + removed.size();
    }

    /**
     * Returns the count of modified entities.
     *
     * @return number of modified entities
     */
    public int modifiedCount() {
        return modified.size();
    }

    /**
     * Returns the count of added entities.
     *
     * @return number of added entities
     */
    public int addedCount() {
        return added.size();
    }

    /**
     * Returns the count of removed entities.
     *
     * @return number of removed entities
     */
    public int removedCount() {
        return removed.size();
    }

    /**
     * Checks if only modifications occurred (no adds or removes).
     *
     * <p>This is useful for determining if a simple value update can be performed
     * without restructuring the snapshot columns.
     *
     * @return true if only modifications occurred
     */
    public boolean isModificationOnly() {
        return !modified.isEmpty() && added.isEmpty() && removed.isEmpty();
    }

    /**
     * Checks if any structural changes occurred (adds or removes).
     *
     * <p>Structural changes require updating the entity index mapping
     * and potentially rebuilding columns.
     *
     * @return true if adds or removes occurred
     */
    public boolean hasStructuralChanges() {
        return !added.isEmpty() || !removed.isEmpty();
    }
}
