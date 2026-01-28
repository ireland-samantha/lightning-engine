/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.dashboard.acceptance.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Playwright Page Object for the Resources page.
 */
public class PWResourcesPage {
    private final Page page;

    public PWResourcesPage(Page page) {
        this.page = page;
    }

    // ==================== Navigation ====================

    public boolean isDisplayed() {
        return page.getByText("Resources").first().isVisible();
    }

    public void waitForPageLoad() {
        page.getByText("Resources").first().waitFor();
    }

    // ==================== State Checks ====================

    public boolean isNoResourcesMessageDisplayed() {
        return page.getByText("No resources").isVisible();
    }

    // ==================== Resource List ====================

    public List<String> getResourceNames() {
        return page.locator("tbody tr td:first-child").allTextContents();
    }

    public int getResourceCount() {
        return page.locator("tbody tr").count();
    }

    public Locator getResourceRow(String resourceName) {
        return page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText(resourceName));
    }

    // ==================== Resource Actions ====================

    public void clickUpload() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Upload Resource")).click();
    }

    public void uploadResource(Path filePath) {
        // Use file chooser approach for Playwright
        page.locator("input[type='file']").setInputFiles(filePath);
        page.locator(".MuiDialog-root").waitFor();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Upload")).click();
        waitForSuccessAlert();
    }

    public void uploadResource(Path filePath, String resourceName) {
        page.locator("input[type='file']").setInputFiles(filePath);
        page.locator(".MuiDialog-root").waitFor();
        page.getByLabel("Resource Name").clear();
        page.getByLabel("Resource Name").fill(resourceName);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Upload")).click();
        waitForSuccessAlert();
    }

    public void downloadResource(String resourceName) {
        getResourceRow(resourceName).locator("button[title='Download'], button:has(svg[data-testid='DownloadIcon'])").click();
    }

    public void deleteResource(String resourceName) {
        getResourceRow(resourceName).locator("button[title='Delete']").click();
        // Confirm
        page.locator(".MuiDialog-root")
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(Pattern.compile("Delete|Confirm")))
                .click();
        waitForSuccessAlert();
    }

    // ==================== Refresh ====================

    public void refresh() {
        page.locator("button[title='Refresh']").click();
    }

    // ==================== Alerts ====================

    public void waitForSuccessAlert() {
        page.locator(".MuiAlert-standardSuccess, .MuiAlert-filledSuccess").waitFor();
    }

    public void waitForErrorAlert() {
        page.locator(".MuiAlert-standardError, .MuiAlert-filledError").waitFor();
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

    public boolean isNoContainerSelectedMessageDisplayed() {
        return page.getByText("No Container Selected").isVisible() ||
               page.getByText("Select a container").isVisible();
    }
}
