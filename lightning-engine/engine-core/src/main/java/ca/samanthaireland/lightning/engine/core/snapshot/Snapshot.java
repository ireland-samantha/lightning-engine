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
 * Represents a snapshot of entity component data organized by module.
 *
 * <p>Snapshots use columnar storage: each component contains a list of values
 * where index corresponds to entity position.
 *
 * <pre>{@code
 * Snapshot snapshot = Snapshot.builder()
 *     .module("EntityModule", "1.0",
 *         ComponentData.of("ENTITY_TYPE", 1.0f, 1.0f, 2.0f),
 *         ComponentData.of("OWNER_ID", 100.0f, 100.0f, 200.0f))
 *     .module("MovementModule", "1.0",
 *         ComponentData.of("POSITION_X", 100.0f, 200.0f, 300.0f),
 *         ComponentData.of("POSITION_Y", 50.0f, 60.0f, 70.0f))
 *     .build();
 *
 * // Access module data fluently
 * snapshot.module("EntityModule")
 *     .flatMap(m -> m.component("ENTITY_TYPE"))
 *     .map(c -> c.valueAt(0));  // 1.0f
 * }</pre>
 *
 * @param modules the list of module data
 */
public record Snapshot(List<ModuleData> modules) {

    private static final Snapshot EMPTY = new Snapshot(List.of());

    /**
     * Creates a new Snapshot instance.
     *
     * @param modules the module data
     * @throws NullPointerException if modules is null
     */
    public Snapshot {
        Objects.requireNonNull(modules, "Modules cannot be null");
        modules = List.copyOf(modules);
    }

    /**
     * Returns an empty snapshot with no data.
     *
     * @return an immutable empty snapshot
     */
    public static Snapshot empty() {
        return EMPTY;
    }

    /**
     * Creates a new snapshot builder.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if this snapshot contains no module data.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return modules.isEmpty();
    }

    /**
     * Returns the number of modules in this snapshot.
     *
     * @return the module count
     */
    public int moduleCount() {
        return modules.size();
    }

    /**
     * Returns the module with the given name, if present.
     *
     * @param moduleName the module name
     * @return the module data, or empty if not found
     */
    public Optional<ModuleData> module(String moduleName) {
        return modules.stream()
                .filter(m -> m.name().equals(moduleName))
                .findFirst();
    }

    /**
     * Returns true if this snapshot contains a module with the given name.
     *
     * @param moduleName the module name
     * @return true if the module exists
     */
    public boolean hasModule(String moduleName) {
        return modules.stream().anyMatch(m -> m.name().equals(moduleName));
    }

    /**
     * Returns all module names in this snapshot.
     *
     * @return list of module names
     */
    public List<String> moduleNames() {
        return modules.stream()
                .map(ModuleData::name)
                .toList();
    }

    /**
     * Converts this snapshot to the legacy map format for backwards compatibility.
     *
     * <p>Note: Version information is lost in this conversion.
     *
     * @return map of module name to component data map
     */
    public Map<String, Map<String, List<Float>>> toLegacyFormat() {
        return modules.stream()
                .collect(Collectors.toMap(
                        ModuleData::name,
                        ModuleData::toComponentMap
                ));
    }

    /**
     * Creates a Snapshot from the legacy map format.
     *
     * <p>All modules are assigned the default version (1.0.0).
     *
     * @param legacyData the legacy format data
     * @return a new Snapshot instance
     */
    public static Snapshot fromLegacyFormat(Map<String, Map<String, List<Float>>> legacyData) {
        if (legacyData == null || legacyData.isEmpty()) {
            return empty();
        }

        List<ModuleData> modules = legacyData.entrySet().stream()
                .map(entry -> {
                    String moduleName = entry.getKey();
                    Map<String, List<Float>> componentMap = entry.getValue();
                    List<ComponentData> components = componentMap.entrySet().stream()
                            .map(ce -> ComponentData.of(ce.getKey(), ce.getValue()))
                            .toList();
                    return ModuleData.of(moduleName, components);
                })
                .toList();

        return new Snapshot(modules);
    }

    /**
     * Builder for creating Snapshot instances fluently.
     */
    public static class Builder {
        private final java.util.ArrayList<ModuleData> modules = new java.util.ArrayList<>();

        private Builder() {}

        /**
         * Adds a module with the given name, version, and components.
         *
         * @param name       the module name
         * @param version    the version string (e.g., "1.0", "0.2.1")
         * @param components the component data
         * @return this builder
         */
        public Builder module(String name, String version, ComponentData... components) {
            modules.add(ModuleData.of(name, version, List.of(components)));
            return this;
        }

        /**
         * Adds a module with the given name, version, and component list.
         *
         * @param name       the module name
         * @param version    the module version
         * @param components the component data
         * @return this builder
         */
        public Builder module(String name, ModuleVersion version, List<ComponentData> components) {
            modules.add(ModuleData.of(name, version, components));
            return this;
        }

        /**
         * Adds pre-built module data.
         *
         * @param moduleData the module data
         * @return this builder
         */
        public Builder module(ModuleData moduleData) {
            modules.add(moduleData);
            return this;
        }

        /**
         * Builds the snapshot.
         *
         * @return a new Snapshot instance
         */
        public Snapshot build() {
            return new Snapshot(modules);
        }
    }
}
