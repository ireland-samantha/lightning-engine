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


package ca.samanthaireland.game.engine.internal.orchestrator;

import ca.samanthaireland.lightning.engine.api.resource.adapter.ModuleAdapter;
import ca.samanthaireland.game.domain.ControlSystem;
import ca.samanthaireland.game.domain.DomainObjectRegistry;
import ca.samanthaireland.game.domain.GameScene;
import ca.samanthaireland.game.domain.SnapshotObserver;
import ca.samanthaireland.game.domain.Sprite;
import ca.samanthaireland.game.backend.installation.GameFactory;
import ca.samanthaireland.game.orchestrator.*;
import ca.samanthaireland.game.orchestrator.resource.ResourceProvider;
import ca.samanthaireland.game.renderering.GameRenderer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameOrchestratorImpl")
class GameOrchestratorImplTest {

    private MockMatchOperations matchOperations;
    private MockModuleAdapter moduleAdapter;
    private MockResourceProvider resourceProvider;
    private MockAIOperations aiOperations;
    private MockSimulationOperations simulationOperations;
    private MockSnapshotSubscriber snapshotSubscriber;
    private MockGameRenderer gameRenderer;
    private GameOrchestratorImpl orchestrator;
    private Path tempCacheDir;

    @BeforeEach
    void setUp() throws IOException {
        DomainObjectRegistry.getInstance().clear();
        matchOperations = new MockMatchOperations();
        moduleAdapter = new MockModuleAdapter();
        resourceProvider = new MockResourceProvider();
        aiOperations = new MockAIOperations();
        simulationOperations = new MockSimulationOperations();
        snapshotSubscriber = new MockSnapshotSubscriber();
        gameRenderer = new MockGameRenderer();
        tempCacheDir = Files.createTempDirectory("game-cache-test");
        orchestrator = new GameOrchestratorImpl(
                matchOperations,
                moduleAdapter,
                resourceProvider,
                aiOperations,
                simulationOperations,
                snapshotSubscriber,
                new SnapshotObserver(),
                gameRenderer
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        orchestrator.shutdown();
        if (tempCacheDir != null && Files.exists(tempCacheDir)) {
            try (var paths = Files.walk(tempCacheDir)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                // ignore
                            }
                        });
            }
        }
    }

    @Nested
    @DisplayName("installGame")
    class InstallGame {

        @Test
        @DisplayName("should upload resources from factory")
        void uploadResourcesFromFactory() {
            var factory = new MockGameFactory();
            factory.resources.add(new GameFactory.GameResource("texture.png", "TEXTURE", new byte[]{1, 2, 3}, "sprite.png"));

            orchestrator.installGame(factory);

            assertThat(orchestrator.isGameInstalled(factory)).isTrue();
            assertThat(resourceProvider.uploadedResources).hasSize(1);
            assertThat(resourceProvider.uploadedResources.get(0).name).isEqualTo("texture.png");
        }

        @Test
        @DisplayName("should upload module JARs from factory")
        void uploadModuleJars() {
            var factory = new MockGameFactory();
            factory.moduleJars.put("TestModule", new byte[]{1, 2, 3});

            orchestrator.installGame(factory);

            assertThat(moduleAdapter.uploadedModules).hasSize(1);
            assertThat(moduleAdapter.uploadedModules.get(0).fileName).contains("TestModule");
        }

        @Test
        @DisplayName("should upload AI JAR when provided")
        void uploadAIJar() {
            var factory = new MockGameFactory();
            factory.aiJar = new byte[]{1, 2, 3};
            factory.aiName = "TestAI";

            orchestrator.installGame(factory);

            assertThat(aiOperations.uploadedAIs).hasSize(1);
            assertThat(aiOperations.uploadedAIs.get(0).fileName).contains("TestAI");
        }

        @Test
        @DisplayName("should not upload AI JAR if already installed")
        void skipUploadAIIfInstalled() {
            var factory = new MockGameFactory();
            factory.aiJar = new byte[]{1, 2, 3};
            factory.aiName = "TestAI";
            aiOperations.installedAIs.add("TestAI");

            orchestrator.installGame(factory);

            assertThat(aiOperations.uploadedAIs).isEmpty();
        }

        @Test
        @DisplayName("should track resource ID mapping")
        void trackResourceMapping() {
            var factory = new MockGameFactory();
            factory.resources.add(new GameFactory.GameResource("texture.png", "TEXTURE", new byte[]{1}, "sprites/texture.png"));
            resourceProvider.nextResourceId = 42L;

            orchestrator.installGame(factory);

            Map<String, Long> mapping = orchestrator.getResourceIdMapping(factory);
            assertThat(mapping).containsEntry("sprites/texture.png", 42L);
        }
    }

    @Nested
    @DisplayName("startGame")
    class StartGame {

        @Test
        @DisplayName("should create match with required modules")
        void createMatchWithModules() {
            var factory = new MockGameFactory();
            factory.requiredModules = List.of("EntityModule", "RenderingModule");

            orchestrator.startGame(factory);

            assertThat(matchOperations.createdMatches).hasSize(1);
            assertThat(matchOperations.createdMatches.get(0).modules).containsExactly("EntityModule", "RenderingModule");
        }

        @Test
        @DisplayName("should subscribe to snapshots")
        void subscribeToSnapshots() {
            var factory = new MockGameFactory();

            orchestrator.startGame(factory);

            assertThat(snapshotSubscriber.subscribedMatchIds).hasSize(1);
        }

        @Test
        @DisplayName("should track game session")
        void trackGameSession() {
            var factory = new MockGameFactory();

            orchestrator.startGame(factory);

            assertThat(orchestrator.isGameRunning(factory)).isTrue();
            GameSession session = orchestrator.getSession(factory);
            assertThat(session).isNotNull();
            assertThat(session.matchId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("stopGame")
    class StopGame {

        @Test
        @DisplayName("should unsubscribe from snapshots")
        void unsubscribeFromSnapshots() {
            var factory = new MockGameFactory();
            orchestrator.startGame(factory);
            AtomicBoolean unsubscribed = snapshotSubscriber.lastUnsubscribe;

            orchestrator.stopGame(factory);

            assertThat(unsubscribed.get()).isTrue();
        }

        @Test
        @DisplayName("should delete match")
        void deleteMatch() {
            var factory = new MockGameFactory();
            orchestrator.startGame(factory);

            orchestrator.stopGame(factory);

            assertThat(matchOperations.deletedMatchIds).contains(1L);
        }

        @Test
        @DisplayName("should stop renderer")
        void stopRenderer() {
            var factory = new MockGameFactory();
            orchestrator.startGame(factory);

            orchestrator.stopGame(factory);

            assertThat(gameRenderer.stopped).isTrue();
        }

        @Test
        @DisplayName("should remove session tracking")
        void removeSessionTracking() {
            var factory = new MockGameFactory();
            orchestrator.startGame(factory);

            orchestrator.stopGame(factory);

            assertThat(orchestrator.isGameRunning(factory)).isFalse();
            assertThat(orchestrator.getSession(factory)).isNull();
        }
    }

    @Nested
    @DisplayName("domainPropertyUpdates")
    class DomainPropertyUpdates {

        @Test
        @DisplayName("should register property update")
        void registerPropertyUpdate() {
            var factory = new MockGameFactory();
            orchestrator.startGame(factory);

            AtomicReference<Float> valueRef = new AtomicReference<>();
            DomainPropertyUpdate update = new DomainPropertyUpdate(
                    "TestModule.POSITION_X", 1L, valueRef::set);

            orchestrator.registerDomainPropertyUpdate(update);

            snapshotSubscriber.triggerSnapshot(Map.of(
                    "TestModule", Map.of(
                            "ENTITY_ID", List.of(1.0f),
                            "POSITION_X", List.of(100.0f)
                    )
            ));

            assertThat(valueRef.get()).isEqualTo(100.0f);
        }

        @Test
        @DisplayName("should unregister property update")
        void unregisterPropertyUpdate() {
            var factory = new MockGameFactory();
            orchestrator.startGame(factory);

            AtomicReference<Float> valueRef = new AtomicReference<>();
            DomainPropertyUpdate update = new DomainPropertyUpdate(
                    "TestModule.POSITION_X", 1L, valueRef::set);
            orchestrator.registerDomainPropertyUpdate(update);

            orchestrator.unregisterDomainPropertyUpdate(update);

            snapshotSubscriber.triggerSnapshot(Map.of(
                    "TestModule", Map.of(
                            "ENTITY_ID", List.of(1.0f),
                            "POSITION_X", List.of(100.0f)
                    )
            ));

            assertThat(valueRef.get()).isNull();
        }
    }

    @Nested
    @DisplayName("resourceCaching")
    class ResourceCaching {

        @Test
        @DisplayName("should cache resources from snapshot")
        void cacheResourcesFromSnapshot() {
            var factory = new MockGameFactory();
            orchestrator.startGame(factory);

            snapshotSubscriber.triggerSnapshot(Map.of(
                    "RenderingModule", Map.of(
                            "RESOURCE_ID", List.of(42.0f, 43.0f)
                    )
            ));

            assertThat(resourceProvider.ensuredResourceIds).contains(42L, 43L);
        }

        @Test
        @DisplayName("should ignore non-positive resource IDs")
        void ignoreNonPositiveResourceIds() {
            var factory = new MockGameFactory();
            orchestrator.startGame(factory);

            snapshotSubscriber.triggerSnapshot(Map.of(
                    "RenderingModule", Map.of(
                            "RESOURCE_ID", List.of(0.0f, -1.0f)
                    )
            ));

            assertThat(resourceProvider.ensuredResourceIds).isEmpty();
        }
    }

    @Nested
    @DisplayName("uninstallGame")
    class UninstallGame {

        @Test
        @DisplayName("should delete uploaded resources")
        void deleteUploadedResources() {
            var factory = new MockGameFactory();
            factory.resources.add(new GameFactory.GameResource("texture.png", "TEXTURE", new byte[]{1}, "sprites/texture.png"));
            resourceProvider.nextResourceId = 42L;
            orchestrator.installGame(factory);

            orchestrator.uninstallGame(factory);

            assertThat(resourceProvider.deletedResourceIds).contains(42L);
            assertThat(orchestrator.isGameInstalled(factory)).isFalse();
        }

        @Test
        @DisplayName("should uninstall AI if we installed it")
        void uninstallAI() {
            var factory = new MockGameFactory();
            factory.aiJar = new byte[]{1, 2, 3};
            factory.aiName = "TestAI";
            orchestrator.installGame(factory);

            orchestrator.uninstallGame(factory);

            assertThat(aiOperations.uninstalledAIs).contains("TestAI");
        }
    }

    // ========== Mock Implementations ==========

    static class MockMatchOperations implements MatchOperations {
        List<MatchCreation> createdMatches = new ArrayList<>();
        List<Long> deletedMatchIds = new ArrayList<>();
        long nextMatchId = 1L;

        @Override
        public MatchResponse createMatch(List<String> modules, List<String> ais) throws IOException {
            createdMatches.add(new MatchCreation(modules, ais));
            return new MatchResponse(nextMatchId++, modules, ais);
        }

        @Override
        public boolean deleteMatch(long matchId) throws IOException {
            deletedMatchIds.add(matchId);
            return true;
        }

        record MatchCreation(List<String> modules, List<String> ais) {}
    }

    static class MockModuleAdapter implements ModuleAdapter {
        List<ModuleUpload> uploadedModules = new ArrayList<>();

        @Override
        public List<ModuleResponse> uploadModule(String fileName, byte[] jarData) throws IOException {
            uploadedModules.add(new ModuleUpload(fileName, jarData));
            return List.of(new ModuleResponse(fileName.replace(".jar", ""), fileName.replace(".jar", "") + "Flag", 0));
        }

        @Override
        public List<ModuleResponse> getAllModules() throws IOException {
            return List.of();
        }

        @Override
        public Optional<ModuleResponse> getModule(String moduleName) throws IOException {
            return Optional.empty();
        }

        @Override
        public List<ModuleResponse> reloadModules() throws IOException {
            return List.of();
        }

        @Override
        public boolean uninstallModule(String moduleName) throws IOException {
            return true;
        }

        record ModuleUpload(String fileName, byte[] data) {}
    }

    static class MockResourceProvider implements ResourceProvider {
        List<ResourceUpload> uploadedResources = new ArrayList<>();
        List<Long> deletedResourceIds = new ArrayList<>();
        List<Long> ensuredResourceIds = new ArrayList<>();
        long nextResourceId = 1L;

        @Override
        public long uploadResource(String name, String type, byte[] data) throws IOException {
            uploadedResources.add(new ResourceUpload(name, type, data));
            return nextResourceId++;
        }

        @Override
        public Optional<ResourceData> downloadResource(long resourceId) throws IOException {
            return Optional.empty();
        }

        @Override
        public void deleteResource(long resourceId) throws IOException {
            deletedResourceIds.add(resourceId);
        }

        @Override
        public void ensureResource(long resourceId) {
            ensuredResourceIds.add(resourceId);
        }

        @Override
        public Optional<Path> getLocalPath(long resourceId) {
            return Optional.empty();
        }

        @Override
        public boolean isAvailableLocally(long resourceId) {
            return false;
        }

        @Override
        public void setDownloadListener(Consumer<ResourceDownloadEvent> listener) {}

        @Override
        public void clearCache() {}

        @Override
        public Optional<Path> getCacheDirectory() {
            return Optional.empty();
        }

        @Override
        public void close() {}

        record ResourceUpload(String name, String type, byte[] data) {}
    }

    static class MockAIOperations implements AIOperations {
        List<String> installedAIs = new ArrayList<>();
        List<AIUpload> uploadedAIs = new ArrayList<>();
        List<String> uninstalledAIs = new ArrayList<>();

        @Override
        public boolean hasAI(String aiName) throws IOException {
            return installedAIs.contains(aiName);
        }

        @Override
        public List<String> listAI() throws IOException {
            return new ArrayList<>(installedAIs);
        }

        @Override
        public void uploadAI(String fileName, byte[] data) throws IOException {
            uploadedAIs.add(new AIUpload(fileName, data));
            String name = fileName.replace(".jar", "");
            installedAIs.add(name);
        }

        @Override
        public void uninstallAI(String aiName) throws IOException {
            uninstalledAIs.add(aiName);
            installedAIs.remove(aiName);
        }

        record AIUpload(String fileName, byte[] data) {}
    }

    static class MockSimulationOperations implements SimulationOperations {
        long currentTick = 0;

        @Override
        public long tick() throws IOException {
            return ++currentTick;
        }

        @Override
        public long currentTick() throws IOException {
            return currentTick;
        }

        @Override
        public void play(int intervalMs) throws IOException {}

        @Override
        public void stop() throws IOException {}
    }

    static class MockSnapshotSubscriber implements SnapshotSubscriber {
        List<Long> subscribedMatchIds = new ArrayList<>();
        Consumer<Map<String, Map<String, List<Float>>>> currentCallback;
        AtomicBoolean lastUnsubscribe = new AtomicBoolean(false);

        @Override
        public Runnable subscribe(long matchId, Consumer<Map<String, Map<String, List<Float>>>> callback) {
            subscribedMatchIds.add(matchId);
            currentCallback = callback;
            lastUnsubscribe = new AtomicBoolean(false);
            final AtomicBoolean unsubFlag = lastUnsubscribe;
            return () -> unsubFlag.set(true);
        }

        void triggerSnapshot(Map<String, Map<String, List<Float>>> data) {
            if (currentCallback != null && !lastUnsubscribe.get()) {
                currentCallback.accept(data);
            }
        }
    }

    static class MockGameRenderer implements GameRenderer {
        boolean stopped = false;

        @Override
        public void setControlSystem(ControlSystem controlSystem) {}

        @Override
        public void setSpriteMapper(SpriteSnapshotMapper mapper) {}

        @Override
        public void renderSnapshot(Object snapshot) {}

        @Override
        public void renderSprites(List<Sprite> sprites) {}

        @Override
        public void start(Runnable onUpdate) {}

        @Override
        public void startAsync(Runnable onUpdate) {}

        @Override
        public void runFrames(int frames, Runnable onUpdate) {}

        @Override
        public void stop() {
            stopped = true;
        }

        @Override
        public boolean isRunning() {
            return !stopped;
        }

        @Override
        public int getWidth() {
            return 800;
        }

        @Override
        public int getHeight() {
            return 600;
        }

        @Override
        public void setOnError(Consumer<Exception> handler) {}

        @Override
        public void dispose() {}
    }

    static class MockGameFactory implements GameFactory {
        List<GameResource> resources = new ArrayList<>();
        Map<String, byte[]> moduleJars = new HashMap<>();
        List<String> requiredModules = List.of("EntityModule");
        byte[] aiJar = null;
        String aiName = null;

        @Override
        public void attachScene(GameScene scene) {}

        @Override
        public List<GameResource> getResources() {
            return resources;
        }

        @Override
        public Map<String, byte[]> getModuleJars() {
            return moduleJars;
        }

        @Override
        public List<String> getRequiredModules() {
            return requiredModules;
        }

        @Override
        public Optional<byte[]> getAIJar() {
            return Optional.ofNullable(aiJar);
        }

        @Override
        public Optional<String> getAIName() {
            return Optional.ofNullable(aiName);
        }
    }
}
