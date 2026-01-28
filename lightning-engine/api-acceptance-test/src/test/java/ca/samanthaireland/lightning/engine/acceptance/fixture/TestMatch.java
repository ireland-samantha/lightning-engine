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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fluent test fixture for match operations.
 *
 * <p>Usage:
 * <pre>{@code
 * TestMatch match = container.createMatch()
 *     .withModules("EntityModule", "RigidBodyModule")
 *     .build();
 *
 * long entityId = match.spawnEntity()
 *     .forPlayer(playerId)
 *     .execute();
 *
 * match.attachRigidBody(entityId)
 *     .position(100, 200, 0)
 *     .velocity(50, 0, 0)
 *     .execute();
 * }</pre>
 */
public class TestMatch {

    private final TestEngineContainer container;
    private final long matchId;
    private final List<String> enabledModules;
    private final List<Long> entityIds = new ArrayList<>();
    private final List<Long> joinedPlayerIds = new ArrayList<>();

    TestMatch(TestEngineContainer container, long matchId, List<String> enabledModules) {
        this.container = container;
        this.matchId = matchId;
        this.enabledModules = List.copyOf(enabledModules);
    }

    /**
     * Get the match ID.
     */
    public long id() {
        return matchId;
    }

    /**
     * Get the container this match belongs to.
     */
    public TestEngineContainer container() {
        return container;
    }

    /**
     * Get the enabled modules for this match.
     */
    public List<String> modules() {
        return enabledModules;
    }

    /**
     * Get all entity IDs spawned in this match.
     */
    public List<Long> entityIds() {
        return List.copyOf(entityIds);
    }

    /**
     * Get all player IDs joined to this match.
     */
    public List<Long> playerIds() {
        return List.copyOf(joinedPlayerIds);
    }

    /**
     * Join a player to this match.
     */
    public TestMatch joinPlayer(long playerId) {
        container.client().joinMatch(matchId, playerId);
        joinedPlayerIds.add(playerId);
        return this;
    }

    /**
     * Spawn an entity in this match.
     */
    public SpawnBuilder spawnEntity() {
        return new SpawnBuilder(this);
    }

    /**
     * Attach a rigid body to an entity.
     */
    public RigidBodyBuilder attachRigidBody(long entityId) {
        return new RigidBodyBuilder(this, entityId);
    }

    /**
     * Set velocity for an entity.
     */
    public TestMatch setVelocity(long entityId, float vx, float vy, float vz) {
        container.client().forMatch(matchId).custom("setVelocity")
                .param("entityId", entityId)
                .param("velocityX", vx)
                .param("velocityY", vy)
                .param("velocityZ", vz)
                .execute();
        return this;
    }

    /**
     * Apply force to an entity.
     */
    public TestMatch applyForce(long entityId, float fx, float fy, float fz) {
        container.client().forMatch(matchId).custom("applyForce")
                .param("entityId", entityId)
                .param("forceX", fx)
                .param("forceY", fy)
                .param("forceZ", fz)
                .execute();
        return this;
    }

    /**
     * Get the current snapshot for this match.
     */
    public EngineClient.Snapshot snapshot() {
        return container.snapshot(matchId);
    }

    /**
     * Track a spawned entity ID.
     */
    void addEntity(long entityId) {
        entityIds.add(entityId);
    }

    /**
     * Builder for spawning entities.
     */
    public static class SpawnBuilder {
        private static final int DEFAULT_MAX_ATTEMPTS = 20;
        private static final long DEFAULT_POLL_INTERVAL_MS = 100;

        private final TestMatch match;
        private long playerId = 1;
        private long entityType = 100;
        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
        private long pollIntervalMs = DEFAULT_POLL_INTERVAL_MS;

        SpawnBuilder(TestMatch match) {
            this.match = match;
        }

        /**
         * Set the player ID for the spawn.
         */
        public SpawnBuilder forPlayer(long playerId) {
            this.playerId = playerId;
            return this;
        }

        /**
         * Set the entity type for the spawn.
         */
        public SpawnBuilder ofType(long entityType) {
            this.entityType = entityType;
            return this;
        }

        /**
         * Set the maximum number of polling attempts.
         */
        public SpawnBuilder withMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        /**
         * Set the polling interval in milliseconds.
         */
        public SpawnBuilder withPollInterval(long intervalMs) {
            this.pollIntervalMs = intervalMs;
            return this;
        }

        /**
         * Execute the spawn and return the entity ID.
         * Uses polling to wait for the entity to appear in the snapshot.
         */
        public long execute() {
            // Get entity count before spawn
            int beforeCount = 0;
            try {
                beforeCount = match.snapshot().entityIds().size();
            } catch (Exception ignored) {
                // No entities yet, that's fine
            }

            match.container.client().forMatch(match.matchId).spawn()
                    .forPlayer(playerId)
                    .ofType(entityType)
                    .execute();

            // Poll until entity appears in snapshot
            final int expectedCount = beforeCount + 1;
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                match.container.tick();

                try {
                    EngineClient.Snapshot snapshot = match.snapshot();
                    List<Float> entityIds = snapshot.entityIds();

                    if (entityIds.size() >= expectedCount) {
                        // Return the last entity ID (most recently spawned)
                        long entityId = entityIds.get(entityIds.size() - 1).longValue();
                        match.addEntity(entityId);
                        return entityId;
                    }
                } catch (Exception ignored) {
                    // Snapshot not ready yet, continue polling
                }

                try {
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Spawn interrupted", e);
                }
            }

            // Final check before failing
            try {
                EngineClient.Snapshot snapshot = match.snapshot();
                throw new IllegalStateException(
                        "Entity did not appear after " + maxAttempts + " attempts. " +
                        "Before: " + beforeCount + ", After: " + snapshot.entityIds().size() +
                        ", Modules: " + snapshot.moduleNames());
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Entity did not appear after " + maxAttempts + " attempts. " +
                        "Before: " + beforeCount + ". Error getting snapshot: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Builder for attaching rigid bodies.
     */
    public static class RigidBodyBuilder {
        private final TestMatch match;
        private final long entityId;
        private float positionX = 0;
        private float positionY = 0;
        private float positionZ = 0;
        private float velocityX = 0;
        private float velocityY = 0;
        private float velocityZ = 0;
        private float mass = 1.0f;
        private float linearDrag = 0.1f;

        RigidBodyBuilder(TestMatch match, long entityId) {
            this.match = match;
            this.entityId = entityId;
        }

        /**
         * Set the initial position.
         */
        public RigidBodyBuilder position(float x, float y, float z) {
            this.positionX = x;
            this.positionY = y;
            this.positionZ = z;
            return this;
        }

        /**
         * Set the initial velocity.
         */
        public RigidBodyBuilder velocity(float vx, float vy, float vz) {
            this.velocityX = vx;
            this.velocityY = vy;
            this.velocityZ = vz;
            return this;
        }

        /**
         * Set the mass.
         */
        public RigidBodyBuilder mass(float mass) {
            this.mass = mass;
            return this;
        }

        /**
         * Set the linear drag.
         */
        public RigidBodyBuilder linearDrag(float drag) {
            this.linearDrag = drag;
            return this;
        }

        /**
         * Execute the attach rigid body command and wait for components to appear.
         */
        public TestMatch execute() {
            match.container.client().forMatch(match.matchId).send("attachRigidBody", Map.of(
                    "entityId", entityId,
                    "positionX", positionX,
                    "positionY", positionY,
                    "positionZ", positionZ,
                    "velocityX", velocityX,
                    "velocityY", velocityY,
                    "velocityZ", velocityZ,
                    "mass", mass,
                    "linearDrag", linearDrag
            ));
            // Wait for rigid body and position components to appear
            EntitySpawner.waitForComponent(
                    match.container.engineClient(),
                    match.container.client(),
                    match.matchId,
                    "RigidBodyModule",
                    "MASS");
            EntitySpawner.waitForComponent(
                    match.container.engineClient(),
                    match.container.client(),
                    match.matchId,
                    "GridMapModule",
                    "POSITION_X");
            return match;
        }
    }

    /**
     * Builder for creating matches.
     */
    public static class Builder {
        private final TestEngineContainer container;
        private final List<String> modules = new ArrayList<>();

        Builder(TestEngineContainer container) {
            this.container = container;
        }

        /**
         * Add modules to enable in this match.
         */
        public Builder withModules(String... moduleNames) {
            modules.addAll(List.of(moduleNames));
            return this;
        }

        /**
         * Build and create the match.
         */
        public TestMatch build() {
            var matchResponse = container.client().createMatch(modules);
            TestMatch match = new TestMatch(container, matchResponse.id(), modules);
            container.addMatch(match);
            return match;
        }
    }
}
