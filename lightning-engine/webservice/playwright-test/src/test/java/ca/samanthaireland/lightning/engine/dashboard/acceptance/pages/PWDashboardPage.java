/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.dashboard.acceptance.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Playwright Page Object for the Container Dashboard (Overview) page.
 * The containers are displayed as cards, not a table.
 */
public class PWDashboardPage {
    private final Page page;

    public PWDashboardPage(Page page) {
        this.page = page;
    }

    // ==================== Navigation ====================

    public boolean isDisplayed() {
        return page.getByText("Execution Containers").isVisible();
    }

    public void waitForPageLoad() {
        page.getByText("Execution Containers").waitFor();
    }

    // ==================== Container Management ====================

    public void clickCreateContainer() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("New Container")).click();
    }

    public void createContainer(String name) {
        clickCreateContainer();
        page.getByLabel("Container Name").waitFor();
        page.getByLabel("Container Name").fill(name);
        // Click outside any open dropdowns to close them, then click Create
        page.locator(".MuiDialogTitle-root").click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create")).click();
        waitForSuccessAlert();
        // Wait for the container card to appear
        getContainerCard(name).waitFor();
    }

    /**
     * Create a container with specific modules selected.
     * This tests the full create container flow including module installation.
     *
     * @param name the container name
     * @param moduleNames the modules to install (e.g., "EntityModule", "RigidBodyModule")
     */
    public void createContainerWithModules(String name, String... moduleNames) {
        clickCreateContainer();
        page.getByLabel("Container Name").waitFor();
        page.getByLabel("Container Name").fill(name);

        // Select modules if any specified
        if (moduleNames.length > 0) {
            // Click the Modules select to open dropdown
            page.getByLabel("Modules").click();
            page.waitForTimeout(300);

            // Select each module from the dropdown
            for (String moduleName : moduleNames) {
                page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(Pattern.compile(moduleName)))
                    .click();
            }

            // Close dropdown by pressing Escape (backdrop intercepts regular clicks)
            page.keyboard().press("Escape");
            page.waitForTimeout(200);
        }

        // Click Create button
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create")).click();
        waitForSuccessAlert();
        // Wait for the container card to appear
        getContainerCard(name).waitFor();
    }

    public List<String> getContainerNames() {
        // Wait for page to load
        page.getByText("Execution Containers").waitFor();

        // Container names are in Typography with variant="h6" inside cards
        // MUI renders this with class MuiTypography-h6
        Locator cards = page.locator(".MuiCard-root");
        int count = cards.count();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Locator nameElement = cards.nth(i).locator(".MuiTypography-h6").first();
            if (nameElement.count() > 0) {
                names.add(nameElement.textContent().trim());
            }
        }
        return names;
    }

    public int getContainerCount() {
        return page.locator(".MuiCard-root").count();
    }

    public Locator getContainerCard(String containerName) {
        // Find the card that contains the container name in its h6 element
        // Use first() to avoid strict mode violation when there are multiple containers with same name
        return page.locator(".MuiCard-root").filter(new Locator.FilterOptions().setHasText(containerName)).first();
    }

    public String getContainerStatus(String containerName) {
        // Status is in a Chip inside the card
        Locator card = getContainerCard(containerName);
        return card.locator(".MuiChip-root").first().textContent();
    }

    public void selectContainer(String containerName) {
        // Click the card to select the container
        getContainerCard(containerName).click();
    }

    public void startContainer(String containerName) {
        // Check if already running
        String status = getContainerStatus(containerName);
        if (status.contains("RUNNING")) {
            return; // Already running, nothing to do
        }

        // Start button is a success-colored IconButton in CardActions
        Locator card = getContainerCard(containerName);
        Locator cardActions = card.locator(".MuiCardActions-root");
        Locator startButton = cardActions.locator("button.MuiIconButton-colorSuccess");

        // Wait for button to be visible and click it
        startButton.waitFor(new Locator.WaitForOptions().setTimeout(5000));
        startButton.click();

        // Wait for success alert to confirm API call succeeded
        waitForSuccessAlert();

        // Wait a moment for status to update, then check status
        page.waitForTimeout(2000);
        String currentStatus = getContainerStatus(containerName);

        // Then wait for the status chip to show RUNNING (with debugging on timeout)
        try {
            waitForContainerStatus(containerName, "RUNNING");
        } catch (Exception e) {
            // Capture current status for debugging
            String finalStatus = getContainerStatus(containerName);
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(java.nio.file.Paths.get("target/screenshots/start-container-timeout-" + containerName + ".png")));
            throw new AssertionError("Container '" + containerName + "' failed to reach RUNNING status. " +
                    "Current status: '" + finalStatus + "'. Initial status was: '" + status + "'", e);
        }
    }

    public void stopContainer(String containerName) {
        // Check if already stopped
        String status = getContainerStatus(containerName);
        if (status.contains("STOPPED") || status.contains("CREATED")) {
            return; // Already stopped or never started
        }

        // Stop button is an error-colored IconButton in CardActions (first one when running)
        Locator card = getContainerCard(containerName);
        Locator cardActions = card.locator(".MuiCardActions-root");
        cardActions.locator("button.MuiIconButton-colorError").first().click();
        waitForContainerStatus(containerName, "STOPPED");
    }

    public void deleteContainer(String containerName) {
        // Delete button is the last error-colored button in CardActions
        Locator card = getContainerCard(containerName);
        Locator cardActions = card.locator(".MuiCardActions-root");
        cardActions.locator("button.MuiIconButton-colorError").last().click();
    }

    private void waitForContainerStatus(String containerName, String status) {
        // Use longer timeout (60s) for container state changes - startup can take time with module loading
        getContainerCard(containerName).locator(".MuiChip-root")
                .filter(new Locator.FilterOptions().setHasText(status))
                .waitFor(new Locator.WaitForOptions().setTimeout(60000));
    }

    // ==================== Tick Control ====================

    public void clickStep(String containerName) {
        // First select the container to ensure the control panel is shown
        selectContainer(containerName);
        // Step button is in the control panel with label "Step"
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Step")).click();
    }

    public void clickPlay(String containerName) {
        selectContainer(containerName);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Play (60 FPS)")).click();
    }

    public void clickPause(String containerName) {
        selectContainer(containerName);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Pause")).click();
    }

    public long getContainerTick(String containerName) {
        // The tick is displayed in a Chip in the selected container's control panel
        // First ensure the container is selected
        selectContainer(containerName);
        // Find the Tick chip - look for primary colored chip with "Tick:" text
        Locator tickChip = page.locator(".MuiChip-colorPrimary").filter(
                new Locator.FilterOptions().setHasText(Pattern.compile("Tick:")));
        if (tickChip.count() > 0) {
            String tickText = tickChip.textContent();
            Pattern pattern = Pattern.compile("\\d+");
            Matcher matcher = pattern.matcher(tickText);
            if (matcher.find()) {
                return Long.parseLong(matcher.group());
            }
        }
        return 0;
    }

    // ==================== Refresh ====================

    public void refresh() {
        page.locator("button[title='Refresh']").click();
    }

    // ==================== Alerts ====================

    public void waitForSuccessAlert() {
        // Use longer timeout for success alerts since some operations take time
        page.locator(".MuiAlert-standardSuccess, .MuiAlert-filledSuccess")
            .waitFor(new Locator.WaitForOptions().setTimeout(10000));
    }

    public void waitForErrorAlert() {
        page.locator(".MuiAlert-standardError, .MuiAlert-filledError").waitFor();
    }

    public boolean isSuccessAlertDisplayed() {
        return page.locator(".MuiAlert-standardSuccess, .MuiAlert-filledSuccess").isVisible();
    }

    public void closeAlert() {
        page.locator(".MuiAlert-root button").click();
    }

    // ==================== Dialog ====================

    public void cancelDialog() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Cancel")).click();
        page.locator(".MuiDialog-root").waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.HIDDEN));
    }

    public boolean isDialogOpen() {
        return page.locator(".MuiDialog-root").isVisible();
    }

    /**
     * Get the container ID by name using the REST API.
     * Requires being logged in (token in localStorage).
     * @param containerName the container name
     * @return the container ID
     */
    public long getContainerId(String containerName) {
        // Use REST API to find container by name
        // Need to include auth token from localStorage
        Object result = page.evaluate("async (name) => { " +
            "try { " +
            "  const token = localStorage.getItem('authToken'); " +
            "  const headers = token ? { 'Authorization': 'Bearer ' + token } : {}; " +
            "  const response = await fetch(window.location.origin + '/api/containers', { headers }); " +
            "  if (!response.ok) { " +
            "    console.error('API response not ok:', response.status, response.statusText); " +
            "    return -1; " +
            "  } " +
            "  const text = await response.text(); " +
            "  if (!text) return -1; " +
            "  const containers = JSON.parse(text); " +
            "  const container = containers.find(c => c.name === name); " +
            "  return container ? container.id : -1; " +
            "} catch (e) { " +
            "  console.error('Error getting container ID:', e); " +
            "  return -1; " +
            "} " +
            "}", containerName);
        if (result instanceof Number) {
            return ((Number) result).longValue();
        }
        return -1;
    }
}
