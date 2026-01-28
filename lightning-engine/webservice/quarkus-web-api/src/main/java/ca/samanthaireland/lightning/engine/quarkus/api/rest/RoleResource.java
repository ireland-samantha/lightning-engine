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

import ca.samanthaireland.auth.Role;
import ca.samanthaireland.auth.RoleService;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.RoleRequest;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.RoleResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * REST resource for role management.
 */
@Path("/api/auth/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoleResource {

    @Inject
    RoleService roleService;

    @POST
    @RolesAllowed("admin")
    public Response createRole(RoleRequest request) {
        Role role = roleService.createRole(
                request.name(),
                request.description(),
                request.includedRoles() != null ? request.includedRoles() : java.util.Set.of()
        );
        return Response.status(Response.Status.CREATED)
                .entity(toResponse(role))
                .build();
    }

    @GET
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public List<RoleResponse> getAllRoles() {
        return roleService.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @GET
    @Path("/{roleId}")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getRole(@PathParam("roleId") long roleId) {
        return roleService.findById(roleId)
                .map(role -> Response.ok(toResponse(role)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/name/{roleName}")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getRoleByName(@PathParam("roleName") String roleName) {
        return roleService.findByName(roleName)
                .map(role -> Response.ok(toResponse(role)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @PUT
    @Path("/{roleId}/description")
    @RolesAllowed("admin")
    public Response updateDescription(@PathParam("roleId") long roleId, String description) {
        Role role = roleService.updateDescription(roleId, description);
        return Response.ok(toResponse(role)).build();
    }

    @PUT
    @Path("/{roleId}/includes")
    @RolesAllowed("admin")
    public Response updateIncludedRoles(@PathParam("roleId") long roleId, java.util.Set<String> includedRoles) {
        Role role = roleService.updateIncludedRoles(roleId, includedRoles);
        return Response.ok(toResponse(role)).build();
    }

    @DELETE
    @Path("/{roleId}")
    @RolesAllowed("admin")
    public Response deleteRole(@PathParam("roleId") long roleId) {
        if (roleService.deleteRole(roleId)) {
            return Response.noContent().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.id(),
                role.name(),
                role.description(),
                role.includedRoles()
        );
    }
}
