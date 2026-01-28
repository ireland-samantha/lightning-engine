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
 * Playwright Page Object for the Logs page.
 */
public class PWLogsPage {
    private final Page page;

    public PWLogsPage(Page page) {
        this.page = page;
    }

    public boolean isDisplayed() {
        page.waitForLoadState();
        page.waitForTimeout(500);
        return page.getByText("Logs").first().isVisible() ||
               page.getByText("No Container Selected").isVisible();
    }

    public void waitForPageLoad() {
        page.waitForLoadState();
        page.waitForTimeout(500);
    }

    public void selectMatch(String matchLabel) {
        page.getByLabel("Match").click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(matchLabel)).click();
    }

    public void selectPlayer(String playerLabel) {
        page.getByLabel("Player").click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(playerLabel)).click();
    }

    public void startStreaming() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Start Streaming")).click();
    }

    public void stopStreaming() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Stop Streaming")).click();
    }

    public boolean isStreaming() {
        return page.locator(".MuiChip-root").filter(new Locator.FilterOptions().setHasText("Connected")).isVisible();
    }

    public void toggleAutoScroll() {
        page.getByLabel("Auto-scroll").click();
    }

    public boolean isAutoScrollEnabled() {
        return page.getByLabel("Auto-scroll").isChecked();
    }

    public int getLogCount() {
        return page.locator("tbody tr").count();
    }

    public List<String> getLogMessages() {
        // Get the message column content
        return page.locator("tbody tr td:nth-child(5)").allTextContents();
    }

    public void expandLogEntry(int index) {
        page.locator("tbody tr").nth(index * 2).click(); // Every other row is the expandable one
    }

    public void clearLogs() {
        page.locator("button[title='Clear logs']").click();
    }

    public void refresh() {
        page.locator("button[title='Refresh matches/players']").click();
    }

    public boolean isNoLogsMessageDisplayed() {
        return page.getByText("No errors logged").isVisible() ||
               page.getByText("Waiting for errors").isVisible();
    }

    public boolean isNoContainerSelectedMessageDisplayed() {
        return page.getByText("No Container Selected").isVisible() ||
               page.getByText("Select a container").isVisible();
    }

    public void waitForErrorAlert() {
        page.locator(".MuiAlert-standardError, .MuiAlert-filledError").waitFor();
    }
}
