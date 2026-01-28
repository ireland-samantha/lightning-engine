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

/**
 * Represents a single entity value change in a snapshot delta.
 *
 * <p>For MODIFIED changes, both oldValue and newValue are significant.
 * For ADDED changes, oldValue is NaN and newValue contains the new value.
 * For REMOVED changes, oldValue contains the old value and newValue is NaN.
 *
 * @param entityIndex the column index in the snapshot (for positional updates)
 * @param entityId    the entity ID
 * @param oldValue    the previous value (NaN for ADDED)
 * @param newValue    the new value (NaN for REMOVED)
 */
public record EntityChange(
        int entityIndex,
        long entityId,
        float oldValue,
        float newValue
) {

    /**
     * Creates a MODIFIED change.
     *
     * @param entityIndex the column index
     * @param entityId    the entity ID
     * @param oldValue    the previous value
     * @param newValue    the new value
     * @return a new EntityChange
     */
    public static EntityChange modified(int entityIndex, long entityId, float oldValue, float newValue) {
        return new EntityChange(entityIndex, entityId, oldValue, newValue);
    }

    /**
     * Creates an ADDED change.
     *
     * @param entityIndex the column index (position in snapshot after add)
     * @param entityId    the entity ID
     * @param newValue    the new value
     * @return a new EntityChange
     */
    public static EntityChange added(int entityIndex, long entityId, float newValue) {
        return new EntityChange(entityIndex, entityId, Float.NaN, newValue);
    }

    /**
     * Creates a REMOVED change.
     *
     * @param entityIndex the column index (position before removal)
     * @param entityId    the entity ID
     * @param oldValue    the value being removed
     * @return a new EntityChange
     */
    public static EntityChange removed(int entityIndex, long entityId, float oldValue) {
        return new EntityChange(entityIndex, entityId, oldValue, Float.NaN);
    }

    /**
     * Determines the delta type based on old/new values.
     *
     * @return the delta type
     */
    public DeltaType deltaType() {
        if (Float.isNaN(oldValue)) {
            return DeltaType.ADDED;
        }
        if (Float.isNaN(newValue)) {
            return DeltaType.REMOVED;
        }
        return DeltaType.MODIFIED;
    }

    /**
     * Returns true if this change represents a modification.
     *
     * @return true if modified
     */
    public boolean isModified() {
        return !Float.isNaN(oldValue) && !Float.isNaN(newValue);
    }

    /**
     * Returns true if this change represents an addition.
     *
     * @return true if added
     */
    public boolean isAdded() {
        return Float.isNaN(oldValue) && !Float.isNaN(newValue);
    }

    /**
     * Returns true if this change represents a removal.
     *
     * @return true if removed
     */
    public boolean isRemoved() {
        return !Float.isNaN(oldValue) && Float.isNaN(newValue);
    }
}
