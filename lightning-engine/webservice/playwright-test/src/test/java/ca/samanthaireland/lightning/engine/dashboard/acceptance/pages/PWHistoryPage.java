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
 * Playwright Page Object for the History page.
 */
public class PWHistoryPage {
    private final Page page;

    public PWHistoryPage(Page page) {
        this.page = page;
    }

    public boolean isDisplayed() {
        page.waitForLoadState();
        page.waitForTimeout(500);
        return page.getByText("History").first().isVisible() ||
               page.getByText("No Container Selected").isVisible();
    }

    public void waitForPageLoad() {
        page.waitForLoadState();
        page.waitForTimeout(500);
    }

    public int getHistoryEntryCount() {
        return page.locator("tbody tr").count();
    }

    public List<String> getHistoryTicks() {
        return page.locator("tbody tr td:first-child").allTextContents();
    }

    public void restoreToTick(long tick) {
        page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText(String.valueOf(tick)))
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Restore"))
                .click();
        waitForSuccessAlert();
    }

    public void viewSnapshotAtTick(long tick) {
        page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText(String.valueOf(tick)))
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("View"))
                .click();
    }

    public void refresh() {
        page.locator("button[title='Refresh']").click();
    }

    public void waitForSuccessAlert() {
        page.locator(".MuiAlert-standardSuccess, .MuiAlert-filledSuccess").waitFor();
    }

    public boolean isNoHistoryMessageDisplayed() {
        return page.getByText("No history").isVisible();
    }

    public boolean isNoContainerSelectedMessageDisplayed() {
        return page.getByText("No Container Selected").isVisible() ||
               page.getByText("Select a container").isVisible();
    }
}
