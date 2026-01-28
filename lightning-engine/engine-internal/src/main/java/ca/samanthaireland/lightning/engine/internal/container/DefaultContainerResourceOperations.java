/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.internal.container;

import ca.samanthaireland.lightning.engine.core.container.ContainerResourceOperations;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.core.resources.Resource;
import ca.samanthaireland.lightning.engine.core.resources.ResourceType;
import ca.samanthaireland.lightning.engine.internal.core.resource.OnDiskResourceManager;

import java.util.List;
import java.util.Optional;

/**
 * Default implementation of ContainerResourceOperations.
 */
public final class DefaultContainerResourceOperations implements ContainerResourceOperations {

    private final ExecutionContainer container;
    private final OnDiskResourceManager resourceManager;

    public DefaultContainerResourceOperations(ExecutionContainer container, OnDiskResourceManager resourceManager) {
        this.container = container;
        this.resourceManager = resourceManager;
    }

    @Override
    public long save(Resource resource) {
        return resourceManager != null ? resourceManager.saveResource(resource) : -1;
    }

    @Override
    public Optional<Resource> get(long resourceId) {
        return resourceManager != null ? resourceManager.getResource(resourceId) : Optional.empty();
    }

    @Override
    public List<Resource> all() {
        return resourceManager != null ? resourceManager.requestResource().toList() : List.of();
    }

    @Override
    public boolean delete(long resourceId) {
        return resourceManager != null && resourceManager.deleteResource(resourceId);
    }

    @Override
    public boolean has(long resourceId) {
        return resourceManager != null && resourceManager.hasResource(resourceId);
    }

    @Override
    public Optional<Resource> findByName(String name) {
        return resourceManager != null ? resourceManager.findByName(name) : Optional.empty();
    }

    @Override
    public int count() {
        return resourceManager != null ? resourceManager.getResourceCount() : 0;
    }

    @Override
    public UploadBuilder upload() {
        return new DefaultUploadBuilder();
    }

    /**
     * Default implementation of the upload builder.
     */
    private class DefaultUploadBuilder implements UploadBuilder {
        private String name;
        private ResourceType type = ResourceType.TEXTURE;
        private byte[] data;

        @Override
        public UploadBuilder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public UploadBuilder type(ResourceType type) {
            this.type = type;
            return this;
        }

        @Override
        public UploadBuilder data(byte[] data) {
            this.data = data;
            return this;
        }

        @Override
        public Resource execute() {
            if (name == null || name.isBlank()) {
                throw new IllegalStateException("Resource name is required");
            }
            if (data == null) {
                throw new IllegalStateException("Resource data is required");
            }

            // Create and save the resource
            Resource resource = new OnDiskResourceManager.StoredResource(
                    0,  // ID will be assigned by manager
                    name,
                    "",  // Path will be set by manager
                    data,
                    type
            );
            long resourceId = resourceManager.saveResource(resource);

            // Return the saved resource with the assigned ID
            return resourceManager.getResource(resourceId).orElseThrow(
                    () -> new IllegalStateException("Failed to retrieve saved resource: " + resourceId)
            );
        }
    }
}
