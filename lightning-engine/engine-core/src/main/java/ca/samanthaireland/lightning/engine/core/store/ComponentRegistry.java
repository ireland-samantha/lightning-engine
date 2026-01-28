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

import java.util.Optional;
import java.util.Set;

/**
 * Registry for mapping component names to component instances.
 *
 * <p>This registry is used during snapshot restoration to resolve
 * component names (stored as strings in snapshots) back to their
 * actual {@link BaseComponent} instances.
 *
 * <p>SOLID compliance:
 * <ul>
 *   <li>SRP: Only handles component name-to-instance mapping</li>
 *   <li>ISP: Focused interface for component lookup</li>
 * </ul>
 */
public interface ComponentRegistry {

    /**
     * Register a component with the registry.
     *
     * <p>The component's name (from {@link BaseComponent#getName()})
     * is used as the lookup key.
     *
     * @param component the component to register
     */
    void register(BaseComponent component);

    /**
     * Find a component by its name.
     *
     * @param name the component name
     * @return an Optional containing the component if found
     */
    Optional<BaseComponent> findByName(String name);

    /**
     * Get all registered component names.
     *
     * @return set of registered component names
     */
    Set<String> getRegisteredNames();

    /**
     * Check if a component with the given name is registered.
     *
     * @param name the component name
     * @return true if registered
     */
    default boolean isRegistered(String name) {
        return findByName(name).isPresent();
    }

    /**
     * Clear all registered components.
     *
     * <p>For testing purposes.
     */
    void clear();
}
