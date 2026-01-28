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

import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.core.container.ContainerConfig;
import ca.samanthaireland.lightning.engine.core.container.ContainerManager;
import ca.samanthaireland.lightning.engine.core.container.ContainerStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of ContainerManager.
 * Manages the lifecycle of execution containers.
 */
@Slf4j
public class InMemoryContainerManager implements ContainerManager {

    private final ConcurrentMap<Long, ExecutionContainer> containers = new ConcurrentHashMap<>();
    private final AtomicLong nextContainerId = new AtomicLong(1);
    private final ContainerConfig defaultConfig;

    /**
     * Creates a new container manager with default configuration.
     */
    public InMemoryContainerManager() {
        this(ContainerConfig.withDefaults("default"));
    }

    /**
     * Creates a new container manager with the specified default configuration.
     *
     * @param defaultConfig configuration to use when creating new containers
     */
    public InMemoryContainerManager(ContainerConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
        log.info("ContainerManager initialized (no default container)");
    }

    /**
     * Creates a new container manager with the specified defaults.
     *
     * @param moduleScanDirectory directory to scan for module JARs
     * @param maxEntities maximum entities per container
     * @param maxComponents maximum components per container
     * @param maxCommandsPerTick maximum commands processed per tick
     */
    public InMemoryContainerManager(String moduleScanDirectory, int maxEntities, int maxComponents, int maxCommandsPerTick) {
        this(ContainerConfig.builder("default")
                .moduleScanDirectory(java.nio.file.Path.of(moduleScanDirectory))
                .maxEntities(maxEntities)
                .maxComponents(maxComponents)
                .maxCommandsPerTick(maxCommandsPerTick)
                .build());
    }

    @Override
    public ExecutionContainer createContainer(ContainerConfig config) {
        long containerId = nextContainerId.getAndIncrement();
        ExecutionContainer container = new InMemoryExecutionContainer(containerId, config);
        containers.put(containerId, container);
        log.info("Created container {} with name '{}'", containerId, config.name());
        return container;
    }

    @Override
    public Optional<ExecutionContainer> getContainer(long containerId) {
        return Optional.ofNullable(containers.get(containerId));
    }

    @Override
    public Optional<ExecutionContainer> getContainerByName(String name) {
        return containers.values().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst();
    }

    @Override
    public List<ExecutionContainer> getAllContainers() {
        return new ArrayList<>(containers.values());
    }

    @Override
    public void deleteContainer(long containerId) {
        ExecutionContainer container = containers.get(containerId);
        if (container == null) {
            throw new EntityNotFoundException("Container not found: " + containerId);
        }

        if (container.getStatus() != ContainerStatus.STOPPED) {
            throw new IllegalStateException("Container must be stopped before deletion. Current status: " + container.getStatus());
        }

        containers.remove(containerId);
        log.info("Deleted container {}", containerId);
    }

    @Override
    public ExecutionContainer getDefaultContainer() {
        // No default container - return first available or null
        return containers.values().stream().findFirst().orElse(null);
    }

    @Override
    public void shutdownAll() {
        log.info("Shutting down all {} containers", containers.size());

        for (ExecutionContainer container : containers.values()) {
            try {
                container.lifecycle().stop();
            } catch (Exception e) {
                log.error("Error stopping container {}: {}", container.getId(), e.getMessage(), e);
            }
        }

        containers.clear();
        log.info("All containers shut down");
    }

    /**
     * Returns the number of containers.
     *
     * @return the number of containers
     */
    public int getContainerCount() {
        return containers.size();
    }
}
