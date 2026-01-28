/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.dashboard.acceptance.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright Page Object for sidebar navigation.
 * Uses ListItemButton role selectors to avoid matching other text on the page.
 */
public class PWSidebarNavigator {
    private final Page page;

    public PWSidebarNavigator(Page page) {
        this.page = page;
    }

    public void waitForSidebar() {
        page.locator(".MuiDrawer-docked").waitFor();
    }

    private Locator getSidebar() {
        return page.locator(".MuiDrawer-docked");
    }

    private Locator getMenuButton(String text) {
        // Menu items are buttons with text in MuiListItemText-primary span
        // Use getByText with exact option to avoid partial matches
        return getSidebar().locator(".MuiListItemButton-root")
                .filter(new Locator.FilterOptions().setHasText(text));
    }

    private Locator getMenuButtonExact(String text) {
        // For short menu items like "AI" that might match substrings, use exact matching
        return getSidebar().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(text).setExact(true));
    }

    private void expandContainerMenu() {
        // Click "Container" to expand if not already expanded
        Locator containerButton = getMenuButton("Container").first();
        // Check if already expanded by looking for "Overview" menu item
        if (!getMenuButton("Overview").isVisible()) {
            containerButton.click();
        }
    }

    private void expandAdminMenu() {
        Locator adminButton = getMenuButton("Administration");
        // Settings is the item in admin menu
        if (!getMenuButton("Settings").isVisible()) {
            adminButton.click();
        }
    }

    private void expandIamMenu() {
        Locator iamButton = getMenuButton("Identity & Access");
        if (!getMenuButton("Users").isVisible()) {
            iamButton.click();
        }
    }

    public PWDashboardPage goToOverview() {
        expandContainerMenu();
        getMenuButton("Overview").click();
        page.waitForLoadState();
        return new PWDashboardPage(page);
    }

    public PWMatchesPage goToMatches() {
        expandContainerMenu();
        getMenuButton("Matches").click();
        page.waitForLoadState();
        return new PWMatchesPage(page);
    }

    public PWPlayersPage goToPlayers() {
        expandContainerMenu();
        getMenuButton("Players").click();
        page.waitForLoadState();
        return new PWPlayersPage(page);
    }

    public PWSessionsPage goToSessions() {
        expandContainerMenu();
        getMenuButton("Sessions").click();
        page.waitForLoadState();
        return new PWSessionsPage(page);
    }

    public PWCommandsPage goToCommands() {
        expandContainerMenu();
        getMenuButton("Commands").click();
        page.waitForLoadState();
        return new PWCommandsPage(page);
    }

    public PWSnapshotPage goToLiveSnapshot() {
        expandContainerMenu();
        getMenuButton("Live Snapshot").click();
        page.waitForLoadState();
        return new PWSnapshotPage(page);
    }

    public PWHistoryPage goToHistory() {
        expandContainerMenu();
        getMenuButton("History").click();
        page.waitForLoadState();
        return new PWHistoryPage(page);
    }

    public PWLogsPage goToLogs() {
        expandContainerMenu();
        getMenuButton("Logs").click();
        page.waitForLoadState();
        return new PWLogsPage(page);
    }

    public PWModulesPage goToModules() {
        expandContainerMenu();
        getMenuButton("Modules").click();
        page.waitForLoadState();
        return new PWModulesPage(page);
    }

    public PWAIPage goToAI() {
        expandContainerMenu();
        // Use exact matching to avoid "AI" matching "Container" substring
        getMenuButtonExact("AI").click();
        page.waitForLoadState();
        return new PWAIPage(page);
    }

    public PWResourcesPage goToResources() {
        expandContainerMenu();
        getMenuButton("Resources").click();
        page.waitForLoadState();
        return new PWResourcesPage(page);
    }

    public PWUsersPage goToUsers() {
        expandIamMenu();
        getMenuButton("Users").click();
        page.waitForLoadState();
        return new PWUsersPage(page);
    }

    public PWRolesPage goToRoles() {
        expandIamMenu();
        getMenuButton("Roles").click();
        page.waitForLoadState();
        return new PWRolesPage(page);
    }

    /**
     * Selects a container from the sidebar dropdown.
     * This sets the active container for container-scoped panels (Modules, AI, Resources).
     *
     * <p>After selection, this method waits for the React context to update and navigates
     * to the Overview page to ensure a clean state before navigating to other container-scoped pages.
     *
     * @param containerName the name of the container to select
     */
    public void selectContainer(String containerName) {
        Locator selector = getSidebar().locator(".MuiSelect-select");
        if (selector.count() > 0) {
            selector.click();
            // Wait for menu to open and click the option
            page.locator(".MuiMenu-paper .MuiMenuItem-root")
                .filter(new Locator.FilterOptions().setHasText(containerName))
                .click();
            // Wait for menu to close and React context to update
            page.waitForTimeout(500);
            // Verify the selector now shows the selected container name
            selector.filter(new Locator.FilterOptions().setHasText(containerName))
                .waitFor(new Locator.WaitForOptions().setTimeout(5000));
            // Wait additional time for the context to propagate
            page.waitForTimeout(500);
        }
    }

    /**
     * Checks if a container is selected in the sidebar dropdown.
     *
     * @return true if a container is selected (selector value is not empty)
     */
    public boolean isContainerSelected() {
        Locator selector = getSidebar().locator(".MuiSelect-select");
        if (selector.count() > 0) {
            String value = selector.textContent();
            return value != null && !value.isEmpty() && !value.equals("Active Container");
        }
        return false;
    }
}
