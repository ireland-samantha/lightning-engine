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

package ca.samanthaireland.lightning.engine.acceptance.fixture;

import ca.samanthaireland.lightning.engine.api.resource.adapter.EngineClient;
import ca.samanthaireland.lightning.engine.api.resource.adapter.EngineClient.ContainerClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Utility class for spawning entities in acceptance tests with proper polling/awaitability.
 *
 * <p>This class provides static methods to spawn entities and wait for them to appear
 * in snapshots, avoiding race conditions that can cause intermittent test failures.
 *
 * <p>Usage:
 * <pre>{@code
 * long entityId = EntitySpawner.spawnEntity(client, container, matchId);
 *
 * long entityWithRigidBody = EntitySpawner.spawnEntityWithRigidBody(
 *     client, container, matchId, 100.0f, 200.0f);
 * }</pre>
 */
public final class EntitySpawner {

    private static final int DEFAULT_MAX_ATTEMPTS = 20;
    private static final long DEFAULT_POLL_INTERVAL_MS = 100;

    private EntitySpawner() {
        // Utility class
    }

    /**
     * Spawn an entity and wait for it to appear in the snapshot.
     *
     * @param client the EngineClient for parsing snapshots
     * @param container the container client to spawn in
     * @param matchId the match ID to spawn the entity in
     * @return the spawned entity ID
     * @throws IOException if spawning fails or entity doesn't appear
     */
    public static long spawnEntity(EngineClient client, ContainerClient container, long matchId) throws IOException {
        return spawnEntity(client, container, matchId, 1, 100);
    }

    /**
     * Spawn an entity with specified player and type, and wait for it to appear in the snapshot.
     *
     * @param client the EngineClient for parsing snapshots
     * @param container the container client to spawn in
     * @param matchId the match ID to spawn the entity in
     * @param playerId the player ID that owns the entity
     * @param entityType the entity type
     * @return the spawned entity ID
     * @throws IOException if spawning fails or entity doesn't appear
     */
    public static long spawnEntity(EngineClient client, ContainerClient container, long matchId,
                                   long playerId, long entityType) throws IOException {
        // Get entity count before spawn
        int beforeCount = getEntityCount(client, container, matchId);

        container.forMatch(matchId).spawn()
                .forPlayer(playerId)
                .ofType(entityType)
                .execute();

        // Poll until entity appears in snapshot
        for (int attempt = 0; attempt < DEFAULT_MAX_ATTEMPTS; attempt++) {
            container.tick();
            try {
                var snapshotOpt = container.getSnapshot(matchId);
                if (snapshotOpt.isPresent()) {
                    var snapshot = client.parseSnapshot(snapshotOpt.get().data());
                    List<Float> entityIds = snapshot.entityIds();
                    if (entityIds.size() > beforeCount) {
                        return entityIds.get(entityIds.size() - 1).longValue();
                    }
                }
            } catch (Exception ignored) {
                // Snapshot not ready yet, continue polling
            }

            sleep(DEFAULT_POLL_INTERVAL_MS);
        }
        throw new IllegalStateException("Failed to spawn entity after " + DEFAULT_MAX_ATTEMPTS + " attempts");
    }

    /**
     * Spawn an entity with a rigid body attached at the specified position.
     *
     * @param client the EngineClient for parsing snapshots
     * @param container the container client to spawn in
     * @param matchId the match ID to spawn the entity in
     * @param x the X position
     * @param y the Y position
     * @return the spawned entity ID
     * @throws IOException if spawning fails
     */
    public static long spawnEntityWithRigidBody(EngineClient client, ContainerClient container,
                                                long matchId, float x, float y) throws IOException {
        return spawnEntityWithRigidBody(client, container, matchId, x, y, 1.0f);
    }

    /**
     * Spawn an entity with a rigid body attached at the specified position and mass.
     *
     * @param client the EngineClient for parsing snapshots
     * @param container the container client to spawn in
     * @param matchId the match ID to spawn the entity in
     * @param x the X position
     * @param y the Y position
     * @param mass the mass of the rigid body
     * @return the spawned entity ID
     * @throws IOException if spawning fails
     */
    public static long spawnEntityWithRigidBody(EngineClient client, ContainerClient container,
                                                long matchId, float x, float y, float mass) throws IOException {
        long entityId = spawnEntity(client, container, matchId);

        container.forMatch(matchId).send("attachRigidBody", Map.of(
                "entityId", entityId,
                "positionX", x,
                "positionY", y,
                "mass", mass
        ));

        // Wait for rigid body components to appear in snapshot
        waitForComponent(client, container, matchId, "RigidBodyModule", "MASS");
        // Also wait for position component (set by attachRigidBody via GridMapModule)
        waitForComponent(client, container, matchId, "GridMapModule", "POSITION_X");
        return entityId;
    }

    /**
     * Wait for a container to reach RUNNING status.
     *
     * @param client the EngineClient
     * @param containerId the container ID to wait for
     * @throws IllegalStateException if container doesn't reach RUNNING status
     */
    public static void waitForContainerRunning(EngineClient client, long containerId) {
        for (int i = 0; i < DEFAULT_MAX_ATTEMPTS; i++) {
            try {
                var containerOpt = client.containers().getContainer(containerId);
                if (containerOpt.isPresent() && "RUNNING".equals(containerOpt.get().status())) {
                    return;
                }
            } catch (Exception ignored) {
                // Container not ready yet
            }
            sleep(DEFAULT_POLL_INTERVAL_MS);
        }
        throw new IllegalStateException("Container " + containerId + " did not reach RUNNING status after " + DEFAULT_MAX_ATTEMPTS + " attempts");
    }

    /**
     * Wait for a specific component to appear in the snapshot with non-empty values.
     *
     * @param client the EngineClient for parsing snapshots
     * @param container the container client
     * @param matchId the match ID
     * @param moduleName the module name (e.g., "RigidBodyModule")
     * @param componentName the component name (e.g., "VELOCITY_X")
     * @throws IllegalStateException if component doesn't appear after max attempts
     */
    public static void waitForComponent(EngineClient client, ContainerClient container,
                                        long matchId, String moduleName, String componentName) {
        for (int attempt = 0; attempt < DEFAULT_MAX_ATTEMPTS; attempt++) {
            container.tick();
            try {
                var snapshotOpt = container.getSnapshot(matchId);
                if (snapshotOpt.isPresent()) {
                    var snapshot = client.parseSnapshot(snapshotOpt.get().data());
                    var module = snapshot.module(moduleName);
                    if (module.has(componentName) && !module.component(componentName).isEmpty()) {
                        return;
                    }
                }
            } catch (Exception ignored) {
                // Snapshot not ready yet, continue polling
            }
            sleep(DEFAULT_POLL_INTERVAL_MS);
        }
        throw new IllegalStateException("Component " + moduleName + "." + componentName +
                " did not appear after " + DEFAULT_MAX_ATTEMPTS + " attempts");
    }

    // ========== Bulk Attachment Methods ==========

    /**
     * Attach rigid bodies to multiple entities in parallel, then wait for all to appear.
     *
     * <p>This method sends all attachment commands in parallel using the provided executor,
     * then polls until all rigid bodies appear in the snapshot.
     *
     * @param container the container client
     * @param matchId the match ID
     * @param attachments the rigid body attachments to apply
     * @param executor the executor for parallel command execution
     */
    public static void attachRigidBodies(ContainerClient container, long matchId,
                                         List<RigidBodyAttachment> attachments, ExecutorService executor) {
        if (attachments.isEmpty()) {
            return;
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>(attachments.size());
        for (RigidBodyAttachment attachment : attachments) {
            futures.add(CompletableFuture.runAsync(() ->
                    container.forMatch(matchId).custom("attachRigidBody")
                            .param("entityId", attachment.entityId())
                            .param("positionX", attachment.positionX())
                            .param("positionY", attachment.positionY())
                            .param("velocityX", attachment.velocityX())
                            .param("velocityY", attachment.velocityY())
                            .param("mass", attachment.mass())
                            .param("linearDrag", attachment.linearDrag())
                            .execute(),
                    executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * Attach sprites to multiple entities in parallel, then wait for all to appear.
     *
     * @param container the container client
     * @param matchId the match ID
     * @param attachments the sprite attachments to apply
     * @param executor the executor for parallel command execution
     */
    public static void attachSprites(ContainerClient container, long matchId,
                                     List<SpriteAttachment> attachments, ExecutorService executor) {
        if (attachments.isEmpty()) {
            return;
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>(attachments.size());
        for (SpriteAttachment attachment : attachments) {
            futures.add(CompletableFuture.runAsync(() ->
                    container.forMatch(matchId).attachSprite()
                            .toEntity(attachment.entityId())
                            .usingResource(attachment.resourceId())
                            .sized(attachment.width(), attachment.height())
                            .visible(attachment.visible())
                            .execute(),
                    executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * Attach rigid bodies and sprites to entities in parallel, then wait for all components.
     *
     * <p>This is the preferred method for bulk entity setup as it:
     * <ul>
     *   <li>Sends all commands in parallel for maximum throughput</li>
     *   <li>Waits for both RigidBodyModule and RenderModule to have the expected entity count</li>
     *   <li>Waits for POSITION_X in GridMapModule (set by rigid body attachment)</li>
     * </ul>
     *
     * @param client the EngineClient for snapshot parsing
     * @param container the container client
     * @param matchId the match ID
     * @param rigidBodies the rigid body attachments
     * @param sprites the sprite attachments
     * @param executor the executor for parallel command execution
     */
    public static void attachRigidBodiesAndSprites(EngineClient client, ContainerClient container, long matchId,
                                                    List<RigidBodyAttachment> rigidBodies,
                                                    List<SpriteAttachment> sprites,
                                                    ExecutorService executor) {
        // Fire all commands in parallel
        List<CompletableFuture<Void>> allFutures = new ArrayList<>();

        for (RigidBodyAttachment rb : rigidBodies) {
            allFutures.add(CompletableFuture.runAsync(() ->
                    container.forMatch(matchId).custom("attachRigidBody")
                            .param("entityId", rb.entityId())
                            .param("positionX", rb.positionX())
                            .param("positionY", rb.positionY())
                            .param("velocityX", rb.velocityX())
                            .param("velocityY", rb.velocityY())
                            .param("mass", rb.mass())
                            .param("linearDrag", rb.linearDrag())
                            .execute(),
                    executor));
        }

        for (SpriteAttachment sprite : sprites) {
            allFutures.add(CompletableFuture.runAsync(() ->
                    container.forMatch(matchId).attachSprite()
                            .toEntity(sprite.entityId())
                            .usingResource(sprite.resourceId())
                            .sized(sprite.width(), sprite.height())
                            .visible(sprite.visible())
                            .execute(),
                    executor));
        }

        CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();

        // Wait for all components to appear
        if (!rigidBodies.isEmpty()) {
            waitForModuleEntityCount(client, container, matchId, "RigidBodyModule", rigidBodies.size());
            waitForModuleComponentCount(client, container, matchId, "GridMapModule", "POSITION_X", rigidBodies.size());
        }
        if (!sprites.isEmpty()) {
            waitForModuleEntityCount(client, container, matchId, "RenderModule", sprites.size());
        }
    }

    /**
     * Wait for a specific component to have at least the expected number of values.
     *
     * @param client the EngineClient for parsing snapshots
     * @param container the container client
     * @param matchId the match ID
     * @param moduleName the module name
     * @param componentName the component name
     * @param expectedCount the minimum number of values expected
     */
    public static void waitForModuleComponentCount(EngineClient client, ContainerClient container,
                                                    long matchId, String moduleName, String componentName,
                                                    int expectedCount) {
        int maxAttempts = 500;
        long pollIntervalMs = 10;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                var snapshotOpt = container.getSnapshot(matchId);
                if (snapshotOpt.isPresent()) {
                    var snapshot = client.parseSnapshot(snapshotOpt.get().data());
                    var module = snapshot.module(moduleName);
                    List<Float> values = module.component(componentName);
                    if (values.size() >= expectedCount) {
                        return;
                    }
                    if (attempt > 0 && attempt % 100 == 0) {
                        System.out.println("Waiting for " + moduleName + "." + componentName + ": " +
                                values.size() + " / " + expectedCount);
                    }
                }
            } catch (Exception ignored) {
                // Snapshot not ready yet
            }
            sleep(pollIntervalMs);
        }
        throw new IllegalStateException("Component " + moduleName + "." + componentName +
                " did not reach " + expectedCount + " values after " + maxAttempts + " attempts");
    }

    /**
     * Wait for a component to have a specific value at index 0.
     *
     * @param client the EngineClient for parsing snapshots
     * @param container the container client
     * @param matchId the match ID
     * @param moduleName the module name
     * @param componentName the component name
     * @param expectedValue the expected value
     * @param tolerance the tolerance for float comparison
     */
    public static void waitForComponentValue(EngineClient client, ContainerClient container,
                                              long matchId, String moduleName, String componentName,
                                              float expectedValue, float tolerance) {
        int maxAttempts = 50;
        long pollIntervalMs = 50;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            container.tick();
            try {
                var snapshotOpt = container.getSnapshot(matchId);
                if (snapshotOpt.isPresent()) {
                    var snapshot = client.parseSnapshot(snapshotOpt.get().data());
                    var module = snapshot.module(moduleName);
                    List<Float> values = module.component(componentName);
                    if (!values.isEmpty() && Math.abs(values.get(0) - expectedValue) < tolerance) {
                        return;
                    }
                }
            } catch (Exception ignored) {
                // Snapshot not ready yet
            }
            sleep(pollIntervalMs);
        }
        throw new IllegalStateException("Component " + moduleName + "." + componentName +
                " did not reach value " + expectedValue + " after " + maxAttempts + " attempts");
    }

    /**
     * Wait for a module to have at least the expected number of entities.
     * Uses extended timeout for large batch operations.
     *
     * @param client the EngineClient for parsing snapshots
     * @param container the container client
     * @param matchId the match ID
     * @param moduleName the module name (e.g., "RigidBodyModule")
     * @param expectedCount the minimum number of entities expected
     * @throws IllegalStateException if count isn't reached after max attempts
     */
    public static void waitForModuleEntityCount(EngineClient client, ContainerClient container,
                                                 long matchId, String moduleName, int expectedCount) {
        // Use extended timeout for large batch operations (500 attempts * 10ms = 5 seconds)
        int maxAttempts = 500;
        long pollIntervalMs = 10;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                var snapshotOpt = container.getSnapshot(matchId);
                if (snapshotOpt.isPresent()) {
                    var snapshot = client.parseSnapshot(snapshotOpt.get().data());
                    int count = getModuleEntityCount(snapshot, moduleName);
                    if (count >= expectedCount) {
                        return;
                    }
                    if (attempt > 0 && attempt % 100 == 0) {
                        // Log progress for long waits
                        System.out.println("Waiting for " + moduleName + ": " + count + " / " + expectedCount);
                    }
                }
            } catch (Exception ignored) {
                // Snapshot not ready yet, continue polling
            }
            sleep(pollIntervalMs);
        }
        throw new IllegalStateException("Module " + moduleName + " did not reach " + expectedCount +
                " entities after " + maxAttempts + " attempts");
    }

    private static int getModuleEntityCount(EngineClient.Snapshot snapshot, String moduleName) {
        var module = snapshot.module(moduleName);
        for (String component : module.componentNames()) {
            List<Float> values = module.component(component);
            if (!values.isEmpty()) {
                return values.size();
            }
        }
        return 0;
    }

    private static int getEntityCount(EngineClient client, ContainerClient container, long matchId) {
        try {
            var snap = container.getSnapshot(matchId);
            if (snap.isPresent()) {
                return client.parseSnapshot(snap.get().data()).entityIds().size();
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Sleep interrupted", e);
        }
    }
}
