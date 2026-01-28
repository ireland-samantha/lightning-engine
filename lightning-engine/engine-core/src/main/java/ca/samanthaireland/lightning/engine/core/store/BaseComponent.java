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


package ca.samanthaireland.lightning.engine.core.store;

import ca.samanthaireland.lightning.engine.util.IdGeneratorV2;
import lombok.ToString;

import java.util.Objects;

/**
 * Base class for all ECS components.
 *
 * <p>Components are data containers attached to entities. Each component has:
 * <ul>
 *   <li>A unique ID (either provided or auto-generated)</li>
 *   <li>A name identifying the component type</li>
 * </ul>
 *
 * <p>Follows SOLID principles:
 * <ul>
 *   <li>LSP: Constructors honor their contracts - if an ID is provided, it is used</li>
 *   <li>SRP: Only manages component identity, no business logic</li>
 * </ul>
 */
@ToString
public abstract class BaseComponent {

    private final long id;
    private final String name;

    /**
     * Create a component with the specified ID and name.
     *
     * @param id the component ID (must be positive)
     * @param name the component name (must not be null or blank)
     * @throws IllegalArgumentException if id is not positive or name is null/blank
     */
    public BaseComponent(long id, String name) {
        if (id <= 0) {
            throw new IllegalArgumentException("Component ID must be positive, got: " + id);
        }
        this.id = id;
        this.name = validateName(name);
    }

    /**
     * Create a component with an auto-generated ID.
     *
     * @param name the component name (must not be null or blank)
     * @throws IllegalArgumentException if name is null or blank
     */
    public BaseComponent(String name) {
        this.id = IdGeneratorV2.newId();
        this.name = validateName(name);
    }

    /**
     * Factory method to create a component with a specific ID.
     * Use this when restoring components from persistence or when IDs must be deterministic.
     *
     * @param id the component ID
     * @param name the component name
     * @return a new component instance (subclass must implement)
     */
    public static BaseComponent withId(long id, String name) {
        throw new UnsupportedOperationException("Subclasses must override withId()");
    }

    /**
     * Factory method to create a component with an auto-generated ID.
     * Use this for new components in normal operation.
     *
     * @param name the component name
     * @return a new component instance (subclass must implement)
     */
    public static BaseComponent create(String name) {
        throw new UnsupportedOperationException("Subclasses must override create()");
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Component name must not be null or blank");
        }
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseComponent that = (BaseComponent) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
