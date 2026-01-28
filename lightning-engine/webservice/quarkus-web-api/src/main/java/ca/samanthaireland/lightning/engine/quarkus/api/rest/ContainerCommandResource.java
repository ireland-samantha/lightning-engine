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
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

import static ca.samanthaireland.lightning.engine.quarkus.api.rest.ContainerResourceSupport.getContainerOrThrow;

/**
 * REST resource for command operations within a container.
 *
 * <p>Handles listing available commands and enqueueing commands for execution.
 */
@Path("/api/containers/{containerId}/commands")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContainerCommandResource {

    @Inject
    ContainerManager containerManager;

    /**
     * Get all available commands in a container.
     * Commands are provided by the modules loaded in this container.
     */
    @GET
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public List<CommandResponse> getCommands(@PathParam("containerId") long containerId) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        return container.getAvailableCommands().stream()
                .map(cmd -> new CommandResponse(
                        cmd.name(),
                        cmd.description(),
                        cmd.module(),
                        cmd.parameters() != null
                                ? cmd.parameters().stream()
                                        .map(p -> new ParameterResponse(p.name(), p.type(), p.required(), p.description()))
                                        .toList()
                                : List.of()
                ))
                .toList();
    }

    /**
     * Enqueue a command in a container.
     */
    @POST
    @RolesAllowed({"admin", "command_manager"})
    public Response enqueueCommand(
            @PathParam("containerId") long containerId,
            CommandRequest request) {
        ExecutionContainer container = getContainerOrThrow(containerManager, containerId);
        container.commands()
                .named(request.commandName())
                .withParams(request.parameters() != null ? request.parameters() : Map.of())
                .execute();
        return Response.accepted().build();
    }

    /**
     * Command response DTO.
     */
    public record CommandResponse(String name, String description, String module, List<ParameterResponse> parameters) {}

    /**
     * Parameter response DTO.
     */
    public record ParameterResponse(String name, String type, boolean required, String description) {}

    /**
     * Command request DTO.
     */
    public record CommandRequest(String commandName, Map<String, Object> parameters) {}
}
