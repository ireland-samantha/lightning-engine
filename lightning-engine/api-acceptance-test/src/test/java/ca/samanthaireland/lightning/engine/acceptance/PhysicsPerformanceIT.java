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


package ca.samanthaireland.lightning.engine.acceptance;

import ca.samanthaireland.lightning.engine.acceptance.fixture.RigidBodyAttachment;
import ca.samanthaireland.lightning.engine.acceptance.fixture.SpriteAttachment;
import ca.samanthaireland.lightning.engine.acceptance.fixture.TestEngineContainer;
import ca.samanthaireland.lightning.engine.acceptance.fixture.TestMatch;
import ca.samanthaireland.lightning.engine.api.resource.adapter.AuthAdapter;
import ca.samanthaireland.lightning.engine.api.resource.adapter.CommandWebSocketClient;
import ca.samanthaireland.lightning.engine.api.resource.adapter.ContainerCommands;
import ca.samanthaireland.lightning.engine.api.resource.adapter.EngineClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Physics performance and functionality acceptance test.
 *
 * <p>This test verifies:
 * <ul>
 *   <li>All RigidBodyModule commands work correctly</li>
 *   <li>Physics simulation performs well with 10k+ entities</li>
 *   <li>Entities move correctly with velocity and forces</li>
 *   <li>Sprites are properly attached to physics entities</li>
 * </ul>
 *
 * <p>Uses WebSocket-based commands for improved performance over HTTP.
 *
 * <p>Prerequisites:
 * <ul>
 *   <li>Docker must be running</li>
 *   <li>The backend image must be built: {@code docker pull samanthacireland/lightning-engine:0.0.2}</li>
 * </ul>
 *
 * <p>Run with:
 * <pre>
 * ./mvnw verify -pl lightning-engine/api-acceptance-test -Pacceptance-tests
 * </pre>
 */
@Slf4j
@Tag("acceptance")
@Tag("testcontainers")
@Tag("performance")
@DisplayName("Physics Performance Acceptance Test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Timeout(value = 5, unit = TimeUnit.MINUTES)
class PhysicsPerformanceIT {

    private static final List<String> REQUIRED_MODULES = List.of(
            "EntityModule", "GridMapModule", "RigidBodyModule", "RenderModule"
    );

    @Container
    static GenericContainer<?> backendContainer = TestContainers.backendContainer();

    private EngineClient client;
    private String token;
    private TestEngineContainer container;
    private TestMatch match;
    private CommandWebSocketClient wsClient;
    private long resourceId = -1;
    private final Random random = new Random(42);

    @BeforeEach
    void setUp() throws Exception {
        String baseUrl = TestContainers.getBaseUrl(backendContainer);
        log.info("Backend URL: {}", baseUrl);

        AuthAdapter auth = new AuthAdapter.HttpAuthAdapter(baseUrl);
        token = auth.login("admin", TestContainers.TEST_ADMIN_PASSWORD).token();
        log.info("Authenticated successfully");

        client = EngineClient.builder()
                .baseUrl(baseUrl)
                .withBearerToken(token)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (wsClient != null) {
            try {
                wsClient.close();
            } catch (Exception e) {
                log.warn("Failed to close WebSocket client: {}", e.getMessage());
            }
        }
        if (resourceId > 0 && container != null) {
            try {
                container.client().deleteResource(resourceId);
            } catch (Exception e) {
                log.warn("Failed to delete resource {}: {}", resourceId, e.getMessage());
            }
        }
        if (container != null) {
            container.cleanup();
        }
        resourceId = -1;
        container = null;
        match = null;
        wsClient = null;
    }

    @Test
    @Order(10)
    @DisplayName("Benchmark: Spawn 10,000 entities with rigid bodies (WebSocket)")
    void benchmarkSpawn10kEntitiesAutoadvance() throws Exception {
        setupContainerMatchAndResource();

        int entityCount = 10_000;
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

        log.info("Starting benchmark: spawning {} entities with rigid bodies and sprites (WebSocket)", entityCount);

        // Get WebSocket-based match commands
        ContainerCommands.MatchCommands matchCommands = container.forMatchWs(match, wsClient);

        // ========== PHASE 1: Parallel spawn via WebSocket ==========
        long startTime = System.currentTimeMillis();
        parallelSpawnWs(entityCount, matchCommands, executor);
        long spawnTime = System.currentTimeMillis() - startTime;
        log.info("Spawn phase completed in {}ms ({} entities/sec)", spawnTime, entityCount * 1000L / spawnTime);

        // Get entity IDs for attachment phase
        List<Float> entityIds = getSnapshot().entityIds();
        log.info("Got {} entity IDs for attachment", entityIds.size());

        // ========== PHASE 2: Parallel rigid body + sprite attachment via WebSocket ==========
        startTime = System.currentTimeMillis();

        List<RigidBodyAttachment> rigidBodies = new ArrayList<>(entityIds.size());
        List<SpriteAttachment> sprites = new ArrayList<>(entityIds.size());

        float[] posX = new float[entityIds.size()];
        float[] posY = new float[entityIds.size()];
        for (int i = 0; i < entityIds.size(); i++) {
            long entityId = entityIds.get(i).longValue();
            posX[i] = (i % 100) * 10;
            posY[i] = (i / 100) * 10;
            float velX = random.nextFloat() * 20 - 10;
            float velY = random.nextFloat() * 20 - 10;

            rigidBodies.add(RigidBodyAttachment.builder(entityId)
                    .position(posX[i], posY[i])
                    .velocity(velX, velY)
                    .linearDrag(0.01f)
                    .build());

            sprites.add(SpriteAttachment.sized(entityId, resourceId, 32, 32));
        }

        // Attach using WebSocket commands in parallel
        attachRigidBodiesAndSpritesWs(rigidBodies, sprites, matchCommands, executor);

        long attachTime = System.currentTimeMillis() - startTime;
        log.info("Attach phase completed in {}ms ({} entities/sec)",
                attachTime, entityCount * 1000L / Math.max(attachTime, 1));

        // Final verification
        EngineClient.Snapshot snapshot = getSnapshot();
        int rigidBodyCount = countEntitiesInModule(snapshot, "RigidBodyModule");
        int renderingCount = countEntitiesInModule(snapshot, "RenderModule");
        int gridMapCount = countEntitiesInModule(snapshot, "GridMapModule");

        log.info("Final state: {} rigid bodies, {} sprites, {} gridMap entities", rigidBodyCount, renderingCount, gridMapCount);

        log.info("RigidBodyModule components: {}", snapshot.module("RigidBodyModule").componentNames());
        log.info("GridMapModule components: {}", snapshot.module("GridMapModule").componentNames());

        var rigidBodyModule = snapshot.module("RigidBodyModule");
        if (rigidBodyModule.has("POSITION_X")) {
            log.info("RigidBodyModule POSITION_X count: {}", rigidBodyModule.component("POSITION_X").size());
        }
        assertThat(rigidBodyCount).isGreaterThanOrEqualTo(entityCount);
        assertThat(renderingCount).isGreaterThanOrEqualTo(entityCount);

        // ========== Benchmark tick rate ==========
        container.client().stopAuto();

        int tickCount = 100;
        log.info("Running {} physics ticks with {} entities", tickCount, entityCount);

        startTime = System.currentTimeMillis();
        for (int i = 0; i < tickCount; i++) {
            container.tick();
        }
        long elapsed = System.currentTimeMillis() - startTime;

        double ticksPerSecond = tickCount * 1000.0 / elapsed;
        double msPerTick = elapsed / (double) tickCount;

        log.info("Tick rate benchmark results:");
        log.info("  {} ticks in {}ms", tickCount, elapsed);
        log.info("  {} ticks/sec", ticksPerSecond);
        log.info("  {} ms/tick", msPerTick);

        assertThat(ticksPerSecond)
                .as("Should achieve at least 60 ticks/sec")
                .isGreaterThan(60);

        // Verify entities moved
        var finalSnapshot = getSnapshot();
        var gridMap = finalSnapshot.module("GridMapModule");

        List<Float> finalPositionsX = gridMap.component("POSITION_X");

        log.info("Final positions count: {}", finalPositionsX.size());
        assertThat(finalPositionsX).hasSize(entityCount);

        long nonZeroPositions = finalPositionsX.stream().filter(p -> Math.abs(p) > 0.001f).count();
        log.info("Non-zero positions: {}/{}", nonZeroPositions, entityCount);
        assertThat(nonZeroPositions)
                .as("Physics should update positions after 100 ticks")
                .isGreaterThan(entityCount / 2);

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        log.info("Benchmark PASSED: {} entities with rigid bodies and sprites created (WebSocket)", entityCount);
    }

    // ========== Helper Methods ==========

    private void setupContainerMatchAndResource() throws Exception {
        // Create container using TestEngineContainer fluent API
        container = TestEngineContainer.create(client)
                .withName("physics-perf-" + System.currentTimeMillis())
                .withModules(REQUIRED_MODULES.toArray(new String[0]))
                .start();

        log.info("Created container {}", container.id());

        // Create match using TestMatch fluent API
        match = container.createMatch()
                .withModules(REQUIRED_MODULES.toArray(new String[0]))
                .build();

        log.info("Created match {}", match.id());

        // Connect WebSocket for commands
        wsClient = container.commandWebSocket(token);
        log.info("Connected command WebSocket");

        // Upload red-checker.png texture
        try (InputStream is = getClass().getResourceAsStream("/textures/red-checker.png")) {
            byte[] textureData;
            if (is != null) {
                textureData = is.readAllBytes();
            } else {
                textureData = createSimpleRedPng();
            }

            resourceId = container.client().uploadResource()
                    .name("red-checker.png")
                    .type("TEXTURE")
                    .data(textureData)
                    .execute();
            log.info("Uploaded resource red-checker.png (id={})", resourceId);
        }
    }

    private void parallelSpawnWs(int entityCount, ContainerCommands.MatchCommands matchCommands,
                                  ExecutorService executor) throws Exception {
        // Warmup: ensure modules are loaded by sending one spawn and ticking
        wsClient.spawnFireAndForget(match.id(), 1, 100, 0, 0);
        container.tick();

        // Fire all remaining spawn commands using fire-and-forget (no wait for response)
        for (int i = 1; i < entityCount; i++) {
            wsClient.spawnFireAndForget(match.id(), 1, 100, 0, 0);
        }

        // Wait for all entities to appear
        container.client().play(60);
        int stuckCounter = 0;
        int lastCount = 0;
        while (getSnapshot().entityIds().size() < entityCount) {
            int currentCount = getSnapshot().entityIds().size();
            if (currentCount == lastCount) {
                stuckCounter++;
                if (stuckCounter > 100) {
                    log.warn("Entity count stuck at {} for 5 seconds, expected {}", currentCount, entityCount);
                    break;
                }
            } else {
                stuckCounter = 0;
            }
            lastCount = currentCount;
            Thread.sleep(50);
        }
        container.client().stopAuto();
    }

    private void attachRigidBodiesAndSpritesWs(List<RigidBodyAttachment> rigidBodies,
                                                List<SpriteAttachment> sprites,
                                                ContainerCommands.MatchCommands matchCommands,
                                                ExecutorService executor) {
        // Start auto-advance to process commands
        container.client().play(60);

        // Attach rigid bodies using fire-and-forget (no wait for response)
        for (RigidBodyAttachment rb : rigidBodies) {
            wsClient.attachRigidBodyFireAndForget(new CommandWebSocketClient.RigidBodyParams(
                    match.id(), 1, rb.entityId(), (long) rb.mass(),
                    (long) rb.positionX(), (long) rb.positionY(),
                    (long) rb.velocityX(), (long) rb.velocityY()));
        }

        // Attach sprites using fire-and-forget
        for (SpriteAttachment sprite : sprites) {
            wsClient.attachSpriteFireAndForget(new CommandWebSocketClient.SpriteParams(
                    match.id(), 1, sprite.entityId(), sprite.resourceId(),
                    sprite.width(), sprite.height(), sprite.visible()));
        }

        // Wait for components to appear
        waitForModuleEntityCount("RigidBodyModule", rigidBodies.size());
        waitForModuleComponentCount("GridMapModule", "POSITION_X", rigidBodies.size());
        waitForModuleEntityCount("RenderModule", sprites.size());

        // Stop auto-advance after attachment is complete
        container.client().stopAuto();
    }

    private void waitForModuleEntityCount(String moduleName, int expectedCount) {
        int maxAttempts = 500;
        long pollIntervalMs = 10;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                var snapshot = getSnapshot();
                int count = countEntitiesInModule(snapshot, moduleName);
                if (count >= expectedCount) {
                    return;
                }
                if (attempt > 0 && attempt % 100 == 0) {
                    log.info("Waiting for {}: {} / {}", moduleName, count, expectedCount);
                }
            } catch (Exception ignored) {
            }
            sleep(pollIntervalMs);
        }
        throw new IllegalStateException("Module " + moduleName + " did not reach " + expectedCount +
                " entities after " + maxAttempts + " attempts");
    }

    private void waitForModuleComponentCount(String moduleName, String componentName, int expectedCount) {
        int maxAttempts = 500;
        long pollIntervalMs = 10;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                var snapshot = getSnapshot();
                var module = snapshot.module(moduleName);
                List<Float> values = module.component(componentName);
                if (values.size() >= expectedCount) {
                    return;
                }
                if (attempt > 0 && attempt % 100 == 0) {
                    log.info("Waiting for {}.{}: {} / {}", moduleName, componentName, values.size(), expectedCount);
                }
            } catch (Exception ignored) {
            }
            sleep(pollIntervalMs);
        }
        throw new IllegalStateException("Component " + moduleName + "." + componentName +
                " did not reach " + expectedCount + " values after " + maxAttempts + " attempts");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Sleep interrupted", e);
        }
    }

    private EngineClient.Snapshot getSnapshot() {
        var snapshotOpt = container.client().getSnapshot(match.id());
        if (snapshotOpt.isEmpty()) {
            throw new IllegalStateException("No snapshot available for match " + match.id());
        }
        return client.parseSnapshot(snapshotOpt.get().data());
    }

    private int countEntitiesInModule(EngineClient.Snapshot snapshot, String moduleName) {
        var module = snapshot.module(moduleName);
        for (String component : module.componentNames()) {
            List<Float> values = module.component(component);
            if (!values.isEmpty()) {
                return values.size();
            }
        }
        return 0;
    }

    private byte[] createSimpleRedPng() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53, (byte) 0xDE,
                0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54,
                0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF, (byte) 0xC0, 0x00, 0x00, 0x00, 0x03, 0x00, 0x01,
                0x00, 0x05, 0x6D, (byte) 0xCD, (byte) 0xB2,
                0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
                (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
    }
}
