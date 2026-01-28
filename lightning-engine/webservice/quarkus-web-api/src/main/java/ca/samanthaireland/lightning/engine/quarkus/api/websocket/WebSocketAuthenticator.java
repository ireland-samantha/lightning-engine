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

package ca.samanthaireland.lightning.engine.quarkus.api.websocket;

import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;

/**
 * Handles JWT authentication for WebSocket connections.
 *
 * <p>Supports two authentication methods:
 * <ul>
 *   <li>Subprotocol: Pass token via Sec-WebSocket-Protocol header as "Bearer.{token}"</li>
 *   <li>Query parameter: Pass token via ?token={token} (legacy, less secure)</li>
 * </ul>
 *
 * <p>This class provides shared authentication logic for all WebSocket endpoints:
 * <ul>
 *   <li>Command WebSocket - requires admin or command_manager role</li>
 *   <li>Snapshot WebSocket - allows admin, command_manager, or view_only role</li>
 *   <li>Delta WebSocket - allows admin, command_manager, or view_only role</li>
 * </ul>
 */
@ApplicationScoped
public class WebSocketAuthenticator {
    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthenticator.class);
    private static final String BEARER_SUBPROTOCOL_PREFIX = "Bearer.";

    /**
     * Roles allowed for command-related operations (write access).
     */
    public static final Set<String> COMMAND_ROLES = Set.of("admin", "command_manager");

    /**
     * Roles allowed for read-only operations (snapshots, viewing).
     */
    public static final Set<String> VIEW_ROLES = Set.of("admin", "command_manager", "view_only");

    @Inject
    JWTParser jwtParser;

    /**
     * Result of authentication attempt.
     */
    public sealed interface AuthResult {
        record Success(String subject, Set<String> roles) implements AuthResult {}
        record Failure(String message) implements AuthResult {}
    }

    /**
     * Authenticate a WebSocket connection using JWT token with default command roles.
     *
     * @param connection the WebSocket connection
     * @return authentication result
     */
    public AuthResult authenticate(WebSocketConnection connection) {
        return authenticate(connection, COMMAND_ROLES);
    }

    /**
     * Authenticate a WebSocket connection using JWT token with custom allowed roles.
     *
     * @param connection the WebSocket connection
     * @param allowedRoles the set of roles that are allowed access
     * @return authentication result
     */
    public AuthResult authenticate(WebSocketConnection connection, Set<String> allowedRoles) {
        // Try subprotocol authentication first (more secure - not logged in URLs)
        String token = extractTokenFromSubprotocol(connection);

        // Fall back to query parameter (legacy support)
        if (token == null || token.isBlank()) {
            String query = connection.handshakeRequest().query();
            token = extractTokenFromQuery(query);
        }

        if (token == null || token.isBlank()) {
            log.warn("Missing token in WebSocket connection");
            return new AuthResult.Failure("Authentication required: provide token via subprotocol or query parameter");
        }

        try {
            JsonWebToken jwt = jwtParser.parse(token);
            Set<String> groups = jwt.getGroups();

            boolean hasRequiredRole = groups.stream().anyMatch(allowedRoles::contains);
            if (!hasRequiredRole) {
                log.warn("User '{}' lacks required role (needs one of {})", jwt.getSubject(), allowedRoles);
                return new AuthResult.Failure("Authorization failed: requires one of " + allowedRoles);
            }

            log.debug("Authenticated user '{}' with roles {}", jwt.getSubject(), groups);
            return new AuthResult.Success(jwt.getSubject(), groups);

        } catch (ParseException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return new AuthResult.Failure("Authentication failed: invalid token");
        }
    }

    /**
     * Extract JWT token from WebSocket subprotocol header.
     * Format: Sec-WebSocket-Protocol: Bearer.{jwt_token}
     */
    private String extractTokenFromSubprotocol(WebSocketConnection connection) {
        String subprotocol = connection.handshakeRequest().header("Sec-WebSocket-Protocol");
        if (subprotocol != null && subprotocol.startsWith(BEARER_SUBPROTOCOL_PREFIX)) {
            return subprotocol.substring(BEARER_SUBPROTOCOL_PREFIX.length());
        }
        return null;
    }

    /**
     * Extract JWT token from query parameter.
     */
    private String extractTokenFromQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String param : query.split("&")) {
            String[] keyValue = param.split("=", 2);
            if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        return null;
    }

    /**
     * Get the allowed roles for command WebSocket access.
     */
    public Set<String> getCommandRoles() {
        return COMMAND_ROLES;
    }

    /**
     * Get the allowed roles for view-only WebSocket access.
     */
    public Set<String> getViewRoles() {
        return VIEW_ROLES;
    }
}
