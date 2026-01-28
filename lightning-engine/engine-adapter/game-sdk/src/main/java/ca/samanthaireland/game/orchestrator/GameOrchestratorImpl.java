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


package ca.samanthaireland.game.orchestrator;

import ca.samanthaireland.lightning.engine.api.resource.adapter.ContainerAdapter;
import ca.samanthaireland.lightning.engine.api.resource.adapter.ModuleAdapter;
import ca.samanthaireland.game.domain.SnapshotObserver;
import ca.samanthaireland.game.backend.installation.GameFactory;
import ca.samanthaireland.game.orchestrator.resource.CachingResourceProvider;
import ca.samanthaireland.game.orchestrator.resource.ResourceProvider;
import ca.samanthaireland.game.orchestrator.resource.ResourceProviderAdapter;
import ca.samanthaireland.game.renderering.GameRenderer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Implementation of GameOrchestrator that coordinates between
 * the game engine, backend server, and domain objects.
 *
 * <p>This orchestrator:
 * <ul>
 *   <li>Installs games by uploading resources and modules to the server</li>
 *   <li>Starts games by creating matches and subscribing to snapshots</li>
 *   <li>Updates domain objects via {@link SnapshotObserver}</li>
 *   <li>Processes individual property watches</li>
 *   <li>Caches resources (textures) locally for rendering</li>
 * </ul>
 *
 * <p>Follows SOLID principles:
 * <ul>
 *   <li>Single Responsibility: Orchestrates game lifecycle</li>
 *   <li>Open/Closed: New operations can be added via interfaces</li>
 *   <li>Liskov Substitution: All dependencies are interfaces</li>
 *   <li>Interface Segregation: Uses focused operation interfaces</li>
 *   <li>Dependency Inversion: Depends on abstractions, not concretions</li>
 * </ul>
 */
@Slf4j
public final class GameOrchestratorImpl implements GameOrchestrator {

    private static final String DEFAULT_CACHE_DIR = ".game-cache";
    private static final String RESOURCE_ID_COMPONENT = "RESOURCE_ID";

    private final MatchOperations matchOperations;
    private final ModuleAdapter moduleAdapter;
    private final ResourceProvider resourceProvider;
    private final AIOperations aiOperations;
    private final SimulationOperations simulationOperations;
    private final SnapshotSubscriber snapshotSubscriber;
    private final SnapshotObserver snapshotObserver;
    private final GameRenderer gameRenderer;

    private final Map<GameFactory, GameSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<GameFactory, InstallationInfo> installedGames = new ConcurrentHashMap<>();
    private final List<DomainPropertyUpdate> watches = new CopyOnWriteArrayList<>();

    /**
     * Create a new orchestrator using a container scope.
     * <p>This is the preferred constructor for container-scoped operations.
     *
     * @param containerScope     the container scope for operations
     * @param moduleAdapter      adapter for module operations
     * @param snapshotSubscriber subscriber for real-time snapshots
     * @param gameRenderer       renderer for displaying the game
     */
    public GameOrchestratorImpl(
            ContainerAdapter.ContainerScope containerScope,
            ModuleAdapter moduleAdapter,
            SnapshotSubscriber snapshotSubscriber,
            GameRenderer gameRenderer) {
        this(containerScope, moduleAdapter, snapshotSubscriber, new SnapshotObserver(), gameRenderer,
                Path.of(System.getProperty("user.home"), DEFAULT_CACHE_DIR));
    }

    /**
     * Create a new orchestrator using a container scope with custom observer.
     *
     * @param containerScope     the container scope for operations
     * @param moduleAdapter      adapter for module operations
     * @param snapshotSubscriber subscriber for real-time snapshots
     * @param snapshotObserver   observer for domain object updates
     * @param gameRenderer       renderer for displaying the game
     * @param cacheDirectory     directory for caching downloaded resources
     */
    public GameOrchestratorImpl(
            ContainerAdapter.ContainerScope containerScope,
            ModuleAdapter moduleAdapter,
            SnapshotSubscriber snapshotSubscriber,
            SnapshotObserver snapshotObserver,
            GameRenderer gameRenderer,
            Path cacheDirectory) {
        this(
                new ContainerMatchOperations(containerScope),
                moduleAdapter,
                new CachingResourceProvider(new ResourceProviderAdapter(containerScope), cacheDirectory),
                new ContainerAIOperations(containerScope),
                new ContainerSimulationOperations(containerScope),
                snapshotSubscriber,
                snapshotObserver,
                gameRenderer
        );
    }

    /**
     * Create a new orchestrator with explicit dependencies.
     * <p>Use this constructor for full control over all dependencies.
     * This follows Dependency Inversion Principle - all dependencies are interfaces.
     *
     * @param matchOperations      operations for match management
     * @param moduleAdapter        adapter for module operations
     * @param resourceProvider     provider for resource operations
     * @param aiOperations         operations for AI management
     * @param simulationOperations operations for simulation control
     * @param snapshotSubscriber   subscriber for real-time snapshots
     * @param snapshotObserver     observer for domain object updates
     * @param gameRenderer         renderer for displaying the game
     */
    public GameOrchestratorImpl(
            MatchOperations matchOperations,
            ModuleAdapter moduleAdapter,
            ResourceProvider resourceProvider,
            AIOperations aiOperations,
            SimulationOperations simulationOperations,
            SnapshotSubscriber snapshotSubscriber,
            SnapshotObserver snapshotObserver,
            GameRenderer gameRenderer) {
        this.matchOperations = matchOperations;
        this.moduleAdapter = moduleAdapter;
        this.resourceProvider = resourceProvider;
        this.aiOperations = aiOperations;
        this.simulationOperations = simulationOperations;
        this.snapshotSubscriber = snapshotSubscriber;
        this.snapshotObserver = snapshotObserver;
        this.gameRenderer = gameRenderer;
    }

    @Override
    public void installGame(GameFactory factory) {
        try {
            Map<String, Long> uploadedResourceIds = new HashMap<>();
            String installedAI = null;
            List<String> uploadedModules = new ArrayList<>();

            // 1) Upload module JARs if provided
            Map<String, byte[]> moduleJars = factory.getModuleJars();
            for (Map.Entry<String, byte[]> entry : moduleJars.entrySet()) {
                String moduleName = entry.getKey();
                byte[] jarData = entry.getValue();
                log.info("Uploading module JAR: {} ({} bytes)", moduleName, jarData.length);
                var response = moduleAdapter.uploadModule(moduleName + ".jar", jarData);
                log.debug("Module upload response: {}", response);
                uploadedModules.add(moduleName);
            }

            // 2) Upload resources (sprites/textures) to the server
            List<GameFactory.GameResource> resources = factory.getResources();
            for (GameFactory.GameResource resource : resources) {
                long resourceId = resourceProvider.uploadResource(
                        resource.name(),
                        resource.type(),
                        resource.data()
                );
                uploadedResourceIds.put(resource.texturePath(), resourceId);
            }

            // 3) Upload AI JAR if bundled with the factory
            Optional<byte[]> aiJar = factory.getAIJar();
            Optional<String> aiName = factory.getAIName();
            if (aiJar.isPresent() && aiName.isPresent()) {
                String name = aiName.get();
                // Only upload if not already installed
                if (!aiOperations.hasAI(name)) {
                    aiOperations.uploadAI(name + ".jar", aiJar.get());
                    installedAI = name;
                }
            }

            // 4) Track installation for potential cleanup
            InstallationInfo info = new InstallationInfo(
                    uploadedResourceIds,
                    installedAI
            );
            installedGames.put(factory, info);

        } catch (IOException e) {
            throw new RuntimeException("Failed to install game", e);
        }
    }

    /**
     * Uninstall a previously installed game.
     * This removes uploaded resources and AIs from the server.
     *
     * @param factory the game factory to uninstall
     */
    public void uninstallGame(GameFactory factory) {
        InstallationInfo info = installedGames.remove(factory);
        if (info == null) {
            return;
        }

        // Delete uploaded resources
        for (Long resourceId : info.uploadedResourceIds().values()) {
            try {
                resourceProvider.deleteResource(resourceId);
            } catch (IOException e) {
                // Log but continue - resource may already be deleted
            }
        }

        // Uninstall AI if we installed it
        if (info.installedAI() != null) {
            try {
                aiOperations.uninstallAI(info.installedAI());
            } catch (IOException e) {
                // Log but continue
            }
        }
    }

    /**
     * Check if a game factory is installed.
     *
     * @param factory the game factory
     * @return true if installed
     */
    public boolean isGameInstalled(GameFactory factory) {
        return installedGames.containsKey(factory);
    }

    /**
     * Get the resource ID mapping for an installed game.
     * Maps texture paths to server resource IDs.
     *
     * @param factory the game factory
     * @return the resource ID mapping, or null if not installed
     */
    public Map<String, Long> getResourceIdMapping(GameFactory factory) {
        InstallationInfo info = installedGames.get(factory);
        return info != null ? info.uploadedResourceIds() : null;
    }


    // Note: GameRenderer is NOT started here - caller is responsible for
    // starting the renderer on the appropriate thread.
    @Override
    public void startGame(GameFactory factory) {
        try {
            // 1) Create a match with the required modules and AI
            List<String> enabledModules = getRequiredModules(factory);

            List<String> enabledAIs = factory.getAIName()
                    .map(List::of)
                    .orElse(List.of());

            MatchOperations.MatchResponse match = matchOperations.createMatch(enabledModules, enabledAIs);
            if (match == null) {
                throw new IllegalStateException("Failed to create match: server returned null response");
            }

            long matchId = match.id();

            // 2) Subscribe to snapshots and set up SnapshotObserver for domain objects
            Runnable unsubscribe = snapshotSubscriber.subscribe(matchId, this::onSnapshotReceived);

            // Track the session
            GameSession session = new GameSession(matchId, unsubscribe);
            activeSessions.put(factory, session);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start game", e);
        }
    }

    @Override
    public void stopGame(GameFactory factory) {
        GameSession session = activeSessions.remove(factory);
        if (session == null) {
            return;
        }

        // 1) Unsubscribe from snapshots
        session.unsubscribe().run();

        // 2) Delete the match
        try {
            matchOperations.deleteMatch(session.matchId());
        } catch (IOException e) {
            // Log but don't fail - match may already be deleted
        }

        // 3) Stop the renderer and free resources
        if (gameRenderer != null) {
            gameRenderer.stop();
        }
    }

    @Override
    public void registerDomainPropertyUpdate(DomainPropertyUpdate update) {
        watches.add(update);
    }

    @Override
    public void unregisterDomainPropertyUpdate(DomainPropertyUpdate update) {
        watches.remove(update);
    }

    /**
     * Get the current active session for a factory.
     *
     * @param factory the game factory
     * @return the session, or null if not active
     */
    public GameSession getSession(GameFactory factory) {
        return activeSessions.get(factory);
    }

    /**
     * Check if a game is currently running.
     *
     * @param factory the game factory
     * @return true if the game is active
     */
    public boolean isGameRunning(GameFactory factory) {
        return activeSessions.containsKey(factory);
    }

    /**
     * Called when a components is received from the server.
     *
     * @param snapshotData the raw components data
     */
    private void onSnapshotReceived(Map<String, Map<String, List<Float>>> snapshotData) {
        // Update domain objects via the observer
        snapshotObserver.onSnapshot(snapshotData);

        // Process individual watches
        processWatches(snapshotData);

        // Extract and cache resources asynchronously
        cacheResourcesFromSnapshot(snapshotData);
    }

    /**
     * Extract and ensure all resources from the snapshot are cached.
     * Delegates to the ResourceProvider for download and caching.
     */
    private void cacheResourcesFromSnapshot(Map<String, Map<String, List<Float>>> snapshotData) {
        if (snapshotData == null) {
            return;
        }

        // Extract all RESOURCE_ID values from all modules
        for (Map<String, List<Float>> moduleData : snapshotData.values()) {
            List<Float> resourceIdList = moduleData.get(RESOURCE_ID_COMPONENT);
            if (resourceIdList != null) {
                for (Float resourceIdFloat : resourceIdList) {
                    if (resourceIdFloat != null && resourceIdFloat > 0) {
                        resourceProvider.ensureResource(resourceIdFloat.longValue());
                    }
                }
            }
        }
    }

    /**
     * Set a listener for resource download events.
     *
     * @param listener the listener to notify on download events
     */
    public void setResourceDownloadListener(Consumer<ResourceDownloadEvent> listener) {
        resourceProvider.setDownloadListener(listener);
    }

    /**
     * Get the local file path for a cached resource.
     *
     * @param resourceId the resource ID
     * @return the path to the cached file, or null if not cached
     */
    public Path getCachedResourcePath(long resourceId) {
        return resourceProvider.getLocalPath(resourceId).orElse(null);
    }

    /**
     * Check if a resource is cached locally.
     *
     * @param resourceId the resource ID
     * @return true if the resource is cached
     */
    public boolean isResourceCached(long resourceId) {
        return resourceProvider.isAvailableLocally(resourceId);
    }

    /**
     * Clear the resource cache.
     */
    public void clearResourceCache() {
        resourceProvider.clearCache();
    }

    /**
     * Get the cache directory path.
     *
     * @return the cache directory, or null if caching is not supported
     */
    public Path getCacheDirectory() {
        return resourceProvider.getCacheDirectory().orElse(null);
    }

    /**
     * Get the underlying resource provider.
     *
     * @return the resource provider
     */
    public ResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    /**
     * Shutdown the orchestrator and release resources.
     */
    @Override
    public void shutdown() {
        resourceProvider.close();
    }

    /**
     * Process all registered property observers against the components.
     * <p>Observers (watches) are notified when their tracked ECS component values change.
     */
    private void processWatches(Map<String, Map<String, List<Float>>> snapshotData) {
        if (snapshotData == null || watches.isEmpty()) {
            return;
        }

        for (DomainPropertyUpdate watch : watches) {
            processWatch(watch, snapshotData);
        }
    }

    /**
     * Process a single watch against the components.
     */
    private void processWatch(DomainPropertyUpdate watch, Map<String, Map<String, List<Float>>> snapshotData) {
        String ecsPath = watch.ecsPath();
        String[] parts = ecsPath.split("\\.", 2);
        if (parts.length != 2) {
            return;
        }

        String moduleName = parts[0];
        String componentName = parts[1];

        Map<String, List<Float>> moduleData = snapshotData.get(moduleName);
        if (moduleData == null) {
            return;
        }

        // Find entity index
        List<Float> entityIds = moduleData.get("ENTITY_ID");
        if (entityIds == null) {
            return;
        }

        int entityIndex = -1;
        float targetId = (float) watch.entityId();
        for (int i = 0; i < entityIds.size(); i++) {
            Float id = entityIds.get(i);
            if (id != null && id == targetId) {
                entityIndex = i;
                break;
            }
        }

        if (entityIndex < 0) {
            return;
        }

        // Get component value
        List<Float> values = moduleData.get(componentName);
        if (values == null || entityIndex >= values.size()) {
            return;
        }

        Float value = values.get(entityIndex);
        if (value != null) {
            watch.callback().accept(value);
        }
    }

    /**
     * Get the list of required module names for a game.
     * Delegates to the factory's getRequiredModules() method.
     */
    private List<String> getRequiredModules(GameFactory factory) {
        return factory.getRequiredModules();
    }
}
