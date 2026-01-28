/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.quarkus.api.rest;

import ca.samanthaireland.lightning.engine.core.container.ContainerManager;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.internal.CommandExecutionMetrics;
import ca.samanthaireland.lightning.engine.internal.GameLoop;
import ca.samanthaireland.lightning.engine.internal.SystemExecutionMetrics;
import ca.samanthaireland.lightning.engine.internal.container.InMemoryExecutionContainer;
import ca.samanthaireland.lightning.engine.internal.core.command.InMemoryCommandQueueManager;
import ca.samanthaireland.lightning.engine.internal.core.snapshot.CachingSnapshotProvider;
import ca.samanthaireland.lightning.engine.internal.core.snapshot.SnapshotMetrics;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static ca.samanthaireland.lightning.engine.quarkus.api.rest.ContainerResourceSupport.getContainerOrThrow;

/**
 * REST resource for container metrics including tick timing.
 */
@Path("/api/containers/{containerId}/metrics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContainerMetricsResource {

    @Inject
    ContainerManager containerManager;

    /**
     * Get tick timing metrics for a container.
     */
    @GET
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getMetrics(@PathParam("containerId") long containerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        if (!(container instanceof InMemoryExecutionContainer inMemoryContainer)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Metrics not available for this container type"))
                    .build();
        }

        GameLoop gameLoop = inMemoryContainer.getGameLoop();
        if (gameLoop == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Container game loop not initialized"))
                    .build();
        }

        GameLoop.TickMetrics tickMetrics = gameLoop.getTickMetrics();

        // Get snapshot metrics if available
        SnapshotMetricsResponse snapshotMetricsResponse = null;
        CachingSnapshotProvider cachingSnapshotProvider = inMemoryContainer.getCachingSnapshotProvider();
        if (cachingSnapshotProvider != null) {
            SnapshotMetrics snapshotMetrics = cachingSnapshotProvider.getMetrics();
            snapshotMetricsResponse = new SnapshotMetricsResponse(
                    snapshotMetrics.totalGenerations(),
                    snapshotMetrics.cacheHits(),
                    snapshotMetrics.cacheMisses(),
                    snapshotMetrics.incrementalUpdates(),
                    snapshotMetrics.fullRebuilds(),
                    snapshotMetrics.avgGenerationMs(),
                    snapshotMetrics.lastGenerationMs(),
                    snapshotMetrics.maxGenerationMs(),
                    snapshotMetrics.cacheHitRate(),
                    snapshotMetrics.incrementalRate()
            );
        }

        // Get per-system metrics from last tick
        List<SystemMetricsResponse> systemMetrics = gameLoop.getLastTickSystemMetrics().stream()
                .map(m -> new SystemMetricsResponse(m.systemName(), m.executionTimeMs(), m.executionTimeNanos(), m.success()))
                .toList();

        // Get per-command metrics from last tick
        List<CommandMetricsResponse> commandMetrics = gameLoop.getLastTickCommandMetrics().stream()
                .map(m -> new CommandMetricsResponse(m.commandName(), m.executionTimeMs(), m.executionTimeNanos(), m.success()))
                .toList();

        // Get entity and component counts
        EntityComponentStore entityStore = inMemoryContainer.getEntityStore();
        int totalEntities = entityStore != null ? entityStore.getEntityCount() : 0;
        int totalComponentTypes = entityStore != null ? entityStore.getComponentTypeCount() : 0;

        // Get command queue size
        InMemoryCommandQueueManager commandQueueManager = inMemoryContainer.getCommandQueueManager();
        int commandQueueSize = commandQueueManager != null ? commandQueueManager.getQueueSize() : 0;

        return Response.ok(new MetricsResponse(
                containerId,
                container.ticks().current(),
                tickMetrics.lastTickMs(),
                tickMetrics.avgTickMs(),
                tickMetrics.minTickMs(),
                tickMetrics.maxTickMs(),
                tickMetrics.totalTicks(),
                tickMetrics.lastTickNanos(),
                tickMetrics.avgTickNanos(),
                tickMetrics.minTickNanos(),
                tickMetrics.maxTickNanos(),
                totalEntities,
                totalComponentTypes,
                commandQueueSize,
                snapshotMetricsResponse,
                systemMetrics,
                commandMetrics
        )).build();
    }

    /**
     * Reset tick timing metrics for a container.
     */
    @POST
    @Path("/reset")
    @RolesAllowed("admin")
    public Response resetMetrics(@PathParam("containerId") long containerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);

        if (!(container instanceof InMemoryExecutionContainer inMemoryContainer)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Metrics not available for this container type"))
                    .build();
        }

        GameLoop gameLoop = inMemoryContainer.getGameLoop();
        if (gameLoop == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Container game loop not initialized"))
                    .build();
        }

        gameLoop.resetTickMetrics();

        // Reset snapshot metrics if available
        CachingSnapshotProvider cachingSnapshotProvider = inMemoryContainer.getCachingSnapshotProvider();
        if (cachingSnapshotProvider != null) {
            cachingSnapshotProvider.resetMetrics();
        }

        return Response.ok(new MessageResponse("Metrics reset")).build();
    }

    /**
     * Metrics response DTO.
     */
    public record MetricsResponse(
            long containerId,
            long currentTick,
            double lastTickMs,
            double avgTickMs,
            double minTickMs,
            double maxTickMs,
            long totalTicks,
            long lastTickNanos,
            long avgTickNanos,
            long minTickNanos,
            long maxTickNanos,
            int totalEntities,
            int totalComponentTypes,
            int commandQueueSize,
            SnapshotMetricsResponse snapshotMetrics,
            List<SystemMetricsResponse> lastTickSystems,
            List<CommandMetricsResponse> lastTickCommands
    ) {}

    /**
     * Snapshot metrics response DTO.
     */
    public record SnapshotMetricsResponse(
            long totalGenerations,
            long cacheHits,
            long cacheMisses,
            long incrementalUpdates,
            long fullRebuilds,
            double avgGenerationMs,
            double lastGenerationMs,
            double maxGenerationMs,
            double cacheHitRate,
            double incrementalRate
    ) {}

    /**
     * System execution metrics response DTO.
     */
    public record SystemMetricsResponse(
            String systemName,
            double executionTimeMs,
            long executionTimeNanos,
            boolean success
    ) {}

    /**
     * Command execution metrics response DTO.
     */
    public record CommandMetricsResponse(
            String commandName,
            double executionTimeMs,
            long executionTimeNanos,
            boolean success
    ) {}

    /**
     * Error response DTO.
     */
    public record ErrorResponse(String error) {}

    /**
     * Message response DTO.
     */
    public record MessageResponse(String message) {}
}
