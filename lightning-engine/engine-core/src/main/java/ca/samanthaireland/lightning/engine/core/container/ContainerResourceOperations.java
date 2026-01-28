/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.core.container;

import ca.samanthaireland.lightning.engine.core.resources.Resource;
import ca.samanthaireland.lightning.engine.core.resources.ResourceType;

import java.util.List;
import java.util.Optional;

/**
 * Fluent API for resource operations within an ExecutionContainer.
 *
 * <p>Provides chainable methods for managing container-scoped resources.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Get all resources in container
 * List<Resource> resources = container.resources().all();
 *
 * // Get a specific resource
 * Optional<Resource> resource = container.resources().get(resourceId);
 *
 * // Upload a new resource
 * Resource uploaded = container.resources()
 *     .upload()
 *     .name("sprite.png")
 *     .type(ResourceType.IMAGE)
 *     .data(bytes)
 *     .execute();
 *
 * // Delete a resource
 * container.resources().delete(resourceId);
 * }</pre>
 */
public interface ContainerResourceOperations {

    /**
     * Saves a resource to the container.
     *
     * @param resource the resource to save
     * @return the assigned resource ID
     */
    long save(Resource resource);

    /**
     * Retrieves a resource by ID.
     *
     * @param resourceId the resource ID
     * @return the resource if found, empty otherwise
     */
    Optional<Resource> get(long resourceId);

    /**
     * Returns all resources in this container.
     *
     * @return list of all resources
     */
    List<Resource> all();

    /**
     * Deletes a resource by ID.
     *
     * @param resourceId the resource ID to delete
     * @return true if the resource was deleted, false if not found
     */
    boolean delete(long resourceId);

    /**
     * Checks if a resource exists in this container.
     *
     * @param resourceId the resource ID to check
     * @return true if the resource exists
     */
    boolean has(long resourceId);

    /**
     * Finds a resource by name.
     *
     * @param name the resource name to search for
     * @return the resource if found, empty otherwise
     */
    Optional<Resource> findByName(String name);

    /**
     * Returns the total number of resources in this container.
     *
     * @return the resource count
     */
    int count();

    /**
     * Creates a fluent builder for uploading a new resource.
     *
     * @return a new upload builder
     */
    UploadBuilder upload();

    /**
     * Fluent builder for uploading resources.
     */
    interface UploadBuilder {

        /**
         * Sets the resource name.
         *
         * @param name the resource name
         * @return this builder for chaining
         */
        UploadBuilder name(String name);

        /**
         * Sets the resource type.
         *
         * @param type the resource type
         * @return this builder for chaining
         */
        UploadBuilder type(ResourceType type);

        /**
         * Sets the resource data.
         *
         * @param data the binary data
         * @return this builder for chaining
         */
        UploadBuilder data(byte[] data);

        /**
         * Executes the upload and returns the created resource.
         *
         * @return the created resource with assigned ID
         * @throws IllegalStateException if required fields are missing
         */
        Resource execute();
    }
}
