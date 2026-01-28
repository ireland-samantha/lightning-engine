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

import ca.samanthaireland.lightning.engine.core.match.MatchService;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;

import java.util.Optional;
import java.util.Set;

/**
 * Context provided to modules for dependency injection.
 *
 * <p>This interface provides explicit access to common dependencies that modules
 * may need. It follows the Dependency Inversion Principle (DIP) by:
 * <ul>
 *   <li>Making dependencies explicit and documented</li>
 *   <li>Allowing compile-time type safety</li>
 *   <li>Enabling proper testing with mock implementations</li>
 * </ul>
 *
 * <p>SOLID compliance:
 * <ul>
 *   <li>ISP: Clients only depend on methods they use</li>
 *   <li>DIP: High-level modules depend on abstractions</li>
 * </ul>
 *
 * <p>Available dependencies:
 * <ul>
 *   <li>{@link EntityComponentStore} - for reading/updating entities and components</li>
 *   <li>{@link MatchService} - for match operations</li>
 *   <li>{@link ModuleResolver} - for resolving other modules</li>
 *   <li>{@link ModuleManager} - for module management operations</li>
 * </ul>
 *
 * @see ModuleFactory
 */
public interface ModuleContext {

    /**
     * Get the entity component store.
     *
     * <p>The store is the central registry for all entities and their components.
     * Most modules will need this dependency.
     *
     * @return the entity component store
     * @throws IllegalStateException if the store is not available
     */
    EntityComponentStore getEntityComponentStore();

    /**
     * Get the entity component store, or empty if not available.
     *
     * @return Optional containing the store, or empty
     */
    default Optional<EntityComponentStore> findEntityComponentStore() {
        try {
            return Optional.ofNullable(getEntityComponentStore());
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }

    /**
     * Get the match service for match-related operations.
     *
     * @return the match service, or null if not available
     */
    MatchService getMatchService();

    /**
     * Get the module resolver for looking up other modules.
     *
     * @return the module resolver, or null if not available
     */
    ModuleResolver getModuleResolver();

    /**
     * Get the module manager for module management operations.
     *
     * <p>Note: This is typically only available within the engine-internal module.
     *
     * @return the module manager, or null if not available
     */
    Object getModuleManager();

    /**
     * Retrieve a module's exported API by type.
     *
     * <p>This method allows one module to access the public API of another module.
     * The export must have been previously registered via {@link #declareModuleExports(Class, ModuleExports)}
     * (typically done automatically during module initialization via {@link EngineModule#getExports()}).
     *
     * <p>Example usage:
     * <pre>{@code
     * // In RigidBodyModule, access GridMapModule's exports
     * GridMapExports gridMap = context.getModuleExports(GridMapExports.class);
     * gridMap.setPosition(matchId, entityId, position);
     * }</pre>
     *
     * <p><b>Security Note:</b> Module exports bypass the permission system - any module
     * can call any exported method. Design exports carefully to avoid exposing
     * internal state that should be protected.
     *
     * @param <T> the type of the module export
     * @param exportType the class of the export to retrieve
     * @return the module export instance, or null if not found
     * @see ModuleExports
     * @see EngineModule#getExports()
     */
    <T extends ModuleExports> T getModuleExports(Class<T> exportType);

    /**
     * Register a module's exported API.
     *
     * <p>This method is typically called automatically during module initialization
     * based on the module's {@link EngineModule#getExports()} method. Modules should
     * not normally call this method directly.
     *
     * <p><b>Note:</b> If an export of the same type is already registered, it will
     * be overwritten. This allows module hot-reloading but may cause issues if
     * multiple modules export the same interface type.
     *
     * @param <T> the type of the module export
     * @param exportType the class type to register the export under
     * @param instance the export instance
     * @see EngineModule#getExports()
     */
    <T extends ModuleExports> void declareModuleExports(Class<T> exportType, T instance);

    /**
     * Look up an arbitrary dependency by type.
     *
     * @throws
     */
    <T> T getClass(Class<? extends T> type);

    /**
     * Register a dependency for lookup.
     *
     * <p><b>Note:</b> This method is typically only used during initialization.
     * Modules should not need to call this method.
     *
     * @param type the class type to register
     * @param instance the instance to associate with the type
     */
    void addClass(Class<?> type, Object instance);

    Set<BaseComponent> getDeclaredComponents();

}
