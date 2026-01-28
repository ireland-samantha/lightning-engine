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
import ca.samanthaireland.lightning.engine.internal.core.store.ModuleScopedStore;

import java.util.Set;

/**
 * A module-scoped context that wraps a shared context but provides
 * a module-specific {@link EntityComponentStore}.
 *
 * <p>This context delegates most operations to a shared parent context,
 * but provides a {@link ModuleScopedStore} that automatically applies
 * module-level permission checking.
 *
 * <p>The store is initially set to an empty scoped store during module
 * creation. After the module's components are known, call
 * {@link #setModuleScopedStore(ModuleScopedStore)} to install the final
 * store with proper ownership.
 *
 * @see ModuleScopedStore
 * @see ModuleContext
 */
public class ModuleScopedContext implements ModuleContext {

    private final ModuleContext delegate;
    private volatile ModuleScopedStore moduleScopedStore;

    /**
     * Create a module-scoped context.
     *
     * @param delegate the shared parent context
     * @param moduleScopedStore the module-specific store (may be empty initially)
     */
    public ModuleScopedContext(ModuleContext delegate, ModuleScopedStore moduleScopedStore) {
        this.delegate = delegate;
        this.moduleScopedStore = moduleScopedStore;
    }

    /**
     * Get the module-scoped store.
     *
     * @return the current module-scoped store
     */
    public ModuleScopedStore getModuleScopedStore() {
        return moduleScopedStore;
    }

    /**
     * Set the module-scoped store.
     *
     * <p>This is called after the module's components are known to install
     * a properly configured store with the correct ownership.
     *
     * @param store the configured module-scoped store
     */
    public void setModuleScopedStore(ModuleScopedStore store) {
        this.moduleScopedStore = store;
    }

    @Override
    public EntityComponentStore getEntityComponentStore() {
        return moduleScopedStore;
    }

    @Override
    public MatchService getMatchService() {
        return delegate.getMatchService();
    }

    @Override
    public ModuleResolver getModuleResolver() {
        return delegate.getModuleResolver();
    }

    @Override
    public Object getModuleManager() {
        return delegate.getModuleManager();
    }

    @Override
    public <T extends ModuleExports> T getModuleExports(Class<T> exportType) {
        return delegate.getModuleExports(exportType);
    }

    @Override
    public <T extends ModuleExports> void declareModuleExports(Class<T> exportType, T instance) {
        delegate.declareModuleExports(exportType, instance);
    }

    @Override
    public Set<BaseComponent> getDeclaredComponents() {
        return delegate.getDeclaredComponents();
    }

    @Override
    @Deprecated
    public <T> T getClass(Class<? extends T> type) {
        return delegate.getClass(type);
    }

    @Override
    public void addClass(Class<?> type, Object instance) {
        delegate.addClass(type, instance);
    }
}
