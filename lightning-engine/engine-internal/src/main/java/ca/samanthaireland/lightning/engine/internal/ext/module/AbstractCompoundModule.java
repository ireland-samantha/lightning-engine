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

package ca.samanthaireland.lightning.engine.internal.ext.module;

import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.system.EngineSystem;
import ca.samanthaireland.lightning.engine.ext.module.CompoundModule;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleExports;
import ca.samanthaireland.lightning.engine.ext.module.ModuleIdentifier;
import ca.samanthaireland.lightning.engine.ext.module.ModuleVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base implementation for compound modules.
 *
 * <p>This class provides the core functionality for compound modules:
 * <ul>
 *   <li>Aggregates exports from all component modules</li>
 *   <li>Delegates system, command, and component creation to components in order</li>
 *   <li>Tracks component modules and their identifiers</li>
 * </ul>
 *
 * <p>Subclasses must provide the module name, version, and resolved component modules.
 */
public abstract class AbstractCompoundModule implements CompoundModule {

    private final String name;
    private final ModuleVersion version;
    private final List<ModuleIdentifier> componentIdentifiers;
    private final List<EngineModule> resolvedComponents;

    /**
     * Create a compound module with the given name, version, and components.
     *
     * @param name the compound module name
     * @param version the compound module version
     * @param componentIdentifiers the identifiers of required component modules
     * @param resolvedComponents the resolved component module instances
     */
    protected AbstractCompoundModule(
            String name,
            ModuleVersion version,
            List<ModuleIdentifier> componentIdentifiers,
            List<EngineModule> resolvedComponents) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Module name cannot be null or blank");
        }
        if (version == null) {
            throw new IllegalArgumentException("Module version cannot be null");
        }
        if (componentIdentifiers == null) {
            throw new IllegalArgumentException("Component identifiers cannot be null");
        }
        if (resolvedComponents == null) {
            throw new IllegalArgumentException("Resolved components cannot be null");
        }
        if (componentIdentifiers.size() != resolvedComponents.size()) {
            throw new IllegalArgumentException(
                    "Number of component identifiers (" + componentIdentifiers.size() +
                            ") must match number of resolved components (" + resolvedComponents.size() + ")");
        }

        this.name = name;
        this.version = version;
        this.componentIdentifiers = Collections.unmodifiableList(new ArrayList<>(componentIdentifiers));
        this.resolvedComponents = Collections.unmodifiableList(new ArrayList<>(resolvedComponents));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ModuleVersion getVersion() {
        return version;
    }

    @Override
    public List<ModuleIdentifier> getComponentModules() {
        return componentIdentifiers;
    }

    @Override
    public boolean containsModule(String moduleName) {
        return componentIdentifiers.stream()
                .anyMatch(id -> id.name().equals(moduleName));
    }

    @Override
    public List<EngineModule> getResolvedComponents() {
        return resolvedComponents;
    }

    @Override
    public List<EngineSystem> createSystems() {
        List<EngineSystem> systems = new ArrayList<>();
        for (EngineModule component : resolvedComponents) {
            List<EngineSystem> componentSystems = component.createSystems();
            if (componentSystems != null) {
                systems.addAll(componentSystems);
            }
        }
        return systems;
    }

    @Override
    public List<EngineCommand> createCommands() {
        List<EngineCommand> commands = new ArrayList<>();
        for (EngineModule component : resolvedComponents) {
            List<EngineCommand> componentCommands = component.createCommands();
            if (componentCommands != null) {
                commands.addAll(componentCommands);
            }
        }
        return commands;
    }

    @Override
    public List<BaseComponent> createComponents() {
        List<BaseComponent> components = new ArrayList<>();
        for (EngineModule component : resolvedComponents) {
            List<BaseComponent> componentComponents = component.createComponents();
            if (componentComponents != null) {
                components.addAll(componentComponents);
            }
        }
        return components;
    }

    @Override
    public BaseComponent createFlagComponent() {
        // Compound modules don't have their own flag component
        // Component modules have their own flags
        return null;
    }

    @Override
    public List<ModuleExports> getExports() {
        List<ModuleExports> exports = new ArrayList<>();
        for (EngineModule component : resolvedComponents) {
            List<ModuleExports> componentExports = component.getExports();
            if (componentExports != null) {
                exports.addAll(componentExports);
            }
        }
        return exports;
    }
}
