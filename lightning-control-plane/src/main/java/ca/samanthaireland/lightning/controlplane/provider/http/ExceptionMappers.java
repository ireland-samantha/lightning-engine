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

package ca.samanthaireland.lightning.controlplane.provider.http;

import ca.samanthaireland.lightning.controlplane.match.exception.MatchNotFoundException;
import ca.samanthaireland.lightning.controlplane.module.exception.ModuleDistributionException;
import ca.samanthaireland.lightning.controlplane.module.exception.ModuleNotFoundException;
import ca.samanthaireland.lightning.controlplane.node.exception.NodeAuthenticationException;
import ca.samanthaireland.lightning.controlplane.node.exception.NodeNotFoundException;
import ca.samanthaireland.lightning.controlplane.scheduler.exception.NoAvailableNodesException;
import ca.samanthaireland.lightning.controlplane.scheduler.exception.NoCapableNodesException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;

/**
 * Exception mappers for the control plane REST API.
 */
public class ExceptionMappers {

    /**
     * Error response DTO.
     */
    public record ErrorResponse(String error, String message, Instant timestamp) {
    }

    /**
     * Maps NodeNotFoundException to HTTP 404.
     */
    @Provider
    public static class NodeNotFoundExceptionMapper implements ExceptionMapper<NodeNotFoundException> {
        @Override
        public Response toResponse(NodeNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NODE_NOT_FOUND", e.getMessage(), Instant.now()))
                    .build();
        }
    }

    /**
     * Maps NodeAuthenticationException to HTTP 401.
     */
    @Provider
    public static class NodeAuthenticationExceptionMapper implements ExceptionMapper<NodeAuthenticationException> {
        @Override
        public Response toResponse(NodeAuthenticationException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("AUTHENTICATION_FAILED", e.getMessage(), Instant.now()))
                    .build();
        }
    }

    /**
     * Maps MatchNotFoundException to HTTP 404.
     */
    @Provider
    public static class MatchNotFoundExceptionMapper implements ExceptionMapper<MatchNotFoundException> {
        @Override
        public Response toResponse(MatchNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("MATCH_NOT_FOUND", e.getMessage(), Instant.now()))
                    .build();
        }
    }

    /**
     * Maps NoAvailableNodesException to HTTP 503 Service Unavailable.
     */
    @Provider
    public static class NoAvailableNodesExceptionMapper implements ExceptionMapper<NoAvailableNodesException> {
        @Override
        public Response toResponse(NoAvailableNodesException e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ErrorResponse("NO_AVAILABLE_NODES", e.getMessage(), Instant.now()))
                    .build();
        }
    }

    /**
     * Maps NoCapableNodesException to HTTP 503 Service Unavailable.
     */
    @Provider
    public static class NoCapableNodesExceptionMapper implements ExceptionMapper<NoCapableNodesException> {
        @Override
        public Response toResponse(NoCapableNodesException e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ErrorResponse("NO_CAPABLE_NODES", e.getMessage(), Instant.now()))
                    .build();
        }
    }

    /**
     * Maps ModuleNotFoundException to HTTP 404.
     */
    @Provider
    public static class ModuleNotFoundExceptionMapper implements ExceptionMapper<ModuleNotFoundException> {
        @Override
        public Response toResponse(ModuleNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("MODULE_NOT_FOUND", e.getMessage(), Instant.now()))
                    .build();
        }
    }

    /**
     * Maps ModuleDistributionException to HTTP 502 Bad Gateway.
     */
    @Provider
    public static class ModuleDistributionExceptionMapper implements ExceptionMapper<ModuleDistributionException> {
        @Override
        public Response toResponse(ModuleDistributionException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new ErrorResponse("MODULE_DISTRIBUTION_FAILED", e.getMessage(), Instant.now()))
                    .build();
        }
    }
}
