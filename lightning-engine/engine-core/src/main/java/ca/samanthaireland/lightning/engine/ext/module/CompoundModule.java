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

package ca.samanthaireland.lightning.engine.ext.module;

import java.util.List;

/**
 * A compound module is composed of other modules.
 *
 * <p>Compound modules aggregate functionality from multiple component modules,
 * providing a unified interface while delegating to the underlying modules.
 *
 * <p>Example: A PhysicsModule might be composed of GridMapModule and RigidBodyModule.
 *
 * <p>Key characteristics:
 * <ul>
 *   <li>Aggregates exports from all component modules</li>
 *   <li>Delegates lifecycle methods (systems, commands, components) to components in order</li>
 *   <li>Supports version requirements for component modules</li>
 * </ul>
 */
public interface CompoundModule extends EngineModule {

    /**
     * Returns the identifiers of the component modules that make up this compound module.
     *
     * <p>The order of identifiers reflects the order in which component modules
     * are initialized and have their systems executed.
     *
     * @return list of component module identifiers (name + required version)
     */
    List<ModuleIdentifier> getComponentModules();

    /**
     * Check if this compound module contains a module with the given name.
     *
     * @param moduleName the name of the module to check
     * @return true if a component module with this name exists
     */
    boolean containsModule(String moduleName);

    /**
     * Returns the resolved component module instances.
     *
     * <p>These are the actual module instances that were resolved during
     * compound module creation.
     *
     * @return list of resolved component modules
     */
    List<EngineModule> getResolvedComponents();
}
