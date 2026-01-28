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

import ca.samanthaireland.auth.*;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.LoginRequest;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.TokenResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * REST resource for authentication operations.
 */
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @Inject
    UserRepository userRepository;

    @POST
    @Path("/login")
    @PermitAll
    public Response login(LoginRequest request) {
        try {
            AuthToken token = authService.login(request.username(), request.password());;
            return Response.ok(toResponse(token)).build();
        } catch (AuthException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/refresh")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response refresh(@HeaderParam("Authorization") String authHeader) {
        try {
            // Extract the token from "Bearer <token>" header
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("Missing or invalid Authorization header"))
                        .build();
            }
            String token = authHeader.substring(7);

            // Use AuthService to refresh the token
            AuthToken newToken = authService.refreshToken(token);
            return Response.ok(toResponse(newToken)).build();
        } catch (AuthException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/me")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getCurrentUser(@Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal().getName();
        User user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("User not found"))
                    .build();
        }

        // Return user info without re-issuing token
        AuthToken authToken = new AuthToken(
                user.id(),
                user.username(),
                user.roles(),
                null,  // expiry from current token not needed here
                null   // don't return token
        );
        return Response.ok(toResponse(authToken)).build();
    }

    private TokenResponse toResponse(AuthToken token) {
        return new TokenResponse(
                token.jwtToken(),
                token.username(),
                token.roles(),
                token.expiresAt()
        );
    }

    public record ErrorResponse(String error) {}
}
