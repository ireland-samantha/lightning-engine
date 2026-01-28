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

package ca.samanthaireland.lightning.engine.quarkus.api.rest;

import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;
import ca.samanthaireland.game.backend.installation.AIFactory;
import ca.samanthaireland.lightning.engine.internal.ext.module.ModuleManager;
import ca.samanthaireland.lightning.engine.internal.ext.ai.AIManager;
import ca.samanthaireland.lightning.engine.internal.container.InMemoryExecutionContainer;
import ca.samanthaireland.lightning.engine.core.container.ContainerConfig;
import ca.samanthaireland.lightning.engine.core.container.ContainerManager;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.ContainerRequest;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.ContainerResponse;
import ca.samanthaireland.lightning.engine.quarkus.api.persistence.ContainerSnapshotPersistenceListener;
import ca.samanthaireland.lightning.engine.quarkus.api.persistence.SnapshotPersistenceConfig;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static ca.samanthaireland.lightning.engine.quarkus.api.rest.ContainerResourceSupport.getContainerOrThrow;

/**
 * REST resource for container CRUD and lifecycle operations.
 *
 * <p>Handles container creation, retrieval, deletion, and lifecycle
 * state changes (start, stop, pause, resume).
 */
@Path("/api/containers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContainerLifecycleResource {
    private static final Logger log = LoggerFactory.getLogger(ContainerLifecycleResource.class);

    @Inject
    ContainerManager containerManager;

    @Inject
    ModuleManager globalModuleManager;

    @Inject
    AIManager globalAIManager;

    @Inject
    SnapshotPersistenceConfig persistenceConfig;

    @Inject
    Instance<com.mongodb.client.MongoClient> mongoClientInstance;

    // =========================================================================
    // CONTAINER CRUD
    // =========================================================================

    /**
     * Create a new execution container.
     */
    @POST
    @RolesAllowed("admin")
    public Response createContainer(ContainerRequest request) {
        log.info("Creating container: {}", request.name());

        ContainerConfig.Builder builder = ContainerConfig.builder(request.name());

        if (request.maxEntities() != null) {
            builder.maxEntities(request.maxEntities());
        }
        if (request.maxComponents() != null) {
            builder.maxComponents(request.maxComponents());
        }
        if (request.maxCommandsPerTick() != null) {
            builder.maxCommandsPerTick(request.maxCommandsPerTick());
        }
        if (request.maxMemoryMb() != null) {
            // Bound memory by JVM max heap
            long jvmMaxMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
            long requestedMb = request.maxMemoryMb();
            if (requestedMb > jvmMaxMb) {
                log.warn("Requested memory {}MB exceeds JVM max {}MB, using JVM max", requestedMb, jvmMaxMb);
                requestedMb = jvmMaxMb;
            }
            builder.maxMemoryMb(requestedMb);
        }
        if (request.moduleJars() != null) {
            builder.moduleJarPaths(request.moduleJars());
        }
        if (request.moduleScanDirectory() != null) {
            builder.moduleScanDirectory(java.nio.file.Path.of(request.moduleScanDirectory()));
        }

        ExecutionContainer container = containerManager.createContainer(builder.build());

        // Start container first so moduleManager is initialized
        boolean hasModulesToInstall = !request.moduleNames().isEmpty() || !request.aiNames().isEmpty();
        if (hasModulesToInstall) {
            container.lifecycle().start();

            // Register container-scoped snapshot persistence listener if enabled
            if (persistenceConfig.enabled()) {
                registerSnapshotPersistenceListener(container);
            }
        }

        // Install selected modules from global pool
        for (String moduleName : request.moduleNames()) {
            ModuleFactory factory = globalModuleManager.getFactory(moduleName);
            if (factory != null) {
                container.modules().install(factory.getClass());
                log.info("Installed module '{}' into container '{}'", moduleName, request.name());
            } else {
                log.warn("Module '{}' not found in global pool, skipping", moduleName);
            }
        }

        // Install selected AI from global pool
        for (String aiName : request.aiNames()) {
            AIFactory factory = globalAIManager.getFactory(aiName);
            if (factory != null) {
                container.ai().install(factory.getClass().getName());
                log.info("Installed AI '{}' into container '{}'", aiName, request.name());
            } else {
                log.warn("AI '{}' not found in global pool, skipping", aiName);
            }
        }

        return Response.status(Response.Status.CREATED)
                .entity(ContainerResponse.from(container))
                .build();
    }

    /**
     * Get all containers.
     */
    @GET
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public List<ContainerResponse> getAllContainers() {
        return containerManager.getAllContainers().stream()
                .map(ContainerResponse::from)
                .toList();
    }

    /**
     * Get a specific container by ID.
     */
    @GET
    @Path("/{containerId}")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getContainer(@PathParam("containerId") long containerId) {
        return containerManager.getContainer(containerId)
                .map(p -> Response.ok(ContainerResponse.from(p)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * Delete a container. The container must be stopped first.
     */
    @DELETE
    @Path("/{containerId}")
    @RolesAllowed("admin")
    public Response deleteContainer(@PathParam("containerId") long containerId) {
        log.info("Deleting container: {}", containerId);
        containerManager.deleteContainer(containerId);
        return Response.noContent().build();
    }

    // =========================================================================
    // CONTAINER LIFECYCLE
    // =========================================================================

    /**
     * Start a container.
     */
    @POST
    @Path("/{containerId}/start")
    @RolesAllowed("admin")
    public Response startContainer(@PathParam("containerId") long containerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        container.lifecycle().start();

        // Register snapshot persistence listener if enabled
        if (persistenceConfig.enabled()) {
            registerSnapshotPersistenceListener(container);
        }

        return Response.ok(ContainerResponse.from(container)).build();
    }

    /**
     * Stop a container.
     */
    @POST
    @Path("/{containerId}/stop")
    @RolesAllowed("admin")
    public Response stopContainer(@PathParam("containerId") long containerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        container.lifecycle().stop();
        return Response.ok(ContainerResponse.from(container)).build();
    }

    /**
     * Pause a running container.
     */
    @POST
    @Path("/{containerId}/pause")
    @RolesAllowed("admin")
    public Response pauseContainer(@PathParam("containerId") long containerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        container.lifecycle().pause();
        return Response.ok(ContainerResponse.from(container)).build();
    }

    /**
     * Resume a paused container.
     */
    @POST
    @Path("/{containerId}/resume")
    @RolesAllowed("admin")
    public Response resumeContainer(@PathParam("containerId") long containerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        container.lifecycle().resume();
        return Response.ok(ContainerResponse.from(container)).build();
    }

    // =========================================================================
    // CONTAINER STATS
    // =========================================================================

    /**
     * Get container statistics including entity counts and memory usage.
     */
    @GET
    @Path("/{containerId}/stats")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getContainerStats(@PathParam("containerId") long containerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        ExecutionContainer.ContainerStats stats = container.getStats();
        return Response.ok(new ContainerStatsResponse(
                stats.entityCount(),
                stats.maxEntities(),
                stats.usedMemoryBytes(),
                stats.maxMemoryBytes(),
                stats.jvmMaxMemoryBytes(),
                stats.jvmUsedMemoryBytes(),
                stats.matchCount(),
                stats.moduleCount()
        )).build();
    }

    /**
     * Container statistics response DTO.
     */
    public record ContainerStatsResponse(
            int entityCount,
            int maxEntities,
            long usedMemoryBytes,
            long maxMemoryBytes,
            long jvmMaxMemoryBytes,
            long jvmUsedMemoryBytes,
            int matchCount,
            int moduleCount
    ) {}

    // =========================================================================
    // HELPERS
    // =========================================================================

    /**
     * Register a container-scoped snapshot persistence listener.
     */
    private void registerSnapshotPersistenceListener(ExecutionContainer container) {
        if (!(container instanceof InMemoryExecutionContainer inMemoryContainer)) {
            log.warn("Cannot register snapshot persistence listener for container {}: not an InMemoryExecutionContainer",
                    container.getId());
            return;
        }

        if (inMemoryContainer.getGameLoop() == null) {
            log.warn("Cannot register snapshot persistence listener for container {}: GameLoop not initialized",
                    container.getId());
            return;
        }

        if (!mongoClientInstance.isResolvable()) {
            log.warn("Cannot register snapshot persistence listener for container {}: MongoClient not available",
                    container.getId());
            return;
        }

        var listener = new ContainerSnapshotPersistenceListener(
                container.getId(),
                container,
                mongoClientInstance.get(),
                persistenceConfig.database(),
                persistenceConfig.collection(),
                persistenceConfig.tickInterval()
        );

        inMemoryContainer.getGameLoop().addTickListener(listener);
        log.info("Registered snapshot persistence listener for container {}", container.getId());
    }
}
