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
 * Adapter interface for resource management operations.
 *
 * <p>Handles listing, retrieving, uploading, and deleting resources (textures, etc.)
 * in a specific container.
 */
public interface ResourceAdapter {

    /**
     * List all resources in this container.
     *
     * @return list of resource metadata
     */
    List<ContainerAdapter.ResourceResponse> listResources() throws IOException;

    /**
     * Get a resource by ID from this container.
     *
     * @param resourceId the resource ID
     * @return the resource if found
     */
    Optional<ContainerAdapter.ResourceResponse> getResource(long resourceId) throws IOException;

    /**
     * Delete a resource from this container.
     *
     * @param resourceId the resource ID
     * @return true if deleted
     */
    boolean deleteResource(long resourceId) throws IOException;

    /**
     * Upload a resource to this container.
     *
     * @param resourceName the resource name
     * @param resourceType the resource type (e.g., "TEXTURE")
     * @param data the binary data
     * @return the created resource ID
     */
    long uploadResource(String resourceName, String resourceType, byte[] data) throws IOException;

    /**
     * Start building an upload request for this container.
     *
     * @return a builder for resource upload
     */
    ContainerAdapter.UploadResourceBuilder upload();
}
