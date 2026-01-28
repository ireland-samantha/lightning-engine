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
 * Playwright Page Object for the AI page.
 */
public class PWAIPage {
    private final Page page;

    public PWAIPage(Page page) {
        this.page = page;
    }

    // ==================== Navigation ====================

    public boolean isDisplayed() {
        return page.getByText("AI").first().isVisible();
    }

    public void waitForPageLoad() {
        page.getByText("AI").first().waitFor();
    }

    // ==================== State Checks ====================

    public boolean isNoAIsMessageDisplayed() {
        return page.getByText("No AIs").isVisible();
    }

    // ==================== AI List ====================

    public List<String> getAINames() {
        return page.locator("tbody tr td:first-child").allTextContents();
    }

    public int getAICount() {
        return page.locator("tbody tr").count();
    }

    public Locator getAIRow(String name) {
        return page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText(name));
    }

    public String getAIVersion(String name) {
        return getAIRow(name).locator(".MuiChip-root").first().textContent();
    }

    public String getAIDescription(String name) {
        return getAIRow(name).locator("td").nth(2).textContent();
    }

    // ==================== AI Actions ====================

    public void clickUpload() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Upload AI")).click();
    }

    public void uploadAI(Path jarPath) {
        page.locator("input[type='file']").setInputFiles(jarPath);
        waitForSuccessAlert();
    }

    public void deleteAI(String name) {
        getAIRow(name).locator("button[title='Delete']").click();
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
