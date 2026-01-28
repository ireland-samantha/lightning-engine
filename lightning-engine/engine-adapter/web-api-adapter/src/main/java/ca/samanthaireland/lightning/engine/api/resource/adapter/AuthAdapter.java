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


package ca.samanthaireland.lightning.engine.api.resource.adapter;

import ca.samanthaireland.lightning.engine.api.resource.adapter.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Set;

/**
 * REST API adapter for authentication operations.
 *
 * <p>Provides methods for login, token refresh, and current user retrieval.
 * This adapter follows the Single Responsibility Principle - it only handles
 * authentication-related operations.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * AuthAdapter auth = new AuthAdapter.HttpAuthAdapter("http://localhost:8080");
 *
 * // Login
 * AuthToken token = auth.login("admin", "password");
 *
 * // Refresh token (requires authenticated adapter)
 * AuthAdapter authWithToken = new AuthAdapter.HttpAuthAdapter(
 *     "http://localhost:8080",
 *     AdapterConfig.defaults().withBearerToken(token.token())
 * );
 * AuthToken newToken = authWithToken.refresh();
 * }</pre>
 */
public interface AuthAdapter {

    /**
     * Authenticate with username and password.
     *
     * @param username the username
     * @param password the password
     * @return authentication token with JWT and user details
     * @throws IOException if authentication fails or network error occurs
     * @throws AuthenticationException if credentials are invalid
     */
    AuthToken login(String username, String password) throws IOException;

    /**
     * Refresh the current JWT token.
     *
     * <p>Requires an authenticated adapter (configured with a bearer token).</p>
     *
     * @return new authentication token
     * @throws IOException if refresh fails or network error occurs
     */
    AuthToken refresh() throws IOException;

    /**
     * Get the currently authenticated user.
     *
     * <p>Requires an authenticated adapter (configured with a bearer token).</p>
     *
     * @return current user information
     * @throws IOException if request fails or network error occurs
     */
    CurrentUser getCurrentUser() throws IOException;

    /**
     * Authentication token response.
     *
     * @param token the JWT token string
     * @param username the authenticated username
     * @param roles the user's roles
     * @param expiresAt token expiration time (may be null)
     */
    record AuthToken(String token, String username, Set<String> roles, Instant expiresAt) {}

    /**
     * Current user information.
     *
     * @param username the username
     * @param roles the user's roles
     */
    record CurrentUser(String username, Set<String> roles) {}

    /**
     * Exception thrown when authentication fails due to invalid credentials.
     */
    class AuthenticationException extends IOException {
        public AuthenticationException(String message) {
            super(message);
        }
    }

    /**
     * HTTP-based implementation of AuthAdapter.
     */
    class HttpAuthAdapter implements AuthAdapter {
        private final HttpClient httpClient;
        private final String baseUrl;
        private final AdapterConfig config;

        public HttpAuthAdapter(String baseUrl) {
            this(baseUrl, AdapterConfig.defaults());
        }

        public HttpAuthAdapter(String baseUrl, AdapterConfig config) {
            this.baseUrl = normalizeUrl(baseUrl);
            this.config = config;
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(config.getConnectTimeout())
                    .build();
        }

        public HttpAuthAdapter(String baseUrl, HttpClient httpClient) {
            this.baseUrl = normalizeUrl(baseUrl);
            this.config = AdapterConfig.defaults();
            this.httpClient = httpClient;
        }

        private static String normalizeUrl(String url) {
            return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        }

        private HttpRequest.Builder requestBuilder(String path) {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path));
            if (config.hasAuthentication()) {
                builder.header("Authorization", "Bearer " + config.getBearerToken());
            }
            return builder;
        }

        @Override
        public AuthToken login(String username, String password) throws IOException {
            String json = String.format("{\"username\":\"%s\",\"password\":\"%s\"}",
                    escapeJson(username), escapeJson(password));

            HttpRequest request = requestBuilder("/api/auth/login")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return parseAuthToken(response.body());
                } else if (response.statusCode() == 401) {
                    throw new AuthenticationException("Invalid username or password");
                }
                throw new IOException("Login failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public AuthToken refresh() throws IOException {
            if (!config.hasAuthentication()) {
                throw new IOException("Bearer token required for refresh");
            }

            HttpRequest request = requestBuilder("/api/auth/refresh")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return parseAuthToken(response.body());
                } else if (response.statusCode() == 401) {
                    throw new AuthenticationException("Token refresh failed - token may be expired");
                }
                throw new IOException("Token refresh failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public CurrentUser getCurrentUser() throws IOException {
            if (!config.hasAuthentication()) {
                throw new IOException("Bearer token required to get current user");
            }

            HttpRequest request = requestBuilder("/api/auth/me")
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    AuthToken token = parseAuthToken(response.body());
                    return new CurrentUser(token.username(), token.roles());
                } else if (response.statusCode() == 401) {
                    throw new AuthenticationException("Not authenticated");
                }
                throw new IOException("Get current user failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        private AuthToken parseAuthToken(String json) {
            String token = JsonMapper.extractString(json, "token");
            String username = JsonMapper.extractString(json, "username");
            Set<String> roles = JsonMapper.extractStringSet(json, "roles");
            String expiresAtStr = JsonMapper.extractString(json, "expiresAt");
            Instant expiresAt = expiresAtStr != null ? Instant.parse(expiresAtStr) : null;
            return new AuthToken(token, username, roles, expiresAt);
        }

        private String escapeJson(String value) {
            return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }
}
