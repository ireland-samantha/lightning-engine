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


package ca.samanthaireland.lightning.engine.internal.ext.ai;

import ca.samanthaireland.lightning.engine.core.command.CommandExecutor;
import ca.samanthaireland.lightning.engine.core.command.CommandPayload;
import ca.samanthaireland.lightning.engine.core.resources.ResourceManager;
import ca.samanthaireland.game.domain.AICommand;
import ca.samanthaireland.game.domain.AIContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of {@link AIContext}.
 *
 * <p>Provides dependency injection and match-scoped context for AI.
 */
@Slf4j
public class DefaultAIContext implements AIContext {

    private final long matchId;
    private final AtomicLong currentTick;
    private final Map<Class<?>, Object> dependencies;
    private CommandExecutor commandExecutor;
    private ResourceManager resourceManager;

    /**
     * Create a new AI context.
     *
     * @param matchId the match ID this context is bound to
     */
    public DefaultAIContext(long matchId) {
        this.matchId = matchId;
        this.currentTick = new AtomicLong(0);
        this.dependencies = new ConcurrentHashMap<>();
    }

    @Override
    public long getMatchId() {
        return matchId;
    }

    @Override
    public long getCurrentTick() {
        return currentTick.get();
    }

    @Override
    public void executeCommand(AICommand aiCommand) {
        if (commandExecutor == null) {
            log.warn("CommandExecutor not set, cannot execute command: {}", aiCommand.commandName());
            return;
        }

        // Convert AICommand to CommandPayload
        CommandPayload payload = new AICommandPayload(aiCommand.payload());

        // Execute the command
        try {
            commandExecutor.executeCommand(aiCommand.commandName(), payload);
        } catch (Exception e) {
            log.warn("Failed to execute command '{}': {}", aiCommand.commandName(), e.getMessage());
        }
    }

    /**
     * Set the command executor for this context.
     *
     * @param commandExecutor the command executor
     */
    public void setCommandExecutor(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    /**
     * Set the resource manager for this context.
     *
     * @param resourceManager the resource manager
     */
    public void setResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @Override
    public long getResourceIdByName(String resourceName) {
        if (resourceManager == null) {
            log.warn("ResourceManager not set, cannot look up resource: {}", resourceName);
            return -1;
        }
        return resourceManager.getResourceIdByName(resourceName);
    }

    /**
     * Internal payload wrapper for AICommand payloads.
     */
    private record AICommandPayload(Map<String, Object> payload) implements CommandPayload {
        @Override
        public Map<String, Object> getPayload() {
            return payload;
        }
    }

    /**
     * Update the current tick number.
     *
     * @param tick the new tick number
     */
    public void setCurrentTick(long tick) {
        currentTick.set(tick);
    }

    /**
     * Register a dependency for lookup.
     *
     * @param type the class type to register
     * @param instance the instance to associate with the type
     */
    public void addDependency(Class<?> type, Object instance) {
        dependencies.put(type, instance);
    }

    /**
     * Copy dependencies from another context or dependency source.
     *
     * @param deps the dependencies to copy
     */
    public void copyDependencies(Map<Class<?>, Object> deps) {
        dependencies.putAll(deps);
    }
}
