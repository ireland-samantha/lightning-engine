/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.dashboard.acceptance.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.util.List;

/**
 * Playwright Page Object for the Snapshot page.
 */
public class PWSnapshotPage {
    private final Page page;

    public PWSnapshotPage(Page page) {
        this.page = page;
    }

    public boolean isDisplayed() {
        page.waitForLoadState();
        page.waitForTimeout(500);
        return page.getByText("Snapshot").first().isVisible() ||
               page.getByText("No Container Selected").isVisible();
    }

    public void waitForPageLoad() {
        page.waitForLoadState();
        page.waitForTimeout(500);
    }

    public void clickLoadSnapshot() {
        // Button has tooltip "Refresh"
        page.locator("button[title='Refresh']").click();
    }

    public void toggleAutoRefresh() {
        page.getByLabel("Auto-refresh").click();
    }

    public boolean isAutoRefreshEnabled() {
        return page.getByLabel("Auto-refresh").isChecked();
    }

    public long getCurrentTick() {
        String tickText = page.locator("//*[contains(text(), 'Tick:')]").textContent();
        return Long.parseLong(tickText.replaceAll("[^0-9]", ""));
    }

    public List<String> getModuleNames() {
        return page.locator(".MuiTreeItem-label").allTextContents();
    }

    public void expandModuleTree(String moduleName) {
        page.locator(".MuiTreeItem-root").filter(new Locator.FilterOptions().setHasText(moduleName))
                .locator(".MuiTreeItem-iconContainer")
                .click();
    }

    public int getEntityCountFromTree() {
        return page.locator(".MuiTreeItem-root").filter(new Locator.FilterOptions()
                .setHasText(java.util.regex.Pattern.compile("Entity \\d+"))).count();
    }

    public void selectEntity(long entityId) {
        page.locator(".MuiTreeItem-label").filter(new Locator.FilterOptions()
                .setHasText("Entity " + entityId)).click();
    }

    public boolean isNoSnapshotMessageDisplayed() {
        return page.getByText("No snapshot").isVisible() || page.getByText("Select a match").isVisible();
    }

    public boolean isNoContainerSelectedMessageDisplayed() {
        return page.getByText("No Container Selected").isVisible() ||
               page.getByText("Select a container").isVisible();
    }

    /**
     * Select a match from the match dropdown.
     * @param matchIndex 0-based index of the match to select
     */
    public void selectMatch(int matchIndex) {
        page.getByLabel("Match").click();
        page.getByRole(AriaRole.OPTION).nth(matchIndex).click();
        page.waitForTimeout(500);
    }

    /**
     * Check if the snapshot contains a specific module name.
     */
    public boolean hasModule(String moduleName) {
        return page.getByText(moduleName).isVisible();
    }

    /**
     * Check if the snapshot contains a specific component name.
     */
    public boolean hasComponent(String componentName) {
        return page.getByText(componentName).isVisible();
    }

    /**
     * Get the text content of the snapshot display (for debugging/verification).
     */
    public String getSnapshotText() {
        return page.locator(".MuiPaper-root").first().textContent();
    }

    /**
     * Wait for snapshot data to load (wait for loading spinner to disappear and content to appear).
     */
    public void waitForSnapshotData() {
        // First, wait for loading spinner to disappear
        Locator spinner = page.locator(".MuiCircularProgress-root");
        if (spinner.count() > 0) {
            spinner.waitFor(new Locator.WaitForOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN)
                .setTimeout(15000));
        }
        page.waitForTimeout(1000);

        // Then wait for either module data or "no snapshot/no entities" message
        page.waitForCondition(() ->
            page.getByText("EntityModule").isVisible() ||
            page.getByText("RigidBodyModule").isVisible() ||
            page.getByText("GridMapModule").isVisible() ||
            page.getByText("No snapshot").isVisible() ||
            page.getByText("Select a match").isVisible() ||
            page.getByText("No entities").isVisible() ||
            page.getByText("Empty snapshot").isVisible() ||
            page.locator(".MuiTreeItem-root").count() > 0,  // Tree items for module names
            new Page.WaitForConditionOptions().setTimeout(10000)
        );
        page.waitForTimeout(500);
    }

    /**
     * Wait for RigidBodyModule data to appear in the snapshot.
     */
    public void waitForRigidBodyModule() {
        page.waitForCondition(() -> page.getByText("RigidBodyModule").isVisible());
        page.waitForTimeout(500);
    }

    /**
     * Get all visible component names from the snapshot tree.
     * This includes components that may be nested inside modules.
     */
    public List<String> getVisibleComponentNames() {
        return page.locator(".MuiTreeItem-label").allTextContents();
    }

    /**
     * Check if the snapshot displays a specific component value pattern.
     * @param componentName the component name (e.g., "VELOCITY_X")
     * @return true if the component is visible in the snapshot
     */
    public boolean hasComponentWithValue(String componentName) {
        // Look for the component name followed by a colon and value
        return page.locator("text=" + componentName).isVisible();
    }

    /**
     * Check if WebSocket is connected (green dot visible).
     */
    public boolean isWebSocketConnected() {
        // The green/red circle icon indicates connection status
        return page.locator("svg[data-testid='CircleIcon']").first()
            .evaluate("el => window.getComputedStyle(el).color.includes('76')") // green has 76 in rgb
            .toString().equals("true");
    }

    /**
     * Wait for WebSocket to connect (up to 30 seconds).
     */
    public void waitForWebSocketConnection() {
        // Wait for the connection indicator to turn green
        page.waitForCondition(() -> {
            Locator indicator = page.locator("[data-testid='CircleIcon']").first();
            if (indicator.count() == 0) return false;
            String color = indicator.evaluate("el => window.getComputedStyle(el).color").toString();
            return color.contains("76, 175"); // MUI success.main green color
        }, new Page.WaitForConditionOptions().setTimeout(30000));
    }

    /**
     * Click the Refresh button to request a new snapshot.
     */
    public void clickRefresh() {
        page.locator("button[title='Refresh'], button:has([data-testid='RefreshIcon'])").click();
        page.waitForTimeout(500);
    }

    /**
     * Get the tick number displayed in the snapshot panel.
     */
    public long getTick() {
        Locator tickChip = page.locator(".MuiChip-label:has-text('Tick')");
        if (tickChip.count() == 0) return -1;
        String text = tickChip.textContent();
        return Long.parseLong(text.replaceAll("[^0-9]", ""));
    }

    /**
     * Get the entity count displayed in the snapshot panel.
     */
    public int getEntityCount() {
        Locator entityChip = page.locator(".MuiChip-label:has-text('Entities')");
        if (entityChip.count() == 0) return -1;
        String text = entityChip.textContent();
        return Integer.parseInt(text.replaceAll("[^0-9]", ""));
    }

    /**
     * Expand a module accordion to see its components.
     */
    public void expandModule(String moduleName) {
        Locator accordion = page.locator(".MuiAccordion-root:has-text('" + moduleName + "')");
        if (accordion.count() > 0) {
            Locator summary = accordion.first().locator(".MuiAccordionSummary-root");
            Locator details = accordion.first().locator(".MuiAccordionDetails-root");

            // Check if already expanded
            if (!details.isVisible()) {
                summary.click();
                // Wait for accordion to expand
                details.waitFor(new Locator.WaitForOptions()
                    .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE)
                    .setTimeout(5000));
            }
            page.waitForTimeout(300);
        }
    }

    /**
     * Get the sample values for a specific component.
     * @param componentName the component name (e.g., "POSITION_X")
     * @return the sample values text, or null if not found
     */
    public String getComponentSampleValues(String componentName) {
        // Look for the table row containing this component name in any expanded accordion
        Locator row = page.locator(".MuiAccordionDetails-root tr:has-text('" + componentName + "')");

        // Wait briefly for the row to appear
        page.waitForTimeout(500);

        if (row.count() == 0) {
            // Try to find in all tables on the page
            row = page.locator("tr:has(th:text-is('" + componentName + "'))");
            if (row.count() == 0) {
                row = page.locator("tr:has(td:has-text('" + componentName + "'))");
            }
        }

        if (row.count() == 0) return null;

        // Get all table cells in the row
        Locator cells = row.first().locator("td");
        int cellCount = cells.count();

        // Values are in the last column (3rd column, index 2)
        if (cellCount >= 3) {
            return cells.nth(2).textContent().trim();
        } else if (cellCount >= 1) {
            // Fallback to last cell
            return cells.last().textContent().trim();
        }
        return null;
    }

    /**
     * Check if snapshot data is loaded (modules and components visible).
     */
    public boolean hasSnapshotData() {
        return page.locator(".MuiAccordion-root").count() > 0;
    }

    /**
     * Get the first numeric value from a component's sample values.
     * @param componentName the component name (e.g., "POSITION_X")
     * @return the first numeric value, or null if not found or not a number
     */
    public Double getFirstComponentValue(String componentName) {
        String sampleValues = getComponentSampleValues(componentName);
        if (sampleValues == null || sampleValues.isEmpty()) return null;

        // Sample values are comma-separated, get the first one
        String[] parts = sampleValues.split(",");
        if (parts.length == 0) return null;

        String firstValue = parts[0].trim();
        try {
            return Double.parseDouble(firstValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Assert that a component has a specific value (within tolerance for floating point).
     * @param componentName the component name
     * @param expectedValue the expected value
     * @param tolerance acceptable difference
     * @return true if the component value matches within tolerance
     */
    public boolean componentHasValue(String componentName, double expectedValue, double tolerance) {
        Double actual = getFirstComponentValue(componentName);
        if (actual == null) return false;
        return Math.abs(actual - expectedValue) <= tolerance;
    }

    /**
     * Assert that a component has an integer value.
     * @param componentName the component name
     * @param expectedValue the expected integer value
     * @return true if the component value matches exactly
     */
    public boolean componentHasIntValue(String componentName, int expectedValue) {
        Double actual = getFirstComponentValue(componentName);
        if (actual == null) return false;
        return actual.intValue() == expectedValue;
    }

    /**
     * Get a summary of all component values for a module (for debugging).
     * @param moduleName the module name
     * @return a string summarizing all components and their values
     */
    public String getModuleSummary(String moduleName) {
        expandModule(moduleName);
        StringBuilder sb = new StringBuilder(moduleName).append(":\n");
        Locator rows = page.locator(".MuiAccordion-root:has-text('" + moduleName + "') tr");
        for (int i = 0; i < rows.count(); i++) {
            Locator row = rows.nth(i);
            List<String> cells = row.locator("td, th").allTextContents();
            if (cells.size() >= 2) {
                sb.append("  ").append(String.join(" | ", cells)).append("\n");
            }
        }
        return sb.toString();
    }
}
