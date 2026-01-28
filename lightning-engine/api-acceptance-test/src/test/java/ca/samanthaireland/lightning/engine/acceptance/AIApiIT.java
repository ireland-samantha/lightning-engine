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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API acceptance test for AI functionality.
 *
 * <p>This test verifies:
 * <ul>
 *   <li>AI REST endpoints work correctly</li>
 *   <li>AIs can be listed, reloaded, and uninstalled</li>
 *   <li>AIs execute per-match when enabled</li>
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
@DisplayName("AI API Acceptance Test")
@Testcontainers
class AIApiIT {

    @Container
    static GenericContainer<?> backendContainer = TestContainers.backendContainer();

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
        log.info("Backend URL from testcontainers: {}", baseUrl);

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
    @DisplayName("GET /api/ai returns list with TickCounter AI")
    void getAIs_returnsTickCounterAI() throws Exception {
        log.info("=== Testing GET /api/ai ===");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/ai"))
                .GET()
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + bearerToken)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.debug("Response status: {}", response.statusCode());
        log.debug("Response body: {}", response.body());

        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode aiList = objectMapper.readTree(response.body());
        assertThat(aiList.isArray()).isTrue();
        assertThat(aiList.size()).as("Should have at least TickCounter AI").isGreaterThanOrEqualTo(1);

        // Verify TickCounter is in the list
        boolean hasTickCounter = false;
        for (JsonNode ai : aiList) {
            if ("TickCounter".equals(ai.path("name").asText())) {
                hasTickCounter = true;
                break;
            }
        }
        assertThat(hasTickCounter).as("TickCounter AI should be installed").isTrue();

        log.info("=== GET /api/ai test passed ===");
    }

    @Test
    @DisplayName("POST /api/ai/reload reloads AIs")
    void reloadAIs_succeeds() throws Exception {
        log.info("=== Testing POST /api/ai/reload ===");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/ai/reload"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Authorization", "Bearer " + bearerToken)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.debug("Response status: {}", response.statusCode());
        log.debug("Response body: {}", response.body());

        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode aiList = objectMapper.readTree(response.body());
        assertThat(aiList.isArray()).isTrue();

        log.info("=== POST /api/ai/reload test passed ===");
    }

    @Test
    @DisplayName("Match with TickCounter AI runs on tick")
    void matchWithTickCounter_executesOnTick() throws Exception {
        log.info("=== Testing TickCounter AI execution per match ===");

        // Create container, start it
        ContainerAdapter.ContainerResponse containerResponse = client.containers()
                .create()
                .name("ai-test")
                .withModules("EntityModule")
                .withAIs("TickCounter")
                .execute();
        containerId = containerResponse.id();
        // Container is already started since modules/AIs were specified

        ContainerClient container = client.container(containerId);

        // Create a match with the TickCounter AI enabled
        ContainerMatch match = container.createMatch(
                List.of("EntityModule"),
                List.of("TickCounter"));
        createdMatchId = match.id();
        log.info("Created match with TickCounter: {}", createdMatchId);

        assertThat(createdMatchId).isGreaterThan(0);

        // Advance several ticks to let AI execute
        // TickCounter logs every 100 ticks, but we just verify it doesn't crash
        for (int i = 0; i < 5; i++) {
            long tick = container.tick();
            log.debug("Advanced to tick: {}", tick);
        }

        // Verify the match still exists and is functional (AI didn't crash)
        var matchOpt = container.getMatch(createdMatchId);
        assertThat(matchOpt).isPresent();

        // Verify the match has the TickCounter enabled
        ContainerMatch matchInfo = matchOpt.get();
        assertThat(matchInfo.enabledAIs()).contains("TickCounter");

        log.debug("Match response: enabledAIs={}", matchInfo.enabledAIs());
        log.info("=== TickCounter AI execution test passed ===");
    }

    @Test
    @DisplayName("DELETE /api/ai/{name} returns 404 for non-existent AI")
    void deleteNonExistentAI_returns404() throws Exception {
        log.info("=== Testing DELETE /api/ai/NonExistent ===");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/ai/NonExistentAI"))
                .DELETE()
                .header("Authorization", "Bearer " + bearerToken)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.debug("Response status: {}", response.statusCode());
        log.debug("Response body: {}", response.body());

        assertThat(response.statusCode()).isEqualTo(404);

        log.info("=== DELETE non-existent AI test passed ===");
    }

    @Test
    @DisplayName("GET /api/ai/{name} returns 404 for non-existent AI")
    void getNonExistentAI_returns404() throws Exception {
        log.info("=== Testing GET /api/ai/NonExistent ===");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/ai/NonExistentAI"))
                .GET()
                .header("Authorization", "Bearer " + bearerToken)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.debug("Response status: {}", response.statusCode());
        log.debug("Response body: {}", response.body());

        assertThat(response.statusCode()).isEqualTo(404);

        log.info("=== GET non-existent AI test passed ===");
    }
}
