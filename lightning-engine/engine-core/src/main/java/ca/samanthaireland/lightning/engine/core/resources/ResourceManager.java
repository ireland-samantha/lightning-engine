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


package ca.samanthaireland.lightning.engine.core.resources;

import java.util.Optional;
import java.util.stream.Stream;

public interface ResourceManager {
    /**
     * Save a resource and return the assigned resource ID.
     * If the resource has ID 0 or negative, a new ID will be assigned.
     *
     * @param resource the resource to save
     * @return the assigned resource ID
     */
    long saveResource(Resource resource);

    /**
     * Get all resources.
     *
     * @return stream of all resources
     */
    Stream<Resource> requestResource();

    /**
     * Find a resource by its name.
     *
     * @param name the resource name to search for
     * @return Optional containing the resource if found, or empty if not found
     */
    default Optional<Resource> findByName(String name) {
        return requestResource()
                .filter(r -> name.equals(r.resourceName()))
                .findFirst();
    }

    /**
     * Get the resource ID for a resource with the given name.
     *
     * @param name the resource name
     * @return the resource ID, or -1 if not found
     */
    default long getResourceIdByName(String name) {
        return findByName(name)
                .map(Resource::resourceId)
                .orElse(-1L);
    }
}
