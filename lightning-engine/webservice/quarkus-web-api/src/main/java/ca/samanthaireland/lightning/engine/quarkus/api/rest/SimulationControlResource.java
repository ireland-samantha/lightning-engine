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
import ca.samanthaireland.lightning.engine.quarkus.api.dto.ContainerResponse;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.PlayStatusResponse;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.TickResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static ca.samanthaireland.lightning.engine.quarkus.api.rest.ContainerResourceSupport.getContainerOrThrow;

/**
 * REST resource for simulation tick control.
 *
 * <p>Handles manual tick advancement, auto-play control, and simulation status.
 */
@Path("/api/containers/{containerId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SimulationControlResource {

    @Inject
    ContainerManager containerManager;

    /**
     * Get the current tick of a container.
     */
    @GET
    @Path("/tick")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public TickResponse getTick(@PathParam("containerId") long containerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        return new TickResponse(container.ticks().current());
    }

    /**
     * Advance the container by one tick.
     */
    @POST
    @Path("/tick")
    @RolesAllowed({"admin", "command_manager"})
    public TickResponse advanceTick(@PathParam("containerId") long containerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        container.ticks().advance();
        return new TickResponse(container.ticks().current());
    }

    /**
     * Start auto-advancing the container at the specified interval.
     */
    @POST
    @Path("/play")
    @RolesAllowed({"admin", "command_manager"})
    public Response startAutoAdvance(
            @PathParam("containerId") long containerId,
            @QueryParam("intervalMs") @DefaultValue("16") long intervalMs) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        container.ticks().play(intervalMs);
        return Response.ok(ContainerResponse.from(container)).build();
    }

    /**
     * Stop auto-advancing the container.
     */
    @POST
    @Path("/stop-auto")
    @RolesAllowed({"admin", "command_manager"})
    public Response stopAutoAdvance(@PathParam("containerId") long containerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        container.ticks().stop();
        return Response.ok(ContainerResponse.from(container)).build();
    }

    /**
     * Get the current play status of a container.
     */
    @GET
    @Path("/status")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public PlayStatusResponse getStatus(@PathParam("containerId") long containerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        return new PlayStatusResponse(
                container.ticks().isPlaying(),
                container.ticks().current(),
                container.ticks().interval()
        );
    }
}
