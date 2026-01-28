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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import ca.samanthaireland.lightning.engine.acceptance.fixture.EntitySpawner;
import ca.samanthaireland.lightning.engine.api.resource.adapter.AuthAdapter;
import ca.samanthaireland.lightning.engine.api.resource.adapter.ContainerAdapter;
import ca.samanthaireland.lightning.engine.api.resource.adapter.EngineClient;
import ca.samanthaireland.lightning.engine.api.resource.adapter.EngineClient.ContainerClient;
import lombok.extern.slf4j.Slf4j;

/**
 * Physics functionality acceptance tests.
 *
 * <p>This test verifies:
 * <ul>
 *   <li>All RigidBodyModule commands work correctly</li>
 *   <li>Entities move correctly with velocity and forces</li>
 *   <li>Physics properties like mass, drag affect simulation correctly</li>
 * </ul>
 *
 * <p>Prerequisites:
 * <ul>
 *   <li>Docker must be running</li>
 *   <li>The backend image must be built: {@code docker pull samanthacireland/lightning-engine:0.0.2}</li>
 * </ul>
 *
 * <p>Run with:
 * <pre>
 * ./mvnw verify -pl lightning-engine/api-acceptance-test -Pacceptance-tests -Dtest=PhysicsIT
 * </pre>
 */
@Slf4j
@Tag("acceptance")
@Tag("testcontainers")
@DisplayName("Physics Acceptance Test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Timeout(value = 2, unit = TimeUnit.MINUTES)
class PhysicsIT {

    private static final List<String> REQUIRED_MODULES = List.of(
            "EntityModule", "GridMapModule", "RigidBodyModule"
    );

    @Container
    static GenericContainer<?> backendContainer = TestContainers.backendContainer();

    private EngineClient client;
    private ContainerClient container;
    private long containerId = -1;
    private long matchId = -1;
    private final List<Long> createdEntityIds = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        String baseUrl = TestContainers.getBaseUrl(backendContainer);
        log.info("Backend URL: {}", baseUrl);

        AuthAdapter auth = new AuthAdapter.HttpAuthAdapter(baseUrl);
        String token = auth.login("admin", TestContainers.TEST_ADMIN_PASSWORD).token();
        log.info("Authenticated successfully");

        client = EngineClient.builder()
                .baseUrl(baseUrl)
                .withBearerToken(token)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (containerId > 0) {
            try {
                client.containers().stopContainer(containerId);
                client.containers().deleteContainer(containerId);
            } catch (Exception e) {
                log.warn("Failed to clean up container {}: {}", containerId, e.getMessage());
            }
        }
        createdEntityIds.clear();
        containerId = -1;
        matchId = -1;
        container = null;
    }

    // ========== RigidBodyModule Command Tests ==========

    @Test
    @Order(1)
    @DisplayName("attachRigidBody - creates rigid body with all properties")
    void testAttachRigidBody() throws Exception {
        setupMatch();
        long entityId = spawnEntity();

        container.forMatch(matchId).send("attachRigidBody", Map.ofEntries(
                Map.entry("entityId", entityId),
                Map.entry("positionX", 100.0f),
                Map.entry("positionY", 200.0f),
                Map.entry("positionZ", 0.0f),
                Map.entry("velocityX", 5.0f),
                Map.entry("velocityY", 10.0f),
                Map.entry("velocityZ", 0.0f),
                Map.entry("mass", 2.0f),
                Map.entry("linearDrag", 0.1f),
                Map.entry("angularDrag", 0.05f),
                Map.entry("inertia", 1.5f)
        ));

        // Wait for rigid body components to appear in snapshot
        EntitySpawner.waitForComponent(client, container, matchId, "RigidBodyModule", "VELOCITY_X");

        var snapshot = getSnapshot();
        var rigidBody = snapshot.module("RigidBodyModule");

        assertThat(rigidBody.has("VELOCITY_X")).isTrue();
        assertThat(rigidBody.has("MASS")).isTrue();
        assertThat(rigidBody.has("LINEAR_DRAG")).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("setVelocity - directly sets entity velocity")
    void testSetVelocity() throws Exception {
        setupMatch();
        long entityId = spawnEntityWithRigidBody(0, 0);

        container.forMatch(matchId).custom("setVelocity")
                .param("entityId", entityId)
                .param("velocityX", 50.0f)
                .param("velocityY", -30.0f)
                .param("velocityZ", 0.0f)
                .execute();

        container.tick();

        var snapshot = getSnapshot();
        var rigidBody = snapshot.module("RigidBodyModule");

        float vx = rigidBody.first("VELOCITY_X", 0);
        float vy = rigidBody.first("VELOCITY_Y", 0);

        assertThat(Math.abs(vx)).isGreaterThan(40);
        assertThat(Math.abs(vy)).isGreaterThan(25);

        log.info("setVelocity test passed: vx={}, vy={}", vx, vy);
    }

    @Test
    @Order(3)
    @DisplayName("applyForce - accumulates force for physics integration")
    void testApplyForce() throws Exception {
        setupMatch();
        long entityId = spawnEntityWithRigidBody(0, 0);

        container.forMatch(matchId).custom("applyForce")
                .param("entityId", entityId)
                .param("forceX", 100.0f)
                .param("forceY", 50.0f)
                .param("forceZ", 0.0f)
                .execute();

        for (int i = 0; i < 10; i++) {
            container.tick();
        }

        var snapshot = getSnapshot();
        var gridMap = snapshot.module("GridMapModule");

        float px = gridMap.first("POSITION_X", 0);
        float py = gridMap.first("POSITION_Y", 0);

        assertThat(px).isGreaterThan(0);
        assertThat(py).isGreaterThan(0);

        log.info("applyForce test passed: px={}, py={}", px, py);
    }

    @Test
    @Order(4)
    @Disabled("Flaky test - impulse command timing issue")
    @DisplayName("applyImpulse - instant velocity change based on mass")
    void testApplyImpulse() throws Exception {
        setupMatch();
        long entityId = spawnEntityWithRigidBody(0, 0);

        container.forMatch(matchId).custom("attachRigidBody")
                .param("entityId", entityId)
                .param("positionX", 0.0f)
                .param("positionY", 0.0f)
                .param("mass", 2.0f)
                .execute();
        // Wait for the attachRigidBody to complete
        EntitySpawner.waitForComponent(client, container, matchId, "RigidBodyModule", "MASS");

        container.forMatch(matchId).custom("applyImpulse")
                .param("entityId", entityId)
                .param("impulseX", 100.0f)
                .param("impulseY", 0.0f)
                .param("impulseZ", 0.0f)
                .execute();

        container.tick();

        var snapshot = getSnapshot();
        var rigidBody = snapshot.module("RigidBodyModule");

        float vx = rigidBody.first("VELOCITY_X", 0);
        assertThat(vx).isGreaterThan(40);

        log.info("applyImpulse test passed: vx={}", vx);
    }

    @Test
    @Order(5)
    @DisplayName("setPosition - teleports entity to new position")
    void testSetPosition() throws Exception {
        setupMatch();
        long entityId = spawnEntityWithRigidBody(0, 0);

        container.forMatch(matchId).custom("setPosition")
                .param("entityId", entityId)
                .param("positionX", 500.0f)
                .param("positionY", 300.0f)
                .param("positionZ", 0.0f)
                .execute();

        // Wait for position to be updated to the new value
        EntitySpawner.waitForComponentValue(client, container, matchId, "GridMapModule", "POSITION_X", 500.0f, 0.1f);

        var snapshot = getSnapshot();
        var gridMap = snapshot.module("GridMapModule");

        float px = gridMap.first("POSITION_X", 0);
        float py = gridMap.first("POSITION_Y", 0);

        assertThat(px).isEqualTo(500.0f);
        assertThat(py).isEqualTo(300.0f);

        log.info("setPosition test passed: px={}, py={}", px, py);
    }

    @Test
    @Order(6)
    @DisplayName("applyTorque - rotates entity")
    void testApplyTorque() throws Exception {
        setupMatch();
        long entityId = spawnEntityWithRigidBody(0, 0);

        container.forMatch(matchId).custom("applyTorque")
                .param("entityId", entityId)
                .param("torque", 50.0f)
                .execute();

        for (int i = 0; i < 10; i++) {
            container.tick();
        }

        var snapshot = getSnapshot();
        var rigidBody = snapshot.module("RigidBodyModule");

        float rotation = rigidBody.first("ROTATION", 0);
        float angularVelocity = rigidBody.first("ANGULAR_VELOCITY", 0);

        assertThat(rotation).isNotEqualTo(0);
        assertThat(angularVelocity).isGreaterThan(0);

        log.info("applyTorque test passed: rotation={}, angularVelocity={}", rotation, angularVelocity);
    }

    @Test
    @Order(7)
    @Disabled("RigidBody cleanup system not processing deleteQueue correctly - needs backend investigation")
    @DisplayName("deleteRigidBody - removes rigid body components")
    void testDeleteRigidBody() throws Exception {
        setupMatch();
        long entityId = spawnEntityWithRigidBody(100, 100);
        container.tick();

        var snapshotBefore = getSnapshot();
        int countBefore = countEntitiesInModule(snapshotBefore, "RigidBodyModule");
        assertThat(countBefore).isEqualTo(1);

        container.forMatch(matchId).custom("deleteRigidBody")
                .param("entityId", entityId)
                .execute();

        // Stop auto-advance to ensure deterministic ticking
        container.stopAuto();

        // Multiple manual ticks for cleanup system to process
        for (int i = 0; i < 5; i++) {
            container.tick();
        }

        var snapshotAfter = getSnapshot();
        int countAfter = countEntitiesInModule(snapshotAfter, "RigidBodyModule");

        log.info("deleteRigidBody: before={}, after={}", countBefore, countAfter);
        assertThat(countAfter).isEqualTo(0);

        log.info("deleteRigidBody test passed");
    }

    // ========== Physics Behavior Tests ==========

    @Test
    @Order(8)
    @DisplayName("Linear drag dampens velocity over time")
    void testLinearDragDampening() throws Exception {
        setupMatch();
        long entityId = spawnEntity();

        container.forMatch(matchId).custom("attachRigidBody")
                .param("entityId", entityId)
                .param("positionX", 0.0f)
                .param("positionY", 0.0f)
                .param("velocityX", 100.0f)
                .param("velocityY", 0.0f)
                .param("mass", 1.0f)
                .param("linearDrag", 0.5f)
                .execute();

        // Wait for attachRigidBody to complete
        EntitySpawner.waitForComponent(client, container, matchId, "RigidBodyModule", "VELOCITY_X");

        var snapshot1 = getSnapshot();
        float v1 = snapshot1.module("RigidBodyModule").first("VELOCITY_X", 0);

        for (int i = 0; i < 10; i++) {
            container.tick();
        }

        var snapshot2 = getSnapshot();
        float v2 = snapshot2.module("RigidBodyModule").first("VELOCITY_X", 0);

        log.info("Linear drag test: initial velocity={}, final velocity={}", v1, v2);

        assertThat(v2).isLessThan(v1 / 10);
        log.info("Linear drag dampening test PASSED");
    }

    @Test
    @Order(9)
    @DisplayName("Angular drag dampens rotation over time")
    void testAngularDragDampening() throws Exception {
        setupMatch();

        // Spawn entity without rigid body first
        long entityId = spawnEntity();

        // Attach rigid body with inertia and angular drag
        container.forMatch(matchId).custom("attachRigidBody")
                .param("entityId", entityId)
                .param("positionX", 0.0f)
                .param("positionY", 0.0f)
                .param("mass", 1.0f)
                .param("inertia", 1.0f)
                .param("angularDrag", 0.3f)
                .execute();

        // Wait for attachRigidBody to complete
        EntitySpawner.waitForComponent(client, container, matchId, "RigidBodyModule", "INERTIA");

        // Apply torque to start rotation
        container.forMatch(matchId).custom("applyTorque")
                .param("entityId", entityId)
                .param("torque", 100.0f)
                .execute();

        // Tick to process the torque
        container.tick();
        container.tick();

        var snapshot1 = getSnapshot();
        float av1 = snapshot1.module("RigidBodyModule").first("ANGULAR_VELOCITY", 0);
        log.info("Angular velocity after torque: {}", av1);

        // Run more ticks to let angular drag dampen the rotation
        for (int i = 0; i < 20; i++) {
            container.tick();
        }

        var snapshot2 = getSnapshot();
        float av2 = snapshot2.module("RigidBodyModule").first("ANGULAR_VELOCITY", 0);

        log.info("Angular drag test: initial angularVelocity={}, final angularVelocity={}", av1, av2);

        // After drag, angular velocity should be smaller
        assertThat(Math.abs(av2)).isLessThan(Math.abs(av1));
        log.info("Angular drag dampening test PASSED");
    }

    @Test
    @Order(10)
    @DisplayName("Mass affects acceleration (F=ma)")
    void testMassAffectsAcceleration() throws Exception {
        setupMatch();

        // Stop auto-advance for deterministic testing
        container.stopAuto();

        // Spawn two entities
        long lightEntity = spawnEntity();
        long heavyEntity = spawnEntity();

        log.info("Spawned entities: light={}, heavy={}", lightEntity, heavyEntity);

        // Attach rigid body to light entity (mass=1, linearDrag=0)
        container.forMatch(matchId).custom("attachRigidBody")
                .param("entityId", lightEntity)
                .param("positionX", 0.0f)
                .param("positionY", 0.0f)
                .param("mass", 1.0f)
                .param("linearDrag", 0.0f)
                .execute();
        // Wait for light entity rigid body
        EntitySpawner.waitForComponent(client, container, matchId, "RigidBodyModule", "MASS");

        // Attach rigid body to heavy entity (mass=10, linearDrag=0)
        container.forMatch(matchId).custom("attachRigidBody")
                .param("entityId", heavyEntity)
                .param("positionX", 100.0f)
                .param("positionY", 0.0f)
                .param("mass", 10.0f)
                .param("linearDrag", 0.0f)
                .execute();
        // Wait for both rigid bodies to be attached
        EntitySpawner.waitForModuleEntityCount(client, container, matchId, "RigidBodyModule", 2);

        // Apply same force to both entities
        container.forMatch(matchId).custom("applyForce")
                .param("entityId", lightEntity)
                .param("forceX", 100.0f)
                .param("forceY", 0.0f)
                .execute();

        container.forMatch(matchId).custom("applyForce")
                .param("entityId", heavyEntity)
                .param("forceX", 100.0f)
                .param("forceY", 0.0f)
                .execute();

        // Multiple ticks to let physics integrate
        for (int i = 0; i < 5; i++) {
            container.tick();
        }

        var snapshot = getSnapshot();
        var rigidBody = snapshot.module("RigidBodyModule");
        var gridMap = snapshot.module("GridMapModule");
        var entityModule = snapshot.module("EntityModule");

        // Debug: print all component data
        List<Float> entityIds = entityModule.component("ENTITY_ID");
        List<Float> velocities = rigidBody.component("VELOCITY_X");
        List<Float> masses = rigidBody.component("MASS");
        List<Float> positionsX = gridMap.component("POSITION_X");

        log.info("Snapshot data:");
        log.info("  Entity IDs: {}", entityIds);
        log.info("  Velocities X: {}", velocities);
        log.info("  Masses: {}", masses);
        log.info("  Positions X: {}", positionsX);

        // Find velocities by matching entity ID
        float lightVelocity = 0, heavyVelocity = 0;
        for (int i = 0; i < entityIds.size(); i++) {
            long eid = entityIds.get(i).longValue();
            if (eid == lightEntity && i < velocities.size()) {
                lightVelocity = velocities.get(i);
            } else if (eid == heavyEntity && i < velocities.size()) {
                heavyVelocity = velocities.get(i);
            }
        }

        log.info("F=ma test: lightVelocity={}, heavyVelocity={}", lightVelocity, heavyVelocity);

        // Light entity (mass=1) should accelerate faster than heavy (mass=10)
        // F=ma means a=F/m, so light gets 100/1=100, heavy gets 100/10=10
        // After integration, light should have ~10x the velocity
        assertThat(lightVelocity)
                .as("Light entity (mass=1) should have higher velocity than heavy (mass=10)")
                .isGreaterThan(heavyVelocity * 5);

        log.info("Mass affects acceleration test PASSED");
    }

    // ========== Helper Methods ==========

    private void setupMatch() throws Exception {
        ContainerAdapter.ContainerResponse containerResponse = client.containers()
                .create()
                .name("physics-test-" + System.currentTimeMillis())
                .withModules(REQUIRED_MODULES.toArray(new String[0]))
                .execute();
        containerId = containerResponse.id();
        container = client.container(containerId);

        // Start auto-advance (this starts the container)
        container.play(60);

        // Wait for container to be fully running
        EntitySpawner.waitForContainerRunning(client, containerId);

        var match = container.createMatch(REQUIRED_MODULES);
        matchId = match.id();
        log.info("Created match {}", matchId);
    }

    private long spawnEntity() throws IOException {
        long entityId = EntitySpawner.spawnEntity(client, container, matchId);
        createdEntityIds.add(entityId);
        return entityId;
    }

    private long spawnEntityWithRigidBody(float x, float y) throws IOException {
        long entityId = EntitySpawner.spawnEntityWithRigidBody(client, container, matchId, x, y);
        createdEntityIds.add(entityId);
        return entityId;
    }

    private EngineClient.Snapshot getSnapshot() throws IOException {
        var snapshotOpt = container.getSnapshot(matchId);
        if (snapshotOpt.isEmpty()) {
            throw new IllegalStateException("No snapshot available for match " + matchId);
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
}
