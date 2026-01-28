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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents module data within a snapshot.
 *
 * <p>Each module contains a name, version, and a list of component data.
 *
 * @param name       the module name (e.g., "EntityModule", "PhysicsModule")
 * @param version    the module version
 * @param components the component data for this module
 */
public record ModuleData(String name, ModuleVersion version, List<ComponentData> components) {

    /**
     * Creates a new ModuleData instance.
     *
     * @param name       the module name
     * @param version    the module version
     * @param components the component data
     * @throws NullPointerException if any parameter is null
     */
    public ModuleData {
        Objects.requireNonNull(name, "Module name cannot be null");
        Objects.requireNonNull(version, "Module version cannot be null");
        Objects.requireNonNull(components, "Components cannot be null");
        components = List.copyOf(components);
    }

    /**
     * Creates a ModuleData with the given name, version, and components.
     *
     * @param name       the module name
     * @param version    the module version
     * @param components the component data
     * @return a new ModuleData instance
     */
    public static ModuleData of(String name, ModuleVersion version, List<ComponentData> components) {
        return new ModuleData(name, version, components);
    }

    /**
     * Creates a ModuleData with the given name, version string, and components.
     *
     * @param name          the module name
     * @param versionString the module version string (e.g., "1.0", "0.2.1")
     * @param components    the component data
     * @return a new ModuleData instance
     */
    public static ModuleData of(String name, String versionString, List<ComponentData> components) {
        return new ModuleData(name, ModuleVersion.parse(versionString), components);
    }

    /**
     * Creates a ModuleData with the default version (1.0.0).
     *
     * @param name       the module name
     * @param components the component data
     * @return a new ModuleData instance
     */
    public static ModuleData of(String name, List<ComponentData> components) {
        return new ModuleData(name, ModuleVersion.of(1, 0), components);
    }

    /**
     * Returns the number of components in this module.
     *
     * @return the component count
     */
    public int componentCount() {
        return components.size();
    }

    /**
     * Returns the component with the given name, if present.
     *
     * @param componentName the component name
     * @return the component data, or empty if not found
     */
    public Optional<ComponentData> component(String componentName) {
        return components.stream()
                .filter(c -> c.name().equals(componentName))
                .findFirst();
    }

    /**
     * Returns true if this module contains a component with the given name.
     *
     * @param componentName the component name
     * @return true if the component exists
     */
    public boolean hasComponent(String componentName) {
        return components.stream().anyMatch(c -> c.name().equals(componentName));
    }

    /**
     * Returns the version as a string (e.g., "1.0.0").
     *
     * @return the version string
     */
    public String versionString() {
        return version.toString();
    }

    /**
     * Returns true if this module has no components.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return components.isEmpty();
    }

    /**
     * Converts the component data to a map format.
     *
     * @return map of component name to values
     */
    public Map<String, List<Float>> toComponentMap() {
        return components.stream()
                .collect(Collectors.toMap(
                        ComponentData::name,
                        ComponentData::values
                ));
    }
}
