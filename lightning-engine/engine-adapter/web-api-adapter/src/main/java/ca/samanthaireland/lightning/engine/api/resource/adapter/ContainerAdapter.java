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

import ca.samanthaireland.lightning.engine.api.resource.adapter.dto.HistoryQueryParams;
import ca.samanthaireland.lightning.engine.api.resource.adapter.dto.HistorySnapshotDto;
import ca.samanthaireland.lightning.engine.api.resource.adapter.dto.MatchHistorySummaryDto;
import ca.samanthaireland.lightning.engine.api.resource.adapter.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API adapter for container operations.
 *
 * <p>Containers provide isolated execution environments for matches.
 * Each container has its own modules, AI, resources, and simulation tick.
 *
 * <p>This interface extends focused interfaces for backwards compatibility.
 * New code can use the focused interfaces directly:
 * <ul>
 *   <li>{@link ContainerLifecycleAdapter} - container CRUD and lifecycle</li>
 *   <li>{@link MatchAdapter} - match CRUD (via ContainerScope)</li>
 *   <li>{@link SimulationAdapter} - tick control (via ContainerScope)</li>
 *   <li>{@link SnapshotAdapter} - snapshot retrieval (via ContainerScope)</li>
 *   <li>{@link ResourceAdapter} - resource management (via ContainerScope)</li>
 *   <li>{@link PlayerAdapter} - player CRUD (via ContainerScope)</li>
 *   <li>{@link SessionAdapter} - session management (via ContainerScope)</li>
 *   <li>{@link HistoryAdapter} - history retrieval (via ContainerScope)</li>
 * </ul>
 */
public interface ContainerAdapter extends ContainerLifecycleAdapter {

    /**
     * Create a new container.
     *
     * @param name the container name
     * @return the created container response
     */
    ContainerResponse createContainer(String name) throws IOException;

    /**
     * Create a new container with configuration.
     *
     * @return a builder for container creation
     */
    CreateContainerBuilder create();

    /**
     * Get a container by ID.
     *
     * @param containerId the container ID
     * @return the container if found
     */
    Optional<ContainerResponse> getContainer(long containerId) throws IOException;

    /**
     * Get all containers.
     *
     * @return list of all containers
     */
    List<ContainerResponse> getAllContainers() throws IOException;

    /**
     * Delete a container.
     *
     * @param containerId the container ID
     * @return true if deleted
     */
    boolean deleteContainer(long containerId) throws IOException;

    /**
     * Start a container.
     *
     * @param containerId the container ID
     * @return the updated container response
     */
    ContainerResponse startContainer(long containerId) throws IOException;

    /**
     * Stop a container.
     *
     * @param containerId the container ID
     * @return the updated container response
     */
    ContainerResponse stopContainer(long containerId) throws IOException;

    /**
     * Get a scoped client for a specific container.
     *
     * @param containerId the container ID
     * @return a scoped client for container operations
     */
    ContainerScope forContainer(long containerId);

    /**
     * Container response DTO.
     */
    record ContainerResponse(long id, String name, String status) {}

    /**
     * Match response DTO for container-scoped matches.
     */
    record MatchResponse(long id, List<String> enabledModules, List<String> enabledAIs) {}

    /**
     * Snapshot response DTO.
     */
    record SnapshotResponse(long matchId, long tick, String data) {}

    /**
     * Resource response DTO for container-scoped resources.
     */
    record ResourceResponse(long resourceId, String resourceName, String resourceType) {}

    /**
     * Builder for creating containers.
     */
    interface CreateContainerBuilder {
        CreateContainerBuilder name(String name);
        CreateContainerBuilder withModules(String... moduleNames);
        CreateContainerBuilder withAIs(String... aiNames);
        ContainerResponse execute() throws IOException;
    }

    /**
     * Scoped operations for a specific container.
     *
     * <p>Extends focused interfaces for backwards compatibility.
     */
    interface ContainerScope extends MatchAdapter, SimulationAdapter, SnapshotAdapter,
            ResourceAdapter, PlayerAdapter, SessionAdapter, HistoryAdapter {
        /**
         * Create a match in this container.
         */
        MatchResponse createMatch(List<String> modules) throws IOException;

        /**
         * Create a match with modules and AIs in this container.
         */
        MatchResponse createMatch(List<String> modules, List<String> ais) throws IOException;

        /**
         * Get all matches in this container.
         */
        List<MatchResponse> getMatches() throws IOException;

        /**
         * Get a specific match in this container.
         */
        Optional<MatchResponse> getMatch(long matchId) throws IOException;

        /**
         * Delete a match in this container.
         */
        boolean deleteMatch(long matchId) throws IOException;

        /**
         * Submit a command in this container.
         */
        void submitCommand(String commandName, Map<String, Object> parameters) throws IOException;

        /**
         * Advance the container's tick.
         */
        long tick() throws IOException;

        /**
         * Get the container's current tick.
         */
        long currentTick() throws IOException;

        /**
         * Start auto-advancing ticks at the specified interval.
         */
        void play(int intervalMs) throws IOException;

        /**
         * Stop auto-advancing ticks.
         */
        void stopAuto() throws IOException;

        /**
         * Get a snapshot for a match in this container.
         */
        Optional<SnapshotResponse> getSnapshot(long matchId) throws IOException;

        /**
         * Get command builder for a match.
         *
         * @see ContainerCommands
         */
        ContainerCommands.MatchCommands forMatch(long matchId);

        // ==================== RESOURCE OPERATIONS ====================

        /**
         * List all resources in this container.
         *
         * @return list of resource metadata
         */
        List<ResourceResponse> listResources() throws IOException;

        /**
         * Get a resource by ID from this container.
         *
         * @param resourceId the resource ID
         * @return the resource if found
         */
        Optional<ResourceResponse> getResource(long resourceId) throws IOException;

        /**
         * Delete a resource from this container.
         *
         * @param resourceId the resource ID
         * @return true if deleted
         */
        boolean deleteResource(long resourceId) throws IOException;

        /**
         * Upload a resource to this container.
         *
         * @param resourceName the resource name
         * @param resourceType the resource type (e.g., "TEXTURE")
         * @param data the binary data
         * @return the created resource ID
         */
        long uploadResource(String resourceName, String resourceType, byte[] data) throws IOException;

        /**
         * Start building an upload request for this container.
         *
         * @return a builder for resource upload
         */
        UploadResourceBuilder upload();

        // ==================== MODULE OPERATIONS ====================

        /**
         * List all modules loaded in this container.
         *
         * @return list of module names
         */
        List<String> listModules() throws IOException;

        // ==================== AI OPERATIONS ====================

        /**
         * List all AI available in this container.
         *
         * @return list of AI names
         */
        List<String> listAI() throws IOException;

        // ==================== PLAYER OPERATIONS ====================

        /**
         * Create a player in this container.
         *
         * @param playerId optional player ID (auto-generated if null or 0)
         * @return the created player ID
         */
        long createPlayer(Long playerId) throws IOException;

        /**
         * Get all players in this container.
         *
         * @return list of player IDs
         */
        List<Long> listPlayers() throws IOException;

        /**
         * Delete a player from this container.
         *
         * @param playerId the player ID
         * @return true if deleted
         */
        boolean deletePlayer(long playerId) throws IOException;

        // ==================== SESSION OPERATIONS ====================

        /**
         * Connect a player to a match (create session).
         *
         * @param matchId the match ID
         * @param playerId the player ID
         */
        void connectSession(long matchId, long playerId) throws IOException;

        /**
         * Disconnect a player from a match.
         *
         * @param matchId the match ID
         * @param playerId the player ID
         */
        void disconnectSession(long matchId, long playerId) throws IOException;

        /**
         * Join a player to a match.
         *
         * @param matchId the match ID
         * @param playerId the player ID
         */
        void joinMatch(long matchId, long playerId) throws IOException;

        // ==================== HISTORY OPERATIONS ====================

        /**
         * Get history summary for a match.
         *
         * @param matchId the match ID
         * @return the history summary
         */
        MatchHistorySummaryDto getMatchHistorySummary(long matchId) throws IOException;

        /**
         * Get historical snapshots for a match.
         *
         * @param matchId the match ID
         * @param params query parameters (tick range, limit)
         * @return list of historical snapshots
         */
        List<HistorySnapshotDto> getHistorySnapshots(long matchId, HistoryQueryParams params) throws IOException;

        /**
         * Get the latest historical snapshots for a match.
         *
         * @param matchId the match ID
         * @param limit maximum snapshots to return
         * @return list of latest snapshots (ordered by tick descending)
         */
        List<HistorySnapshotDto> getLatestHistorySnapshots(long matchId, int limit) throws IOException;

        /**
         * Get a specific historical snapshot by tick.
         *
         * @param matchId the match ID
         * @param tick the tick number
         * @return the snapshot if found
         */
        Optional<HistorySnapshotDto> getHistorySnapshotAtTick(long matchId, long tick) throws IOException;
    }

    /**
     * Builder for uploading resources to a container.
     */
    interface UploadResourceBuilder {
        UploadResourceBuilder name(String resourceName);
        UploadResourceBuilder type(String resourceType);
        UploadResourceBuilder data(byte[] data);
        long execute() throws IOException;
    }


    /**
     * HTTP-based implementation.
     */
    class HttpContainerAdapter implements ContainerAdapter {
        private final HttpClient httpClient;
        private final String baseUrl;
        private final AdapterConfig config;

        public HttpContainerAdapter(String baseUrl) {
            this(baseUrl, AdapterConfig.defaults());
        }

        public HttpContainerAdapter(String baseUrl, AdapterConfig config) {
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            this.config = config;
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(config.getConnectTimeout())
                    .build();
        }

        public HttpContainerAdapter(String baseUrl, HttpClient httpClient) {
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            this.config = AdapterConfig.defaults();
            this.httpClient = httpClient;
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
        public ContainerResponse createContainer(String name) throws IOException {
            return create().name(name).execute();
        }

        @Override
        public CreateContainerBuilder create() {
            return new HttpCreateContainerBuilder(this);
        }

        @Override
        public Optional<ContainerResponse> getContainer(long containerId) throws IOException {
            HttpRequest request = requestBuilder("/api/containers/" + containerId)
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return Optional.of(parseContainerResponse(response.body()));
                } else if (response.statusCode() == 404) {
                    return Optional.empty();
                }
                throw new IOException("Get container failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public List<ContainerResponse> getAllContainers() throws IOException {
            HttpRequest request = requestBuilder("/api/containers")
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return parseContainerList(response.body());
                }
                throw new IOException("Get all containers failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public boolean deleteContainer(long containerId) throws IOException {
            HttpRequest request = requestBuilder("/api/containers/" + containerId)
                    .DELETE()
                    .build();

            try {
                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                return response.statusCode() == 204 || response.statusCode() == 200;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public ContainerResponse startContainer(long containerId) throws IOException {
            HttpRequest request = requestBuilder("/api/containers/" + containerId + "/start")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return parseContainerResponse(response.body());
                }
                throw new IOException("Start container failed with status: " + response.statusCode() + " - " + response.body());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public ContainerResponse stopContainer(long containerId) throws IOException {
            HttpRequest request = requestBuilder("/api/containers/" + containerId + "/stop")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return parseContainerResponse(response.body());
                }
                throw new IOException("Stop container failed with status: " + response.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        @Override
        public ContainerScope forContainer(long containerId) {
            return new HttpContainerScope(this, containerId);
        }

        ContainerResponse createContainerInternal(String name, List<String> modules, List<String> ais) throws IOException {
            Map<String, Object> body = new HashMap<>();
            body.put("name", name);
            if (!modules.isEmpty()) {
                body.put("moduleNames", modules);
            }
            if (!ais.isEmpty()) {
                body.put("aiNames", ais);
            }
            String json = JsonMapper.toJson(body);

            HttpRequest request = requestBuilder("/api/containers")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 201 || response.statusCode() == 200) {
                    return parseContainerResponse(response.body());
                }
                throw new IOException("Create container failed with status: " + response.statusCode() + " - " + response.body());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        private ContainerResponse parseContainerResponse(String json) throws IOException {
            Map<String, Object> map = JsonMapper.fromJson(json, Map.class);
            long id = ((Number) map.get("id")).longValue();
            String name = (String) map.get("name");
            String status = (String) map.get("status");
            return new ContainerResponse(id, name, status);
        }

        private List<ContainerResponse> parseContainerList(String json) throws IOException {
            List<Map<String, Object>> list = JsonMapper.fromJson(json, List.class);
            List<ContainerResponse> result = new ArrayList<>();
            for (Map<String, Object> map : list) {
                long id = ((Number) map.get("id")).longValue();
                String name = (String) map.get("name");
                String status = (String) map.get("status");
                result.add(new ContainerResponse(id, name, status));
            }
            return result;
        }

        /**
         * Create container builder implementation.
         */
        private static class HttpCreateContainerBuilder implements CreateContainerBuilder {
            private final HttpContainerAdapter adapter;
            private String name;
            private final List<String> modules = new ArrayList<>();
            private final List<String> ais = new ArrayList<>();

            HttpCreateContainerBuilder(HttpContainerAdapter adapter) {
                this.adapter = adapter;
            }

            @Override
            public CreateContainerBuilder name(String name) {
                this.name = name;
                return this;
            }

            @Override
            public CreateContainerBuilder withModules(String... moduleNames) {
                modules.addAll(List.of(moduleNames));
                return this;
            }

            @Override
            public CreateContainerBuilder withAIs(String... aiNames) {
                ais.addAll(List.of(aiNames));
                return this;
            }

            @Override
            public ContainerResponse execute() throws IOException {
                if (name == null || name.isBlank()) {
                    throw new IllegalStateException("Container name is required");
                }
                return adapter.createContainerInternal(name, modules, ais);
            }
        }

        /**
         * Container scope implementation.
         */
        private static class HttpContainerScope implements ContainerScope {
            private final HttpContainerAdapter adapter;
            private final long containerId;

            HttpContainerScope(HttpContainerAdapter adapter, long containerId) {
                this.adapter = adapter;
                this.containerId = containerId;
            }

            private HttpRequest.Builder requestBuilder(String path) {
                return adapter.requestBuilder("/api/containers/" + containerId + path);
            }

            @Override
            public MatchResponse createMatch(List<String> modules) throws IOException {
                return createMatch(modules, List.of());
            }

            @Override
            public MatchResponse createMatch(List<String> modules, List<String> ais) throws IOException {
                Map<String, Object> body = new HashMap<>();
                body.put("enabledModuleNames", modules);
                body.put("enabledAINames", ais);
                String json = JsonMapper.toJson(body);

                HttpRequest request = requestBuilder("/matches")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 201 || response.statusCode() == 200) {
                        return parseMatchResponse(response.body());
                    }
                    throw new IOException("Create match failed with status: " + response.statusCode() + " - " + response.body());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public List<MatchResponse> getMatches() throws IOException {
                HttpRequest request = requestBuilder("/matches")
                        .GET()
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        return parseMatchList(response.body());
                    }
                    throw new IOException("Get matches failed with status: " + response.statusCode());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public Optional<MatchResponse> getMatch(long matchId) throws IOException {
                HttpRequest request = requestBuilder("/matches/" + matchId)
                        .GET()
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        return Optional.of(parseMatchResponse(response.body()));
                    } else if (response.statusCode() == 404) {
                        return Optional.empty();
                    }
                    throw new IOException("Get match failed with status: " + response.statusCode());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public boolean deleteMatch(long matchId) throws IOException {
                HttpRequest request = requestBuilder("/matches/" + matchId)
                        .DELETE()
                        .build();

                try {
                    HttpResponse<Void> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                    return response.statusCode() == 204 || response.statusCode() == 200;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public void submitCommand(String commandName, Map<String, Object> parameters) throws IOException {
                Map<String, Object> body = new HashMap<>();
                body.put("commandName", commandName);
                body.put("parameters", parameters);
                String json = JsonMapper.toJson(body);

                HttpRequest request = requestBuilder("/commands")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() != 200 && response.statusCode() != 201 && response.statusCode() != 202) {
                        throw new IOException("Submit command failed with status: " + response.statusCode() + " - " + response.body());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public long tick() throws IOException {
                HttpRequest request = requestBuilder("/tick")
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        Map<String, Object> map = JsonMapper.fromJson(response.body(), Map.class);
                        return ((Number) map.get("tick")).longValue();
                    }
                    throw new IOException("Tick failed with status: " + response.statusCode());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public long currentTick() throws IOException {
                HttpRequest request = requestBuilder("/tick")
                        .GET()
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        Map<String, Object> map = JsonMapper.fromJson(response.body(), Map.class);
                        return ((Number) map.get("tick")).longValue();
                    }
                    throw new IOException("Get tick failed with status: " + response.statusCode());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public void play(int intervalMs) throws IOException {
                HttpRequest request = requestBuilder("/play?intervalMs=" + intervalMs)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() != 200) {
                        throw new IOException("Play failed with status: " + response.statusCode());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public void stopAuto() throws IOException {
                HttpRequest request = requestBuilder("/stop-auto")
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() != 200) {
                        throw new IOException("Stop auto failed with status: " + response.statusCode());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            public Optional<SnapshotResponse> getSnapshot(long matchId) throws IOException {
                HttpRequest request = requestBuilder("/matches/" + matchId + "/snapshot")
                        .GET()
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        Map<String, Object> map = JsonMapper.fromJson(response.body(), Map.class);
                        long mId = ((Number) map.get("matchId")).longValue();
                        long tick = ((Number) map.get("tick")).longValue();
                        // Convert new modules format to legacy data format for backwards compatibility
                        Object modules = map.get("modules");
                        String dataStr;
                        if (modules instanceof List<?> moduleList) {
                            Map<String, Map<String, List<Number>>> legacyData = new java.util.LinkedHashMap<>();
                            for (Object moduleObj : moduleList) {
                                if (moduleObj instanceof Map<?, ?> moduleMap) {
                                    String moduleName = (String) ((Map<String, Object>) moduleMap).get("name");
                                    List<?> components = (List<?>) ((Map<String, Object>) moduleMap).get("components");
                                    Map<String, List<Number>> componentData = new java.util.LinkedHashMap<>();
                                    if (components != null) {
                                        for (Object compObj : components) {
                                            if (compObj instanceof Map<?, ?> compMap) {
                                                String compName = (String) ((Map<String, Object>) compMap).get("name");
                                                List<Number> values = (List<Number>) ((Map<String, Object>) compMap).get("values");
                                                componentData.put(compName, values != null ? values : List.of());
                                            }
                                        }
                                    }
                                    legacyData.put(moduleName, componentData);
                                }
                            }
                            dataStr = JsonMapper.toJson(legacyData);
                        } else {
                            // Fallback to direct data field for backwards compatibility
                            Object data = map.get("data");
                            dataStr = data != null ? JsonMapper.toJson(data) : "{}";
                        }
                        return Optional.of(new SnapshotResponse(mId, tick, dataStr));
                    } else if (response.statusCode() == 404) {
                        return Optional.empty();
                    }
                    throw new IOException("Get snapshot failed with status: " + response.statusCode());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public ContainerCommands.MatchCommands forMatch(long matchId) {
                return new ContainerCommands.HttpMatchCommands(this::submitCommand, matchId);
            }

            // ==================== RESOURCE OPERATIONS ====================

            @Override
            public List<ResourceResponse> listResources() throws IOException {
                HttpRequest request = requestBuilder("/resources")
                        .GET()
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        return parseResourceList(response.body());
                    }
                    throw new IOException("List resources failed with status: " + response.statusCode());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public Optional<ResourceResponse> getResource(long resourceId) throws IOException {
                HttpRequest request = requestBuilder("/resources/" + resourceId)
                        .GET()
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        return Optional.of(parseResourceResponse(response.body()));
                    } else if (response.statusCode() == 404) {
                        return Optional.empty();
                    }
                    throw new IOException("Get resource failed with status: " + response.statusCode());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public boolean deleteResource(long resourceId) throws IOException {
                HttpRequest request = requestBuilder("/resources/" + resourceId)
                        .DELETE()
                        .build();

                try {
                    HttpResponse<Void> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                    return response.statusCode() == 204 || response.statusCode() == 200;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public long uploadResource(String resourceName, String resourceType, byte[] data) throws IOException {
                String boundary = "WebKitFormBoundary" + System.currentTimeMillis();

                String body = "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"resourceName\"\r\n\r\n" +
                        resourceName + "\r\n" +
                        "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"resourceType\"\r\n\r\n" +
                        resourceType + "\r\n" +
                        "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"" + resourceName + "\"\r\n" +
                        "Content-Type: application/octet-stream\r\n\r\n";

                byte[] prefix = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byte[] requestBody = new byte[prefix.length + data.length + suffix.length];
                System.arraycopy(prefix, 0, requestBody, 0, prefix.length);
                System.arraycopy(data, 0, requestBody, prefix.length, data.length);
                System.arraycopy(suffix, 0, requestBody, prefix.length + data.length, suffix.length);

                HttpRequest request = requestBuilder("/resources")
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 201 || response.statusCode() == 200) {
                        Map<String, Object> map = JsonMapper.fromJson(response.body(), Map.class);
                        return ((Number) map.get("resourceId")).longValue();
                    }
                    throw new IOException("Upload resource failed with status: " + response.statusCode() + " - " + response.body());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public UploadResourceBuilder upload() {
                return new HttpUploadResourceBuilder(this);
            }

            // ==================== MODULE OPERATIONS ====================

            @Override
            public List<String> listModules() throws IOException {
                HttpRequest request = requestBuilder("/modules")
                        .GET()
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        return JsonMapper.fromJsonList(response.body(), String.class);
                    }
                    throw new IOException("List modules failed with status: " + response.statusCode());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            // ==================== AI OPERATIONS ====================

            @Override
            public List<String> listAI() throws IOException {
                HttpRequest request = requestBuilder("/ai")
                        .GET()
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        return JsonMapper.fromJsonList(response.body(), String.class);
                    }
                    throw new IOException("List AI failed with status: " + response.statusCode());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            // ==================== PLAYER OPERATIONS ====================

            @Override
            public long createPlayer(Long playerId) throws IOException {
                Map<String, Object> body = new HashMap<>();
                if (playerId != null && playerId > 0) {
                    body.put("id", playerId);
                }
                String json = JsonMapper.toJson(body);

                HttpRequest request = requestBuilder("/players")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 201 || response.statusCode() == 200) {
                        Map<String, Object> map = JsonMapper.fromJson(response.body(), Map.class);
                        return ((Number) map.get("id")).longValue();
                    }
                    throw new IOException("Create player failed with status: " + response.statusCode() + " - " + response.body());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public List<Long> listPlayers() throws IOException {
                HttpRequest request = requestBuilder("/players")
                        .GET()
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        List<Map<String, Object>> list = JsonMapper.fromJson(response.body(), List.class);
                        List<Long> playerIds = new ArrayList<>();
                        for (Map<String, Object> map : list) {
                            playerIds.add(((Number) map.get("id")).longValue());
                        }
                        return playerIds;
                    }
                    throw new IOException("List players failed with status: " + response.statusCode());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public boolean deletePlayer(long playerId) throws IOException {
                HttpRequest request = requestBuilder("/players/" + playerId)
                        .DELETE()
                        .build();

                try {
                    HttpResponse<Void> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                    return response.statusCode() == 204 || response.statusCode() == 200;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            // ==================== SESSION OPERATIONS ====================

            @Override
            public void connectSession(long matchId, long playerId) throws IOException {
                Map<String, Object> body = new HashMap<>();
                body.put("playerId", playerId);
                String json = JsonMapper.toJson(body);

                HttpRequest request = requestBuilder("/matches/" + matchId + "/sessions")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() != 200 && response.statusCode() != 201) {
                        throw new IOException("Connect session failed with status: " + response.statusCode() + " - " + response.body());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public void disconnectSession(long matchId, long playerId) throws IOException {
                HttpRequest request = requestBuilder("/matches/" + matchId + "/sessions/" + playerId + "/disconnect")
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() != 200 && response.statusCode() != 204) {
                        throw new IOException("Disconnect session failed with status: " + response.statusCode());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public void joinMatch(long matchId, long playerId) throws IOException {
                Map<String, Object> body = new HashMap<>();
                body.put("playerId", playerId);
                String json = JsonMapper.toJson(body);

                HttpRequest request = requestBuilder("/matches/" + matchId + "/players")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() != 200 && response.statusCode() != 201) {
                        throw new IOException("Join match failed with status: " + response.statusCode() + " - " + response.body());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            // ==================== HISTORY OPERATIONS ====================

            @Override
            public MatchHistorySummaryDto getMatchHistorySummary(long matchId) throws IOException {
                HttpRequest request = requestBuilder("/matches/" + matchId + "/history")
                        .GET()
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        return parseMatchHistorySummary(response.body());
                    }
                    throw new IOException("Get match history summary failed with status: " + response.statusCode() + " - " + response.body());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public List<HistorySnapshotDto> getHistorySnapshots(long matchId, HistoryQueryParams params) throws IOException {
                String path = "/matches/" + matchId + "/history/snapshots" +
                        "?fromTick=" + params.fromTick() +
                        "&toTick=" + params.toTick() +
                        "&limit=" + params.limit();

                HttpRequest request = requestBuilder(path)
                        .GET()
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        return parseHistorySnapshotList(response.body());
                    }
                    throw new IOException("Get history snapshots failed with status: " + response.statusCode() + " - " + response.body());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public List<HistorySnapshotDto> getLatestHistorySnapshots(long matchId, int limit) throws IOException {
                HttpRequest request = requestBuilder("/matches/" + matchId + "/history/snapshots/latest?limit=" + limit)
                        .GET()
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        return parseHistorySnapshotList(response.body());
                    }
                    throw new IOException("Get latest history snapshots failed with status: " + response.statusCode() + " - " + response.body());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            @Override
            public Optional<HistorySnapshotDto> getHistorySnapshotAtTick(long matchId, long tick) throws IOException {
                HttpRequest request = requestBuilder("/matches/" + matchId + "/history/snapshots/" + tick)
                        .GET()
                        .build();

                try {
                    HttpResponse<String> response = adapter.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        return Optional.of(parseHistorySnapshot(response.body()));
                    } else if (response.statusCode() == 404) {
                        return Optional.empty();
                    }
                    throw new IOException("Get history snapshot failed with status: " + response.statusCode());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", e);
                }
            }

            private MatchHistorySummaryDto parseMatchHistorySummary(String json) throws IOException {
                Map<String, Object> map = JsonMapper.fromJson(json, Map.class);
                long cId = ((Number) map.get("containerId")).longValue();
                long mId = ((Number) map.get("matchId")).longValue();
                long count = ((Number) map.get("snapshotCount")).longValue();
                long firstTick = map.get("firstTick") != null ? ((Number) map.get("firstTick")).longValue() : -1;
                long lastTick = map.get("lastTick") != null ? ((Number) map.get("lastTick")).longValue() : -1;
                Instant firstTs = map.get("firstTimestamp") != null ? Instant.parse((String) map.get("firstTimestamp")) : null;
                Instant lastTs = map.get("lastTimestamp") != null ? Instant.parse((String) map.get("lastTimestamp")) : null;
                return new MatchHistorySummaryDto(cId, mId, count, firstTick, lastTick, firstTs, lastTs);
            }

            @SuppressWarnings("unchecked")
            private List<HistorySnapshotDto> parseHistorySnapshotList(String json) throws IOException {
                Map<String, Object> response = JsonMapper.fromJson(json, Map.class);
                List<Map<String, Object>> snapshots = (List<Map<String, Object>>) response.get("snapshots");
                List<HistorySnapshotDto> result = new ArrayList<>();
                if (snapshots != null) {
                    for (Map<String, Object> snap : snapshots) {
                        result.add(parseHistorySnapshotFromMap(snap));
                    }
                }
                return result;
            }

            @SuppressWarnings("unchecked")
            private HistorySnapshotDto parseHistorySnapshot(String json) throws IOException {
                Map<String, Object> map = JsonMapper.fromJson(json, Map.class);
                return parseHistorySnapshotFromMap(map);
            }

            @SuppressWarnings("unchecked")
            private HistorySnapshotDto parseHistorySnapshotFromMap(Map<String, Object> map) {
                long cId = ((Number) map.get("containerId")).longValue();
                long mId = ((Number) map.get("matchId")).longValue();
                long tick = ((Number) map.get("tick")).longValue();
                Instant ts = map.get("timestamp") != null ? Instant.parse((String) map.get("timestamp")) : null;
                Map<String, Object> data = (Map<String, Object>) map.get("data");
                return new HistorySnapshotDto(cId, mId, tick, ts, data != null ? data : Map.of());
            }

            private ResourceResponse parseResourceResponse(String json) throws IOException {
                Map<String, Object> map = JsonMapper.fromJson(json, Map.class);
                long resourceId = ((Number) map.get("resourceId")).longValue();
                String resourceName = (String) map.get("resourceName");
                String resourceType = (String) map.get("resourceType");
                return new ResourceResponse(resourceId, resourceName, resourceType);
            }

            private List<ResourceResponse> parseResourceList(String json) throws IOException {
                List<Map<String, Object>> list = JsonMapper.fromJson(json, List.class);
                List<ResourceResponse> result = new ArrayList<>();
                for (Map<String, Object> map : list) {
                    long resourceId = ((Number) map.get("resourceId")).longValue();
                    String resourceName = (String) map.get("resourceName");
                    String resourceType = (String) map.get("resourceType");
                    result.add(new ResourceResponse(resourceId, resourceName, resourceType));
                }
                return result;
            }

            private MatchResponse parseMatchResponse(String json) throws IOException {
                Map<String, Object> map = JsonMapper.fromJson(json, Map.class);
                long id = ((Number) map.get("id")).longValue();
                List<String> modules = map.get("modules") != null
                        ? (List<String>) map.get("modules")
                        : List.of();
                List<String> ais = map.get("ais") != null
                        ? (List<String>) map.get("ais")
                        : List.of();
                return new MatchResponse(id, modules, ais);
            }

            private List<MatchResponse> parseMatchList(String json) throws IOException {
                List<Map<String, Object>> list = JsonMapper.fromJson(json, List.class);
                List<MatchResponse> result = new ArrayList<>();
                for (Map<String, Object> map : list) {
                    long id = ((Number) map.get("id")).longValue();
                    List<String> modules = map.get("modules") != null
                            ? (List<String>) map.get("modules")
                            : List.of();
                    List<String> ais = map.get("ais") != null
                            ? (List<String>) map.get("ais")
                            : List.of();
                    result.add(new MatchResponse(id, modules, ais));
                }
                return result;
            }
        }

        // Command builder implementations are now in ContainerCommands.java

        /**
         * Upload resource builder implementation for container-scoped uploads.
         */
        private static class HttpUploadResourceBuilder implements UploadResourceBuilder {
            private final HttpContainerScope scope;
            private String resourceName;
            private String resourceType = "TEXTURE";
            private byte[] data;

            HttpUploadResourceBuilder(HttpContainerScope scope) {
                this.scope = scope;
            }

            @Override
            public UploadResourceBuilder name(String resourceName) {
                this.resourceName = resourceName;
                return this;
            }

            @Override
            public UploadResourceBuilder type(String resourceType) {
                this.resourceType = resourceType;
                return this;
            }

            @Override
            public UploadResourceBuilder data(byte[] data) {
                this.data = data;
                return this;
            }

            @Override
            public long execute() throws IOException {
                if (resourceName == null || resourceName.isBlank()) {
                    throw new IllegalStateException("Resource name is required");
                }
                if (data == null || data.length == 0) {
                    throw new IllegalStateException("Resource data is required");
                }
                return scope.uploadResource(resourceName, resourceType, data);
            }
        }
    }
}
