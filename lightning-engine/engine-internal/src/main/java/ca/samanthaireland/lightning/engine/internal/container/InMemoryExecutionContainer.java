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


package ca.samanthaireland.lightning.engine.internal.container;

import ca.samanthaireland.lightning.engine.core.command.CommandPayload;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.container.ContainerAIOperations;
import ca.samanthaireland.lightning.engine.core.container.ContainerCommandOperations;
import ca.samanthaireland.lightning.engine.core.container.ContainerConfig;
import ca.samanthaireland.lightning.engine.core.container.ContainerLifecycleOperations;
import ca.samanthaireland.lightning.engine.core.container.ContainerMatchOperations;
import ca.samanthaireland.lightning.engine.core.container.ContainerModuleOperations;
import ca.samanthaireland.lightning.engine.core.container.ContainerPlayerOperations;
import ca.samanthaireland.lightning.engine.core.container.ContainerResourceOperations;
import ca.samanthaireland.lightning.engine.core.container.ContainerSessionOperations;
import ca.samanthaireland.lightning.engine.core.container.ContainerSnapshotOperations;
import ca.samanthaireland.lightning.engine.core.container.ContainerStatus;
import ca.samanthaireland.lightning.engine.core.container.ContainerTickOperations;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.core.match.Match;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.internal.GameLoop;
import ca.samanthaireland.lightning.engine.internal.core.command.CommandResolver;
import ca.samanthaireland.lightning.engine.internal.core.match.InMemoryMatchService;
import ca.samanthaireland.lightning.engine.internal.core.snapshot.SnapshotProvider;
import ca.samanthaireland.lightning.engine.internal.ext.module.DefaultInjector;
import ca.samanthaireland.lightning.engine.internal.ext.module.ModuleManager;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Full implementation of ExecutionContainer with complete runtime isolation.
 *
 * <p>Each container has:
 * <ul>
 *     <li>Isolated classloader for module JARs</li>
 *     <li>Separate EntityComponentStore</li>
 *     <li>Independent GameLoop with its own tick thread</li>
 *     <li>Container-scoped command queue</li>
 * </ul>
 */
@Slf4j
public class InMemoryExecutionContainer implements ExecutionContainer, Closeable {

    private final long id;
    private final ContainerConfig config;
    private final AtomicReference<ContainerStatus> status;

    // Collaborators (extracted for SRP)
    private ContainerComponentInitializer componentInitializer;
    private final ContainerTickExecutor tickExecutor;

    // Fluent API operations
    private ContainerModuleOperations moduleOperations;
    private ContainerAIOperations aiOperations;
    private ContainerResourceOperations resourceOperations;
    private ContainerLifecycleOperations lifecycleOperations;
    private ContainerTickOperations tickOperations;
    private ContainerCommandOperations commandOperations;
    private ContainerMatchOperations matchOperations;
    private ContainerSnapshotOperations snapshotOperations;
    private ContainerPlayerOperations playerOperations;
    private ContainerSessionOperations sessionOperations;

    /**
     * Creates a new execution container.
     *
     * @param id     the unique container ID
     * @param config the container configuration
     */
    public InMemoryExecutionContainer(long id, ContainerConfig config) {
        this.id = id;
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.status = new AtomicReference<>(ContainerStatus.CREATED);

        // Create tick executor collaborator
        this.tickExecutor = new ContainerTickExecutor(id, config.name(), this::getStatus);

        log.info("Created container {} with name '{}'", id, config.name());
    }

    /**
     * Get the game loop for this container.
     *
     * @return the game loop, or null if container not started
     */
    public GameLoop getGameLoop() {
        return componentInitializer != null ? componentInitializer.getGameLoop() : null;
    }

    /**
     * Get the caching snapshot provider for this container.
     *
     * @return the caching snapshot provider, or null if container not started
     */
    public ca.samanthaireland.lightning.engine.internal.core.snapshot.CachingSnapshotProvider getCachingSnapshotProvider() {
        return componentInitializer != null ? componentInitializer.getCachingSnapshotProvider() : null;
    }

    /**
     * Get the entity component store for this container.
     *
     * @return the entity store, or null if container not started
     */
    public ca.samanthaireland.lightning.engine.core.store.EntityComponentStore getEntityStore() {
        return componentInitializer != null ? componentInitializer.getEntityStore() : null;
    }

    /**
     * Get the command queue manager for this container.
     *
     * @return the command queue manager, or null if container not started
     */
    public ca.samanthaireland.lightning.engine.internal.core.command.InMemoryCommandQueueManager getCommandQueueManager() {
        return componentInitializer != null ? componentInitializer.getCommandQueueManager() : null;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getName() {
        return config.name();
    }

    @Override
    public ContainerConfig getConfig() {
        return config;
    }

    @Override
    public ContainerStatus getStatus() {
        return status.get();
    }

    // =========================================================================
    // LIFECYCLE (Internal methods - use lifecycle() fluent API)
    // =========================================================================

    /**
     * Internal implementation of start. Use {@link #lifecycle()}.start() instead.
     */
    void startInternal() {
        if (!status.compareAndSet(ContainerStatus.CREATED, ContainerStatus.STARTING)) {
            throw new IllegalStateException("Container can only be started from CREATED state, current: " + status.get());
        }

        try {
            log.info("Starting container {} '{}'", id, config.name());
            componentInitializer = new ContainerComponentInitializer(id, config, tickExecutor::getCurrentTick);
            componentInitializer.initialize();
            tickExecutor.setGameLoop(componentInitializer.getGameLoop());
            status.set(ContainerStatus.RUNNING);
            log.info("Container {} '{}' started successfully", id, config.name());
        } catch (Exception e) {
            status.set(ContainerStatus.STOPPED);
            log.error("Failed to start container {} '{}': {}", id, config.name(), e.getMessage(), e);
            throw new RuntimeException("Failed to start container: " + e.getMessage(), e);
        }
    }

    /**
     * Internal implementation of stop. Use {@link #lifecycle()}.stop() instead.
     */
    void stopInternal() {
        ContainerStatus current = status.get();
        if (current == ContainerStatus.STOPPED || current == ContainerStatus.STOPPING) {
            return; // Already stopped/stopping
        }

        if (!status.compareAndSet(current, ContainerStatus.STOPPING)) {
            return; // Another thread is stopping
        }

        log.info("Stopping container {} '{}'", id, config.name());

        try {
            tickExecutor.shutdown(5, TimeUnit.SECONDS);

            if (componentInitializer != null) {
                GameLoop gameLoop = componentInitializer.getGameLoop();
                if (gameLoop != null) {
                    gameLoop.shutdown();
                }

                ContainerClassLoader classLoader = componentInitializer.getContainerClassLoader();
                if (classLoader != null) {
                    classLoader.close();
                }
            }

            status.set(ContainerStatus.STOPPED);
            log.info("Container {} '{}' stopped", id, config.name());
        } catch (Exception e) {
            log.error("Error stopping container {} '{}': {}", id, config.name(), e.getMessage(), e);
            status.set(ContainerStatus.STOPPED);
        }
    }

    /**
     * Internal implementation of pause. Use {@link #lifecycle()}.pause() instead.
     */
    void pauseInternal() {
        if (!status.compareAndSet(ContainerStatus.RUNNING, ContainerStatus.PAUSED)) {
            throw new IllegalStateException("Container can only be paused from RUNNING state");
        }
        tickExecutor.stopAutoAdvance();
        log.info("Container {} '{}' paused", id, config.name());
    }

    /**
     * Internal implementation of resume. Use {@link #lifecycle()}.resume() instead.
     */
    void resumeInternal() {
        if (!status.compareAndSet(ContainerStatus.PAUSED, ContainerStatus.RUNNING)) {
            throw new IllegalStateException("Container can only be resumed from PAUSED state");
        }
        log.info("Container {} '{}' resumed", id, config.name());
    }

    @Override
    public void close() throws IOException {
        stopInternal();
    }

    // =========================================================================
    // TICK CONTROL (Internal methods - use ticks() fluent API)
    // =========================================================================

    /**
     * Internal implementation of getCurrentTick. Use {@link #ticks()}.current() instead.
     */
    long getCurrentTickInternal() {
        return tickExecutor.getCurrentTick();
    }

    /**
     * Internal implementation of advanceTick. Use {@link #ticks()}.advance() instead.
     */
    long advanceTickInternal() {
        checkRunning();
        return tickExecutor.advanceTick();
    }

    /**
     * Internal implementation of startAutoAdvance. Use {@link #ticks()}.play() instead.
     */
    void startAutoAdvanceInternal(long intervalMs) {
        checkRunning();
        tickExecutor.startAutoAdvance(intervalMs);
    }

    /**
     * Internal implementation of stopAutoAdvance. Use {@link #ticks()}.stop() instead.
     */
    void stopAutoAdvanceInternal() {
        tickExecutor.stopAutoAdvance();
    }

    /**
     * Internal implementation of isAutoAdvancing. Use {@link #ticks()}.isPlaying() instead.
     */
    boolean isAutoAdvancingInternal() {
        return tickExecutor.isAutoAdvancing();
    }

    /**
     * Internal implementation of getAutoAdvanceInterval. Use {@link #ticks()}.interval() instead.
     */
    long getAutoAdvanceIntervalInternal() {
        return tickExecutor.getAutoAdvanceInterval();
    }

    // =========================================================================
    // MATCH MANAGEMENT (Internal methods - use matches() fluent API)
    // =========================================================================

    /**
     * Internal implementation of createMatch. Use {@link #matches()}.create() instead.
     */
    Match createMatchInternal(Match match) {
        checkRunning();
        Match containerMatch = match.withContainerId(id);
        return componentInitializer.getMatchService().createMatch(containerMatch);
    }

    /**
     * Internal implementation of getMatch. Use {@link #matches()}.get() instead.
     */
    Optional<Match> getMatchInternal(long matchId) {
        InMemoryMatchService matchService = componentInitializer != null ? componentInitializer.getMatchService() : null;
        return matchService != null ? matchService.getMatch(matchId) : Optional.empty();
    }

    /**
     * Internal implementation of getAllMatches. Use {@link #matches()}.all() instead.
     */
    List<Match> getAllMatchesInternal() {
        InMemoryMatchService matchService = componentInitializer != null ? componentInitializer.getMatchService() : null;
        return matchService != null ? matchService.getAllMatches() : List.of();
    }

    /**
     * Internal implementation of deleteMatch. Use {@link #matches()}.delete() instead.
     */
    void deleteMatchInternal(long matchId) {
        componentInitializer.getMatchService().deleteMatch(matchId);
    }

    @Override
    public List<ExecutionContainer.CommandInfo> getAvailableCommands() {
        CommandResolver commandResolver = componentInitializer != null ? componentInitializer.getCommandResolver() : null;
        if (commandResolver == null) {
            return List.of();
        }

        return commandResolver.getAll().stream()
                .map(cmd -> {
                    List<ExecutionContainer.ParameterInfo> params;
                    if (cmd.getParameters() != null) {
                        // Use explicit parameter definitions if available
                        params = cmd.getParameters().stream()
                                .map(p -> new ExecutionContainer.ParameterInfo(
                                        p.name(),
                                        p.type(),
                                        p.required(),
                                        p.description()))
                                .toList();
                    } else {
                        // Fall back to schema() for parameter info
                        params = cmd.schema().entrySet().stream()
                                .map(entry -> new ExecutionContainer.ParameterInfo(
                                        entry.getKey(),
                                        getSimpleTypeName(entry.getValue()),
                                        true,  // Assume required
                                        null))
                                .toList();
                    }
                    return new ExecutionContainer.CommandInfo(
                            cmd.getName(),
                            cmd.getDescription(),
                            cmd.getModuleName(),
                            params
                    );
                })
                .toList();
    }

    /**
     * Get a simple type name for display (e.g., "long" instead of "class java.lang.Long").
     */
    private String getSimpleTypeName(Class<?> type) {
        if (type == null) return "unknown";
        if (type == Long.class || type == long.class) return "long";
        if (type == Integer.class || type == int.class) return "int";
        if (type == Double.class || type == double.class) return "double";
        if (type == Float.class || type == float.class) return "float";
        if (type == Boolean.class || type == boolean.class) return "boolean";
        if (type == String.class) return "String";
        return type.getSimpleName();
    }

    /**
     * Enqueues a command for execution on the next tick.
     * The matchId should be included in the payload if needed.
     */
    void enqueueCommandInternal(String commandName, CommandPayload payload) {
        checkRunning();

        CommandResolver commandResolver = componentInitializer.getCommandResolver();
        EngineCommand command = commandResolver.resolveByName(commandName);
        if (command == null) {
            throw new EntityNotFoundException("Command not found: " + commandName);
        }

        componentInitializer.getCommandQueueManager().enqueue(command, payload);
    }

    /**
     * Enqueues a command with explicit match context (matchId should be in payload).
     * Provided for API clarity; delegates to {@link #enqueueCommandInternal(String, CommandPayload)}.
     */
    void enqueueCommandInternal(long matchId, String commandName, CommandPayload payload) {
        enqueueCommandInternal(commandName, payload);
    }

    @Override
    public ContainerStats getStats() {
        Runtime runtime = Runtime.getRuntime();

        EntityComponentStore entityStore = componentInitializer != null ? componentInitializer.getEntityStore() : null;
        int entityCount = entityStore != null ? entityStore.getEntityCount() : 0;
        int maxEntities = config.maxEntities();

        // Estimate memory used by the ECS store (entities * maxComponents * 4 bytes per float)
        long usedMemoryBytes = (long) entityCount * config.maxComponents() * Float.BYTES;

        // Max memory from config (convert MB to bytes)
        long maxMemoryBytes = config.maxMemoryMb() > 0
                ? config.maxMemoryMb() * 1024 * 1024
                : 0;

        // JVM memory stats
        long jvmMaxMemoryBytes = runtime.maxMemory();
        long jvmUsedMemoryBytes = runtime.totalMemory() - runtime.freeMemory();

        InMemoryMatchService matchService = componentInitializer != null ? componentInitializer.getMatchService() : null;
        ModuleManager moduleManager = componentInitializer != null ? componentInitializer.getModuleManager() : null;

        int matchCount = matchService != null ? matchService.getAllMatches().size() : 0;
        int moduleCount = moduleManager != null ? moduleManager.getAvailableModules().size() : 0;

        return new ContainerStats(
                entityCount,
                maxEntities,
                usedMemoryBytes,
                maxMemoryBytes,
                jvmMaxMemoryBytes,
                jvmUsedMemoryBytes,
                matchCount,
                moduleCount
        );
    }

    private void checkRunning() {
        if (status.get() != ContainerStatus.RUNNING) {
            throw new IllegalStateException("Container must be in RUNNING state, current: " + status.get());
        }
    }

    // =========================================================================
    // FLUENT API - Lazy initialization with consistent patterns
    // =========================================================================

    @Override
    public ContainerLifecycleOperations lifecycle() {
        if (lifecycleOperations == null) {
            lifecycleOperations = new DefaultContainerLifecycleOperations(this);
        }
        return lifecycleOperations;
    }

    @Override
    public ContainerTickOperations ticks() {
        if (tickOperations == null) {
            tickOperations = new DefaultContainerTickOperations(this);
        }
        return tickOperations;
    }

    @Override
    public ContainerCommandOperations commands() {
        if (commandOperations == null) {
            commandOperations = new DefaultContainerCommandOperations(this);
        }
        return commandOperations;
    }

    @Override
    public ContainerMatchOperations matches() {
        if (matchOperations == null) {
            matchOperations = new DefaultContainerMatchOperations(this);
        }
        return matchOperations;
    }

    // Operations below may return null if container not yet started

    @Override
    public ContainerModuleOperations modules() {
        if (moduleOperations == null) {
            // Create with null manager - will return empty lists before start
            moduleOperations = new DefaultContainerModuleOperations(this,
                    componentInitializer != null ? componentInitializer.getModuleManager() : null);
        }
        return moduleOperations;
    }

    @Override
    public ContainerAIOperations ai() {
        if (aiOperations == null) {
            // Create with null manager - will return empty lists before start
            aiOperations = new DefaultContainerAIOperations(this,
                    componentInitializer != null ? componentInitializer.getAiManager() : null);
        }
        return aiOperations;
    }

    @Override
    public ContainerResourceOperations resources() {
        if (resourceOperations == null) {
            // Create with null manager - will return empty lists before start
            resourceOperations = new DefaultContainerResourceOperations(this,
                    componentInitializer != null ? componentInitializer.getResourceManager() : null);
        }
        return resourceOperations;
    }

    @Override
    public ContainerSnapshotOperations snapshots() {
        if (snapshotOperations == null && componentInitializer != null) {
            DefaultInjector injector = componentInitializer.getInjector();
            SnapshotProvider provider = injector.getClass(SnapshotProvider.class);
            if (provider != null) {
                snapshotOperations = new DefaultContainerSnapshotOperations(provider);
            }
        }
        return snapshotOperations;
    }

    @Override
    public ContainerPlayerOperations players() {
        if (playerOperations == null) {
            playerOperations = new DefaultContainerPlayerOperations(this);
        }
        return playerOperations;
    }

    @Override
    public ContainerSessionOperations sessions() {
        if (sessionOperations == null) {
            sessionOperations = new DefaultContainerSessionOperations(this);
        }
        return sessionOperations;
    }
}
