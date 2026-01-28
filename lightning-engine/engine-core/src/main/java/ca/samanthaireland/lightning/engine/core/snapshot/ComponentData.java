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

/**
 * Represents component data within a snapshot.
 *
 * <p>Each component contains a name (e.g., "POSITION_X") and a list of values,
 * where each value corresponds to an entity in columnar storage format.
 *
 * @param name   the component name (e.g., "POSITION_X", "VELOCITY_Y")
 * @param values the component values, one per entity
 */
public record ComponentData(String name, List<Float> values) {

    /**
     * Creates a new ComponentData instance.
     *
     * @param name   the component name
     * @param values the component values
     * @throws NullPointerException if name or values is null
     */
    public ComponentData {
        Objects.requireNonNull(name, "Component name cannot be null");
        Objects.requireNonNull(values, "Component values cannot be null");
        values = List.copyOf(values);
    }

    /**
     * Creates a ComponentData with the given name and values.
     *
     * @param name   the component name
     * @param values the component values
     * @return a new ComponentData instance
     */
    public static ComponentData of(String name, List<Float> values) {
        return new ComponentData(name, values);
    }

    /**
     * Creates a ComponentData with the given name and vararg values.
     *
     * @param name   the component name
     * @param values the component values
     * @return a new ComponentData instance
     */
    public static ComponentData of(String name, Float... values) {
        return new ComponentData(name, List.of(values));
    }

    /**
     * Returns the number of entity values in this component.
     *
     * @return the entity count
     */
    public int entityCount() {
        return values.size();
    }

    /**
     * Returns the value for a specific entity index.
     *
     * @param entityIndex the entity index
     * @return the value at the given index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public float valueAt(int entityIndex) {
        return values.get(entityIndex);
    }

    /**
     * Returns true if this component has no values.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }
}
