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

import ca.samanthaireland.lightning.engine.core.match.MatchService;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.module.ModuleExports;
import ca.samanthaireland.lightning.engine.ext.module.ModuleResolver;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link ModuleContext}.
 *
 * <p>Provides modules with access to engine services through both explicit typed
 * methods and a general-purpose lookup. The explicit methods are preferred as they
 * provide compile-time type safety and better documentation.
 *
 * <p>SOLID compliance:
 * <ul>
 *   <li>DIP: Implements ModuleContext abstraction</li>
 *   <li>SRP: Only responsible for dependency resolution</li>
 * </ul>
 *
 * <p>Thread-safe: All operations use ConcurrentHashMap for thread safety.
 *
 * @see ModuleContext
 */
@Slf4j
public class DefaultInjector implements ModuleContext {

    private final ConcurrentHashMap<Class<?>, Object> beans = new ConcurrentHashMap<>();
    private final Set<BaseComponent> declaredComponents = ConcurrentHashMap.newKeySet();

    // Cached references for explicit getters
    private EntityComponentStore entityComponentStore;
    private MatchService matchService;
    private ModuleResolver moduleResolver;
    private Object moduleManager;

    @Override
    public EntityComponentStore getEntityComponentStore() {
        if (entityComponentStore == null) {
            entityComponentStore = findBean(EntityComponentStore.class);
        }
        return entityComponentStore;
    }

    @Override
    public MatchService getMatchService() {
        if (matchService == null) {
            matchService = findBean(MatchService.class);
        }
        return matchService;
    }

    @Override
    public ModuleResolver getModuleResolver() {
        if (moduleResolver == null) {
            moduleResolver = findBean(ModuleResolver.class);
        }
        return moduleResolver;
    }

    @Override
    public Object getModuleManager() {
        if (moduleManager == null) {
            // Use string lookup to avoid circular dependency with ModuleManager
            moduleManager = beans.get(loadModuleManagerClass());
        }
        return moduleManager;
    }

    @Override
    public <T extends ModuleExports> T getModuleExports(Class<T> exportType) {
        return findBean(exportType);
    }

    /**
     * Look up a bean by type, returning null if not found.
     *
     * @param <T>  the expected type
     * @param type the class to look up
     * @return the bean instance, or null if not registered
     */
    @SuppressWarnings("unchecked")
    private <T> T findBean(Class<T> type) {
        return (T) beans.get(type);
    }

    @Override
    public <T extends ModuleExports> void declareModuleExports(Class<T> exportType, T instance) {
        addClass(exportType, instance);
    }

    @Override
    public Set<BaseComponent> getDeclaredComponents() {
        return Collections.unmodifiableSet(declaredComponents);
    }

    /**
     * Declare a component as part of the current module context.
     *
     * @param component the component to declare
     */
    public void declareComponent(BaseComponent component) {
        declaredComponents.add(component);
    }

    /**
     * Declare multiple components as part of the current module context.
     *
     * @param components the components to declare
     */
    public void declareComponents(Set<BaseComponent> components) {
        declaredComponents.addAll(components);
    }

    private Class<?> loadModuleManagerClass() {
        try {
            return Class.forName("ca.samanthaireland.lightning.engine.internal.ext.module.ModuleManager");
        } catch (ClassNotFoundException e) {
            log.debug("ModuleManager class not available");
            return null;
        }
    }

    @Override
    public <T> T getClass(Class<? extends T> bean) {
        log.debug("Get bean {}", bean);
        Object found = beans.get(bean);
        if (found == null) {
            throw new RuntimeException(String.format("Class not found %s in injector", bean.getName()));
        }

        return (T) found;
    }

    @Override
    public void addClass(Class<?> clazz, Object impl) {
        log.debug("Add bean {}", clazz);
        beans.put(clazz, impl);
        // Invalidate cached references when dependencies change
        invalidateCaches();
    }

    private void invalidateCaches() {
        entityComponentStore = null;
        matchService = null;
        moduleResolver = null;
        moduleManager = null;
    }

    /**
     * Clear all registered beans and declared components.
     * For testing purposes.
     */
    public void clear() {
        beans.clear();
        declaredComponents.clear();
        invalidateCaches();
    }

    /**
     * Get the number of registered beans.
     * For testing purposes.
     *
     * @return the number of registered beans
     */
    public int size() {
        return beans.size();
    }
}
