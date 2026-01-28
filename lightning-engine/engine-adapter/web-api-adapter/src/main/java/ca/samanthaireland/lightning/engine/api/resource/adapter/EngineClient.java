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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;

/**
 * Unified client for accessing the Lightning Engine API.
 *
 * <p>This is the main entry point for interacting with the Lightning Engine REST API.
 * All operations are container-scoped - you create a container first, then perform
 * operations within that container's context.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Connect
 * EngineClient client = EngineClient.connect("http://localhost:8080");
 *
 * // Create and start a container
 * var container = client.createContainer()
 *     .name("my-game")
 *     .execute();
 * client.containers().startContainer(container.id());
 *
 * // Get a scoped client for the container
 * var scope = client.container(container.id());
 *
 * // Create a match with modules
 * var match = scope.createMatch(List.of("EntityModule", "RigidBodyModule"));
 *
 * // Spawn entities
 * scope.forMatch(match.id())
 *     .spawn().forPlayer(1).ofType(100).execute();
 *
 * // Control simulation
 * scope.tick();
 * scope.play(16);  // 60 FPS
 *
 * // Fetch snapshots
 * var snapshot = scope.getSnapshot(match.id());
 * }</pre>
 */
public class EngineClient {

    private final String baseUrl;
    private final AdapterConfig config;
    private final HttpClient httpClient;
    private final SnapshotParser snapshotParser;

    private volatile ContainerAdapter containerAdapter;
    private volatile ModuleAdapter moduleAdapter;
    private volatile AuthAdapter authAdapter;
    private volatile UserAdapter userAdapter;
    private volatile RoleAdapter roleAdapter;

    private EngineClient(String baseUrl, AdapterConfig config, HttpClient httpClient, SnapshotParser snapshotParser) {
        this.baseUrl = normalizeUrl(baseUrl);
        this.config = config;
        this.httpClient = httpClient;
        this.snapshotParser = snapshotParser;
    }

    /**
     * Connect to the Lightning Engine API with default configuration.
     *
     * @param baseUrl the base URL of the server (e.g., "http://localhost:8080")
     * @return a new EngineClient instance
     */
    public static EngineClient connect(String baseUrl) {
        return builder().baseUrl(baseUrl).build();
    }

    /**
     * Create a builder for custom configuration.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the container adapter for container operations.
     *
     * @return the container adapter
     */
    public ContainerAdapter containers() {
        if (containerAdapter == null) {
            synchronized (this) {
                if (containerAdapter == null) {
                    containerAdapter = httpClient != null
                        ? new ContainerAdapter.HttpContainerAdapter(baseUrl, httpClient)
                        : new ContainerAdapter.HttpContainerAdapter(baseUrl, config);
                }
            }
        }
        return containerAdapter;
    }

    /**
     * Get the module adapter for global module operations.
     *
     * @return the module adapter
     */
    public ModuleAdapter modules() {
        if (moduleAdapter == null) {
            synchronized (this) {
                if (moduleAdapter == null) {
                    moduleAdapter = httpClient != null
                        ? new ModuleAdapter.HttpModuleAdapter(baseUrl, httpClient)
                        : new ModuleAdapter.HttpModuleAdapter(baseUrl, config);
                }
            }
        }
        return moduleAdapter;
    }

    /**
     * Get the authentication adapter for login/logout operations.
     *
     * @return the auth adapter
     */
    public AuthAdapter auth() {
        if (authAdapter == null) {
            synchronized (this) {
                if (authAdapter == null) {
                    authAdapter = httpClient != null
                        ? new AuthAdapter.HttpAuthAdapter(baseUrl, httpClient)
                        : new AuthAdapter.HttpAuthAdapter(baseUrl, config);
                }
            }
        }
        return authAdapter;
    }

    /**
     * Get the user adapter for user management operations.
     * Requires admin role.
     *
     * @return the user adapter
     */
    public UserAdapter users() {
        if (userAdapter == null) {
            synchronized (this) {
                if (userAdapter == null) {
                    userAdapter = httpClient != null
                        ? new UserAdapter.HttpUserAdapter(baseUrl, httpClient)
                        : new UserAdapter.HttpUserAdapter(baseUrl, config);
                }
            }
        }
        return userAdapter;
    }

    /**
     * Get the role adapter for role management operations.
     * Read operations require any authenticated user, write operations require admin.
     *
     * @return the role adapter
     */
    public RoleAdapter roles() {
        if (roleAdapter == null) {
            synchronized (this) {
                if (roleAdapter == null) {
                    roleAdapter = httpClient != null
                        ? new RoleAdapter.HttpRoleAdapter(baseUrl, httpClient)
                        : new RoleAdapter.HttpRoleAdapter(baseUrl, config);
                }
            }
        }
        return roleAdapter;
    }

    /**
     * Get a scoped client for a specific container with unchecked exceptions.
     *
     * @param containerId the container ID
     * @return a scoped client for container operations
     */
    public ContainerClient container(long containerId) {
        return new ContainerClient(containers().forContainer(containerId), containerId);
    }

    /**
     * Get the base URL this client is connected to.
     *
     * @return the base URL
     */
    public String baseUrl() {
        return baseUrl;
    }

    // ========== Container Operations (Unchecked) ==========

    /**
     * Start creating a new container.
     */
    public ContainerBuilder createContainer() {
        return new ContainerBuilder(this);
    }

    /**
     * List all containers.
     */
    public List<Container> listContainers() {
        try {
            return containers().getAllContainers().stream()
                    .map(r -> new Container(r.id(), r.name(), r.status()))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list containers", e);
        }
    }

    /**
     * Get a container by ID.
     */
    public Optional<Container> getContainer(long containerId) {
        try {
            return containers().getContainer(containerId)
                    .map(r -> new Container(r.id(), r.name(), r.status()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to get container " + containerId, e);
        }
    }

    /**
     * Start a container.
     */
    public Container startContainer(long containerId) {
        try {
            var r = containers().startContainer(containerId);
            return new Container(r.id(), r.name(), r.status());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to start container " + containerId, e);
        }
    }

    /**
     * Stop a container.
     */
    public Container stopContainer(long containerId) {
        try {
            var r = containers().stopContainer(containerId);
            return new Container(r.id(), r.name(), r.status());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to stop container " + containerId, e);
        }
    }

    /**
     * Delete a container.
     */
    public boolean deleteContainer(long containerId) {
        try {
            return containers().deleteContainer(containerId);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete container " + containerId, e);
        }
    }

    // ========== Module Operations (Unchecked) ==========

    /**
     * List all modules (global).
     */
    public List<Module> listModules() {
        try {
            return modules().getAllModules().stream()
                    .map(m -> new Module(m.name(), m.flagComponentName()))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list modules", e);
        }
    }

    /**
     * Get a module by name.
     */
    public Optional<Module> getModule(String moduleName) {
        try {
            return modules().getModule(moduleName)
                    .map(m -> new Module(m.name(), m.flagComponentName()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to get module " + moduleName, e);
        }
    }

    /**
     * Reload modules.
     */
    public void reloadModules() {
        try {
            modules().reloadModules();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to reload modules", e);
        }
    }

    // ========== Snapshot Parsing ==========

    /**
     * Parse a snapshot JSON string into a Snapshot object.
     * Useful when working with container-scoped snapshot responses.
     *
     * @param jsonData the JSON string containing snapshot data
     * @return a Snapshot object with parsed module/component data
     */
    public Snapshot parseSnapshot(String jsonData) {
        return new Snapshot(snapshotParser.parse(jsonData));
    }

    /**
     * Parse snapshot data from a Map (e.g., from HistorySnapshotDto.data()).
     * Useful when working with history snapshots that return parsed data.
     *
     * @param dataMap the map containing snapshot data (module -> component -> values)
     * @return a Snapshot object with parsed module/component data
     */
    public Snapshot parseSnapshotFromMap(Map<String, Object> dataMap) {
        return new Snapshot(snapshotParser.parseFromMap(dataMap));
    }

    // ========== Domain Records ==========

    /** Container information. */
    public record Container(long id, String name, String status) {}

    /** Module information. */
    public record Module(String name, String flagComponentName) {}

    /** Snapshot of ECS state containing module/component data. */
    public record Snapshot(Map<String, Map<String, List<Float>>> data) {
        /** Access a module's data. */
        public ModuleData module(String moduleName) {
            return new ModuleData(data.getOrDefault(moduleName, Map.of()));
        }

        /** Check if a module exists. */
        public boolean hasModule(String moduleName) {
            return data.containsKey(moduleName);
        }

        /** Get all module names. */
        public Set<String> moduleNames() {
            return data.keySet();
        }

        /** Get entity IDs from any module. */
        public List<Float> entityIds() {
            for (Map<String, List<Float>> moduleData : data.values()) {
                List<Float> entityIds = moduleData.get("ENTITY_ID");
                if (entityIds != null && !entityIds.isEmpty()) {
                    return entityIds;
                }
            }
            return List.of();
        }
    }

    /** Module data from a snapshot. */
    public record ModuleData(Map<String, List<Float>> components) {
        /** Get a component's values. */
        public List<Float> component(String componentName) {
            return components.getOrDefault(componentName, List.of());
        }

        /** Check if a component has values. */
        public boolean has(String componentName) {
            List<Float> values = components.get(componentName);
            return values != null && !values.isEmpty();
        }

        /** Get all component names. */
        public Set<String> componentNames() {
            return components.keySet();
        }

        /** Get first value or default. */
        public float first(String componentName, float defaultValue) {
            List<Float> values = components.get(componentName);
            return (values != null && !values.isEmpty()) ? values.get(0) : defaultValue;
        }
    }

    // ========== Fluent Builders ==========

    /** Builder for creating containers. */
    public static class ContainerBuilder {
        private final EngineClient client;
        private String name;
        private final List<String> modules = new ArrayList<>();
        private final List<String> ais = new ArrayList<>();

        ContainerBuilder(EngineClient client) {
            this.client = client;
        }

        public ContainerBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ContainerBuilder withModule(String moduleName) {
            modules.add(moduleName);
            return this;
        }

        public ContainerBuilder withModules(String... moduleNames) {
            modules.addAll(Arrays.asList(moduleNames));
            return this;
        }

        public ContainerBuilder withAI(String aiName) {
            ais.add(aiName);
            return this;
        }

        public ContainerBuilder withAIs(String... aiNames) {
            ais.addAll(Arrays.asList(aiNames));
            return this;
        }

        public Container execute() {
            try {
                var builder = client.containers().create();
                builder.name(name != null ? name : "container-" + System.currentTimeMillis());
                modules.forEach(m -> builder.withModules(m));
                ais.forEach(a -> builder.withAIs(a));
                var response = builder.execute();
                return new Container(response.id(), response.name(), response.status());
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to create container", e);
            }
        }
    }

    private static String normalizeUrl(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    // ========== Container Client (Unchecked Exceptions) ==========

    /**
     * Scoped client for container operations with unchecked exceptions.
     */
    public static class ContainerClient {
        private final ContainerAdapter.ContainerScope scope;
        private final long containerId;

        ContainerClient(ContainerAdapter.ContainerScope scope, long containerId) {
            this.scope = scope;
            this.containerId = containerId;
        }

        /** Get the container ID. */
        public long id() {
            return containerId;
        }

        /** Create a match with modules. */
        public ContainerMatch createMatch(List<String> modules) {
            try {
                ContainerAdapter.MatchResponse response = scope.createMatch(modules);
                return new ContainerMatch(response.id(), response.enabledModules(), response.enabledAIs());
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to create match", e);
            }
        }

        /** Create a match with modules and AIs. */
        public ContainerMatch createMatch(List<String> modules, List<String> ais) {
            try {
                ContainerAdapter.MatchResponse response = scope.createMatch(modules, ais);
                return new ContainerMatch(response.id(), response.enabledModules(), response.enabledAIs());
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to create match", e);
            }
        }

        /** Get all matches in this container. */
        public List<ContainerMatch> listMatches() {
            try {
                return scope.getMatches().stream()
                        .map(r -> new ContainerMatch(r.id(), r.enabledModules(), r.enabledAIs()))
                        .toList();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to list matches", e);
            }
        }

        /** Get a specific match. */
        public Optional<ContainerMatch> getMatch(long matchId) {
            try {
                return scope.getMatch(matchId)
                        .map(r -> new ContainerMatch(r.id(), r.enabledModules(), r.enabledAIs()));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to get match " + matchId, e);
            }
        }

        /** Delete a match. */
        public boolean deleteMatch(long matchId) {
            try {
                return scope.deleteMatch(matchId);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to delete match " + matchId, e);
            }
        }

        /** Submit a command. */
        public void submitCommand(String commandName, Map<String, Object> parameters) {
            try {
                scope.submitCommand(commandName, parameters);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to submit command " + commandName, e);
            }
        }

        /** Advance the container's tick. */
        public long tick() {
            try {
                return scope.tick();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to advance tick", e);
            }
        }

        /** Get the container's current tick. */
        public long currentTick() {
            try {
                return scope.currentTick();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to get current tick", e);
            }
        }

        /** Start auto-advancing ticks. */
        public void play(int intervalMs) {
            try {
                scope.play(intervalMs);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to start play", e);
            }
        }

        /** Stop auto-advancing ticks. */
        public void stopAuto() {
            try {
                scope.stopAuto();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to stop auto", e);
            }
        }

        /** Get a snapshot for a match. */
        public Optional<ContainerSnapshot> getSnapshot(long matchId) {
            try {
                return scope.getSnapshot(matchId)
                        .map(r -> new ContainerSnapshot(r.matchId(), r.tick(), r.data()));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to get snapshot for match " + matchId, e);
            }
        }

        /** Get command builder for a match. */
        public ContainerMatchCommands forMatch(long matchId) {
            return new ContainerMatchCommands(scope.forMatch(matchId), matchId);
        }

        // ==================== RESOURCE OPERATIONS ====================

        /** List all resources in this container. */
        public List<ContainerResource> listResources() {
            try {
                return scope.listResources().stream()
                        .map(r -> new ContainerResource(r.resourceId(), r.resourceName(), r.resourceType()))
                        .toList();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to list resources", e);
            }
        }

        /** Get a specific resource. */
        public Optional<ContainerResource> getResource(long resourceId) {
            try {
                return scope.getResource(resourceId)
                        .map(r -> new ContainerResource(r.resourceId(), r.resourceName(), r.resourceType()));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to get resource " + resourceId, e);
            }
        }

        /** Delete a resource. */
        public boolean deleteResource(long resourceId) {
            try {
                return scope.deleteResource(resourceId);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to delete resource " + resourceId, e);
            }
        }

        /** Upload a resource. */
        public long uploadResource(String resourceName, String resourceType, byte[] data) {
            try {
                return scope.uploadResource(resourceName, resourceType, data);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to upload resource " + resourceName, e);
            }
        }

        /** Start building a resource upload. */
        public ContainerUploadBuilder uploadResource() {
            return new ContainerUploadBuilder(scope.upload());
        }

        // ==================== MODULE OPERATIONS ====================

        /** List all modules in this container. */
        public List<String> listModules() {
            try {
                return scope.listModules();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to list modules", e);
            }
        }

        // ==================== AI OPERATIONS ====================

        /** List all AI in this container. */
        public List<String> listAI() {
            try {
                return scope.listAI();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to list AI", e);
            }
        }

        // ==================== PLAYER OPERATIONS ====================

        /** Create a player (auto-generated ID). */
        public long createPlayer() {
            try {
                return scope.createPlayer(null);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to create player", e);
            }
        }

        /** Create a player with specific ID. */
        public long createPlayer(long playerId) {
            try {
                return scope.createPlayer(playerId);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to create player " + playerId, e);
            }
        }

        /** List all players. */
        public List<Long> listPlayers() {
            try {
                return scope.listPlayers();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to list players", e);
            }
        }

        /** Delete a player. */
        public boolean deletePlayer(long playerId) {
            try {
                return scope.deletePlayer(playerId);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to delete player " + playerId, e);
            }
        }

        // ==================== SESSION OPERATIONS ====================

        /** Connect a player to a match (create session). */
        public void connectSession(long matchId, long playerId) {
            try {
                scope.connectSession(matchId, playerId);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to connect session for player " + playerId, e);
            }
        }

        /** Disconnect a player from a match. */
        public void disconnectSession(long matchId, long playerId) {
            try {
                scope.disconnectSession(matchId, playerId);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to disconnect session for player " + playerId, e);
            }
        }

        /** Join a player to a match. */
        public void joinMatch(long matchId, long playerId) {
            try {
                scope.joinMatch(matchId, playerId);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to join match " + matchId + " for player " + playerId, e);
            }
        }

        // ==================== HISTORY OPERATIONS ====================

        /** Get history summary for a match. */
        public ca.samanthaireland.lightning.engine.api.resource.adapter.dto.MatchHistorySummaryDto getMatchHistorySummary(long matchId) {
            try {
                return scope.getMatchHistorySummary(matchId);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to get history summary for match " + matchId, e);
            }
        }

        /** Get historical snapshots for a match with query parameters. */
        public List<ca.samanthaireland.lightning.engine.api.resource.adapter.dto.HistorySnapshotDto> getHistorySnapshots(
                long matchId, ca.samanthaireland.lightning.engine.api.resource.adapter.dto.HistoryQueryParams params) {
            try {
                return scope.getHistorySnapshots(matchId, params);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to get history snapshots for match " + matchId, e);
            }
        }

        /** Get the latest historical snapshots for a match. */
        public List<ca.samanthaireland.lightning.engine.api.resource.adapter.dto.HistorySnapshotDto> getLatestHistorySnapshots(long matchId, int limit) {
            try {
                return scope.getLatestHistorySnapshots(matchId, limit);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to get latest history snapshots for match " + matchId, e);
            }
        }

        /** Get a specific historical snapshot by tick. */
        public Optional<ca.samanthaireland.lightning.engine.api.resource.adapter.dto.HistorySnapshotDto> getHistorySnapshotAtTick(long matchId, long tick) {
            try {
                return scope.getHistorySnapshotAtTick(matchId, tick);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to get history snapshot for match " + matchId + " at tick " + tick, e);
            }
        }
    }

    /** Resource information in a container. */
    public record ContainerResource(long resourceId, String resourceName, String resourceType) {}

    /** Upload builder with unchecked exceptions. */
    public static class ContainerUploadBuilder {
        private final ContainerAdapter.UploadResourceBuilder delegate;

        ContainerUploadBuilder(ContainerAdapter.UploadResourceBuilder delegate) {
            this.delegate = delegate;
        }

        public ContainerUploadBuilder name(String resourceName) {
            delegate.name(resourceName);
            return this;
        }

        public ContainerUploadBuilder type(String resourceType) {
            delegate.type(resourceType);
            return this;
        }

        public ContainerUploadBuilder data(byte[] data) {
            delegate.data(data);
            return this;
        }

        public long execute() {
            try {
                return delegate.execute();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to upload resource", e);
            }
        }
    }

    /** Match information in a container. */
    public record ContainerMatch(long id, List<String> enabledModules, List<String> enabledAIs) {}

    /** Snapshot in a container. */
    public record ContainerSnapshot(long matchId, long tick, String data) {}

    /** Commands for a specific match in a container with unchecked exceptions. */
    public static class ContainerMatchCommands {
        private final ContainerCommands.MatchCommands delegate;
        private final long matchId;

        ContainerMatchCommands(ContainerCommands.MatchCommands delegate, long matchId) {
            this.delegate = delegate;
            this.matchId = matchId;
        }

        /** Spawn an entity. */
        public ContainerSpawnBuilder spawn() {
            return new ContainerSpawnBuilder(delegate.spawn());
        }

        /** Attach movement to an entity. */
        public ContainerAttachMovementBuilder attachMovement() {
            return new ContainerAttachMovementBuilder(delegate.attachMovement());
        }

        /** Attach sprite to an entity. */
        public ContainerAttachSpriteBuilder attachSprite() {
            return new ContainerAttachSpriteBuilder(delegate.attachSprite());
        }

        /** Create a custom command builder. */
        public ContainerCustomCommandBuilder custom(String commandName) {
            return new ContainerCustomCommandBuilder(delegate.custom(commandName));
        }

        /** Send a custom command. */
        public void send(String commandName, Map<String, Object> payload) {
            try {
                delegate.send(commandName, payload);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to send command " + commandName, e);
            }
        }
    }

    /** Spawn builder with unchecked exceptions. */
    public static class ContainerSpawnBuilder {
        private final ContainerCommands.SpawnBuilder delegate;

        ContainerSpawnBuilder(ContainerCommands.SpawnBuilder delegate) {
            this.delegate = delegate;
        }

        public ContainerSpawnBuilder forPlayer(long playerId) {
            delegate.forPlayer(playerId);
            return this;
        }

        public ContainerSpawnBuilder ofType(long entityType) {
            delegate.ofType(entityType);
            return this;
        }

        public void execute() {
            try {
                delegate.execute();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to spawn entity", e);
            }
        }
    }

    /** Attach movement builder with unchecked exceptions. */
    public static class ContainerAttachMovementBuilder {
        private final ContainerCommands.AttachMovementBuilder delegate;

        ContainerAttachMovementBuilder(ContainerCommands.AttachMovementBuilder delegate) {
            this.delegate = delegate;
        }

        public ContainerAttachMovementBuilder entity(long entityId) {
            delegate.entity(entityId);
            return this;
        }

        public ContainerAttachMovementBuilder position(int x, int y, int z) {
            delegate.position(x, y, z);
            return this;
        }

        public ContainerAttachMovementBuilder velocity(int vx, int vy, int vz) {
            delegate.velocity(vx, vy, vz);
            return this;
        }

        public void execute() {
            try {
                delegate.execute();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to attach movement", e);
            }
        }
    }

    /** Attach sprite builder with unchecked exceptions. */
    public static class ContainerAttachSpriteBuilder {
        private final ContainerCommands.AttachSpriteBuilder delegate;

        ContainerAttachSpriteBuilder(ContainerCommands.AttachSpriteBuilder delegate) {
            this.delegate = delegate;
        }

        public ContainerAttachSpriteBuilder toEntity(long entityId) {
            delegate.toEntity(entityId);
            return this;
        }

        public ContainerAttachSpriteBuilder usingResource(long resourceId) {
            delegate.usingResource(resourceId);
            return this;
        }

        public ContainerAttachSpriteBuilder sized(int width, int height) {
            delegate.sized(width, height);
            return this;
        }

        public ContainerAttachSpriteBuilder visible(boolean visible) {
            delegate.visible(visible);
            return this;
        }

        public void execute() {
            try {
                delegate.execute();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to attach sprite", e);
            }
        }
    }

    /** Custom command builder with unchecked exceptions. */
    public static class ContainerCustomCommandBuilder {
        private final ContainerCommands.CustomCommandBuilder delegate;

        ContainerCustomCommandBuilder(ContainerCommands.CustomCommandBuilder delegate) {
            this.delegate = delegate;
        }

        public ContainerCustomCommandBuilder param(String name, Object value) {
            delegate.param(name, value);
            return this;
        }

        public void execute() {
            try {
                delegate.execute();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to execute custom command", e);
            }
        }
    }

    /**
     * Builder for creating EngineClient instances with custom configuration.
     */
    public static class Builder {
        private String baseUrl;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private HttpClient httpClient;
        private String bearerToken;
        private SnapshotParser snapshotParser;

        private Builder() {}

        /**
         * Set the base URL of the Lightning Engine server.
         *
         * @param baseUrl the base URL (e.g., "http://localhost:8080")
         * @return this builder
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Set the connection timeout for HTTP requests.
         *
         * @param timeout the connection timeout
         * @return this builder
         */
        public Builder connectTimeout(Duration timeout) {
            this.connectTimeout = timeout;
            return this;
        }

        /**
         * Use a custom HttpClient instance.
         * If set, the connectTimeout setting will be ignored.
         *
         * @param httpClient the HttpClient to use
         * @return this builder
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Set a bearer token for authentication.
         * This token will be included in the Authorization header of all requests.
         *
         * @param token the JWT bearer token
         * @return this builder
         */
        public Builder withBearerToken(String token) {
            this.bearerToken = token;
            return this;
        }

        /**
         * Set a custom SnapshotParser for parsing snapshot data.
         * If not set, a default parser will be created using SnapshotParserFactory.
         *
         * @param parser the SnapshotParser to use
         * @return this builder
         */
        public Builder withSnapshotParser(SnapshotParser parser) {
            this.snapshotParser = parser;
            return this;
        }

        /**
         * Build the EngineClient instance.
         *
         * @return a new EngineClient
         * @throws IllegalStateException if baseUrl is not set
         */
        public EngineClient build() {
            if (baseUrl == null || baseUrl.isBlank()) {
                throw new IllegalStateException("baseUrl is required");
            }

            AdapterConfig config = AdapterConfig.withConnectTimeout(connectTimeout);
            if (bearerToken != null) {
                config = config.withBearerToken(bearerToken);
            }

            SnapshotParser parser = snapshotParser != null
                    ? snapshotParser
                    : SnapshotParserFactory.create();

            return new EngineClient(baseUrl, config, httpClient, parser);
        }
    }
}
