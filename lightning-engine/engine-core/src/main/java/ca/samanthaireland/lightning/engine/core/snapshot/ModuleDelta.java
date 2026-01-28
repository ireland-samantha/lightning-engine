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

import ca.samanthaireland.lightning.engine.ext.module.ModuleVersion;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents changes to a single module within a snapshot delta.
 *
 * <p>Contains the module name, version, and a list of component deltas
 * representing changes that occurred since the last snapshot.
 *
 * @param name       the module name (e.g., "EntityModule", "PhysicsModule")
 * @param version    the module version
 * @param components list of component deltas
 */
public record ModuleDelta(String name, ModuleVersion version, List<ComponentDelta> components) {

    /**
     * Creates a new ModuleDelta.
     *
     * @param name       the module name
     * @param version    the module version
     * @param components the component deltas
     * @throws NullPointerException if any parameter is null
     */
    public ModuleDelta {
        Objects.requireNonNull(name, "Module name cannot be null");
        Objects.requireNonNull(version, "Module version cannot be null");
        Objects.requireNonNull(components, "Components cannot be null");
        components = List.copyOf(components);
    }

    /**
     * Creates a ModuleDelta with the given name, version, and components.
     *
     * @param name       the module name
     * @param version    the module version
     * @param components the component deltas
     * @return a new ModuleDelta
     */
    public static ModuleDelta of(String name, ModuleVersion version, List<ComponentDelta> components) {
        return new ModuleDelta(name, version, components);
    }

    /**
     * Creates a ModuleDelta with the given name, version string, and components.
     *
     * @param name          the module name
     * @param versionString the module version string (e.g., "1.0", "0.2.1")
     * @param components    the component deltas
     * @return a new ModuleDelta
     */
    public static ModuleDelta of(String name, String versionString, List<ComponentDelta> components) {
        return new ModuleDelta(name, ModuleVersion.parse(versionString), components);
    }

    /**
     * Returns the number of component deltas in this module.
     *
     * @return the component delta count
     */
    public int componentCount() {
        return components.size();
    }

    /**
     * Returns true if this delta has no component changes.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return components.isEmpty() || components.stream().allMatch(ComponentDelta::isEmpty);
    }

    /**
     * Returns the total number of entity changes across all components.
     *
     * @return total change count
     */
    public int totalChangeCount() {
        return components.stream()
                .mapToInt(ComponentDelta::changeCount)
                .sum();
    }

    /**
     * Returns the component delta with the given name, if present.
     *
     * @param componentName the component name
     * @return the component delta, or empty if not found
     */
    public Optional<ComponentDelta> component(String componentName) {
        return components.stream()
                .filter(c -> c.name().equals(componentName))
                .findFirst();
    }

    /**
     * Returns true if this module delta contains a component with the given name.
     *
     * @param componentName the component name
     * @return true if the component delta exists
     */
    public boolean hasComponent(String componentName) {
        return components.stream().anyMatch(c -> c.name().equals(componentName));
    }

    /**
     * Returns the version as a string.
     *
     * @return the version string
     */
    public String versionString() {
        return version.toString();
    }
}
