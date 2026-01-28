/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.dashboard.acceptance;

import ca.samanthaireland.lightning.engine.dashboard.acceptance.pages.*;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Playwright integration tests for the Container Dashboard.
 * Tests container management, navigation, and tick control.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DashboardPlaywrightIT extends PlaywrightTestBase {

    private static final String TEST_CONTAINER = "test-container";

    @Test
    @Order(1)
    void loginAndViewDashboard() {
        // Given I navigate to the app
        navigateToHome();
        PWLoginPage loginPage = new PWLoginPage(page);

        // Then I should see the login form
        assertThat(loginPage.isDisplayed()).isTrue();

        // When I log in as admin
        PWDashboardPage dashboard = loginPage.loginAsAdmin();

        // Then I should see the dashboard
        assertThat(dashboard.isDisplayed()).isTrue();
    }

    @Test
    @Order(2)
    void createNewContainer() {
        // Given I am logged in
        loginAsAdmin();
        PWDashboardPage dashboard = new PWDashboardPage(page);
        dashboard.waitForPageLoad();

        int initialCount = dashboard.getContainerCount();

        // When I create a new container
        String containerName = "container-" + System.currentTimeMillis();
        dashboard.createContainer(containerName);

        // Then the container should appear
        assertThat(dashboard.getContainerCount()).isEqualTo(initialCount + 1);
        assertThat(dashboard.getContainerNames()).contains(containerName);
    }

    @Test
    @Order(3)
    void startAndStopContainer() {
        // Given I am logged in and create a container
        loginAsAdmin();
        PWDashboardPage dashboard = new PWDashboardPage(page);
        dashboard.waitForPageLoad();

        // Create a test container first
        dashboard.createContainer(TEST_CONTAINER);

        // When I start the container
        dashboard.startContainer(TEST_CONTAINER);

        // Then it should be running
        assertThat(dashboard.getContainerStatus(TEST_CONTAINER)).contains("RUNNING");

        // When I stop it
        dashboard.stopContainer(TEST_CONTAINER);

        // Then it should be stopped
        assertThat(dashboard.getContainerStatus(TEST_CONTAINER)).contains("STOPPED");
    }

    @Test
    @Order(4)
    void selectContainerAndNavigateToMatches() {
        // Given I am logged in and have a running container
        loginAsAdmin();
        PWDashboardPage dashboard = new PWDashboardPage(page);
        dashboard.waitForPageLoad();

        // Create and start a container
        String containerName = "nav-test-" + System.currentTimeMillis();
        dashboard.createContainer(containerName);
        dashboard.startContainer(containerName);

        // When I select the container
        dashboard.selectContainer(containerName);

        // And navigate to matches
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWMatchesPage matchesPage = sidebar.goToMatches();

        // Then I should see the matches page
        assertThat(matchesPage.isDisplayed()).isTrue();
    }

    @Test
    @Order(5)
    void tickAdvancesWithStep() {
        // Given I am logged in with a running container
        loginAsAdmin();
        PWDashboardPage dashboard = new PWDashboardPage(page);
        dashboard.waitForPageLoad();

        // Create and start a container
        String containerName = "tick-test-" + System.currentTimeMillis();
        dashboard.createContainer(containerName);
        dashboard.startContainer(containerName);

        long initialTick = dashboard.getContainerTick(containerName);

        // When I click step for this container
        dashboard.clickStep(containerName);

        // Wait a bit for the tick to update
        page.waitForTimeout(500);

        // Then the tick should advance
        assertThat(dashboard.getContainerTick(containerName)).isGreaterThan(initialTick);
    }
}
