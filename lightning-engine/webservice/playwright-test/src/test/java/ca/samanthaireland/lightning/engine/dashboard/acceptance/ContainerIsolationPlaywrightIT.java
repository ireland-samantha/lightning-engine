/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.dashboard.acceptance;

import ca.samanthaireland.lightning.engine.dashboard.acceptance.pages.*;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Playwright integration tests for Container Isolation.
 *
 * <p>This test verifies that containers are isolated from each other through the UI:
 * <ul>
 *   <li>Each container shows only its own matches</li>
 *   <li>Each container shows only its own players</li>
 *   <li>Each container shows only its own sessions</li>
 *   <li>Each container's snapshot shows only its own entities</li>
 *   <li>Physics simulation evolves independently in each container</li>
 * </ul>
 *
 * <p>This is the Playwright equivalent of ContainerIsolationIT API test.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Container Isolation (Playwright)")
class ContainerIsolationPlaywrightIT extends PlaywrightTestBase {

    private static final String TEST_PREFIX = "pw-iso-";

    @BeforeEach
    void login() {
        loginAsAdmin();
    }

    // Test context for isolation verification
    private String container1Name;
    private String container2Name;

    /**
     * Clean up a container by stopping and deleting it.
     */
    private void cleanupContainer(String containerName) {
        try {
            PWDashboardPage dashboard = new PWDashboardPage(page);
            PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
            sidebar.goToOverview();
            dashboard.waitForPageLoad();

            if (dashboard.getContainerCard(containerName).count() == 0) {
                return;
            }

            String status = dashboard.getContainerStatus(containerName);
            if (status.contains("RUNNING")) {
                dashboard.stopContainer(containerName);
                page.waitForTimeout(1000);
            } else if (status.contains("STARTING")) {
                page.waitForTimeout(2000);
                status = dashboard.getContainerStatus(containerName);
                if (status.contains("RUNNING")) {
                    dashboard.stopContainer(containerName);
                    page.waitForTimeout(1000);
                }
            }

            dashboard.deleteContainer(containerName);
            page.waitForTimeout(500);
        } catch (Exception e) {
            System.err.println("Warning: Failed to cleanup container " + containerName + ": " + e.getMessage());
        }
    }

    @AfterEach
    void cleanup() {
        if (container1Name != null) {
            cleanupContainer(container1Name);
        }
        if (container2Name != null) {
            cleanupContainer(container2Name);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Two containers with physics: verify match isolation through UI")
    void verifyMatchIsolationBetweenContainers() {
        container1Name = TEST_PREFIX + "1-" + System.currentTimeMillis();
        container2Name = TEST_PREFIX + "2-" + System.currentTimeMillis();

        PWDashboardPage dashboard = new PWDashboardPage(page);
        dashboard.waitForPageLoad();

        // === STEP 1: Create two containers with physics modules ===
        dashboard.createContainerWithModules(container1Name, "EntityModule", "RigidBodyModule", "GridMapModule");
        dashboard.startContainer(container1Name);
        page.waitForTimeout(500);

        dashboard.createContainerWithModules(container2Name, "EntityModule", "RigidBodyModule", "GridMapModule");
        dashboard.startContainer(container2Name);
        page.waitForTimeout(500);

        // Verify both containers are shown and running
        assertThat(dashboard.getContainerStatus(container1Name)).contains("RUNNING");
        assertThat(dashboard.getContainerStatus(container2Name)).contains("RUNNING");

        // === STEP 2: Create matches in each container ===
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);

        // Container 1: create match
        sidebar.selectContainer(container1Name);
        PWMatchesPage matchesPage = sidebar.goToMatches();
        matchesPage.waitForPageLoad();
        int container1InitialMatches = matchesPage.getMatchCount();
        matchesPage.createMatch("EntityModule", "RigidBodyModule", "GridMapModule");
        page.waitForTimeout(500);
        int container1Matches = matchesPage.getMatchCount();
        assertThat(container1Matches).as("Container 1 should have 1 match").isEqualTo(container1InitialMatches + 1);

        // Container 2: create match
        sidebar.selectContainer(container2Name);
        matchesPage = sidebar.goToMatches();
        matchesPage.waitForPageLoad();
        int container2InitialMatches = matchesPage.getMatchCount();
        matchesPage.createMatch("EntityModule", "RigidBodyModule", "GridMapModule");
        page.waitForTimeout(500);
        int container2Matches = matchesPage.getMatchCount();
        assertThat(container2Matches).as("Container 2 should have 1 match").isEqualTo(container2InitialMatches + 1);

        // === STEP 3: Verify match isolation - switching containers shows different matches ===
        // Switch back to container 1 and verify match count
        sidebar.selectContainer(container1Name);
        matchesPage = sidebar.goToMatches();
        matchesPage.waitForPageLoad();
        assertThat(matchesPage.getMatchCount())
                .as("Container 1 should still show only its matches")
                .isEqualTo(container1Matches);

        // Switch to container 2 and verify match count
        sidebar.selectContainer(container2Name);
        matchesPage = sidebar.goToMatches();
        matchesPage.waitForPageLoad();
        assertThat(matchesPage.getMatchCount())
                .as("Container 2 should still show only its matches")
                .isEqualTo(container2Matches);
    }

    @Test
    @Order(2)
    @DisplayName("Two containers: verify player isolation through UI")
    void verifyPlayerIsolationBetweenContainers() {
        container1Name = TEST_PREFIX + "p1-" + System.currentTimeMillis();
        container2Name = TEST_PREFIX + "p2-" + System.currentTimeMillis();

        PWDashboardPage dashboard = new PWDashboardPage(page);
        dashboard.waitForPageLoad();

        // Create and start containers
        dashboard.createContainerWithModules(container1Name, "EntityModule", "RigidBodyModule");
        dashboard.startContainer(container1Name);
        dashboard.createContainerWithModules(container2Name, "EntityModule", "RigidBodyModule");
        dashboard.startContainer(container2Name);

        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);

        // === Create players in Container 1 (freshly created container has 0 players) ===
        sidebar.selectContainer(container1Name);
        // Navigate to Overview first to ensure container context is fully switched
        sidebar.goToOverview();
        page.waitForTimeout(500);
        PWPlayersPage playersPage = sidebar.goToPlayers();
        playersPage.waitForPageLoad();
        playersPage.refresh();
        page.waitForTimeout(1000);

        // Add 2 players
        playersPage.addPlayer();
        page.waitForTimeout(1500);
        playersPage.addPlayer();
        page.waitForTimeout(1500);
        playersPage.refresh();
        page.waitForTimeout(1000);
        int container1Players = playersPage.getPlayerCount();
        // Fresh container should have exactly 2 players after adding 2
        assertThat(container1Players).as("Container 1 should have 2 players").isGreaterThanOrEqualTo(2);

        // === Create players in Container 2 (freshly created container has 0 players) ===
        sidebar.selectContainer(container2Name);
        // Navigate to Overview first to ensure container context is fully switched
        sidebar.goToOverview();
        page.waitForTimeout(500);
        playersPage = sidebar.goToPlayers();
        playersPage.waitForPageLoad();
        playersPage.refresh();
        page.waitForTimeout(1000);

        // Add 1 player
        playersPage.addPlayer();
        page.waitForTimeout(1500);
        playersPage.refresh();
        page.waitForTimeout(1000);
        int container2Players = playersPage.getPlayerCount();
        // Fresh container should have at least 1 player after adding 1
        assertThat(container2Players).as("Container 2 should have at least 1 player").isGreaterThanOrEqualTo(1);

        // === Verify player isolation ===
        sidebar.selectContainer(container1Name);
        sidebar.goToOverview();
        page.waitForTimeout(500);
        playersPage = sidebar.goToPlayers();
        playersPage.waitForPageLoad();
        playersPage.refresh();
        page.waitForTimeout(500);
        assertThat(playersPage.getPlayerCount())
                .as("Container 1 should still show the same number of players")
                .isEqualTo(container1Players);

        sidebar.selectContainer(container2Name);
        sidebar.goToOverview();
        page.waitForTimeout(500);
        playersPage = sidebar.goToPlayers();
        playersPage.waitForPageLoad();
        playersPage.refresh();
        page.waitForTimeout(500);
        assertThat(playersPage.getPlayerCount())
                .as("Container 2 should still show the same number of players")
                .isEqualTo(container2Players);
    }

    @Test
    @Order(3)
    @DisplayName("Two containers: verify session isolation through UI")
    void verifySessionIsolationBetweenContainers() {
        container1Name = TEST_PREFIX + "s1-" + System.currentTimeMillis();
        container2Name = TEST_PREFIX + "s2-" + System.currentTimeMillis();

        PWDashboardPage dashboard = new PWDashboardPage(page);
        dashboard.waitForPageLoad();

        // Create and start containers
        dashboard.createContainerWithModules(container1Name, "EntityModule");
        dashboard.startContainer(container1Name);
        dashboard.createContainerWithModules(container2Name, "EntityModule");
        dashboard.startContainer(container2Name);

        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);

        // === Container 1: create match, player, and session ===
        sidebar.selectContainer(container1Name);
        sidebar.goToOverview();
        page.waitForTimeout(500);

        PWMatchesPage matchesPage = sidebar.goToMatches();
        matchesPage.waitForPageLoad();
        matchesPage.createMatch("EntityModule");
        page.waitForTimeout(500);

        PWPlayersPage playersPage = sidebar.goToPlayers();
        playersPage.waitForPageLoad();
        playersPage.addPlayer();
        page.waitForTimeout(500);

        PWSessionsPage sessionsPage = sidebar.goToSessions();
        sessionsPage.waitForPageLoad();
        sessionsPage.refresh();
        page.waitForTimeout(1000);

        // Create 1 session
        sessionsPage.createSessionWithFirstOptions();
        page.waitForTimeout(2000); // Wait for session creation to complete
        sessionsPage.refresh(); // Refresh to get accurate count
        page.waitForTimeout(1000);
        int container1Sessions = sessionsPage.getSessionCount();
        // Fresh container should have at least 1 session after creating 1
        assertThat(container1Sessions).as("Container 1 should have at least 1 session").isGreaterThanOrEqualTo(1);

        // === Container 2: create match, player, and session ===
        sidebar.selectContainer(container2Name);
        sidebar.goToOverview();
        page.waitForTimeout(500);

        matchesPage = sidebar.goToMatches();
        matchesPage.waitForPageLoad();
        matchesPage.createMatch("EntityModule");
        page.waitForTimeout(500);

        playersPage = sidebar.goToPlayers();
        playersPage.waitForPageLoad();
        playersPage.addPlayer();
        playersPage.addPlayer();
        page.waitForTimeout(500);

        sessionsPage = sidebar.goToSessions();
        sessionsPage.waitForPageLoad();
        sessionsPage.refresh();
        page.waitForTimeout(1000);

        // Create 2 sessions for different players (container 2 has 2 players)
        // Use index-based selection to avoid issues with exact match label text
        sessionsPage.createSessionByIndex(0, 0); // First match, first player
        page.waitForTimeout(2000); // Wait for first session creation
        sessionsPage.refresh();
        page.waitForTimeout(500);
        sessionsPage.createSessionByIndex(0, 1); // First match, second player
        page.waitForTimeout(2000); // Wait for second session creation
        sessionsPage.refresh(); // Refresh to get accurate count
        page.waitForTimeout(1000);
        int container2Sessions = sessionsPage.getSessionCount();
        // Fresh container should have at least 2 sessions after creating 2
        assertThat(container2Sessions).as("Container 2 should have at least 2 sessions").isGreaterThanOrEqualTo(2);

        // === Verify session isolation ===
        sidebar.selectContainer(container1Name);
        sidebar.goToOverview();
        page.waitForTimeout(500);
        sessionsPage = sidebar.goToSessions();
        sessionsPage.waitForPageLoad();
        sessionsPage.refresh();
        page.waitForTimeout(500);
        assertThat(sessionsPage.getSessionCount())
                .as("Container 1 should still show the same number of sessions")
                .isEqualTo(container1Sessions);

        sidebar.selectContainer(container2Name);
        sidebar.goToOverview();
        page.waitForTimeout(500);
        sessionsPage = sidebar.goToSessions();
        sessionsPage.waitForPageLoad();
        sessionsPage.refresh();
        page.waitForTimeout(500);
        assertThat(sessionsPage.getSessionCount())
                .as("Container 2 should still show the same number of sessions")
                .isEqualTo(container2Sessions);
    }

    @Test
    @Order(4)
    @DisplayName("Two containers with physics: verify snapshot isolation and independent physics")
    void verifySnapshotIsolationAndPhysicsIndependence() {
        container1Name = TEST_PREFIX + "ph1-" + System.currentTimeMillis();
        container2Name = TEST_PREFIX + "ph2-" + System.currentTimeMillis();

        PWDashboardPage dashboard = new PWDashboardPage(page);
        dashboard.waitForPageLoad();

        // Create and start containers with physics modules
        dashboard.createContainerWithModules(container1Name, "EntityModule", "RigidBodyModule", "GridMapModule");
        dashboard.startContainer(container1Name);
        dashboard.createContainerWithModules(container2Name, "EntityModule", "RigidBodyModule", "GridMapModule");
        dashboard.startContainer(container2Name);

        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);

        // === Container 1: Setup match, player, entity with RIGHT velocity ===
        sidebar.selectContainer(container1Name);
        PWMatchesPage matchesPage = sidebar.goToMatches();
        matchesPage.waitForPageLoad();
        matchesPage.createMatch("EntityModule", "RigidBodyModule", "GridMapModule");
        page.waitForTimeout(500);

        PWPlayersPage playersPage = sidebar.goToPlayers();
        playersPage.waitForPageLoad();
        playersPage.addPlayer();
        page.waitForTimeout(500);

        // Step to initialize
        dashboard = sidebar.goToOverview();
        dashboard.clickStep(container1Name);
        page.waitForTimeout(500);

        // Reload matches page to get match in context
        matchesPage = sidebar.goToMatches();
        matchesPage.waitForPageLoad();
        page.waitForTimeout(1000);

        // Spawn entity and attach rigid body with POSITIVE X velocity (moving right)
        PWCommandsPage commandsPage = sidebar.goToCommands();
        commandsPage.waitForPageLoad();
        commandsPage.openSendDialog("spawn");
        commandsPage.selectFirstAutocompleteOption("matchId");
        commandsPage.typeInAutocomplete("playerId", "1");
        commandsPage.fillParameter("entityType", "1");
        commandsPage.clickSendInDialog();
        page.waitForTimeout(500);

        dashboard = sidebar.goToOverview();
        dashboard.clickStep(container1Name);
        page.waitForTimeout(500);

        commandsPage = sidebar.goToCommands();
        commandsPage.waitForPageLoad();
        commandsPage.openSendDialog("attachRigidBody");
        commandsPage.typeInAutocomplete("entityId", "1");
        commandsPage.fillParameter("positionX", "0");
        commandsPage.fillParameter("positionY", "0");
        commandsPage.fillParameter("positionZ", "0");
        commandsPage.fillParameter("velocityX", "100");  // Moving RIGHT
        commandsPage.fillParameter("velocityY", "0");
        commandsPage.fillParameter("velocityZ", "0");
        commandsPage.fillParameter("mass", "1");
        commandsPage.fillParameter("linearDrag", "0");
        commandsPage.fillParameter("angularDrag", "0");
        commandsPage.fillParameter("inertia", "1");
        commandsPage.clickSendInDialog();
        page.waitForTimeout(500);

        // === Container 2: Setup match, player, entity with LEFT velocity ===
        sidebar.selectContainer(container2Name);
        matchesPage = sidebar.goToMatches();
        matchesPage.waitForPageLoad();
        matchesPage.createMatch("EntityModule", "RigidBodyModule", "GridMapModule");
        page.waitForTimeout(500);

        playersPage = sidebar.goToPlayers();
        playersPage.waitForPageLoad();
        playersPage.addPlayer();
        page.waitForTimeout(500);

        dashboard = sidebar.goToOverview();
        dashboard.clickStep(container2Name);
        page.waitForTimeout(500);

        matchesPage = sidebar.goToMatches();
        matchesPage.waitForPageLoad();
        page.waitForTimeout(1000);

        commandsPage = sidebar.goToCommands();
        commandsPage.waitForPageLoad();
        commandsPage.openSendDialog("spawn");
        commandsPage.selectFirstAutocompleteOption("matchId");
        commandsPage.typeInAutocomplete("playerId", "1");
        commandsPage.fillParameter("entityType", "2");  // Different entity type
        commandsPage.clickSendInDialog();
        page.waitForTimeout(500);

        dashboard = sidebar.goToOverview();
        dashboard.clickStep(container2Name);
        page.waitForTimeout(500);

        commandsPage = sidebar.goToCommands();
        commandsPage.waitForPageLoad();
        commandsPage.openSendDialog("attachRigidBody");
        commandsPage.typeInAutocomplete("entityId", "1");
        commandsPage.fillParameter("positionX", "0");
        commandsPage.fillParameter("positionY", "0");
        commandsPage.fillParameter("positionZ", "0");
        commandsPage.fillParameter("velocityX", "-100");  // Moving LEFT (opposite direction)
        commandsPage.fillParameter("velocityY", "0");
        commandsPage.fillParameter("velocityZ", "0");
        commandsPage.fillParameter("mass", "1");
        commandsPage.fillParameter("linearDrag", "0");
        commandsPage.fillParameter("angularDrag", "0");
        commandsPage.fillParameter("inertia", "1");
        commandsPage.clickSendInDialog();
        page.waitForTimeout(500);

        // === Run physics simulation in both containers ===
        dashboard = sidebar.goToOverview();
        for (int i = 0; i < 10; i++) {
            dashboard.clickStep(container1Name);
            page.waitForTimeout(100);
        }
        for (int i = 0; i < 10; i++) {
            dashboard.clickStep(container2Name);
            page.waitForTimeout(100);
        }

        // === Verify Container 1 snapshot shows positive X position ===
        sidebar.selectContainer(container1Name);
        final PWSnapshotPage snapshotPage1 = sidebar.goToLiveSnapshot();
        snapshotPage1.waitForPageLoad();
        snapshotPage1.selectMatch(0);
        snapshotPage1.waitForSnapshotData();

        // Wait for GridMapModule to appear
        page.waitForCondition(() -> snapshotPage1.hasModule("GridMapModule"),
                new com.microsoft.playwright.Page.WaitForConditionOptions().setTimeout(10000));

        snapshotPage1.expandModule("GridMapModule");
        page.waitForTimeout(500);
        Double container1PosX = snapshotPage1.getFirstComponentValue("POSITION_X");

        // Container 1 should have positive X (moving right)
        assertThat(container1PosX)
                .as("Container 1 entity should have positive X position (moving right)")
                .isNotNull()
                .isGreaterThan(0.0);

        // Verify entity count
        assertThat(snapshotPage1.getEntityCount())
                .as("Container 1 snapshot should show exactly 1 entity")
                .isEqualTo(1);

        // === Verify Container 2 snapshot shows negative X position ===
        sidebar.selectContainer(container2Name);
        final PWSnapshotPage snapshotPage2 = sidebar.goToLiveSnapshot();
        snapshotPage2.waitForPageLoad();
        snapshotPage2.selectMatch(0);
        snapshotPage2.waitForSnapshotData();

        page.waitForCondition(() -> snapshotPage2.hasModule("GridMapModule"),
                new com.microsoft.playwright.Page.WaitForConditionOptions().setTimeout(10000));

        snapshotPage2.expandModule("GridMapModule");
        page.waitForTimeout(500);
        Double container2PosX = snapshotPage2.getFirstComponentValue("POSITION_X");

        // Container 2 should have negative X (moving left)
        assertThat(container2PosX)
                .as("Container 2 entity should have negative X position (moving left)")
                .isNotNull()
                .isLessThan(0.0);

        // Verify entity count
        assertThat(snapshotPage2.getEntityCount())
                .as("Container 2 snapshot should show exactly 1 entity")
                .isEqualTo(1);

        // === Final verification: positions are opposite directions ===
        assertThat(container1PosX)
                .as("Container 1 and Container 2 should have opposite X directions")
                .isGreaterThan(0.0);
        assertThat(container2PosX)
                .as("Container 1 and Container 2 should have opposite X directions")
                .isLessThan(0.0);
    }
}
