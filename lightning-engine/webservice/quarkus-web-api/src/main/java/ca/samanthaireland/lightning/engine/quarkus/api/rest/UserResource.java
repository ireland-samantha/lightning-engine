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

import ca.samanthaireland.auth.User;
import ca.samanthaireland.auth.UserService;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.UserRequest;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.UserResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Set;

/**
 * REST resource for user management.
 */
@Path("/api/auth/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserService userService;

    @POST
    @RolesAllowed("admin")
    public Response createUser(UserRequest request) {
        User user = userService.createUser(
                request.username(),
                request.password(),
                request.roles() != null ? request.roles() : Set.of("view_only")
        );
        return Response.status(Response.Status.CREATED)
                .entity(toResponse(user))
                .build();
    }

    @GET
    @RolesAllowed("admin")
    public List<UserResponse> getAllUsers() {
        return userService.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @GET
    @Path("/{userId}")
    @RolesAllowed("admin")
    public Response getUser(@PathParam("userId") long userId) {
        return userService.findById(userId)
                .map(user -> Response.ok(toResponse(user)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/username/{username}")
    @RolesAllowed("admin")
    public Response getUserByUsername(@PathParam("username") String username) {
        return userService.findByUsername(username)
                .map(user -> Response.ok(toResponse(user)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @PUT
    @Path("/{userId}/password")
    @RolesAllowed("admin")
    public Response updatePassword(@PathParam("userId") long userId, String newPassword) {
        User user = userService.updatePassword(userId, newPassword);
        return Response.ok(toResponse(user)).build();
    }

    @PUT
    @Path("/{userId}/roles")
    @RolesAllowed("admin")
    public Response updateRoles(@PathParam("userId") long userId, Set<String> roles) {
        User user = userService.updateRoles(userId, roles);
        return Response.ok(toResponse(user)).build();
    }

    @POST
    @Path("/{userId}/roles/{roleName}")
    @RolesAllowed("admin")
    public Response addRole(@PathParam("userId") long userId, @PathParam("roleName") String roleName) {
        User user = userService.addRole(userId, roleName);
        return Response.ok(toResponse(user)).build();
    }

    @DELETE
    @Path("/{userId}/roles/{roleName}")
    @RolesAllowed("admin")
    public Response removeRole(@PathParam("userId") long userId, @PathParam("roleName") String roleName) {
        User user = userService.removeRole(userId, roleName);
        return Response.ok(toResponse(user)).build();
    }

    @PUT
    @Path("/{userId}/enabled")
    @RolesAllowed("admin")
    public Response setEnabled(@PathParam("userId") long userId, boolean enabled) {
        User user = userService.setEnabled(userId, enabled);
        return Response.ok(toResponse(user)).build();
    }

    @DELETE
    @Path("/{userId}")
    @RolesAllowed("admin")
    public Response deleteUser(@PathParam("userId") long userId) {
        if (userService.deleteUser(userId)) {
            return Response.noContent().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.id(),
                user.username(),
                user.roles(),
                user.createdAt(),
                user.enabled()
        );
    }
}
