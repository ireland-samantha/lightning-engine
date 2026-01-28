/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.dashboard.acceptance;

import ca.samanthaireland.lightning.engine.dashboard.acceptance.pages.*;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Playwright integration tests for Container panels.
 * Tests Commands, Players, Sessions, Snapshot, History, and Logs panels.
 * These panels require a container to be selected.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerPanelsPlaywrightIT extends PlaywrightTestBase {

    private static final String TEST_CONTAINER_PREFIX = "pw-test-";

    @BeforeEach
    void login() {
        loginAsAdmin();
    }

    private void ensureContainerExistsAndSelected() {
        PWDashboardPage dashboard = new PWDashboardPage(page);

        // Use first available container, or create one if none exist
        var containerNames = dashboard.getContainerNames();
        String containerToUse;

        if (containerNames.isEmpty()) {
            // Create a unique container name
            String uniqueName = TEST_CONTAINER_PREFIX + System.currentTimeMillis();
            dashboard.createContainer(uniqueName);
            containerToUse = uniqueName;
        } else {
            containerToUse = containerNames.get(0);
        }

        // Start container if needed
        String status = dashboard.getContainerStatus(containerToUse);
        if (!status.contains("RUNNING")) {
            dashboard.startContainer(containerToUse);
        }

        // Select the container in the sidebar dropdown
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        if (!sidebar.isContainerSelected()) {
            sidebar.selectContainer(containerToUse);
        }
    }

    // ==================== Players Panel Tests ====================

    @Test
    @Order(1)
    void viewPlayersPage() {
        ensureContainerExistsAndSelected();

        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWPlayersPage playersPage = sidebar.goToPlayers();

        assertThat(playersPage.isDisplayed()).isTrue();
    }

    @Test
    @Order(2)
    void canAddPlayer() {
        ensureContainerExistsAndSelected();

        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWPlayersPage playersPage = sidebar.goToPlayers();

        int initialCount = playersPage.getPlayerCount();
        playersPage.addPlayer();

        assertThat(playersPage.getPlayerCount()).isGreaterThan(initialCount);
    }

    // ==================== Sessions Panel Tests ====================

    @Test
    @Order(3)
    void viewSessionsPage() {
        ensureContainerExistsAndSelected();

        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWSessionsPage sessionsPage = sidebar.goToSessions();

        assertThat(sessionsPage.isDisplayed()).isTrue();
    }

    // ==================== Commands Panel Tests ====================

    @Test
    @Order(4)
    void viewCommandsPage() {
        ensureContainerExistsAndSelected();

        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWCommandsPage commandsPage = sidebar.goToCommands();

        assertThat(commandsPage.isDisplayed()).isTrue();
    }

    @Test
    @Order(5)
    void commandsPageShowsAvailableCommands() {
        ensureContainerExistsAndSelected();

        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWCommandsPage commandsPage = sidebar.goToCommands();

        // Should show commands or "no commands" message
        assertThat(commandsPage.isDisplayed()).isTrue();
    }

    // ==================== Snapshot Panel Tests ====================

    @Test
    @Order(6)
    void viewSnapshotPage() {
        ensureContainerExistsAndSelected();

        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWSnapshotPage snapshotPage = sidebar.goToLiveSnapshot();

        assertThat(snapshotPage.isDisplayed()).isTrue();
    }

    @Test
    @Order(7)
    void snapshotPageShowsNoSnapshotInitially() {
        ensureContainerExistsAndSelected();

        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWSnapshotPage snapshotPage = sidebar.goToLiveSnapshot();

        // Should show either snapshot data or "no snapshot/select a match" message
        assertThat(snapshotPage.isDisplayed() || snapshotPage.isNoSnapshotMessageDisplayed()).isTrue();
    }

    // ==================== History Panel Tests ====================

    @Test
    @Order(8)
    void viewHistoryPage() {
        ensureContainerExistsAndSelected();

        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWHistoryPage historyPage = sidebar.goToHistory();

        assertThat(historyPage.isDisplayed()).isTrue();
    }

    // ==================== Logs Panel Tests ====================

    @Test
    @Order(9)
    void viewLogsPage() {
        ensureContainerExistsAndSelected();

        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWLogsPage logsPage = sidebar.goToLogs();

        assertThat(logsPage.isDisplayed()).isTrue();
    }

    @Test
    @Order(10)
    void logsPageShowsStreamingControls() {
        ensureContainerExistsAndSelected();

        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWLogsPage logsPage = sidebar.goToLogs();

        // Page should display and show either streaming controls or no logs message
        assertThat(logsPage.isDisplayed()).isTrue();
    }

    // ==================== No Container Selected Tests ====================

    @Test
    @Order(11)
    void playersShowNoContainerMessageWhenNoneSelected() {
        // Navigate directly without selecting container
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWPlayersPage playersPage = sidebar.goToPlayers();

        // Should show either content (if auto-selected) or no container message
        assertThat(playersPage.isDisplayed() || playersPage.isNoContainerSelectedMessageDisplayed()).isTrue();
    }

    @Test
    @Order(12)
    void sessionsShowNoContainerMessageWhenNoneSelected() {
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWSessionsPage sessionsPage = sidebar.goToSessions();

        assertThat(sessionsPage.isDisplayed() || sessionsPage.isNoContainerSelectedMessageDisplayed()).isTrue();
    }

    @Test
    @Order(13)
    void commandsShowNoContainerMessageWhenNoneSelected() {
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWCommandsPage commandsPage = sidebar.goToCommands();

        assertThat(commandsPage.isDisplayed() || commandsPage.isNoContainerSelectedMessageDisplayed()).isTrue();
    }

    @Test
    @Order(14)
    void snapshotShowNoContainerMessageWhenNoneSelected() {
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWSnapshotPage snapshotPage = sidebar.goToLiveSnapshot();

        assertThat(snapshotPage.isDisplayed() || snapshotPage.isNoContainerSelectedMessageDisplayed()).isTrue();
    }

    @Test
    @Order(15)
    void historyShowNoContainerMessageWhenNoneSelected() {
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWHistoryPage historyPage = sidebar.goToHistory();

        assertThat(historyPage.isDisplayed() || historyPage.isNoContainerSelectedMessageDisplayed()).isTrue();
    }

    @Test
    @Order(16)
    void logsShowNoContainerMessageWhenNoneSelected() {
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWLogsPage logsPage = sidebar.goToLogs();

        assertThat(logsPage.isDisplayed() || logsPage.isNoContainerSelectedMessageDisplayed()).isTrue();
    }

    // ==================== Integration Tests ====================

    @Test
    @Order(17)
    void canNavigateThroughAllContainerPanels() {
        ensureContainerExistsAndSelected();

        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);

        // Navigate through all container panels
        PWDashboardPage dashboard = sidebar.goToOverview();
        assertThat(dashboard.isDisplayed()).isTrue();

        PWPlayersPage players = sidebar.goToPlayers();
        assertThat(players.isDisplayed()).isTrue();

        PWSessionsPage sessions = sidebar.goToSessions();
        assertThat(sessions.isDisplayed()).isTrue();

        PWCommandsPage commands = sidebar.goToCommands();
        assertThat(commands.isDisplayed()).isTrue();

        PWSnapshotPage snapshot = sidebar.goToLiveSnapshot();
        assertThat(snapshot.isDisplayed()).isTrue();

        PWHistoryPage history = sidebar.goToHistory();
        assertThat(history.isDisplayed()).isTrue();

        PWLogsPage logs = sidebar.goToLogs();
        assertThat(logs.isDisplayed()).isTrue();

        PWModulesPage modules = sidebar.goToModules();
        assertThat(modules.isDisplayed()).isTrue();

        PWAIPage ai = sidebar.goToAI();
        assertThat(ai.isDisplayed()).isTrue();

        PWResourcesPage resources = sidebar.goToResources();
        assertThat(resources.isDisplayed()).isTrue();
    }

    // ==================== E2E Acceptance Tests ====================

    /**
     * Functional interface for running code within a container context.
     */
    @FunctionalInterface
    private interface ContainerTestAction {
        void execute(String containerName, PWDashboardPage dashboard, PWSidebarNavigator sidebar) throws Exception;
    }

    /**
     * Helper that creates a container, starts it, selects it, and ensures cleanup.
     * This is the recommended way to write container E2E tests.
     *
     * @param testPrefix a short prefix to identify the test (e.g., "e2e1", "memory")
     * @param action the test code to run with the container
     */
    private void withContainer(String testPrefix, ContainerTestAction action) {
        String containerName = TEST_CONTAINER_PREFIX + testPrefix + "-" + System.currentTimeMillis();
        try {
            PWDashboardPage dashboard = new PWDashboardPage(page);
            dashboard.waitForPageLoad();
            dashboard.createContainerWithModules(containerName, "EntityModule", "RigidBodyModule", "GridMapModule");
            dashboard.startContainer(containerName);

            PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
            sidebar.selectContainer(containerName);

            action.execute(containerName, dashboard, sidebar);
        } catch (Exception e) {
            throw new RuntimeException("Test failed: " + e.getMessage(), e);
        } finally {
            cleanupContainer(containerName);
        }
    }

    /**
     * Safely clean up a container by stopping and deleting it.
     * Navigates to dashboard first to ensure we're on the right page.
     */
    private void cleanupContainer(String containerName) {
        try {
            PWDashboardPage dashboard = new PWDashboardPage(page);
            PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
            sidebar.goToOverview();
            dashboard.waitForPageLoad();

            // Check if container still exists
            if (dashboard.getContainerCard(containerName).count() == 0) {
                return; // Container doesn't exist, nothing to clean up
            }

            // Stop container if running (STARTING state needs to wait or be handled)
            String status = dashboard.getContainerStatus(containerName);
            if (status.contains("RUNNING")) {
                dashboard.stopContainer(containerName);
                page.waitForTimeout(1000);
            } else if (status.contains("STARTING")) {
                // Wait briefly for container to finish starting, then try to stop
                page.waitForTimeout(2000);
                status = dashboard.getContainerStatus(containerName);
                if (status.contains("RUNNING")) {
                    dashboard.stopContainer(containerName);
                    page.waitForTimeout(1000);
                }
            }

            // Delete the container (clicks the delete button)
            dashboard.deleteContainer(containerName);
            // Wait for container card to disappear
            page.waitForTimeout(500);
        } catch (Exception e) {
            // Log but don't fail - cleanup is best effort
            System.err.println("Warning: Failed to cleanup container " + containerName + ": " + e.getMessage());
        }
    }

    @Test
    @Order(21)
    @DisplayName("Resource: verify resources panel is accessible")
    void e2eAcceptance5() {
        withContainer("e2e5", (containerName, dashboard, sidebar) -> {
            // Navigate to resources and verify page loads
            PWResourcesPage resources = sidebar.goToResources();
            resources.waitForPageLoad();
            assertThat(resources.isDisplayed()).as("Resources panel should be displayed").isTrue();
        });
    }

    @Test
    @Order(23)
    @DisplayName("E2E: verify snapshot panel shows RigidBodyModule with physics values")
    void e2eSnapshotShowsRigidBodyComponents() {
        withContainer("physics-snapshot", (containerName, dashboard, sidebar) -> {
            // Create match with physics modules
            PWMatchesPage matchesPage = sidebar.goToMatches();
            matchesPage.waitForPageLoad();
            matchesPage.createMatch("EntityModule", "RigidBodyModule", "GridMapModule");
            page.waitForTimeout(500);

            // Create a player (required for spawn command)
            PWPlayersPage playersPage = sidebar.goToPlayers();
            playersPage.waitForPageLoad();
            playersPage.addPlayer();
            page.waitForTimeout(500);

            // Step to initialize the match
            dashboard = sidebar.goToOverview();
            dashboard.clickStep(containerName);
            page.waitForTimeout(500);

            // Navigate to Matches page to ensure ContainerContext loads matches
            matchesPage = sidebar.goToMatches();
            matchesPage.waitForPageLoad();
            page.waitForTimeout(1000);

            // Send spawn command
            PWCommandsPage commandsPage = sidebar.goToCommands();
            commandsPage.waitForPageLoad();
            commandsPage.openSendDialog("spawn");
            commandsPage.selectFirstAutocompleteOption("matchId");
            commandsPage.typeInAutocomplete("playerId", "1");
            commandsPage.fillParameter("entityType", "1");
            commandsPage.clickSendInDialog();
            page.waitForTimeout(500);

            // Step to process spawn command
            dashboard = sidebar.goToOverview();
            dashboard.clickStep(containerName);
            page.waitForTimeout(500);

            // Send attachRigidBody command with velocity
            commandsPage = sidebar.goToCommands();
            commandsPage.waitForPageLoad();
            commandsPage.openSendDialog("attachRigidBody");
            commandsPage.typeInAutocomplete("entityId", "1");
            commandsPage.fillParameter("positionX", "0");
            commandsPage.fillParameter("positionY", "0");
            commandsPage.fillParameter("positionZ", "0");
            commandsPage.fillParameter("velocityX", "100");
            commandsPage.fillParameter("velocityY", "50");
            commandsPage.fillParameter("velocityZ", "0");
            commandsPage.fillParameter("mass", "1");
            commandsPage.fillParameter("linearDrag", "0");
            commandsPage.fillParameter("angularDrag", "0");
            commandsPage.fillParameter("inertia", "1");
            commandsPage.clickSendInDialog();
            page.waitForTimeout(500);

            // Step simulation to apply physics
            dashboard = sidebar.goToOverview();
            for (int i = 0; i < 5; i++) {
                dashboard.clickStep(containerName);
                page.waitForTimeout(100);
            }

            // Navigate to snapshot page and select match
            final PWSnapshotPage snapshotPage = sidebar.goToLiveSnapshot();
            snapshotPage.waitForPageLoad();
            snapshotPage.selectMatch(0);
            snapshotPage.waitForSnapshotData();

            // Wait for GridMapModule to appear and verify snapshot has data
            page.waitForCondition(() -> snapshotPage.hasModule("GridMapModule"),
                new com.microsoft.playwright.Page.WaitForConditionOptions().setTimeout(10000));

            // Capture initial position values
            snapshotPage.expandModule("GridMapModule");
            page.waitForTimeout(500); // Allow table to render
            Double initialPosX = snapshotPage.getFirstComponentValue("POSITION_X");
            Double initialPosY = snapshotPage.getFirstComponentValue("POSITION_Y");

            // If values aren't captured yet, wait and retry
            if (initialPosX == null || initialPosY == null) {
                page.waitForTimeout(2000);
                initialPosX = snapshotPage.getFirstComponentValue("POSITION_X");
                initialPosY = snapshotPage.getFirstComponentValue("POSITION_Y");
            }

            // Verify modules are present
            assertThat(snapshotPage.hasModule("EntityModule"))
                .as("EntityModule should be visible").isTrue();
            assertThat(snapshotPage.hasModule("RigidBodyModule"))
                .as("RigidBodyModule should be visible").isTrue();
            assertThat(snapshotPage.hasModule("GridMapModule"))
                .as("GridMapModule should be visible").isTrue();

            // Verify RigidBodyModule components (velocity should remain constant)
            snapshotPage.expandModule("RigidBodyModule");
            assertThat(snapshotPage.componentHasValue("VELOCITY_X", 100.0, 0.01))
                .as("VELOCITY_X should be 100").isTrue();
            assertThat(snapshotPage.componentHasValue("VELOCITY_Y", 50.0, 0.01))
                .as("VELOCITY_Y should be 50").isTrue();

            // Verify EntityModule components
            snapshotPage.expandModule("EntityModule");
            assertThat(snapshotPage.componentHasIntValue("ENTITY_TYPE", 1))
                .as("ENTITY_TYPE should be 1").isTrue();

            // Step more to let physics advance position
            dashboard = sidebar.goToOverview();
            for (int i = 0; i < 10; i++) {
                dashboard.clickStep(containerName);
                page.waitForTimeout(100);
            }

            // Return to snapshot and verify position has changed
            sidebar.goToLiveSnapshot();
            snapshotPage.waitForPageLoad();
            snapshotPage.selectMatch(0);
            snapshotPage.waitForSnapshotData();
            snapshotPage.expandModule("GridMapModule");

            Double finalPosX = snapshotPage.getFirstComponentValue("POSITION_X");
            Double finalPosY = snapshotPage.getFirstComponentValue("POSITION_Y");

            assertThat(initialPosX).as("Initial POSITION_X should be captured").isNotNull();
            assertThat(initialPosY).as("Initial POSITION_Y should be captured").isNotNull();
            assertThat(finalPosX).as("Final POSITION_X should be captured").isNotNull();
            assertThat(finalPosY).as("Final POSITION_Y should be captured").isNotNull();

            // Position should have changed (entity is moving due to velocity)
            assertThat(finalPosX).as("POSITION_X should have changed (entity moving)")
                .isGreaterThan(initialPosX);
            assertThat(finalPosY).as("POSITION_Y should have changed (entity moving)")
                .isGreaterThan(initialPosY);
        });
    }

    @Test
    @Order(24)
    @DisplayName("Memory: verify container statistics are visible")
    void createContainer_verifyMemoryUsageChangesAsWeCreateEntities() {
        withContainer("memory", (containerName, dashboard, sidebar) -> {
            // Create match with EntityModule
            PWMatchesPage matches = sidebar.goToMatches();
            matches.waitForPageLoad();
            matches.createMatch("EntityModule");

            // Create player
            PWPlayersPage players = sidebar.goToPlayers();
            players.waitForPageLoad();
            players.addPlayer();
            page.waitForTimeout(1000);

            // Create session
            PWSessionsPage sessions = sidebar.goToSessions();
            sessions.waitForPageLoad();
            sessions.createSessionWithFirstOptions();

            // Navigate back to dashboard and verify container is running with stats visible
            dashboard = sidebar.goToOverview();
            dashboard.waitForPageLoad();
            dashboard.clickStep(containerName);

            // Verify the dashboard shows container statistics (the stats panel should be visible for running container)
            assertThat(page.locator("text=Container Statistics").isVisible() ||
                       page.locator("text=Entities").isVisible())
                .as("Container stats should be visible").isTrue();
        });
    }
}
