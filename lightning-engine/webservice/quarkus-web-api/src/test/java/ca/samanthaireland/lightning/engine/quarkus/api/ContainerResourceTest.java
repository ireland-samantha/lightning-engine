/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.quarkus.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * Comprehensive tests for container-scoped REST endpoints.
 * Replaces the legacy global endpoint tests.
 */
@QuarkusTest
@TestSecurity(user = "admin", roles = "admin")
@DisplayName("ContainerResource")
class ContainerResourceTest {

    private long containerId;

    /**
     * Helper to create a request with JSON content type.
     */
    private RequestSpecification jsonRequest() {
        return given().contentType(ContentType.JSON);
    }

    @BeforeEach
    void setUp() {
        // Create and start a container for each test
        Response createResponse = jsonRequest()
                .body("{\"name\": \"test-container\"}")
                .when().post("/api/containers")
                .then()
                .statusCode(201)
                .extract().response();

        containerId = createResponse.jsonPath().getLong("id");

        // Start the container
        jsonRequest()
                .when().post("/api/containers/" + containerId + "/start")
                .then()
                .statusCode(200);
    }

    @AfterEach
    void tearDown() {
        // Stop auto-advance if running (ignore errors)
        try {
            jsonRequest().when().post("/api/containers/" + containerId + "/stop-auto");
        } catch (Exception ignored) {}

        // Stop the container (ignore errors)
        try {
            jsonRequest().when().post("/api/containers/" + containerId + "/stop");
        } catch (Exception ignored) {}

        // Delete the container (ignore errors)
        try {
            jsonRequest().when().delete("/api/containers/" + containerId);
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // Container CRUD
    // =========================================================================

    @Test
    @DisplayName("getAllContainers returns list")
    void getAllContainers_returnsList() {
        given()
                .when().get("/api/containers")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("getContainer returns container by ID")
    void getContainer_returnsById() {
        given()
                .when().get("/api/containers/" + containerId)
                .then()
                .statusCode(200)
                .body("id", is((int) containerId))
                .body("name", is("test-container"));
    }

    @Test
    @DisplayName("getContainer returns 404 for non-existent ID")
    void getContainer_returns404() {
        given()
                .when().get("/api/containers/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("deleteContainer removes container")
    void deleteContainer_removes() {
        // Create new container for this test
        Response createResponse = jsonRequest()
                .body("{\"name\": \"to-delete\"}")
                .when().post("/api/containers")
                .then()
                .statusCode(201)
                .extract().response();

        long newContainerId = createResponse.jsonPath().getLong("id");

        // Start the container
        jsonRequest()
                .when().post("/api/containers/" + newContainerId + "/start")
                .then()
                .statusCode(200);

        // Stop the container (required before deletion)
        jsonRequest()
                .when().post("/api/containers/" + newContainerId + "/stop")
                .then()
                .statusCode(200);

        // Delete it
        jsonRequest()
                .when().delete("/api/containers/" + newContainerId)
                .then()
                .statusCode(204);

        // Verify it's gone
        given()
                .when().get("/api/containers/" + newContainerId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("deleteContainer returns 404 for non-existent container")
    void deleteContainer_returns404() {
        jsonRequest()
                .when().delete("/api/containers/999999")
                .then()
                .statusCode(404);
    }

    // =========================================================================
    // Container Lifecycle
    // =========================================================================

    @Test
    @DisplayName("start transitions container to RUNNING")
    void start_transitionsToRunning() {
        // Container already started in setUp, verify status
        given()
                .when().get("/api/containers/" + containerId)
                .then()
                .statusCode(200)
                .body("status", is("RUNNING"));
    }

    @Test
    @DisplayName("pause transitions container to PAUSED")
    void pause_transitionsToPaused() {
        jsonRequest()
                .when().post("/api/containers/" + containerId + "/pause")
                .then()
                .statusCode(200)
                .body("status", is("PAUSED"));

        // Resume to allow tearDown to work
        jsonRequest()
                .when().post("/api/containers/" + containerId + "/resume");
    }

    @Test
    @DisplayName("resume transitions container to RUNNING from PAUSED")
    void resume_transitionsToRunning() {
        jsonRequest()
                .when().post("/api/containers/" + containerId + "/pause")
                .then()
                .statusCode(200);

        jsonRequest()
                .when().post("/api/containers/" + containerId + "/resume")
                .then()
                .statusCode(200)
                .body("status", is("RUNNING"));
    }

    @Test
    @DisplayName("stop transitions container to STOPPED")
    void stop_transitionsToStopped() {
        // Create and start a new container for this test
        Response createResponse = jsonRequest()
                .body("{\"name\": \"to-stop\"}")
                .when().post("/api/containers")
                .then()
                .statusCode(201)
                .extract().response();

        long newContainerId = createResponse.jsonPath().getLong("id");

        // Start it
        jsonRequest()
                .when().post("/api/containers/" + newContainerId + "/start")
                .then()
                .statusCode(200);

        // Stop it
        jsonRequest()
                .when().post("/api/containers/" + newContainerId + "/stop")
                .then()
                .statusCode(200)
                .body("status", is("STOPPED"));

        // Cleanup
        jsonRequest().when().delete("/api/containers/" + newContainerId);
    }

    @Test
    @DisplayName("lifecycle endpoints return 404 for non-existent container")
    void lifecycle_returns404_forNonExistentContainer() {
        jsonRequest()
                .when().post("/api/containers/999999/start")
                .then()
                .statusCode(404);

        jsonRequest()
                .when().post("/api/containers/999999/stop")
                .then()
                .statusCode(404);

        jsonRequest()
                .when().post("/api/containers/999999/pause")
                .then()
                .statusCode(404);

        jsonRequest()
                .when().post("/api/containers/999999/resume")
                .then()
                .statusCode(404);
    }

    // =========================================================================
    // Tick Control
    // =========================================================================

    @Test
    @DisplayName("getTick returns current tick")
    void getTick_returnsCurrent() {
        given()
                .when().get("/api/containers/" + containerId + "/tick")
                .then()
                .statusCode(200)
                .body("tick", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("advanceTick increments tick")
    void advanceTick_increments() {
        int currentTick = given()
                .when().get("/api/containers/" + containerId + "/tick")
                .then()
                .statusCode(200)
                .extract().path("tick");

        jsonRequest()
                .when().post("/api/containers/" + containerId + "/tick")
                .then()
                .statusCode(200)
                .body("tick", is(currentTick + 1));
    }

    @Test
    @DisplayName("play starts auto-advance")
    void play_startsAutoAdvance() {
        jsonRequest()
                .when().post("/api/containers/" + containerId + "/play?intervalMs=50")
                .then()
                .statusCode(200)
                .body("autoAdvancing", is(true));
    }

    @Test
    @DisplayName("stop-auto stops auto-advance")
    void stopAuto_stopsAutoAdvance() {
        jsonRequest()
                .when().post("/api/containers/" + containerId + "/play?intervalMs=50")
                .then()
                .statusCode(200);

        jsonRequest()
                .when().post("/api/containers/" + containerId + "/stop-auto")
                .then()
                .statusCode(200)
                .body("autoAdvancing", is(false));
    }

    @Test
    @DisplayName("getStatus returns play status")
    void getStatus_returnsPlayStatus() {
        given()
                .when().get("/api/containers/" + containerId + "/status")
                .then()
                .statusCode(200)
                .body("playing", notNullValue())
                .body("tick", notNullValue());
    }

    @Test
    @DisplayName("play auto-advances tick")
    void play_autoAdvancesTick() throws InterruptedException {
        int initialTick = given()
                .when().get("/api/containers/" + containerId + "/tick")
                .then()
                .statusCode(200)
                .extract().path("tick");

        jsonRequest()
                .when().post("/api/containers/" + containerId + "/play?intervalMs=10")
                .then()
                .statusCode(200);

        Thread.sleep(100);

        jsonRequest()
                .when().post("/api/containers/" + containerId + "/stop-auto")
                .then()
                .statusCode(200);

        given()
                .when().get("/api/containers/" + containerId + "/tick")
                .then()
                .statusCode(200)
                .body("tick", greaterThan(initialTick));
    }

    @Test
    @DisplayName("play with default interval uses default value")
    void play_withDefaultInterval_usesDefault() {
        jsonRequest()
                .when().post("/api/containers/" + containerId + "/play")
                .then()
                .statusCode(200)
                .body("autoAdvancing", is(true));
    }

    // =========================================================================
    // Match Management
    // =========================================================================

    @Test
    @DisplayName("createMatch returns created match")
    void createMatch_returnsCreated() {
        jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .body("id", greaterThan(0));
    }

    @Test
    @DisplayName("getMatches returns list")
    void getMatches_returnsList() {
        // Create a match first
        jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches");

        given()
                .when().get("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("getMatch returns match by ID")
    void getMatch_returnsById() {
        Response createResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();

        int matchId = createResponse.jsonPath().getInt("id");

        given()
                .when().get("/api/containers/" + containerId + "/matches/" + matchId)
                .then()
                .statusCode(200)
                .body("id", is(matchId));
    }

    @Test
    @DisplayName("getMatch returns 404 for non-existent ID")
    void getMatch_returns404() {
        given()
                .when().get("/api/containers/" + containerId + "/matches/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("deleteMatch removes match")
    void deleteMatch_removes() {
        Response createResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();

        int matchId = createResponse.jsonPath().getInt("id");

        jsonRequest()
                .when().delete("/api/containers/" + containerId + "/matches/" + matchId)
                .then()
                .statusCode(204);

        given()
                .when().get("/api/containers/" + containerId + "/matches/" + matchId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("deleteMatch returns 404 for non-existent match")
    void deleteMatch_returns404_forNonExistent() {
        jsonRequest()
                .when().delete("/api/containers/" + containerId + "/matches/999999")
                .then()
                .statusCode(404);
    }

    // =========================================================================
    // Module Management
    // =========================================================================

    @Test
    @DisplayName("getModules returns list")
    void getModules_returnsList() {
        given()
                .when().get("/api/containers/" + containerId + "/modules")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("reloadModules returns list")
    void reloadModules_returnsList() {
        jsonRequest()
                .when().post("/api/containers/" + containerId + "/modules/reload")
                .then()
                .statusCode(200);
    }

    // =========================================================================
    // Command Management
    // =========================================================================

    @Test
    @DisplayName("getCommands returns list")
    void getCommands_returnsList() {
        given()
                .when().get("/api/containers/" + containerId + "/commands")
                .then()
                .statusCode(200);
    }

    // Note: submitCommand tests require proper CommandPayload deserialization setup
    // which involves concrete type info in the JSON. Testing the "command not found"
    // scenario would require complex payload setup beyond the scope of basic endpoint tests.

    // =========================================================================
    // Player Management
    // =========================================================================

    @Test
    @DisplayName("createPlayer returns created player")
    void createPlayer_returnsCreated() {
        jsonRequest()
                .body("{}")
                .when().post("/api/containers/" + containerId + "/players")
                .then()
                .statusCode(201)
                .body("id", greaterThan(0L));
    }

    @Test
    @DisplayName("getAllPlayers returns list")
    void getAllPlayers_returnsList() {
        given()
                .when().get("/api/containers/" + containerId + "/players")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("getPlayer returns player by ID")
    void getPlayer_returnsById() {
        Response createResponse = jsonRequest()
                .body("{}")
                .when().post("/api/containers/" + containerId + "/players")
                .then()
                .statusCode(201)
                .extract().response();

        long playerId = createResponse.jsonPath().getLong("id");

        given()
                .when().get("/api/containers/" + containerId + "/players/" + playerId)
                .then()
                .statusCode(200)
                .body("id", equalTo(playerId));
    }

    @Test
    @DisplayName("getPlayer returns 404 for non-existent player")
    void getPlayer_returns404_forNonExistent() {
        given()
                .when().get("/api/containers/" + containerId + "/players/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("deletePlayer removes player")
    void deletePlayer_removes() {
        Response createResponse = jsonRequest()
                .body("{}")
                .when().post("/api/containers/" + containerId + "/players")
                .then()
                .statusCode(201)
                .extract().response();

        long playerId = createResponse.jsonPath().getLong("id");

        jsonRequest()
                .when().delete("/api/containers/" + containerId + "/players/" + playerId)
                .then()
                .statusCode(204);

        given()
                .when().get("/api/containers/" + containerId + "/players/" + playerId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("deletePlayer returns 404 for non-existent player")
    void deletePlayer_returns404_forNonExistent() {
        jsonRequest()
                .when().delete("/api/containers/" + containerId + "/players/999999")
                .then()
                .statusCode(404);
    }

    // =========================================================================
    // Player-Match Association
    // =========================================================================

    @Test
    @DisplayName("joinMatch adds player to match")
    void joinMatch_addsPlayer() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        // Create player
        Response playerResponse = jsonRequest()
                .body("{}")
                .when().post("/api/containers/" + containerId + "/players")
                .then()
                .statusCode(201)
                .extract().response();
        long playerId = playerResponse.jsonPath().getLong("id");

        // Join player to match
        jsonRequest()
                .body("{\"playerId\": " + playerId + "}")
                .when().post("/api/containers/" + containerId + "/matches/" + matchId + "/players")
                .then()
                .statusCode(201)
                .body("playerId", equalTo(playerId))
                .body("matchId", is(matchId));
    }

    @Test
    @DisplayName("getPlayersInMatch returns players")
    void getPlayersInMatch_returns() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        // Create player
        Response playerResponse = jsonRequest()
                .body("{}")
                .when().post("/api/containers/" + containerId + "/players")
                .then()
                .statusCode(201)
                .extract().response();
        long playerId = playerResponse.jsonPath().getLong("id");

        // Join player to match
        jsonRequest()
                .body("{\"playerId\": " + playerId + "}")
                .when().post("/api/containers/" + containerId + "/matches/" + matchId + "/players");

        // Get players in match
        given()
                .when().get("/api/containers/" + containerId + "/matches/" + matchId + "/players")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("leaveMatch removes player from match")
    void leaveMatch_removesPlayerFromMatch() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        // Create player
        Response playerResponse = jsonRequest()
                .body("{}")
                .when().post("/api/containers/" + containerId + "/players")
                .then()
                .statusCode(201)
                .extract().response();
        long playerId = playerResponse.jsonPath().getLong("id");

        // Join player to match
        jsonRequest()
                .body("{\"playerId\": " + playerId + "}")
                .when().post("/api/containers/" + containerId + "/matches/" + matchId + "/players")
                .then()
                .statusCode(201);

        // Leave match
        jsonRequest()
                .when().delete("/api/containers/" + containerId + "/matches/" + matchId + "/players/" + playerId)
                .then()
                .statusCode(204);

        // Verify player is no longer in match
        given()
                .when().get("/api/containers/" + containerId + "/matches/" + matchId + "/players/" + playerId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("getPlayerMatch returns 404 for non-existent association")
    void getPlayerMatch_returns404_forNonExistent() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        // Create player (not joined to match)
        Response playerResponse = jsonRequest()
                .body("{}")
                .when().post("/api/containers/" + containerId + "/players")
                .then()
                .statusCode(201)
                .extract().response();
        long playerId = playerResponse.jsonPath().getLong("id");

        // Try to get player-match association that doesn't exist
        given()
                .when().get("/api/containers/" + containerId + "/matches/" + matchId + "/players/" + playerId)
                .then()
                .statusCode(404);
    }

    // =========================================================================
    // Container Stats
    // =========================================================================

    @Test
    @DisplayName("getStats returns container statistics")
    void getStats_returnsStats() {
        given()
                .when().get("/api/containers/" + containerId + "/stats")
                .then()
                .statusCode(200)
                .body("entityCount", notNullValue())
                .body("maxEntities", notNullValue())
                .body("matchCount", notNullValue())
                .body("moduleCount", notNullValue());
    }

    // =========================================================================
    // AI Management
    // =========================================================================

    @Test
    @DisplayName("getContainerAI returns list")
    void getContainerAI_returnsList() {
        given()
                .when().get("/api/containers/" + containerId + "/ai")
                .then()
                .statusCode(200);
    }

    // =========================================================================
    // Resource Management
    // =========================================================================

    @Test
    @DisplayName("getContainerResources returns list")
    void getContainerResources_returnsList() {
        given()
                .when().get("/api/containers/" + containerId + "/resources")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("getResource returns 404 for non-existent resource")
    void getResource_returns404_forNonExistent() {
        given()
                .when().get("/api/containers/" + containerId + "/resources/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("deleteResource returns 404 for non-existent resource")
    void deleteResource_returns404_forNonExistent() {
        jsonRequest()
                .when().delete("/api/containers/" + containerId + "/resources/999999")
                .then()
                .statusCode(404);
    }

    // =========================================================================
    // Session Management
    // =========================================================================

    @Test
    @DisplayName("getSessions returns list for match")
    void getSessions_returnsList() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        given()
                .when().get("/api/containers/" + containerId + "/matches/" + matchId + "/sessions")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("getAllContainerSessions returns list")
    void getAllContainerSessions_returnsList() {
        given()
                .when().get("/api/containers/" + containerId + "/sessions")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("createSession returns created session")
    void createSession_returnsCreated() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        // Create player
        Response playerResponse = jsonRequest()
                .body("{}")
                .when().post("/api/containers/" + containerId + "/players")
                .then()
                .statusCode(201)
                .extract().response();
        long playerId = playerResponse.jsonPath().getLong("id");

        // Create session
        jsonRequest()
                .body("{\"playerId\": " + playerId + "}")
                .when().post("/api/containers/" + containerId + "/matches/" + matchId + "/sessions")
                .then()
                .statusCode(201)
                .body("playerId", equalTo(playerId))
                .body("matchId", is(matchId));
    }

    // =========================================================================
    // Container 404 Scenarios
    // =========================================================================

    @Test
    @DisplayName("tick endpoints return 404 for non-existent container")
    void tick_returns404_forNonExistentContainer() {
        given()
                .when().get("/api/containers/999999/tick")
                .then()
                .statusCode(404);

        jsonRequest()
                .when().post("/api/containers/999999/tick")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("match endpoints return 404 for non-existent container")
    void match_returns404_forNonExistentContainer() {
        given()
                .when().get("/api/containers/999999/matches")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("player endpoints return 404 for non-existent container")
    void player_returns404_forNonExistentContainer() {
        given()
                .when().get("/api/containers/999999/players")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("module endpoints return 404 for non-existent container")
    void module_returns404_forNonExistentContainer() {
        given()
                .when().get("/api/containers/999999/modules")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("command endpoints return 404 for non-existent container")
    void command_returns404_forNonExistentContainer() {
        given()
                .when().get("/api/containers/999999/commands")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("stats endpoint returns 404 for non-existent container")
    void stats_returns404_forNonExistentContainer() {
        given()
                .when().get("/api/containers/999999/stats")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("AI endpoint returns 404 for non-existent container")
    void ai_returns404_forNonExistentContainer() {
        given()
                .when().get("/api/containers/999999/ai")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("resources endpoint returns 404 for non-existent container")
    void resources_returns404_forNonExistentContainer() {
        given()
                .when().get("/api/containers/999999/resources")
                .then()
                .statusCode(404);
    }

    // =========================================================================
    // E2E: Container with Pre-selected Modules
    // =========================================================================

    @Test
    @DisplayName("E2E: create container with modules, send spawn command, verify snapshot")
    void e2e_createContainerWithModules_sendSpawn_verifySnapshot() {
        // Create container with EntityModule pre-selected
        // This tests the code path that was causing NullPointerException
        // Note: Container is automatically started when modules are specified
        Response createResponse = jsonRequest()
                .body("{\"name\": \"e2e-with-modules\", \"moduleNames\": [\"EntityModule\"]}")
                .when().post("/api/containers")
                .then()
                .statusCode(201)
                .extract().response();

        long e2eContainerId = createResponse.jsonPath().getLong("id");

        try {
            // Verify container was auto-started due to module installation
            given()
                    .when().get("/api/containers/" + e2eContainerId)
                    .then()
                    .statusCode(200)
                    .body("status", is("RUNNING"));

            // Verify modules are installed in container
            Response modulesResponse = given()
                    .when().get("/api/containers/" + e2eContainerId + "/modules")
                    .then()
                    .statusCode(200)
                    .extract().response();
            String modulesBody = modulesResponse.getBody().asString();
            assertThat(modulesBody)
                    .as("EntityModule should be installed in container")
                    .contains("EntityModule");

            // Create match with EntityModule
            Response matchResponse = jsonRequest()
                    .body("{\"enabledModuleNames\": [\"EntityModule\"]}")
                    .when().post("/api/containers/" + e2eContainerId + "/matches")
                    .then()
                    .statusCode(201)
                    .extract().response();
            int matchId = matchResponse.jsonPath().getInt("id");

            // Create player
            Response playerResponse = jsonRequest()
                    .body("{}")
                    .when().post("/api/containers/" + e2eContainerId + "/players")
                    .then()
                    .statusCode(201)
                    .extract().response();
            long playerId = playerResponse.jsonPath().getLong("id");

            // Create session (join player to match)
            jsonRequest()
                    .body("{\"playerId\": " + playerId + "}")
                    .when().post("/api/containers/" + e2eContainerId + "/matches/" + matchId + "/sessions")
                    .then()
                    .statusCode(201);

            // Send spawn command
            String spawnPayload = String.format("""
                {
                    "commandName": "spawn",
                    "matchId": %d,
                    "playerId": %d,
                    "parameters": {
                        "matchId": %d,
                        "playerId": %d,
                        "entityType": 1
                    }
                }
                """, matchId, playerId, matchId, playerId);

            jsonRequest()
                    .body(spawnPayload)
                    .when().post("/api/containers/" + e2eContainerId + "/commands")
                    .then()
                    .statusCode(202);  // 202 Accepted - command queued

            // Poll until entity appears in snapshot (command processing is async)
            String snapshotBody = null;
            for (int attempt = 0; attempt < 20; attempt++) {
                // Advance tick to execute the command
                jsonRequest()
                        .when().post("/api/containers/" + e2eContainerId + "/tick")
                        .then()
                        .statusCode(200);

                // Get container-scoped snapshot
                Response snapshotResponse = given()
                        .when().get("/api/containers/" + e2eContainerId + "/matches/" + matchId + "/snapshot")
                        .then()
                        .statusCode(200)
                        .extract().response();

                snapshotBody = snapshotResponse.getBody().asString();
                if (snapshotBody.contains("EntityModule")) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            assertThat(snapshotBody)
                    .as("Snapshot should contain EntityModule data after spawning entity")
                    .contains("EntityModule");

        } finally {
            // Cleanup
            try {
                jsonRequest().when().post("/api/containers/" + e2eContainerId + "/stop");
            } catch (Exception ignored) {}
            try {
                jsonRequest().when().delete("/api/containers/" + e2eContainerId);
            } catch (Exception ignored) {}
        }
    }

    // =========================================================================
    // Snapshot Management (ContainerSnapshotResource)
    // =========================================================================

    @Test
    @DisplayName("getMatchSnapshot returns snapshot data")
    void getMatchSnapshot_returnsSnapshot() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        given()
                .when().get("/api/containers/" + containerId + "/matches/" + matchId + "/snapshot")
                .then()
                .statusCode(200)
                .body("matchId", is(matchId))
                .body("tick", notNullValue());
    }

    @Test
    @DisplayName("getMatchSnapshot with playerId filter returns filtered snapshot")
    void getMatchSnapshot_withPlayerFilter() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        // Create player
        Response playerResponse = jsonRequest()
                .body("{}")
                .when().post("/api/containers/" + containerId + "/players")
                .then()
                .statusCode(201)
                .extract().response();
        long playerId = playerResponse.jsonPath().getLong("id");

        given()
                .when().get("/api/containers/" + containerId + "/matches/" + matchId + "/snapshot?playerId=" + playerId)
                .then()
                .statusCode(200)
                .body("matchId", is(matchId));
    }

    @Test
    @DisplayName("getMatchSnapshot returns 404 for non-existent match")
    void getMatchSnapshot_returns404_forNonExistentMatch() {
        given()
                .when().get("/api/containers/" + containerId + "/matches/999999/snapshot")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("recordSnapshot records current state")
    void recordSnapshot_recordsCurrentState() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        jsonRequest()
                .when().post("/api/containers/" + containerId + "/matches/" + matchId + "/snapshots/record")
                .then()
                .statusCode(200)
                .body("matchId", is(matchId))
                .body("recorded", is(true))
                .body("tick", notNullValue());
    }

    @Test
    @DisplayName("getSnapshotHistoryInfo returns history info")
    void getSnapshotHistoryInfo_returnsInfo() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        // Record a snapshot first
        jsonRequest()
                .when().post("/api/containers/" + containerId + "/matches/" + matchId + "/snapshots/record")
                .then()
                .statusCode(200);

        given()
                .when().get("/api/containers/" + containerId + "/matches/" + matchId + "/snapshots/history-info")
                .then()
                .statusCode(200)
                .body("matchId", is(matchId))
                .body("snapshotCount", greaterThanOrEqualTo(0))
                .body("currentTick", notNullValue());
    }

    @Test
    @DisplayName("getDeltaSnapshot requires fromTick parameter")
    void getDeltaSnapshot_requiresFromTick() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        given()
                .when().get("/api/containers/" + containerId + "/matches/" + matchId + "/snapshots/delta")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("getDeltaSnapshot returns 404 when fromTick not in history")
    void getDeltaSnapshot_returns404_whenFromTickNotInHistory() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        given()
                .when().get("/api/containers/" + containerId + "/matches/" + matchId + "/snapshots/delta?fromTick=999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("getDeltaSnapshot computes delta between recorded snapshots")
    void getDeltaSnapshot_computesDelta() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        // Record snapshot at tick 0
        jsonRequest()
                .when().post("/api/containers/" + containerId + "/matches/" + matchId + "/snapshots/record")
                .then()
                .statusCode(200);

        int recordedTick = given()
                .when().get("/api/containers/" + containerId + "/tick")
                .then()
                .extract().path("tick");

        // Advance tick and record again
        jsonRequest()
                .when().post("/api/containers/" + containerId + "/tick")
                .then()
                .statusCode(200);

        // Get delta from recorded tick to current
        given()
                .when().get("/api/containers/" + containerId + "/matches/" + matchId + "/snapshots/delta?fromTick=" + recordedTick)
                .then()
                .statusCode(200)
                .body("matchId", is(matchId))
                .body("fromTick", is(recordedTick))
                .body("toTick", notNullValue());
    }

    @Test
    @DisplayName("clearSnapshotHistory clears history")
    void clearSnapshotHistory_clearsHistory() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        // Record a snapshot
        jsonRequest()
                .when().post("/api/containers/" + containerId + "/matches/" + matchId + "/snapshots/record")
                .then()
                .statusCode(200);

        // Clear history
        jsonRequest()
                .when().delete("/api/containers/" + containerId + "/matches/" + matchId + "/snapshots/history")
                .then()
                .statusCode(200)
                .body("matchId", is(matchId))
                .body("cleared", is(true));
    }

    // =========================================================================
    // MongoDB History (ContainerHistoryResource) - Persistence Disabled
    // =========================================================================

    @Test
    @DisplayName("getHistorySummary returns 503 when persistence disabled")
    void getHistorySummary_returns503_whenPersistenceDisabled() {
        given()
                .when().get("/api/containers/" + containerId + "/history")
                .then()
                .statusCode(503);
    }

    @Test
    @DisplayName("getMatchHistorySummary returns 503 when persistence disabled")
    void getMatchHistorySummary_returns503_whenPersistenceDisabled() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        given()
                .when().get("/api/containers/" + containerId + "/matches/" + matchId + "/history")
                .then()
                .statusCode(503);
    }

    @Test
    @DisplayName("getHistorySnapshots returns 503 when persistence disabled")
    void getHistorySnapshots_returns503_whenPersistenceDisabled() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        given()
                .when().get("/api/containers/" + containerId + "/matches/" + matchId + "/history/snapshots")
                .then()
                .statusCode(503);
    }

    @Test
    @DisplayName("getLatestHistorySnapshots returns 503 when persistence disabled")
    void getLatestHistorySnapshots_returns503_whenPersistenceDisabled() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        given()
                .when().get("/api/containers/" + containerId + "/matches/" + matchId + "/history/snapshots/latest")
                .then()
                .statusCode(503);
    }

    @Test
    @DisplayName("getHistorySnapshot returns 503 when persistence disabled")
    void getHistorySnapshot_returns503_whenPersistenceDisabled() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        given()
                .when().get("/api/containers/" + containerId + "/matches/" + matchId + "/history/snapshots/0")
                .then()
                .statusCode(503);
    }

    @Test
    @DisplayName("deleteMatchHistory returns 503 when persistence disabled")
    void deleteMatchHistory_returns503_whenPersistenceDisabled() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        jsonRequest()
                .when().delete("/api/containers/" + containerId + "/matches/" + matchId + "/history")
                .then()
                .statusCode(503);
    }

    @Test
    @DisplayName("deleteOlderHistorySnapshots returns 503 when persistence disabled")
    void deleteOlderHistorySnapshots_returns503_whenPersistenceDisabled() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        jsonRequest()
                .when().delete("/api/containers/" + containerId + "/matches/" + matchId + "/history/older-than/100")
                .then()
                .statusCode(503);
    }

    // =========================================================================
    // Restore (ContainerRestoreResource) - Persistence Disabled
    // =========================================================================

    @Test
    @DisplayName("restoreMatch returns 503 when persistence disabled")
    void restoreMatch_returns503_whenPersistenceDisabled() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        jsonRequest()
                .when().post("/api/containers/" + containerId + "/matches/" + matchId + "/restore")
                .then()
                .statusCode(503);
    }

    @Test
    @DisplayName("restoreAllMatches returns 503 when persistence disabled")
    void restoreAllMatches_returns503_whenPersistenceDisabled() {
        jsonRequest()
                .when().post("/api/containers/" + containerId + "/restore/all")
                .then()
                .statusCode(503);
    }

    @Test
    @DisplayName("canRestoreMatch returns canRestore=false when persistence disabled")
    void canRestoreMatch_returnsFalse_whenPersistenceDisabled() {
        // Create match
        Response matchResponse = jsonRequest()
                .body("{\"enabledModuleNames\": []}")
                .when().post("/api/containers/" + containerId + "/matches")
                .then()
                .statusCode(201)
                .extract().response();
        int matchId = matchResponse.jsonPath().getInt("id");

        given()
                .when().get("/api/containers/" + containerId + "/matches/" + matchId + "/restore/available")
                .then()
                .statusCode(200)
                .body("matchId", is(matchId))
                .body("canRestore", is(false));
    }

    @Test
    @DisplayName("getRestoreConfig returns persistence and restore status")
    void getRestoreConfig_returnsConfig() {
        given()
                .when().get("/api/containers/" + containerId + "/restore/config")
                .then()
                .statusCode(200)
                .body("persistenceEnabled", notNullValue())
                .body("restoreEnabled", notNullValue())
                .body("autoRestoreOnStartup", notNullValue());
    }

    // =========================================================================
    // History/Restore 404 Scenarios for non-existent containers
    // =========================================================================

    @Test
    @DisplayName("history endpoints return 404 for non-existent container")
    void history_returns404_forNonExistentContainer() {
        given()
                .when().get("/api/containers/999999/history")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("restore endpoints return 404 for non-existent container")
    void restore_returns404_forNonExistentContainer() {
        given()
                .when().get("/api/containers/999999/restore/config")
                .then()
                .statusCode(404);
    }
}
