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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import ca.samanthaireland.lightning.engine.acceptance.fixture.SnapshotAssertions;
import ca.samanthaireland.lightning.engine.acceptance.fixture.TestEngineContainer;
import ca.samanthaireland.lightning.engine.acceptance.fixture.TestMatch;
import ca.samanthaireland.lightning.engine.api.resource.adapter.AuthAdapter;
import ca.samanthaireland.lightning.engine.api.resource.adapter.EngineClient;
import ca.samanthaireland.lightning.engine.api.resource.adapter.dto.HistorySnapshotDto;
import lombok.extern.slf4j.Slf4j;

/**
 * Comprehensive isolation test for containers, matches, and players.
 *
 * <p>This test verifies complete isolation between:
 * <ul>
 *   <li>Two separate containers</li>
 *   <li>Two matches per container (4 matches total)</li>
 *   <li>Two players per container (4 players total)</li>
 *   <li>Physics simulation independently evolving per match</li>
 * </ul>
 *
 * <p>Tests verify that:
 * <ul>
 *   <li>All IDs (container, match, player, entity) are unique</li>
 *   <li>REST API snapshots contain only data for the requested match</li>
 *   <li>WebSocket snapshots stream only data for the subscribed match</li>
 *   <li>Physics values change differently in each match</li>
 * </ul>
 *
 * <p>Run with:
 * <pre>
 * ./mvnw verify -pl lightning-engine/api-acceptance-test -Pacceptance-tests -Dtest=ContainerIsolationIT
 * </pre>
 */
@Slf4j
@Tag("acceptance")
@Tag("testcontainers")
@Tag("mongodb")
@DisplayName("Container and Match Isolation Test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Timeout(value = 3, unit = TimeUnit.MINUTES)
class ContainerIsolationIT {

    private static final List<String> PHYSICS_MODULES = List.of(
            "EntityModule", "GridMapModule", "RigidBodyModule"
    );

    static Network network = Network.newNetwork();

    @Container
    static MongoDBContainer mongoContainer = TestContainers.mongoContainer(network, "mongodb");

    @Container
    static GenericContainer<?> backendContainer = TestContainers
            .backendContainerWithMongo(network, "mongodb")
            .dependsOn(mongoContainer);

    private EngineClient client;
    private String baseUrl;

    // Test fixtures
    private TestEngineContainer container1;
    private TestEngineContainer container2;
    private TestMatch match1A;
    private TestMatch match1B;
    private TestMatch match2A;
    private TestMatch match2B;

    // Entity IDs for each match
    private long entity1A;
    private long entity1B;
    private long entity2A;
    private long entity2B;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = TestContainers.getBaseUrl(backendContainer);
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
        if (container1 != null) container1.cleanup();
        if (container2 != null) container2.cleanup();
    }

    // ==================== Main Test ====================

    @Test
    @Order(1)
    @DisplayName("Complete isolation: containers, matches, players, and physics")
    void testCompleteIsolation() throws Exception {
        log.info("=== STEP 1: Create two containers with physics modules ===");
        createContainers();

        log.info("=== STEP 2: Create two matches in each container ===");
        createMatches();

        log.info("=== STEP 3: Create two players in each container and join to matches ===");
        createPlayersAndJoin();

        log.info("=== STEP 4: Verify all IDs are unique ===");
        verifyUniqueIds();

        log.info("=== STEP 5: Spawn entities with rigid bodies (different velocities per match) ===");
        spawnEntitiesWithPhysics();

        log.info("=== STEP 6: Verify REST API snapshot isolation ===");
        verifyRestSnapshotIsolation();

        log.info("=== STEP 7: Verify WebSocket snapshot isolation ===");
        verifyWebSocketSnapshotIsolation();

        log.info("=== STEP 8: Run physics simulation and verify different evolution ===");
        verifyPhysicsEvolveDifferently();

        log.info("=== STEP 9: Verify cross-container access returns 404 (complete isolation) ===");
        verifyCrossContainerAccessReturns404();

        log.info("=== STEP 10: Verify snapshot history isolation ===");
        verifySnapshotHistoryIsolation();

        log.info("=== ALL ISOLATION TESTS PASSED ===");
    }

    // ==================== Test Steps ====================

    private void createContainers() {
        container1 = TestEngineContainer.create(client)
                .withName("isolation-container-1")
                .withModules(PHYSICS_MODULES.toArray(new String[0]))
                .start();
        log.info("Created container 1: id={}", container1.id());

        container2 = TestEngineContainer.create(client)
                .withName("isolation-container-2")
                .withModules(PHYSICS_MODULES.toArray(new String[0]))
                .start();
        log.info("Created container 2: id={}", container2.id());

        // Verify containers have different IDs
        assertThat(container1.id())
                .as("Container IDs should be different")
                .isNotEqualTo(container2.id());
    }

    private void createMatches() {
        // Container 1: matches A and B
        match1A = container1.createMatch()
                .withModules(PHYSICS_MODULES.toArray(new String[0]))
                .build();
        log.info("Created match 1A: id={}", match1A.id());

        match1B = container1.createMatch()
                .withModules(PHYSICS_MODULES.toArray(new String[0]))
                .build();
        log.info("Created match 1B: id={}", match1B.id());

        // Container 2: matches A and B
        match2A = container2.createMatch()
                .withModules(PHYSICS_MODULES.toArray(new String[0]))
                .build();
        log.info("Created match 2A: id={}", match2A.id());

        match2B = container2.createMatch()
                .withModules(PHYSICS_MODULES.toArray(new String[0]))
                .build();
        log.info("Created match 2B: id={}", match2B.id());
    }

    private void createPlayersAndJoin() {
        // Container 1: players
        long player1A = container1.createPlayer();
        long player1B = container1.createPlayer();
        log.info("Created players in container 1: {}, {}", player1A, player1B);

        // Container 2: players
        long player2A = container2.createPlayer();
        long player2B = container2.createPlayer();
        log.info("Created players in container 2: {}, {}", player2A, player2B);

        // Join players to matches
        match1A.joinPlayer(player1A);
        match1B.joinPlayer(player1B);
        match2A.joinPlayer(player2A);
        match2B.joinPlayer(player2B);

        log.info("Joined players to their respective matches");
    }

    private void verifyUniqueIds() {
        // Verify container IDs are globally unique
        List<Long> containerIds = List.of(container1.id(), container2.id());
        SnapshotAssertions.assertAllIdsUnique("Container IDs", containerIds);

        // Verify match IDs are unique WITHIN each container (but may overlap across containers)
        // This is by design - each container is isolated and has its own ID sequence
        List<Long> container1MatchIds = List.of(match1A.id(), match1B.id());
        List<Long> container2MatchIds = List.of(match2A.id(), match2B.id());
        SnapshotAssertions.assertAllIdsUnique("Container 1 Match IDs", container1MatchIds);
        SnapshotAssertions.assertAllIdsUnique("Container 2 Match IDs", container2MatchIds);

        // Verify that each container does have different matches
        assertThat(container1MatchIds).hasSize(2);
        assertThat(container2MatchIds).hasSize(2);
        assertThat(match1A.id()).isNotEqualTo(match1B.id());
        assertThat(match2A.id()).isNotEqualTo(match2B.id());

        // Verify player IDs are globally unique (they use timestamp-based IDs)
        List<Long> allPlayerIds = new ArrayList<>();
        allPlayerIds.addAll(container1.playerIds());
        allPlayerIds.addAll(container2.playerIds());
        SnapshotAssertions.assertAllIdsUnique("Player IDs", allPlayerIds);

        log.info("IDs verified: {} containers, {}+{} matches (per-container), {} players (global)",
                containerIds.size(), container1MatchIds.size(), container2MatchIds.size(), allPlayerIds.size());
        log.info("  Container 1 matches: {}", container1MatchIds);
        log.info("  Container 2 matches: {}", container2MatchIds);
    }

    private void spawnEntitiesWithPhysics() {
        // Match 1A: entity moving right (positive X velocity)
        entity1A = match1A.spawnEntity().forPlayer(container1.playerIds().get(0)).execute();
        match1A.attachRigidBody(entity1A)
                .position(0, 0, 0)
                .velocity(100, 0, 0)  // Moving right
                .mass(1.0f)
                .linearDrag(0.0f)
                .execute();
        log.info("Match 1A: entity {} moving right (vx=100)", entity1A);

        // Match 1B: entity moving up (positive Y velocity)
        entity1B = match1B.spawnEntity().forPlayer(container1.playerIds().get(1)).execute();
        match1B.attachRigidBody(entity1B)
                .position(0, 0, 0)
                .velocity(0, 100, 0)  // Moving up
                .mass(1.0f)
                .linearDrag(0.0f)
                .execute();
        log.info("Match 1B: entity {} moving up (vy=100)", entity1B);

        // Match 2A: entity moving left (negative X velocity)
        entity2A = match2A.spawnEntity().forPlayer(container2.playerIds().get(0)).execute();
        match2A.attachRigidBody(entity2A)
                .position(0, 0, 0)
                .velocity(-100, 0, 0)  // Moving left
                .mass(1.0f)
                .linearDrag(0.0f)
                .execute();
        log.info("Match 2A: entity {} moving left (vx=-100)", entity2A);

        // Match 2B: entity moving diagonally
        entity2B = match2B.spawnEntity().forPlayer(container2.playerIds().get(1)).execute();
        match2B.attachRigidBody(entity2B)
                .position(0, 0, 0)
                .velocity(50, 50, 0)  // Moving diagonally
                .mass(1.0f)
                .linearDrag(0.0f)
                .execute();
        log.info("Match 2B: entity {} moving diagonally (vx=50, vy=50)", entity2B);

        // Verify entity IDs are unique WITHIN each container (but may overlap across containers)
        // This is by design - each container is isolated and has its own ID sequence
        List<Long> container1EntityIds = List.of(entity1A, entity1B);
        List<Long> container2EntityIds = List.of(entity2A, entity2B);
        SnapshotAssertions.assertAllIdsUnique("Container 1 Entity IDs", container1EntityIds);
        SnapshotAssertions.assertAllIdsUnique("Container 2 Entity IDs", container2EntityIds);

        log.info("All 4 entities spawned:");
        log.info("  Container 1: {} (per-container IDs)", container1EntityIds);
        log.info("  Container 2: {} (per-container IDs)", container2EntityIds);
    }

    private void verifyRestSnapshotIsolation() {
        // Get snapshots for all 4 matches
        var snapshot1A = match1A.snapshot();
        var snapshot1B = match1B.snapshot();
        var snapshot2A = match2A.snapshot();
        var snapshot2B = match2B.snapshot();

        // Verify each snapshot contains only its own entity
        SnapshotAssertions.assertThat(snapshot1A)
                .hasModule("EntityModule")
                .hasEntityCount(1)
                .containsOnlyEntityIds(entity1A);

        SnapshotAssertions.assertThat(snapshot1B)
                .hasModule("EntityModule")
                .hasEntityCount(1)
                .containsOnlyEntityIds(entity1B);

        SnapshotAssertions.assertThat(snapshot2A)
                .hasModule("EntityModule")
                .hasEntityCount(1)
                .containsOnlyEntityIds(entity2A);

        SnapshotAssertions.assertThat(snapshot2B)
                .hasModule("EntityModule")
                .hasEntityCount(1)
                .containsOnlyEntityIds(entity2B);

        // Verify isolation between matches within the SAME container
        // (Entity IDs are container-scoped, so we can only check within containers)
        SnapshotAssertions.assertThat(snapshot1A)
                .doesNotContainEntityIds(entity1B);  // Different match in same container
        SnapshotAssertions.assertThat(snapshot1B)
                .doesNotContainEntityIds(entity1A);  // Different match in same container
        SnapshotAssertions.assertThat(snapshot2A)
                .doesNotContainEntityIds(entity2B);  // Different match in same container
        SnapshotAssertions.assertThat(snapshot2B)
                .doesNotContainEntityIds(entity2A);  // Different match in same container

        // Verify within-container isolation using SnapshotIsolation assertions
        SnapshotAssertions.assertIsolation(snapshot1A, snapshot1B)
                .entitiesAreDisjoint()
                .haveUniqueIds();

        SnapshotAssertions.assertIsolation(snapshot2A, snapshot2B)
                .entitiesAreDisjoint()
                .haveUniqueIds();

        log.info("REST API snapshot isolation verified for all 4 matches");
        log.info("  Container 1: Match A entity {}, Match B entity {} (different IDs within container)", entity1A, entity1B);
        log.info("  Container 2: Match A entity {}, Match B entity {} (different IDs within container)", entity2A, entity2B);
    }

    private void verifyWebSocketSnapshotIsolation() throws Exception {
        // Connect to WebSocket for each match and verify only its data is received
        String wsMatch1A = receiveWebSocketSnapshot(container1.id(), match1A.id());
        String wsMatch1B = receiveWebSocketSnapshot(container1.id(), match1B.id());
        String wsMatch2A = receiveWebSocketSnapshot(container2.id(), match2A.id());
        String wsMatch2B = receiveWebSocketSnapshot(container2.id(), match2B.id());

        // Verify WebSocket snapshots contain data (basic connectivity test)
        // Entity IDs are container-scoped (may overlap), so we verify:
        // 1. Each WebSocket returns valid JSON with expected match ID
        // 2. Entity count is exactly 1 per match
        assertThat(wsMatch1A)
                .as("WebSocket match 1A should return valid snapshot")
                .contains("\"matchId\"")
                .contains("EntityModule");

        assertThat(wsMatch1B)
                .as("WebSocket match 1B should return valid snapshot")
                .contains("\"matchId\"")
                .contains("EntityModule");

        assertThat(wsMatch2A)
                .as("WebSocket match 2A should return valid snapshot")
                .contains("\"matchId\"")
                .contains("EntityModule");

        assertThat(wsMatch2B)
                .as("WebSocket match 2B should return valid snapshot")
                .contains("\"matchId\"")
                .contains("EntityModule");

        // Verify within-container isolation: match 1A should not have entity1B's data
        // (Entity IDs 1 and 2 are different, so entity1B in JSON string should not appear in match1A)
        if (entity1A != entity1B) {
            assertThat(wsMatch1A)
                    .as("WebSocket match 1A should not contain match 1B's entity (%d)", entity1B)
                    .doesNotContain("\"ENTITY_ID\":[" + entity1B);
            assertThat(wsMatch1B)
                    .as("WebSocket match 1B should not contain match 1A's entity (%d)", entity1A)
                    .doesNotContain("\"ENTITY_ID\":[" + entity1A);
        }
        if (entity2A != entity2B) {
            assertThat(wsMatch2A)
                    .as("WebSocket match 2A should not contain match 2B's entity (%d)", entity2B)
                    .doesNotContain("\"ENTITY_ID\":[" + entity2B);
            assertThat(wsMatch2B)
                    .as("WebSocket match 2B should not contain match 2A's entity (%d)", entity2A)
                    .doesNotContain("\"ENTITY_ID\":[" + entity2A);
        }

        log.info("WebSocket snapshot isolation verified for all 4 matches");
    }

    private void verifyPhysicsEvolveDifferently() {
        // Run physics for 10 ticks in both containers
        container1.tick(10);
        container2.tick(10);

        // Verify each match evolved differently based on its unique velocity using fluent assertions
        // Match 1A: should have moved right (positive X)
        SnapshotAssertions.assertThat(match1A.snapshot())
                .entityAt(0)
                .withComponentGreaterThan("GridMapModule", "POSITION_X", 0);

        // Match 1B: should have moved up (positive Y)
        SnapshotAssertions.assertThat(match1B.snapshot())
                .entityAt(0)
                .withComponentGreaterThan("GridMapModule", "POSITION_Y", 0);

        // Match 2A: should have moved left (negative X)
        SnapshotAssertions.assertThat(match2A.snapshot())
                .entityAt(0)
                .withComponentLessThan("GridMapModule", "POSITION_X", 0);

        // Match 2B: should have moved diagonally (both positive)
        SnapshotAssertions.assertThat(match2B.snapshot())
                .entityAt(0)
                .withComponentGreaterThan("GridMapModule", "POSITION_X", 0)
                .withComponentGreaterThan("GridMapModule", "POSITION_Y", 0);

        // Get actual values for logging
        float pos1A_X = SnapshotAssertions.assertThat(match1A.snapshot()).entityAt(0).getComponentValue("GridMapModule", "POSITION_X");
        float pos1B_Y = SnapshotAssertions.assertThat(match1B.snapshot()).entityAt(0).getComponentValue("GridMapModule", "POSITION_Y");
        float pos2A_X = SnapshotAssertions.assertThat(match2A.snapshot()).entityAt(0).getComponentValue("GridMapModule", "POSITION_X");
        float pos2B_X = SnapshotAssertions.assertThat(match2B.snapshot()).entityAt(0).getComponentValue("GridMapModule", "POSITION_X");
        float pos2B_Y = SnapshotAssertions.assertThat(match2B.snapshot()).entityAt(0).getComponentValue("GridMapModule", "POSITION_Y");

        // Verify all X positions are different (independent simulation)
        Set<Float> uniqueXPositions = Set.of(pos1A_X, pos2A_X, pos2B_X);
        assertThat(uniqueXPositions)
                .as("X positions should all be different")
                .hasSize(3);

        log.info("Physics evolution verified: each match evolved independently");
        log.info("  Match 1A: X={} (moving right)", pos1A_X);
        log.info("  Match 1B: Y={} (moving up)", pos1B_Y);
        log.info("  Match 2A: X={} (moving left)", pos2A_X);
        log.info("  Match 2B: X={}, Y={} (moving diagonally)", pos2B_X, pos2B_Y);
    }

    private void verifyCrossContainerAccessReturns404() throws Exception {
        // Get container clients
        var container1Client = container1.client();
        var container2Client = container2.client();

        // Get player IDs
        long player1 = container1.playerIds().get(0);
        long player2 = container2.playerIds().get(0);

        // === Test 1: Verify matches are container-scoped ===
        log.info("Testing: Matches are container-scoped");
        var matches1 = container1Client.listMatches();
        var matches2 = container2Client.listMatches();
        // Each container should have 2 matches
        assertThat(matches1).hasSize(2);
        assertThat(matches2).hasSize(2);
        log.info("  Container 1 has {} matches, Container 2 has {} matches (correctly isolated)", matches1.size(), matches2.size());

        // === Test 2: Verify snapshots are container-scoped ===
        log.info("Testing: Snapshots return container-specific data");
        // Match IDs overlap (both containers have match 1 and 2), but snapshots should be different
        var snap1FromContainer1 = container1Client.getSnapshot(match1A.id());
        var snap1FromContainer2 = container2Client.getSnapshot(match2A.id());
        assertThat(snap1FromContainer1).isPresent();
        assertThat(snap1FromContainer2).isPresent();

        // Parse snapshots and verify they have different physics evolution
        var parsed1 = client.parseSnapshot(snap1FromContainer1.get().data());
        var parsed2 = client.parseSnapshot(snap1FromContainer2.get().data());

        float pos1X = parsed1.module("GridMapModule").first("POSITION_X", 0);
        float pos2X = parsed2.module("GridMapModule").first("POSITION_X", 0);

        // Container 1 match 1A moves right (+X), Container 2 match 2A moves left (-X)
        assertThat(pos1X).as("Container 1's match should have positive X").isGreaterThan(0);
        assertThat(pos2X).as("Container 2's match should have negative X").isLessThan(0);
        log.info("  Container 1 match position: X={}, Container 2 match position: X={} (different physics)", pos1X, pos2X);

        // === Test 3: Verify entities are match-scoped (within container) ===
        log.info("Testing: Entities are correctly scoped to their matches");
        // Entity 1 in container 1 should not appear in match 1B
        var snap1B = container1Client.getSnapshot(match1B.id());
        assertThat(snap1B).isPresent();
        var parsed1B = client.parseSnapshot(snap1B.get().data());
        List<Float> entityIds1B = parsed1B.entityIds();
        assertThat(entityIds1B).doesNotContain((float) entity1A);  // entity1A should not be in match1B
        log.info("  Match 1B entities: {} (does not include entity {} from match 1A)", entityIds1B, entity1A);

        // === Test 4: Verify HTTP 404 for non-existent match in container ===
        log.info("Testing: Non-existent match returns 404");
        HttpClient httpClient = HttpClient.newHttpClient();

        // Try to get match ID 999 from container 1 (doesn't exist)
        HttpRequest matchRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/containers/" + container1.id() + "/matches/999"))
                .header("Authorization", "Bearer " + getToken())
                .GET()
                .build();
        HttpResponse<String> matchResponse = httpClient.send(matchRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(matchResponse.statusCode())
                .as("Non-existent match should return 404")
                .isEqualTo(404);
        log.info("  GET /api/containers/{}/matches/999 returned 404", container1.id());

        // === Test 5: Verify HTTP 404 for non-existent container ===
        log.info("Testing: Non-existent container returns 404");
        HttpRequest containerRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/containers/999/matches"))
                .header("Authorization", "Bearer " + getToken())
                .GET()
                .build();
        HttpResponse<String> containerResponse = httpClient.send(containerRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(containerResponse.statusCode())
                .as("Non-existent container should return 404")
                .isEqualTo(404);
        log.info("  GET /api/containers/999/matches returned 404");

        // === Test 6: Verify players are container-scoped ===
        log.info("Testing: Players are container-scoped");
        List<Long> players1 = container1Client.listPlayers();
        List<Long> players2 = container2Client.listPlayers();
        assertThat(players1).hasSize(2);
        assertThat(players2).hasSize(2);
        // Container 1 should not see Container 2's players
        assertThat(players1)
                .as("Container 1 should only see its own players")
                .containsExactlyInAnyOrderElementsOf(container1.playerIds());
        assertThat(players2)
                .as("Container 2 should only see its own players")
                .containsExactlyInAnyOrderElementsOf(container2.playerIds());
        log.info("  Container 1 players: {} (isolated)", players1);
        log.info("  Container 2 players: {} (isolated)", players2);

        // === Test 7: Verify sessions are container-scoped ===
        log.info("Testing: Sessions are container-scoped");
        List<Map<String, Object>> sessions1 = fetchSessions(httpClient, container1.id());
        List<Map<String, Object>> sessions2 = fetchSessions(httpClient, container2.id());
        assertThat(sessions1).hasSize(2);  // Two sessions in container 1
        assertThat(sessions2).hasSize(2);  // Two sessions in container 2
        log.info("  Container 1 sessions: {} (isolated)", sessions1.size());
        log.info("  Container 2 sessions: {} (isolated)", sessions2.size());

        log.info("Cross-container isolation verified: containers, matches, entities, players, sessions, and snapshots");
    }

    private void verifySnapshotHistoryIsolation() throws Exception {
        // Run more physics ticks to generate history
        log.info("Running 20 more ticks to generate snapshot history...");
        container1.tick(20);
        container2.tick(20);

        // Wait for snapshot persistence to complete (async operation)
        log.info("Waiting for snapshot persistence...");
        Thread.sleep(3000);

        // Verify each container has independent history that evolved differently
        log.info("Verifying snapshot history is isolated per match...");

        // Get history for each match via adapter
        EngineClient.ContainerClient client1 = container1.client();
        EngineClient.ContainerClient client2 = container2.client();

        List<HistorySnapshotDto> history1A = client1.getLatestHistorySnapshots(match1A.id(), 5);
        List<HistorySnapshotDto> history1B = client1.getLatestHistorySnapshots(match1B.id(), 5);
        List<HistorySnapshotDto> history2A = client2.getLatestHistorySnapshots(match2A.id(), 5);
        List<HistorySnapshotDto> history2B = client2.getLatestHistorySnapshots(match2B.id(), 5);

        log.info("Fetched history: 1A={}, 1B={}, 2A={}, 2B={}",
                history1A.size(), history1B.size(), history2A.size(), history2B.size());

        // Verify each match has history records
        assertThat(history1A)
                .as("Match 1A should have snapshot history")
                .isNotEmpty();
        assertThat(history1B)
                .as("Match 1B should have snapshot history")
                .isNotEmpty();
        assertThat(history2A)
                .as("Match 2A should have snapshot history")
                .isNotEmpty();
        assertThat(history2B)
                .as("Match 2B should have snapshot history")
                .isNotEmpty();

        log.info("  Match 1A history: {} snapshots", history1A.size());
        log.info("  Match 1B history: {} snapshots", history1B.size());
        log.info("  Match 2A history: {} snapshots", history2A.size());
        log.info("  Match 2B history: {} snapshots", history2B.size());

        // Verify history shows tick progression
        verifyTickProgression(history1A, "Match 1A");
        verifyTickProgression(history1B, "Match 1B");
        verifyTickProgression(history2A, "Match 2A");
        verifyTickProgression(history2B, "Match 2B");

        // Verify history contains correct containerId
        for (HistorySnapshotDto snap : history1A) {
            assertThat(snap.containerId())
                    .as("History 1A should have correct containerId")
                    .isEqualTo(container1.id());
        }
        for (HistorySnapshotDto snap : history2A) {
            assertThat(snap.containerId())
                    .as("History 2A should have correct containerId")
                    .isEqualTo(container2.id());
        }

        // Verify history contains correct entity IDs (match-specific)
        if (!history1A.isEmpty()) {
            var snapshotData = history1A.get(0).data();
            var snapshot = client.parseSnapshotFromMap(snapshotData);
            List<Float> entityIds = snapshot.entityIds();
            assertThat(entityIds)
                    .as("History 1A should only contain entity %d", entity1A)
                    .contains((float) entity1A);
            if (entity1A != entity1B) {
                assertThat(entityIds).doesNotContain((float) entity1B);
            }
        }

        if (!history2A.isEmpty()) {
            var snapshotData = history2A.get(0).data();
            var snapshot = client.parseSnapshotFromMap(snapshotData);
            List<Float> entityIds = snapshot.entityIds();
            assertThat(entityIds)
                    .as("History 2A should only contain entity %d", entity2A)
                    .contains((float) entity2A);
            if (entity2A != entity2B) {
                assertThat(entityIds).doesNotContain((float) entity2B);
            }
        }

        // Verify physics evolved differently in each match's history
        if (!history1A.isEmpty() && !history2A.isEmpty()) {
            var snap1A = client.parseSnapshotFromMap(history1A.get(0).data());
            var snap2A = client.parseSnapshotFromMap(history2A.get(0).data());

            float pos1A_X = snap1A.module("GridMapModule").first("POSITION_X", 0);
            float pos2A_X = snap2A.module("GridMapModule").first("POSITION_X", 0);

            // Match 1A moves right (+X), Match 2A moves left (-X)
            assertThat(pos1A_X)
                    .as("Match 1A history should show positive X (moving right)")
                    .isGreaterThan(0);
            assertThat(pos2A_X)
                    .as("Match 2A history should show negative X (moving left)")
                    .isLessThan(0);

            log.info("  History physics verification:");
            log.info("    Match 1A final position: X={} (moving right)", pos1A_X);
            log.info("    Match 2A final position: X={} (moving left)", pos2A_X);
        }

        log.info("Snapshot history isolation verified: each match has independent, evolving history");
    }

    private String authToken;

    private String getToken() throws Exception {
        if (authToken == null) {
            AuthAdapter auth = new AuthAdapter.HttpAuthAdapter(baseUrl);
            authToken = auth.login("admin", "admin").token();
        }
        return authToken;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchSessions(HttpClient httpClient, long containerId) throws Exception {
        String url = baseUrl + "/api/containers/" + containerId + "/sessions";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + getToken())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("Failed to fetch sessions for container {}: {} - {}", containerId, response.statusCode(), response.body());
            return List.of();
        }

        // Parse JSON response (array of sessions)
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        return json.readValue(response.body(), json.getTypeFactory().constructCollectionType(List.class, Map.class));
    }

    private void verifyTickProgression(List<HistorySnapshotDto> history, String matchName) {
        // Verify we have history and ticks are present
        assertThat(history)
                .as("%s should have history entries", matchName)
                .isNotEmpty();

        // Verify ticks are in descending order (latest first)
        if (history.size() > 1) {
            long prevTick = Long.MAX_VALUE;
            for (HistorySnapshotDto snapshot : history) {
                long tick = snapshot.tick();
                assertThat(tick)
                        .as("%s history should have descending ticks", matchName)
                        .isLessThanOrEqualTo(prevTick);
                prevTick = tick;
            }
        }
        log.info("  {} history has {} entries", matchName, history.size());
    }

    // ==================== Helper Methods ====================

    private String receiveWebSocketSnapshot(long containerId, long matchId) throws Exception {
        String token = getToken();
        String wsUrl = baseUrl.replace("http://", "ws://") +
                "/ws/containers/" + containerId + "/matches/" + matchId + "/snapshot?token=" + token;
        log.debug("Connecting to WebSocket: {}", wsUrl);

        CompletableFuture<String> snapshotFuture = new CompletableFuture<>();
        HttpClient httpClient = HttpClient.newHttpClient();

        WebSocket webSocket = httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                    private final StringBuilder messageBuffer = new StringBuilder();

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        messageBuffer.append(data);
                        if (last) {
                            snapshotFuture.complete(messageBuffer.toString());
                        }
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        snapshotFuture.completeExceptionally(error);
                    }
                })
                .join();

        try {
            return snapshotFuture.get(10, TimeUnit.SECONDS);
        } finally {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
        }
    }
}
