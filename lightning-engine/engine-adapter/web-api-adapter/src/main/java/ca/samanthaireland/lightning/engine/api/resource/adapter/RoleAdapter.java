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
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * REST API adapter for role management operations.
 *
 * <p>Provides CRUD operations for roles including nested role management.
 * This adapter follows the Single Responsibility Principle - it only handles
 * role-related operations. Requires admin role for write operations.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * RoleAdapter roles = new RoleAdapter.HttpRoleAdapter(
 *     "http://localhost:8080",
 *     AdapterConfig.defaults().withBearerToken(adminToken)
 * );
 *
 * // Create role with nested roles
 * RoleResponse role = roles.createRole("moderator", "Site moderator", Set.of("view_only"));
 *
 * // List all roles
 * List<RoleResponse> allRoles = roles.getAllRoles();
 *
 * // Update included roles
 * roles.updateIncludedRoles(role.id(), Set.of("view_only", "command_manager"));
 * }</pre>
 */
public interface RoleAdapter {

    /**
     * Create a new role.
     *
     * @param name the role name
     * @param description the role description
     * @param includedRoles roles that this role includes (for hierarchical permissions)
     * @return the created role
     * @throws IOException if creation fails
     */
    RoleResponse createRole(String name, String description, Set<String> includedRoles) throws IOException;

    /**
     * Get all roles.
     *
     * @return list of all roles
     * @throws IOException if request fails
     */
    List<RoleResponse> getAllRoles() throws IOException;

    /**
     * Get a role by ID.
     *
     * @param roleId the role ID
     * @return the role if found
     * @throws IOException if request fails
     */
    Optional<RoleResponse> getRole(long roleId) throws IOException;

    /**
     * Get a role by name.
     *
     * @param roleName the role name
     * @return the role if found
     * @throws IOException if request fails
     */
    Optional<RoleResponse> getRoleByName(String roleName) throws IOException;

    /**
     * Update a role's description.
     *
     * @param roleId the role ID
     * @param description the new description
     * @return the updated role
     * @throws IOException if update fails
     */
    RoleResponse updateDescription(long roleId, String description) throws IOException;

    /**
     * Update which roles this role includes.
     *
     * @param roleId the role ID
     * @param includedRoles the new set of included roles
     * @return the updated role
     * @throws IOException if update fails
     */
    RoleResponse updateIncludedRoles(long roleId, Set<String> includedRoles) throws IOException;

    /**
     * Delete a role.
     *
     * @param roleId the role ID
     * @return true if deleted
     * @throws IOException if deletion fails
     */
    boolean deleteRole(long roleId) throws IOException;

    /**
     * Check if a role exists by name.
     *
     * @param roleName the role name
     * @return true if exists
     * @throws IOException if request fails
     */
    default boolean hasRole(String roleName) throws IOException {
        return getRoleByName(roleName).isPresent();
    }

    /**
     * Role response DTO.
     *
     * @param id the role ID
     * @param name the role name
     * @param description the role description
     * @param includedRoles roles that this role includes
     */
    record RoleResponse(long id, String name, String description, Set<String> includedRoles) {}

    /**
     * HTTP-based implementation of RoleAdapter.
     */
    class HttpRoleAdapter implements RoleAdapter {
        private final HttpClient httpClient;
        private final String baseUrl;
        private final AdapterConfig config;

        public HttpRoleAdapter(String baseUrl, AdapterConfig config) {
            this.baseUrl = normalizeUrl(baseUrl);
            this.config = config;
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(config.getConnectTimeout())
                    .build();
        }

        public HttpRoleAdapter(String baseUrl, HttpClient httpClient) {
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
        public RoleResponse createRole(String name, String description, Set<String> includedRoles) throws IOException {
            StringBuilder json = new StringBuilder();
            json.append("{\"name\":\"").append(escapeJson(name)).append("\"");
            if (description != null) {
                json.append(",\"description\":\"").append(escapeJson(description)).append("\"");
            }
            if (includedRoles != null && !includedRoles.isEmpty()) {
                json.append(",\"includedRoles\":[");
                json.append(String.join(",", includedRoles.stream().map(r -> "\"" + r + "\"").toList()));
                json.append("]");
            }
            json.append("}");

            HttpRequest request = requestBuilder("/api/auth/roles")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 201) {
                    return parseRoleResponse(response.body());
                }
                throw new IOException("Create role failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public List<RoleResponse> getAllRoles() throws IOException {
            HttpRequest request = requestBuilder("/api/auth/roles")
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return JsonMapper.fromJsonList(response.body(), RoleResponseDto.class).stream()
                            .map(this::toRoleResponse)
                            .toList();
                }
                throw new IOException("Get roles failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public Optional<RoleResponse> getRole(long roleId) throws IOException {
            HttpRequest request = requestBuilder("/api/auth/roles/" + roleId)
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return Optional.of(parseRoleResponse(response.body()));
                } else if (response.statusCode() == 404) {
                    return Optional.empty();
                }
                throw new IOException("Get role failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public Optional<RoleResponse> getRoleByName(String roleName) throws IOException {
            HttpRequest request = requestBuilder("/api/auth/roles/name/" + roleName)
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return Optional.of(parseRoleResponse(response.body()));
                } else if (response.statusCode() == 404) {
                    return Optional.empty();
                }
                throw new IOException("Get role failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public RoleResponse updateDescription(long roleId, String description) throws IOException {
            HttpRequest request = requestBuilder("/api/auth/roles/" + roleId + "/description")
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString("\"" + escapeJson(description) + "\""))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return parseRoleResponse(response.body());
                }
                throw new IOException("Update description failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public RoleResponse updateIncludedRoles(long roleId, Set<String> includedRoles) throws IOException {
            String json = "[" + String.join(",", includedRoles.stream().map(r -> "\"" + r + "\"").toList()) + "]";

            HttpRequest request = requestBuilder("/api/auth/roles/" + roleId + "/includes")
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return parseRoleResponse(response.body());
                }
                throw new IOException("Update included roles failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public boolean deleteRole(long roleId) throws IOException {
            HttpRequest request = requestBuilder("/api/auth/roles/" + roleId)
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

        private RoleResponse parseRoleResponse(String json) {
            long id = JsonMapper.extractLong(json, "id");
            String name = JsonMapper.extractString(json, "name");
            String description = JsonMapper.extractString(json, "description");
            Set<String> includedRoles = JsonMapper.extractStringSet(json, "includedRoles");
            return new RoleResponse(id, name, description, includedRoles);
        }

        private RoleResponse toRoleResponse(RoleResponseDto dto) {
            return new RoleResponse(dto.id(), dto.name(), dto.description(), dto.includedRoles());
        }

        private String escapeJson(String value) {
            if (value == null) return "";
            return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }

        /**
         * Internal DTO for JSON deserialization.
         */
        private record RoleResponseDto(long id, String name, String description, Set<String> includedRoles) {}
    }
}
