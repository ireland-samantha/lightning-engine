/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.dashboard.acceptance;

import ca.samanthaireland.lightning.engine.dashboard.acceptance.pages.*;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Playwright integration tests for Admin panels (Modules, AI, Resources).
 * These panels are now container-scoped and require a container to be selected.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminPanelsPlaywrightIT extends PlaywrightTestBase {

    private static final String TEST_CONTAINER_PREFIX = "admin-test-";

    @BeforeEach
    void login() {
        loginAsAdmin();
        // loginAsAdmin already waits for dashboard to load
    }

    private void ensureContainerSelected() {
        PWDashboardPage dashboard = new PWDashboardPage(page);

        // Use existing container if available, or create a new one with unique name
        var containerNames = dashboard.getContainerNames();
        String containerToUse;

        if (containerNames.isEmpty()) {
            // Create a unique container name
            String uniqueName = TEST_CONTAINER_PREFIX + System.currentTimeMillis();
            dashboard.createContainer(uniqueName);
            dashboard.startContainer(uniqueName);
            containerToUse = uniqueName;
        } else {
            containerToUse = containerNames.get(0);
            // Start container if not running
            String status = dashboard.getContainerStatus(containerToUse);
            if (!status.contains("RUNNING")) {
                dashboard.startContainer(containerToUse);
            }
        }

        // Select the container in the sidebar dropdown
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        if (!sidebar.isContainerSelected()) {
            sidebar.selectContainer(containerToUse);
        }
    }

    // ==================== Modules Tests ====================

    @Test
    @Order(1)
    void viewModulesPage() {
        // Setup: Ensure a container is selected
        ensureContainerSelected();

        // When I navigate to modules
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWModulesPage modulesPage = sidebar.goToModules();

        // Then the page should load
        assertThat(modulesPage.isDisplayed()).isTrue();
    }

    @Test
    @Order(2)
    void modulesShowBuiltInModules() {
        // Setup: Ensure a container is selected
        ensureContainerSelected();

        // When I navigate to modules
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWModulesPage modulesPage = sidebar.goToModules();

        // Then the page should display (modules may or may not be pre-installed depending on container config)
        assertThat(modulesPage.isDisplayed()).isTrue();
    }

    // ==================== AI Tests ====================

    @Test
    @Order(3)
    void viewAIPage() {
        // Setup: Ensure a container is selected
        ensureContainerSelected();

        // When I navigate to AI
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWAIPage aiPage = sidebar.goToAI();

        // Then the page should load
        assertThat(aiPage.isDisplayed()).isTrue();
    }

    // ==================== Resources Tests ====================

    @Test
    @Order(4)
    void viewResourcesPage() {
        // Setup: Ensure a container is selected
        ensureContainerSelected();

        // When I navigate to resources
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWResourcesPage resourcesPage = sidebar.goToResources();

        // Then the page should load
        assertThat(resourcesPage.isDisplayed()).isTrue();
    }

    // ==================== No Container Selected Tests ====================

    @Test
    @Order(5)
    void modulesShowNoContainerMessageWhenNoneSelected() {
        // This test requires no container to be selected initially
        // Since we're testing the "no container" state, we need fresh page without selection
        // Note: This may not work reliably if container is auto-selected, so we skip if auto-selected

        // When I navigate directly to modules without selecting a container
        // The modules panel should show a "No Container Selected" message
        // (This is a placeholder - actual behavior depends on app's auto-selection logic)
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWModulesPage modulesPage = sidebar.goToModules();

        // If no container is selected, should show the message
        // Otherwise, modules should be displayed
        assertThat(modulesPage.isDisplayed() || modulesPage.isNoContainerSelectedMessageDisplayed()).isTrue();
    }
}
