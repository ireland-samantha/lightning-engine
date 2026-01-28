/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.internal.container;

import ca.samanthaireland.lightning.engine.core.container.ContainerAIOperations;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.internal.ext.ai.AIManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Default implementation of ContainerAIOperations.
 */
public final class DefaultContainerAIOperations implements ContainerAIOperations {

    private final ExecutionContainer container;
    private final AIManager aiManager;

    public DefaultContainerAIOperations(ExecutionContainer container, AIManager aiManager) {
        this.container = container;
        this.aiManager = aiManager;
    }

    @Override
    public ContainerAIOperations install(Path jarPath) throws IOException {
        if (aiManager != null) {
            aiManager.installAI(jarPath);
        }
        return this;
    }

    @Override
    public ContainerAIOperations install(String factoryClassName) {
        if (aiManager == null) {
            return this;
        }
        try {
            @SuppressWarnings("unchecked")
            Class<? extends ca.samanthaireland.game.backend.installation.AIFactory> factoryClass =
                    (Class<? extends ca.samanthaireland.game.backend.installation.AIFactory>)
                            Class.forName(factoryClassName);
            aiManager.installAI(factoryClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("AI factory class not found: " + factoryClassName, e);
        } catch (ClassCastException e) {
            throw new RuntimeException("Class is not an AIFactory: " + factoryClassName, e);
        }
        return this;
    }

    /**
     * Installs AI from a factory class.
     * Internal method not exposed in the interface due to module boundaries.
     *
     * @param factoryClass the AI factory class
     * @return this for fluent chaining
     */
    public ContainerAIOperations installFromClass(Class<? extends ca.samanthaireland.game.backend.installation.AIFactory> factoryClass) {
        if (aiManager != null) {
            aiManager.installAI(factoryClass);
        }
        return this;
    }

    @Override
    public ContainerAIOperations reload() throws IOException {
        if (aiManager != null) {
            aiManager.reloadInstalled();
        }
        return this;
    }

    @Override
    public List<String> available() {
        return aiManager != null ? aiManager.getAvailableAIs() : List.of();
    }

    @Override
    public boolean has(String aiName) {
        return aiManager != null && aiManager.hasAI(aiName);
    }

    @Override
    public ContainerAIOperations uninstall(String aiName) {
        if (aiManager != null) {
            aiManager.uninstallAI(aiName);
        }
        return this;
    }
}
