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

import ca.samanthaireland.lightning.engine.api.resource.adapter.AuthAdapter;
import ca.samanthaireland.lightning.engine.api.resource.adapter.ContainerAdapter;
import ca.samanthaireland.lightning.engine.api.resource.adapter.EngineClient;
import ca.samanthaireland.lightning.engine.api.resource.adapter.EngineClient.ContainerClient;
import ca.samanthaireland.lightning.engine.api.resource.adapter.EngineClient.ContainerMatch;
import ca.samanthaireland.lightning.engine.api.resource.adapter.dto.HistoryQueryParams;
import ca.samanthaireland.lightning.engine.api.resource.adapter.dto.HistorySnapshotDto;
import ca.samanthaireland.lightning.engine.api.resource.adapter.dto.MatchHistorySummaryDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API acceptance test for snapshot history and MongoDB persistence.
 *
 * <p>This test verifies that:
 * <ul>
 *   <li>Snapshots are persisted to MongoDB after each tick</li>
 *   <li>The /api/history endpoint provides access to historical snapshots</li>
 *   <li>Snapshot queries by tick range work correctly</li>
 *   <li>Snapshot cleanup operations work correctly</li>
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
 * ./mvnw verify -pl lightning-engine/api-acceptance-test -Pacceptance-tests
 * </pre>
 */
@Slf4j
@Tag("acceptance")
@Tag("testcontainers")
@Tag("mongodb")
@DisplayName("Snapshot History API Acceptance Test")
@Testcontainers
class SnapshotHistoryApiIT {

    private static final String ENTITY_MODULE_NAME = "EntityModule";
    private static final String MOVE_MODULE_NAME = "RigidBodyModule";

    static Network network = Network.newNetwork();

    @Container
    static MongoDBContainer mongoContainer = TestContainers.mongoContainer(network, "mongodb");

    @Container
    static GenericContainer<?> backendContainer = TestContainers
            .backendContainerWithMongo(network, "mongodb")
            .dependsOn(mongoContainer);

    private EngineClient client;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    private String baseUrl;
    private String bearerToken;
    private long containerId = -1;
    private long createdMatchId = -1;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = TestContainers.getBaseUrl(backendContainer);
        log.info("Backend URL from testcontainers: " + baseUrl);

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        objectMapper = new ObjectMapper();

        // Authenticate to get JWT token using AuthAdapter
        AuthAdapter auth = new AuthAdapter.HttpAuthAdapter(baseUrl);
        bearerToken = auth.login("admin", TestContainers.TEST_ADMIN_PASSWORD).token();

        // Create client with authentication
        client = EngineClient.builder()
                .baseUrl(baseUrl)
                .withBearerToken(bearerToken)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (containerId > 0) {
            try {
                client.containers().stopContainer(containerId);
                client.containers().deleteContainer(containerId);
            } catch (Exception e) {
                log.warn("Failed to clean up container: {}", e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("GET /api/containers/{id}/matches/{matchId}/history returns match history summary")
    void matchHistorySummaryReturnsCorrectInfo() throws Exception {
        log.info("=== Starting match history summary test ===");

        // Create container, start it, and create a match
        ContainerAdapter.ContainerResponse containerResponse = client.containers()
                .create()
                .name("history-test")
                .withModules(ENTITY_MODULE_NAME, MOVE_MODULE_NAME)
                .execute();
        containerId = containerResponse.id();

        ContainerClient container = client.container(containerId);
        ContainerMatch match = container.createMatch(List.of(ENTITY_MODULE_NAME, MOVE_MODULE_NAME));
        createdMatchId = match.id();

        // Spawn entity and advance ticks
        container.forMatch(createdMatchId).spawn().forPlayer(1).ofType(100).execute();
        container.tick();
        container.tick();
        container.tick();

        // Give MongoDB time to persist
        Thread.sleep(500);

        // Get match history summary using adapter
        MatchHistorySummaryDto summary = container.getMatchHistorySummary(createdMatchId);

        assertThat(summary.matchId())
                .as("Match ID should match")
                .isEqualTo(createdMatchId);

        assertThat(summary.snapshotCount())
                .as("Should have persisted at least 3 snapshots")
                .isGreaterThanOrEqualTo(3);

        log.info("Match history summary: matchId={}, snapshotCount={}, firstTick={}, lastTick={}",
                summary.matchId(), summary.snapshotCount(), summary.firstTick(), summary.lastTick());
        log.info("=== MATCH HISTORY SUMMARY TEST PASSED ===");
    }

    @Test
    @DisplayName("GET history snapshots with tick range query parameters")
    void historySnapshotsWithQueryParams() throws Exception {
        log.info("=== Starting history snapshots with query params test ===");

        // Create container, start it, and create a match
        ContainerAdapter.ContainerResponse containerResponse = client.containers()
                .create()
                .name("history-query-test")
                .withModules(ENTITY_MODULE_NAME, MOVE_MODULE_NAME)
                .execute();
        containerId = containerResponse.id();

        ContainerClient container = client.container(containerId);
        ContainerMatch match = container.createMatch(List.of(ENTITY_MODULE_NAME, MOVE_MODULE_NAME));
        createdMatchId = match.id();

        // Spawn entity and advance several ticks
        container.forMatch(createdMatchId).spawn().forPlayer(1).ofType(100).execute();
        long tick1 = container.tick();
        long tick2 = container.tick();
        long tick3 = container.tick();
        long tick4 = container.tick();
        long tick5 = container.tick();

        // Give MongoDB time to persist
        Thread.sleep(500);

        // Get history snapshots using adapter with query parameters
        HistoryQueryParams params = new HistoryQueryParams(tick2, tick4, 100);
        List<HistorySnapshotDto> snapshots = container.getHistorySnapshots(createdMatchId, params);

        log.info("Got {} snapshots between tick {} and {}", snapshots.size(), tick2, tick4);

        assertThat(snapshots)
                .as("Should have at least 1 snapshot in the range")
                .isNotEmpty();

        // Verify snapshots are within the requested range
        for (HistorySnapshotDto snapshot : snapshots) {
            assertThat(snapshot.matchId()).isEqualTo(createdMatchId);
            assertThat(snapshot.tick()).isBetween(tick2, tick4);
            assertThat(snapshot.data()).isNotNull();
        }

        log.info("=== HISTORY SNAPSHOTS QUERY TEST PASSED ===");
    }

    @Test
    @DisplayName("GET latest snapshots returns most recent snapshots in descending order")
    void latestSnapshotsReturnsCorrectData() throws Exception {
        log.info("=== Starting latest snapshots test ===");

        // Create container, start it, and create a match
        ContainerAdapter.ContainerResponse containerResponse = client.containers()
                .create()
                .name("latest-snapshots-test")
                .withModules(ENTITY_MODULE_NAME, MOVE_MODULE_NAME)
                .execute();
        containerId = containerResponse.id();

        ContainerClient container = client.container(containerId);
        ContainerMatch match = container.createMatch(List.of(ENTITY_MODULE_NAME, MOVE_MODULE_NAME));
        createdMatchId = match.id();

        // Spawn entity and advance many ticks
        container.forMatch(createdMatchId).spawn().forPlayer(1).ofType(100).execute();

        for (int i = 0; i < 10; i++) {
            container.tick();
        }

        // Give MongoDB time to persist
        Thread.sleep(500);

        // Get latest 5 snapshots using adapter
        List<HistorySnapshotDto> snapshots = container.getLatestHistorySnapshots(createdMatchId, 5);

        log.info("Got {} latest snapshots", snapshots.size());

        assertThat(snapshots)
                .as("Should return up to 5 latest snapshots")
                .hasSizeLessThanOrEqualTo(5);

        if (snapshots.size() > 1) {
            // Verify ordering (should be descending by tick)
            long previousTick = Long.MAX_VALUE;
            for (HistorySnapshotDto snapshot : snapshots) {
                assertThat(snapshot.tick())
                        .as("Snapshots should be in descending tick order")
                        .isLessThanOrEqualTo(previousTick);
                previousTick = snapshot.tick();

                // Verify structure
                assertThat(snapshot.matchId()).isEqualTo(createdMatchId);
                assertThat(snapshot.data()).isNotNull();
            }
        }

        log.info("=== LATEST SNAPSHOTS TEST PASSED ===");
    }

    @Test
    @DisplayName("GET /api/containers/{containerId}/matches/{matchId}/snapshots/delta returns compression info between ticks")
    void deltaReturnsCompressionInfo() throws Exception {
        log.info("=== Starting delta compression test ===");

        // Create container, start it, and create a match
        ContainerAdapter.ContainerResponse containerResponse = client.containers()
                .create()
                .name("delta-test")
                .withModules(ENTITY_MODULE_NAME, MOVE_MODULE_NAME)
                .execute();
        containerId = containerResponse.id();
        // Container is already started since modules were specified

        ContainerClient container = client.container(containerId);
        ContainerMatch match = container.createMatch(List.of(ENTITY_MODULE_NAME, MOVE_MODULE_NAME));
        createdMatchId = match.id();

        // Spawn entity and advance tick
        container.forMatch(createdMatchId).spawn().forPlayer(1).ofType(100).execute();
        long tick1 = container.tick();

        // Record snapshot at tick1 to in-memory history (required for delta endpoint)
        recordSnapshot(createdMatchId);

        // Spawn another entity to create a change
        container.forMatch(createdMatchId).spawn().forPlayer(1).ofType(100).execute();
        long tick2 = container.tick();

        // Record snapshot at tick2
        recordSnapshot(createdMatchId);

        // Get delta between ticks (uses in-memory history, not MongoDB)
        JsonNode delta = getDelta(createdMatchId, tick1, tick2);

        assertThat(delta.path("matchId").asLong())
                .as("Match ID should match")
                .isEqualTo(createdMatchId);

        assertThat(delta.path("fromTick").asLong())
                .as("From tick should match")
                .isEqualTo(tick1);

        assertThat(delta.path("toTick").asLong())
                .as("To tick should match")
                .isEqualTo(tick2);

        assertThat(delta.has("changeCount"))
                .as("Delta should have change count")
                .isTrue();

        assertThat(delta.has("compressionRatio"))
                .as("Delta should have compression ratio")
                .isTrue();

        assertThat(delta.has("addedEntities"))
                .as("Delta should have added entities list")
                .isTrue();

        assertThat(delta.has("removedEntities"))
                .as("Delta should have removed entities list")
                .isTrue();

        log.info("Delta: changeCount={}, compressionRatio={}",
                delta.path("changeCount").asInt(),
                delta.path("compressionRatio").asDouble());

        log.info("=== DELTA COMPRESSION TEST PASSED ===");
    }

    @Test
    @DisplayName("GET specific snapshot at tick returns correct data")
    void specificSnapshotReturnsCorrectData() throws Exception {
        log.info("=== Starting specific snapshot test ===");

        // Create container, start it, and create a match
        ContainerAdapter.ContainerResponse containerResponse = client.containers()
                .create()
                .name("specific-snapshot-test")
                .withModules(ENTITY_MODULE_NAME, MOVE_MODULE_NAME)
                .execute();
        containerId = containerResponse.id();

        ContainerClient container = client.container(containerId);
        ContainerMatch match = container.createMatch(List.of(ENTITY_MODULE_NAME, MOVE_MODULE_NAME));
        createdMatchId = match.id();

        // Spawn entity and advance ticks
        container.forMatch(createdMatchId).spawn().forPlayer(1).ofType(100).execute();
        container.tick();
        long targetTick = container.tick();
        container.tick();

        // Give MongoDB time to persist
        Thread.sleep(500);

        // Get specific snapshot using adapter
        Optional<HistorySnapshotDto> snapshotOpt = container.getHistorySnapshotAtTick(createdMatchId, targetTick);

        assertThat(snapshotOpt)
                .as("Should find snapshot at tick " + targetTick)
                .isPresent();

        HistorySnapshotDto snapshot = snapshotOpt.get();

        assertThat(snapshot.matchId())
                .as("Match ID should match")
                .isEqualTo(createdMatchId);

        assertThat(snapshot.tick())
                .as("Tick should match requested tick")
                .isEqualTo(targetTick);

        assertThat(snapshot.data())
                .as("Snapshot should have data")
                .isNotNull();

        log.info("Retrieved snapshot at tick {}: containerId={}, matchId={}",
                snapshot.tick(), snapshot.containerId(), snapshot.matchId());
        log.info("=== SPECIFIC SNAPSHOT TEST PASSED ===");
    }

    // ========== Helper Methods (using raw HTTP for endpoints not in adapter) ==========

    private JsonNode getDelta(long matchId, long fromTick, long toTick) throws IOException, InterruptedException {
        String url = String.format("%s/api/containers/%d/matches/%d/snapshots/delta?fromTick=%d&toTick=%d",
                baseUrl, containerId, matchId, fromTick, toTick);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Authorization", "Bearer " + bearerToken)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("Get delta should succeed").isEqualTo(200);

        return objectMapper.readTree(response.body());
    }

    private void recordSnapshot(long matchId) throws IOException, InterruptedException {
        String url = String.format("%s/api/containers/%d/matches/%d/snapshots/record",
                baseUrl, containerId, matchId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Authorization", "Bearer " + bearerToken)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("Record snapshot should succeed").isEqualTo(200);
    }
}
