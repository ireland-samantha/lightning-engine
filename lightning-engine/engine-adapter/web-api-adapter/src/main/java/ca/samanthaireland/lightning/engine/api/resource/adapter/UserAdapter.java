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
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * REST API adapter for user management operations.
 *
 * <p>Provides CRUD operations for users including role management.
 * This adapter follows the Single Responsibility Principle - it only handles
 * user-related operations. Requires admin role for all operations.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * UserAdapter users = new UserAdapter.HttpUserAdapter(
 *     "http://localhost:8080",
 *     AdapterConfig.defaults().withBearerToken(adminToken)
 * );
 *
 * // Create user
 * UserResponse user = users.createUser("newuser", "password123", Set.of("view_only"));
 *
 * // Add role
 * users.addRole(user.id(), "command_manager");
 *
 * // List all users
 * List<UserResponse> allUsers = users.getAllUsers();
 * }</pre>
 */
public interface UserAdapter {

    /**
     * Create a new user.
     *
     * @param username the username
     * @param password the password
     * @param roles the initial roles (defaults to "view_only" if null)
     * @return the created user
     * @throws IOException if creation fails
     */
    UserResponse createUser(String username, String password, Set<String> roles) throws IOException;

    /**
     * Get all users.
     *
     * @return list of all users
     * @throws IOException if request fails
     */
    List<UserResponse> getAllUsers() throws IOException;

    /**
     * Get a user by ID.
     *
     * @param userId the user ID
     * @return the user if found
     * @throws IOException if request fails
     */
    Optional<UserResponse> getUser(long userId) throws IOException;

    /**
     * Get a user by username.
     *
     * @param username the username
     * @return the user if found
     * @throws IOException if request fails
     */
    Optional<UserResponse> getUserByUsername(String username) throws IOException;

    /**
     * Update a user's password.
     *
     * @param userId the user ID
     * @param newPassword the new password
     * @return the updated user
     * @throws IOException if update fails
     */
    UserResponse updatePassword(long userId, String newPassword) throws IOException;

    /**
     * Replace all roles for a user.
     *
     * @param userId the user ID
     * @param roles the new roles
     * @return the updated user
     * @throws IOException if update fails
     */
    UserResponse updateRoles(long userId, Set<String> roles) throws IOException;

    /**
     * Add a role to a user.
     *
     * @param userId the user ID
     * @param roleName the role to add
     * @return the updated user
     * @throws IOException if update fails
     */
    UserResponse addRole(long userId, String roleName) throws IOException;

    /**
     * Remove a role from a user.
     *
     * @param userId the user ID
     * @param roleName the role to remove
     * @return the updated user
     * @throws IOException if update fails
     */
    UserResponse removeRole(long userId, String roleName) throws IOException;

    /**
     * Enable or disable a user.
     *
     * @param userId the user ID
     * @param enabled true to enable, false to disable
     * @return the updated user
     * @throws IOException if update fails
     */
    UserResponse setEnabled(long userId, boolean enabled) throws IOException;

    /**
     * Delete a user.
     *
     * @param userId the user ID
     * @return true if deleted
     * @throws IOException if deletion fails
     */
    boolean deleteUser(long userId) throws IOException;

    /**
     * User response DTO.
     *
     * @param id the user ID
     * @param username the username
     * @param roles the user's roles
     * @param createdAt when the user was created
     * @param enabled whether the user is enabled
     */
    record UserResponse(long id, String username, Set<String> roles, Instant createdAt, boolean enabled) {}

    /**
     * HTTP-based implementation of UserAdapter.
     */
    class HttpUserAdapter implements UserAdapter {
        private final HttpClient httpClient;
        private final String baseUrl;
        private final AdapterConfig config;

        public HttpUserAdapter(String baseUrl, AdapterConfig config) {
            this.baseUrl = normalizeUrl(baseUrl);
            this.config = config;
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(config.getConnectTimeout())
                    .build();
        }

        public HttpUserAdapter(String baseUrl, HttpClient httpClient) {
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
        public UserResponse createUser(String username, String password, Set<String> roles) throws IOException {
            StringBuilder json = new StringBuilder();
            json.append("{\"username\":\"").append(escapeJson(username))
                .append("\",\"password\":\"").append(escapeJson(password)).append("\"");
            if (roles != null && !roles.isEmpty()) {
                json.append(",\"roles\":[");
                json.append(String.join(",", roles.stream().map(r -> "\"" + r + "\"").toList()));
                json.append("]");
            }
            json.append("}");

            HttpRequest request = requestBuilder("/api/auth/users")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 201) {
                    return parseUserResponse(response.body());
                }
                throw new IOException("Create user failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public List<UserResponse> getAllUsers() throws IOException {
            HttpRequest request = requestBuilder("/api/auth/users")
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return JsonMapper.fromJsonList(response.body(), UserResponseDto.class).stream()
                            .map(this::toUserResponse)
                            .toList();
                }
                throw new IOException("Get users failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public Optional<UserResponse> getUser(long userId) throws IOException {
            HttpRequest request = requestBuilder("/api/auth/users/" + userId)
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return Optional.of(parseUserResponse(response.body()));
                } else if (response.statusCode() == 404) {
                    return Optional.empty();
                }
                throw new IOException("Get user failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public Optional<UserResponse> getUserByUsername(String username) throws IOException {
            HttpRequest request = requestBuilder("/api/auth/users/username/" + username)
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return Optional.of(parseUserResponse(response.body()));
                } else if (response.statusCode() == 404) {
                    return Optional.empty();
                }
                throw new IOException("Get user failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public UserResponse updatePassword(long userId, String newPassword) throws IOException {
            HttpRequest request = requestBuilder("/api/auth/users/" + userId + "/password")
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString("\"" + escapeJson(newPassword) + "\""))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return parseUserResponse(response.body());
                }
                throw new IOException("Update password failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public UserResponse updateRoles(long userId, Set<String> roles) throws IOException {
            String json = "[" + String.join(",", roles.stream().map(r -> "\"" + r + "\"").toList()) + "]";

            HttpRequest request = requestBuilder("/api/auth/users/" + userId + "/roles")
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return parseUserResponse(response.body());
                }
                throw new IOException("Update roles failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public UserResponse addRole(long userId, String roleName) throws IOException {
            HttpRequest request = requestBuilder("/api/auth/users/" + userId + "/roles/" + roleName)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return parseUserResponse(response.body());
                }
                throw new IOException("Add role failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public UserResponse removeRole(long userId, String roleName) throws IOException {
            HttpRequest request = requestBuilder("/api/auth/users/" + userId + "/roles/" + roleName)
                    .DELETE()
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return parseUserResponse(response.body());
                }
                throw new IOException("Remove role failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public UserResponse setEnabled(long userId, boolean enabled) throws IOException {
            HttpRequest request = requestBuilder("/api/auth/users/" + userId + "/enabled")
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(String.valueOf(enabled)))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return parseUserResponse(response.body());
                }
                throw new IOException("Set enabled failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public boolean deleteUser(long userId) throws IOException {
            HttpRequest request = requestBuilder("/api/auth/users/" + userId)
                    .DELETE()
                    .build();

            try {
                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                return response.statusCode() == 204;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        private UserResponse parseUserResponse(String json) {
            long id = JsonMapper.extractLong(json, "id");
            String username = JsonMapper.extractString(json, "username");
            Set<String> roles = JsonMapper.extractStringSet(json, "roles");
            String createdAtStr = JsonMapper.extractString(json, "createdAt");
            Instant createdAt = createdAtStr != null ? Instant.parse(createdAtStr) : null;
            boolean enabled = JsonMapper.extractBoolean(json, "enabled");
            return new UserResponse(id, username, roles, createdAt, enabled);
        }

        private UserResponse toUserResponse(UserResponseDto dto) {
            return new UserResponse(dto.id(), dto.username(), dto.roles(), dto.createdAt(), dto.enabled());
        }

        private String escapeJson(String value) {
            return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }

        /**
         * Internal DTO for JSON deserialization.
         */
        private record UserResponseDto(long id, String username, Set<String> roles, Instant createdAt, boolean enabled) {}
    }
}
