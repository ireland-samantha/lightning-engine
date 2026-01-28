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


package ca.samanthaireland.lightning.engine.core.container;

import java.util.List;
import java.util.Optional;

/**
 * Central registry and factory for execution containers.
 * Manages container lifecycle and provides access to containers by ID or name.
 */
public interface ContainerManager {

    /**
     * Creates a new execution container with the given configuration.
     *
     * @param config the container configuration
     * @return the created container (in CREATED state)
     */
    ExecutionContainer createContainer(ContainerConfig config);

    /**
     * Retrieves a container by ID.
     *
     * @param containerId the container ID
     * @return the container if found, empty otherwise
     */
    Optional<ExecutionContainer> getContainer(long containerId);

    /**
     * Retrieves a container by name.
     *
     * @param name the container name
     * @return the container if found, empty otherwise
     */
    Optional<ExecutionContainer> getContainerByName(String name);

    /**
     * Returns all containers.
     *
     * @return list of all containers
     */
    List<ExecutionContainer> getAllContainers();

    /**
     * Deletes a container. The container must be in STOPPED state.
     *
     * @param containerId the container ID to delete
     * @throws IllegalStateException if the container is not stopped
     * @throws ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException if container not found
     */
    void deleteContainer(long containerId);

    /**
     * Returns the default container used for backward compatibility.
     * This container is automatically created at startup with ID 0.
     *
     * @return the default container
     */
    ExecutionContainer getDefaultContainer();

    /**
     * Shuts down all containers and releases resources.
     * Called during application shutdown.
     */
    void shutdownAll();
}
