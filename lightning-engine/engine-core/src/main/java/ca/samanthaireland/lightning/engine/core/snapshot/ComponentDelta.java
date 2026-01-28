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
 * Represents changes to a single component within a module delta.
 *
 * <p>Contains the component name and a list of entity changes that occurred
 * for this component since the last snapshot.
 *
 * @param name    the component name (e.g., "POSITION_X")
 * @param changes list of entity value changes
 */
public record ComponentDelta(String name, List<EntityChange> changes) {

    /**
     * Creates a new ComponentDelta.
     *
     * @param name    the component name
     * @param changes the entity changes
     * @throws NullPointerException if name or changes is null
     */
    public ComponentDelta {
        Objects.requireNonNull(name, "Component name cannot be null");
        Objects.requireNonNull(changes, "Changes cannot be null");
        changes = List.copyOf(changes);
    }

    /**
     * Creates a ComponentDelta with the given name and changes.
     *
     * @param name    the component name
     * @param changes the entity changes
     * @return a new ComponentDelta
     */
    public static ComponentDelta of(String name, List<EntityChange> changes) {
        return new ComponentDelta(name, changes);
    }

    /**
     * Returns the number of changes in this delta.
     *
     * @return the change count
     */
    public int changeCount() {
        return changes.size();
    }

    /**
     * Returns true if this delta has no changes.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return changes.isEmpty();
    }

    /**
     * Returns only the modified changes.
     *
     * @return list of modified changes
     */
    public List<EntityChange> modifiedChanges() {
        return changes.stream()
                .filter(EntityChange::isModified)
                .toList();
    }

    /**
     * Returns only the added changes.
     *
     * @return list of added changes
     */
    public List<EntityChange> addedChanges() {
        return changes.stream()
                .filter(EntityChange::isAdded)
                .toList();
    }

    /**
     * Returns only the removed changes.
     *
     * @return list of removed changes
     */
    public List<EntityChange> removedChanges() {
        return changes.stream()
                .filter(EntityChange::isRemoved)
                .toList();
    }
}
