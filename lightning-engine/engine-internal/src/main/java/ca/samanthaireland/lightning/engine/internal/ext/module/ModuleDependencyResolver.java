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

import ca.samanthaireland.lightning.engine.ext.module.CompoundModule;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves module dependencies and provides topological ordering.
 *
 * <p>This class handles:
 * <ul>
 *   <li>Building a dependency graph from compound modules</li>
 *   <li>Topological sorting for initialization order</li>
 *   <li>Circular dependency detection</li>
 * </ul>
 */
public class ModuleDependencyResolver {

    private final Map<String, Set<String>> dependencyGraph = new HashMap<>();
    private final Map<String, EngineModule> moduleMap = new HashMap<>();

    /**
     * Add a module to the dependency resolver.
     *
     * @param module the module to add
     */
    public void addModule(EngineModule module) {
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        }
        String name = module.getName();
        moduleMap.put(name, module);
        dependencyGraph.putIfAbsent(name, new HashSet<>());

        if (module instanceof CompoundModule compound) {
            for (ModuleIdentifier dep : compound.getComponentModules()) {
                dependencyGraph.get(name).add(dep.name());
                dependencyGraph.putIfAbsent(dep.name(), new HashSet<>());
            }
        }
    }

    /**
     * Add all modules from a collection.
     *
     * @param modules the modules to add
     */
    public void addModules(Iterable<EngineModule> modules) {
        if (modules == null) {
            throw new IllegalArgumentException("Modules cannot be null");
        }
        for (EngineModule module : modules) {
            addModule(module);
        }
    }

    /**
     * Check for circular dependencies.
     *
     * @return list of module names involved in circular dependencies, or empty if none
     */
    public List<String> detectCircularDependencies() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new LinkedHashSet<>();

        for (String node : dependencyGraph.keySet()) {
            List<String> cycle = findCycle(node, visited, recursionStack);
            if (!cycle.isEmpty()) {
                return cycle;
            }
        }
        return List.of();
    }

    private List<String> findCycle(String node, Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(node)) {
            // Found cycle - build cycle path
            List<String> cycle = new ArrayList<>();
            boolean inCycle = false;
            for (String n : recursionStack) {
                if (n.equals(node)) {
                    inCycle = true;
                }
                if (inCycle) {
                    cycle.add(n);
                }
            }
            cycle.add(node); // Complete the cycle
            return cycle;
        }

        if (visited.contains(node)) {
            return List.of();
        }

        visited.add(node);
        recursionStack.add(node);

        Set<String> dependencies = dependencyGraph.getOrDefault(node, Set.of());
        for (String dep : dependencies) {
            List<String> cycle = findCycle(dep, visited, recursionStack);
            if (!cycle.isEmpty()) {
                return cycle;
            }
        }

        recursionStack.remove(node);
        return List.of();
    }

    /**
     * Get modules in topological order (dependencies first).
     *
     * @return list of modules in initialization order
     * @throws CircularDependencyException if circular dependencies are detected
     */
    public List<EngineModule> getTopologicalOrder() {
        List<String> cycle = detectCircularDependencies();
        if (!cycle.isEmpty()) {
            throw new CircularDependencyException(
                    "Circular dependency detected: " + String.join(" -> ", cycle));
        }

        List<String> sortedNames = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> processing = new HashSet<>();

        for (String node : dependencyGraph.keySet()) {
            topologicalSort(node, visited, processing, sortedNames);
        }

        List<EngineModule> result = new ArrayList<>();
        for (String name : sortedNames) {
            EngineModule module = moduleMap.get(name);
            if (module != null) {
                result.add(module);
            }
        }
        return result;
    }

    private void topologicalSort(String node, Set<String> visited, Set<String> processing, List<String> result) {
        if (visited.contains(node)) {
            return;
        }

        processing.add(node);

        Set<String> dependencies = dependencyGraph.getOrDefault(node, Set.of());
        for (String dep : dependencies) {
            topologicalSort(dep, visited, processing, result);
        }

        processing.remove(node);
        visited.add(node);
        result.add(node);
    }

    /**
     * Get the direct dependencies for a module.
     *
     * @param moduleName the module name
     * @return set of direct dependency names
     */
    public Set<String> getDependencies(String moduleName) {
        return Collections.unmodifiableSet(
                dependencyGraph.getOrDefault(moduleName, Set.of()));
    }

    /**
     * Get all transitive dependencies for a module.
     *
     * @param moduleName the module name
     * @return set of all dependency names (direct and transitive)
     */
    public Set<String> getTransitiveDependencies(String moduleName) {
        Set<String> result = new HashSet<>();
        collectTransitiveDependencies(moduleName, result, new HashSet<>());
        return result;
    }

    private void collectTransitiveDependencies(String moduleName, Set<String> result, Set<String> visited) {
        if (visited.contains(moduleName)) {
            return;
        }
        visited.add(moduleName);

        Set<String> deps = dependencyGraph.getOrDefault(moduleName, Set.of());
        for (String dep : deps) {
            result.add(dep);
            collectTransitiveDependencies(dep, result, visited);
        }
    }

    /**
     * Clear all modules and dependencies.
     */
    public void clear() {
        dependencyGraph.clear();
        moduleMap.clear();
    }

    /**
     * Exception thrown when circular dependencies are detected.
     */
    public static class CircularDependencyException extends RuntimeException {
        public CircularDependencyException(String message) {
            super(message);
        }
    }
}
