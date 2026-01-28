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

import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.system.EngineSystem;

import java.util.List;

public interface EngineModule {
    /**
     * Allocates heap memory for any systems the module adds to the game loop
     */
    List<EngineSystem> createSystems();

    /**
     * Allocates heap memory for any public-facing interfaces the module exposes
     */
    List<EngineCommand> createCommands();

    /**
     * Allocates heap memory for any components the module declares and is responsible for the lifecycle of.
     */
    List<BaseComponent> createComponents();

    BaseComponent createFlagComponent();

    String getName();

    /**
     * Returns the public APIs (exports) that this module exposes to other modules.
     *
     * <p>Module exports allow inter-module communication by providing typed
     * interfaces that other modules can retrieve via
     * {@link ModuleContext#getModuleExports(Class)}.
     *
     * <p>Example:
     * <pre>{@code
     * public class MyModule implements EngineModule {
     *     private final MyModuleExports exports;
     *
     *     public MyModule(ModuleContext context) {
     *         this.exports = new MyModuleExports(/* dependencies *\/);
     *     }
     *
     *     @Override
     *     public List<ModuleExports> getExports() {
     *         return List.of(exports);
     *     }
     * }
     * }</pre>
     *
     * @return list of module exports, or empty list if none
     */
    default List<ModuleExports> getExports() {
        return List.of();
    }

    /**
     * Returns the version of this module.
     *
     * <p>The default implementation returns 1.0 for backwards compatibility
     * with modules that don't specify a version.
     *
     * @return the module version
     */
    default ModuleVersion getVersion() {
        return ModuleVersion.of(1, 0);
    }

    /**
     * Returns the unique identifier for this module (name + version).
     *
     * @return the module identifier
     */
    default ModuleIdentifier getIdentifier() {
        return ModuleIdentifier.of(getName(), getVersion());
    }
}
