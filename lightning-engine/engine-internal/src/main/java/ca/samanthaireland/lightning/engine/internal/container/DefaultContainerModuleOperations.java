/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.internal.container;

import ca.samanthaireland.lightning.engine.core.container.ContainerModuleOperations;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;
import ca.samanthaireland.lightning.engine.internal.ext.module.ModuleManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Default implementation of ContainerModuleOperations.
 */
public final class DefaultContainerModuleOperations implements ContainerModuleOperations {

    private final ExecutionContainer container;
    private final ModuleManager moduleManager;

    public DefaultContainerModuleOperations(ExecutionContainer container, ModuleManager moduleManager) {
        this.container = container;
        this.moduleManager = moduleManager;
    }

    @Override
    public ContainerModuleOperations install(String jarPath) {
        try {
            moduleManager.installModule(Path.of(jarPath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to install module from " + jarPath, e);
        }
        return this;
    }

    @Override
    public ContainerModuleOperations install(Class<? extends ModuleFactory> factoryClass) {
        moduleManager.installModule(factoryClass);
        return this;
    }

    @Override
    public ContainerModuleOperations reload() throws IOException {
        if (moduleManager != null) {
            moduleManager.reloadInstalled();
        }
        return this;
    }

    @Override
    public List<String> available() {
        return moduleManager != null ? moduleManager.getAvailableModules() : List.of();
    }

    @Override
    public boolean has(String moduleName) {
        return moduleManager != null && moduleManager.hasModule(moduleName);
    }
}
