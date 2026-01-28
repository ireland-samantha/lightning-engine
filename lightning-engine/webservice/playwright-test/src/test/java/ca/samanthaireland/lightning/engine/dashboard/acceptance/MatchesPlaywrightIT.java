/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.dashboard.acceptance;

import ca.samanthaireland.lightning.engine.dashboard.acceptance.pages.*;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Playwright integration tests for Match management.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MatchesPlaywrightIT extends PlaywrightTestBase {

    private static final String TEST_CONTAINER_PREFIX = "matches-test-";
    private String containerToUse;

    @BeforeEach
    void setupContainer() {
        loginAsAdmin();
        PWDashboardPage dashboard = new PWDashboardPage(page);
        dashboard.waitForPageLoad();

        // Create a new test container with required modules for each test
        // This ensures we have all the modules needed for match creation
        containerToUse = TEST_CONTAINER_PREFIX + System.currentTimeMillis();
        dashboard.createContainerWithModules(containerToUse, "EntityModule", "GridMapModule", "RigidBodyModule");

        // Start container if not running
        String status = dashboard.getContainerStatus(containerToUse);
        if (!status.contains("RUNNING")) {
            dashboard.startContainer(containerToUse);
        }

        // Select container using sidebar dropdown (not just clicking the card)
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        sidebar.selectContainer(containerToUse);
    }

    @AfterEach
    void cleanupContainer() {
        if (containerToUse == null) {
            return;
        }
        try {
            PWDashboardPage dashboard = new PWDashboardPage(page);
            PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
            sidebar.goToOverview();
            dashboard.waitForPageLoad();

            if (dashboard.getContainerCard(containerToUse).count() == 0) {
                return;
            }

            String status = dashboard.getContainerStatus(containerToUse);
            if (status.contains("RUNNING")) {
                dashboard.stopContainer(containerToUse);
                page.waitForTimeout(1000);
            }

            dashboard.deleteContainer(containerToUse);
            page.waitForTimeout(500);
        } catch (Exception e) {
            System.err.println("Warning: Failed to cleanup container " + containerToUse + ": " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    void viewMatchesPage() {
        // When I navigate to matches
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWMatchesPage matchesPage = sidebar.goToMatches();

        // Then the page should load
        assertThat(matchesPage.isDisplayed()).isTrue();
    }

    @Test
    @Order(2)
    void createMatch() {
        // Given I am on the matches page
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWMatchesPage matchesPage = sidebar.goToMatches();

        int initialCount = matchesPage.getMatchCount();

        // When I create a new match with EntityModule
        matchesPage.createMatch("EntityModule");

        // Then the match count should increase
        matchesPage.refresh();
        assertThat(matchesPage.getMatchCount()).isGreaterThan(initialCount);
    }

    @Test
    @Order(3)
    void createMatchWithGridMapModule() {
        // Given I am on the matches page
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWMatchesPage matchesPage = sidebar.goToMatches();

        int initialCount = matchesPage.getMatchCount();

        // When I create a new match with GridMapModule
        matchesPage.createMatch("GridMapModule");

        // Then the match should be created
        matchesPage.refresh();
        assertThat(matchesPage.getMatchCount()).isGreaterThan(initialCount);
    }

    @Test
    @Order(4)
    void createMatchWithRigidBodyModule() {
        // Given I am on the matches page
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWMatchesPage matchesPage = sidebar.goToMatches();

        int initialCount = matchesPage.getMatchCount();

        // When I create a new match with RigidBodyModule (which requires GridMapModule for positions)
        // EntityModule is also needed for entity management
        matchesPage.createMatch("EntityModule", "GridMapModule", "RigidBodyModule");

        // Then the match should be created
        matchesPage.refresh();
        assertThat(matchesPage.getMatchCount()).isGreaterThan(initialCount);
    }
}
