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

package ca.samanthaireland.lightning.engine.api.resource.adapter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Adapter interface for container lifecycle operations.
 *
 * <p>Handles container CRUD and lifecycle state changes (start, stop).
 */
public interface ContainerLifecycleAdapter {

    /**
     * Create a new container.
     *
     * @param name the container name
     * @return the created container response
     */
    ContainerAdapter.ContainerResponse createContainer(String name) throws IOException;

    /**
     * Create a new container with configuration.
     *
     * @return a builder for container creation
     */
    ContainerAdapter.CreateContainerBuilder create();

    /**
     * Get a container by ID.
     *
     * @param containerId the container ID
     * @return the container if found
     */
    Optional<ContainerAdapter.ContainerResponse> getContainer(long containerId) throws IOException;

    /**
     * Get all containers.
     *
     * @return list of all containers
     */
    List<ContainerAdapter.ContainerResponse> getAllContainers() throws IOException;

    /**
     * Delete a container.
     *
     * @param containerId the container ID
     * @return true if deleted
     */
    boolean deleteContainer(long containerId) throws IOException;

    /**
     * Start a container.
     *
     * @param containerId the container ID
     * @return the updated container response
     */
    ContainerAdapter.ContainerResponse startContainer(long containerId) throws IOException;

    /**
     * Stop a container.
     *
     * @param containerId the container ID
     * @return the updated container response
     */
    ContainerAdapter.ContainerResponse stopContainer(long containerId) throws IOException;
}
