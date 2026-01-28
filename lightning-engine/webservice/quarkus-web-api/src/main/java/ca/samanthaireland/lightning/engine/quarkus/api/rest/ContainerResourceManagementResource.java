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

import ca.samanthaireland.lightning.engine.core.container.ContainerManager;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.core.resources.ResourceType;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static ca.samanthaireland.lightning.engine.quarkus.api.rest.ContainerResourceSupport.getContainerOrThrow;

/**
 * REST resource for resource management within a container.
 *
 * <p>Handles listing, retrieving, uploading, and deleting resources (textures, etc.)
 * in a specific container.
 */
@Path("/api/containers/{containerId}/resources")
@Produces(MediaType.APPLICATION_JSON)
public class ContainerResourceManagementResource {
    private static final Logger log = LoggerFactory.getLogger(ContainerResourceManagementResource.class);

    @Inject
    ContainerManager containerManager;

    /**
     * Get all resources in a container.
     */
    @GET
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getContainerResources(@PathParam("containerId") long containerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        var resourceOps = container.resources();
        if (resourceOps == null) {
            return Response.ok(List.of()).build();
        }
        List<Map<String, Object>> resources = resourceOps.all().stream()
                .map(r -> Map.<String, Object>of(
                        "resourceId", r.resourceId(),
                        "resourceName", r.resourceName(),
                        "resourceType", r.resourceType().name()
                ))
                .toList();
        return Response.ok(resources).build();
    }

    /**
     * Get a specific resource from a container.
     */
    @GET
    @Path("/{resourceId}")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getContainerResource(
            @PathParam("containerId") long containerId,
            @PathParam("resourceId") long resourceId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        var resourceOps = container.resources();
        if (resourceOps == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Resource management not available for this container"))
                    .build();
        }
        return resourceOps.get(resourceId)
                .map(r -> Response.ok(Map.of(
                        "resourceId", r.resourceId(),
                        "resourceName", r.resourceName(),
                        "resourceType", r.resourceType().name()
                )).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Resource not found: " + resourceId))
                        .build());
    }

    /**
     * Delete a resource from a container.
     */
    @DELETE
    @Path("/{resourceId}")
    @RolesAllowed("admin")
    public Response deleteContainerResource(
            @PathParam("containerId") long containerId,
            @PathParam("resourceId") long resourceId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        var resourceOps = container.resources();
        if (resourceOps == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Resource management not available for this container"))
                    .build();
        }
        if (resourceOps.delete(resourceId)) {
            return Response.noContent().build();
        }
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Resource not found: " + resourceId))
                .build();
    }

    /**
     * Upload a resource to a container.
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed("admin")
    public Response uploadContainerResource(
            @PathParam("containerId") long containerId,
            @RestForm("file") FileUpload file,
            @RestForm("resourceName") String resourceName,
            @RestForm("resourceType") String resourceType) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        var resourceOps = container.resources();
        if (resourceOps == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Resource management not available for this container"))
                    .build();
        }
        try {
            byte[] data = Files.readAllBytes(file.uploadedFile());
            ResourceType type = ResourceType.valueOf(resourceType);
            var resource = resourceOps.upload()
                    .name(resourceName)
                    .type(type)
                    .data(data)
                    .execute();
            return Response.status(Response.Status.CREATED)
                    .entity(Map.of(
                            "resourceId", resource.resourceId(),
                            "resourceName", resource.resourceName(),
                            "resourceType", resource.resourceType().name()
                    ))
                    .build();
        } catch (IOException e) {
            log.error("Failed to upload resource to container {}", containerId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to upload resource: " + e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid resource type: " + resourceType))
                    .build();
        }
    }
}
