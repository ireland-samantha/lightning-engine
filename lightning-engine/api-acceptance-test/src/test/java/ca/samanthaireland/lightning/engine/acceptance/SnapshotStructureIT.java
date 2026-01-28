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

import ca.samanthaireland.lightning.engine.acceptance.fixture.EntitySpawner;
import ca.samanthaireland.lightning.engine.api.resource.adapter.AuthAdapter;
import ca.samanthaireland.lightning.engine.api.resource.adapter.ContainerAdapter;
import ca.samanthaireland.lightning.engine.api.resource.adapter.EngineClient;
import ca.samanthaireland.lightning.engine.api.resource.adapter.EngineClient.ContainerClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the new snapshot structure with modules and versioning.
 *
 * <p>These tests verify that:
 * <ul>
 *   <li>Snapshot responses contain modules array with version information</li>
 *   <li>Each module has name, version, and components</li>
 *   <li>Components have name and values fields</li>
 *   <li>The adapter correctly converts the new format for backwards compatibility</li>
 * </ul>
 */
@Slf4j
@Tag("acceptance")
@Tag("testcontainers")
@DisplayName("Snapshot Structure Integration Tests")
@Testcontainers
@TestMethodOrder(OrderAnnotation.class)
@Timeout(value = 2, unit = TimeUnit.MINUTES)
class SnapshotStructureIT {

    private static final List<String> REQUIRED_MODULES = List.of(
            "EntityModule", "GridMapModule", "RigidBodyModule", "RenderModule"
    );

    @Container
    static GenericContainer<?> backendContainer = TestContainers.backendContainer();

    private EngineClient client;
    private ContainerClient container;
    private long containerId = -1;
    private long matchId = -1;
    private String baseUrl;
    private String authToken;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = TestContainers.getBaseUrl(backendContainer);
        log.info("Backend URL: {}", baseUrl);

        AuthAdapter auth = new AuthAdapter.HttpAuthAdapter(baseUrl);
        authToken = auth.login("admin", TestContainers.TEST_ADMIN_PASSWORD).token();
        log.info("Authenticated successfully");

        client = EngineClient.builder()
                .baseUrl(baseUrl)
                .withBearerToken(authToken)
                .build();

        httpClient = HttpClient.newHttpClient();
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
        containerId = -1;
        matchId = -1;
        container = null;
    }

    @Test
    @Order(1)
    @DisplayName("REST snapshot response has modules array with version information")
    void testRestSnapshotHasModulesWithVersion() throws Exception {
        setupMatchAndContainer();

        // Spawn entity
        long entityId = spawnEntity();
        log.info("Spawned entity: {}", entityId);
        container.tick();

        // Make direct HTTP request to verify raw JSON structure
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/containers/" + containerId + "/matches/" + matchId + "/snapshot"))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        String json = response.body();
        log.info("Raw snapshot response: {}", json);

        // Verify new structure
        assertThat(json)
                .as("Response should have modules array")
                .contains("\"modules\":");

        assertThat(json)
                .as("Response should have matchId")
                .contains("\"matchId\":" + matchId);

        assertThat(json)
                .as("Response should have tick")
                .contains("\"tick\":");

        // Verify module structure
        assertThat(json)
                .as("Modules should have version field")
                .contains("\"version\":");

        assertThat(json)
                .as("Modules should have components array")
                .contains("\"components\":");

        // Verify at least EntityModule is present
        assertThat(json)
                .as("Should contain EntityModule")
                .contains("EntityModule");

        log.info("REST snapshot structure test PASSED");
    }

    @Test
    @Order(2)
    @DisplayName("Adapter correctly converts new format to legacy format")
    void testAdapterConvertsNewFormatToLegacy() throws Exception {
        setupMatchAndContainer();

        // Spawn entity
        long entityId = spawnEntity();
        log.info("Spawned entity: {}", entityId);
        container.tick();

        // Use the adapter to get snapshot
        var snapshot = getSnapshot();
        log.info("Parsed snapshot modules: {}", snapshot.moduleNames());

        // Verify the adapter converted the new format correctly
        assertThat(snapshot.moduleNames())
                .as("Should have modules in the parsed snapshot")
                .isNotEmpty();

        assertThat(snapshot.hasModule("EntityModule"))
                .as("Should have EntityModule")
                .isTrue();

        // Verify entity data is present
        var entityModule = snapshot.module("EntityModule");
        assertThat(entityModule.has("ENTITY_ID"))
                .as("EntityModule should have ENTITY_ID component")
                .isTrue();

        List<Float> entityIds = entityModule.component("ENTITY_ID");
        assertThat(entityIds)
                .as("ENTITY_ID should contain the spawned entity")
                .contains((float) entityId);

        log.info("Adapter conversion test PASSED");
    }

    @Test
    @Order(3)
    @DisplayName("Module version information is included in snapshot")
    void testModuleVersionInSnapshot() throws Exception {
        setupMatchAndContainer();

        // Spawn entity and attach components to get multiple modules
        long entityId = spawnEntity();
        container.forMatch(matchId).send("attachRigidBody", Map.of(
                "entityId", entityId,
                "positionX", 100.0f,
                "positionY", 200.0f,
                "mass", 1.0f
        ));
        // Wait for rigid body components
        EntitySpawner.waitForComponent(client, container, matchId, "RigidBodyModule", "MASS");
        container.tick();

        // Make direct HTTP request to check version format
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/containers/" + containerId + "/matches/" + matchId + "/snapshot"))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String json = response.body();
        log.info("Snapshot with multiple modules: {}", json);

        // Verify version format (should be semver-like, e.g., "1.0" or "1.0.0")
        assertThat(json)
                .as("Version should be in semver format")
                .containsPattern("\"version\":\\s*\"\\d+\\.\\d+");

        // Count modules - should have multiple modules loaded
        int moduleCount = countOccurrences(json, "\"name\":");
        assertThat(moduleCount)
                .as("Should have multiple modules")
                .isGreaterThanOrEqualTo(2);

        log.info("Module version test PASSED - found {} modules", moduleCount);
    }

    @Test
    @Order(4)
    @DisplayName("Component values are nested under components array")
    void testComponentValuesStructure() throws Exception {
        setupMatchAndContainer();

        // Spawn entity
        long entityId = spawnEntity();
        container.tick();

        // Make direct HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/containers/" + containerId + "/matches/" + matchId + "/snapshot"))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String json = response.body();

        // Verify component structure: {"name": "COMPONENT_NAME", "values": [...]}
        assertThat(json)
                .as("Components should have name field")
                .containsPattern("\"name\":\\s*\"[A-Z_]+\"");

        assertThat(json)
                .as("Components should have values array")
                .containsPattern("\"values\":\\s*\\[");

        // Verify ENTITY_ID is present as a component
        assertThat(json)
                .as("Should have ENTITY_ID component")
                .contains("\"name\":\"ENTITY_ID\"");

        log.info("Component structure test PASSED");
    }

    // ========== Helper Methods ==========

    private void setupMatchAndContainer() throws Exception {
        ContainerAdapter.ContainerResponse containerResponse = client.containers()
                .create()
                .name("snapshot-structure-test-" + System.currentTimeMillis())
                .withModules(REQUIRED_MODULES.toArray(new String[0]))
                .execute();
        containerId = containerResponse.id();
        container = client.container(containerId);
        container.play(60);

        // Wait for container to be fully running
        EntitySpawner.waitForContainerRunning(client, containerId);

        var match = container.createMatch(REQUIRED_MODULES);
        matchId = match.id();
        log.info("Created container {} and match {}", containerId, matchId);
    }

    private long spawnEntity() throws IOException {
        return EntitySpawner.spawnEntity(client, container, matchId);
    }

    private EngineClient.Snapshot getSnapshot() throws IOException {
        var snapshotOpt = container.getSnapshot(matchId);
        if (snapshotOpt.isEmpty()) {
            throw new IllegalStateException("No snapshot available for match " + matchId);
        }
        return client.parseSnapshot(snapshotOpt.get().data());
    }

    private int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
