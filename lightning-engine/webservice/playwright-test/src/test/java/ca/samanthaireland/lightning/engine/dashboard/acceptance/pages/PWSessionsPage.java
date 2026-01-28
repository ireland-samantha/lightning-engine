/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.dashboard.acceptance.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Playwright Page Object for the Sessions page.
 */
public class PWSessionsPage {
    private final Page page;

    public PWSessionsPage(Page page) {
        this.page = page;
    }

    public boolean isDisplayed() {
        page.waitForLoadState();
        page.waitForTimeout(500);
        return page.getByText("Sessions").first().isVisible() ||
               page.getByText("No Container Selected").isVisible();
    }

    public void waitForPageLoad() {
        page.waitForLoadState();
        page.waitForTimeout(500);
    }

    public void clickCreateSession() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create Session")).click();
    }

    public void createSession(String matchLabel, String playerLabel) {
        clickCreateSession();
        page.locator(".MuiDialog-root").waitFor();
        page.waitForTimeout(500); // Wait for dialog content and data to load

        // Select match from dropdown
        Locator matchInput = page.getByLabel("Select Match");
        matchInput.click();
        page.locator("[role='listbox']").waitFor(new Locator.WaitForOptions().setTimeout(10000));
        page.waitForTimeout(300);
        // Use exact match pattern to avoid matching "Match 1" with "Match 10", etc.
        Locator matchOption = page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions()
                .setName(Pattern.compile("^" + Pattern.quote(matchLabel) + "$")));
        matchOption.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        matchOption.click();
        page.waitForTimeout(300);

        // Select player from dropdown
        Locator playerInput = page.getByLabel("Select Player");
        playerInput.click();
        page.locator("[role='listbox']").waitFor(new Locator.WaitForOptions().setTimeout(10000));
        page.waitForTimeout(300);
        // Use exact match pattern to avoid matching "Player 1" with "Player 10", etc.
        Locator playerOption = page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions()
                .setName(Pattern.compile("^" + Pattern.quote(playerLabel) + "$")));
        playerOption.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        playerOption.click();
        page.waitForTimeout(300);

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect")).click();
        waitForSuccessAlert();
        // Wait for alert to auto-dismiss
        waitForAlertToClose();
    }

    /**
     * Creates a session by selecting options by index (0-based).
     * Useful when the exact option text may vary.
     *
     * @param matchIndex index of match to select (0-based)
     * @param playerIndex index of player to select (0-based)
     */
    public void createSessionByIndex(int matchIndex, int playerIndex) {
        clickCreateSession();
        page.locator(".MuiDialog-root").waitFor();
        page.waitForTimeout(500); // Wait for dialog content and data to load

        // Select match by index
        Locator matchInput = page.getByLabel("Select Match");
        matchInput.click();
        page.locator("[role='listbox']").waitFor(new Locator.WaitForOptions().setTimeout(10000));
        page.waitForTimeout(300);
        page.getByRole(AriaRole.OPTION).nth(matchIndex).click();
        page.waitForTimeout(300);

        // Select player by index
        Locator playerInput = page.getByLabel("Select Player");
        playerInput.click();
        page.locator("[role='listbox']").waitFor(new Locator.WaitForOptions().setTimeout(10000));
        page.waitForTimeout(300);
        page.getByRole(AriaRole.OPTION).nth(playerIndex).click();
        page.waitForTimeout(300);

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect")).click();
        waitForSuccessAlert();
        waitForAlertToClose();
    }

    /**
     * Creates a session by selecting the first available match and player in the dropdowns.
     */
    public void createSessionWithFirstOptions() {
        clickCreateSession();
        page.locator(".MuiDialog-root").waitFor();
        page.waitForTimeout(500); // Wait for dialog content and data to load

        // Select first match from dropdown
        Locator matchInput = page.getByLabel("Select Match");
        matchInput.click();
        page.locator("[role='listbox']").waitFor(new Locator.WaitForOptions().setTimeout(10000));
        page.waitForTimeout(300);
        page.getByRole(AriaRole.OPTION).first().click();
        page.waitForTimeout(300);

        // Select first player from dropdown
        Locator playerInput = page.getByLabel("Select Player");
        playerInput.click();
        page.locator("[role='listbox']").waitFor(new Locator.WaitForOptions().setTimeout(10000));
        page.waitForTimeout(300);
        page.getByRole(AriaRole.OPTION).first().click();
        page.waitForTimeout(300);

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Connect")).click();
        waitForSuccessAlert();
        // Wait for alert to auto-dismiss
        waitForAlertToClose();
    }

    private void waitForAlertToClose() {
        // Wait for any visible alert to close
        Locator alert = page.locator(".MuiAlert-root");
        try {
            if (alert.count() > 0 && alert.isVisible()) {
                alert.waitFor(new Locator.WaitForOptions()
                        .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN)
                        .setTimeout(5000));
            }
        } catch (Exception e) {
            // Alert might have already closed
        }
        // Also wait for any dialog to close
        Locator dialog = page.locator(".MuiDialog-root");
        try {
            if (dialog.count() > 0 && dialog.isVisible()) {
                dialog.waitFor(new Locator.WaitForOptions()
                        .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN)
                        .setTimeout(5000));
            }
        } catch (Exception e) {
            // Dialog might have already closed
        }
    }

    public int getSessionCount() {
        return page.locator("tbody tr").count();
    }

    public String getSessionStatus(String playerName) {
        return page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText(playerName))
                .locator("td").nth(2).locator(".MuiChip-label").textContent();
    }

    public void disconnectSession(String playerName) {
        page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText(playerName))
                .locator("button[title='Disconnect']")
                .click();
    }

    public void reconnectSession(String playerName) {
        page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText(playerName))
                .locator("button[title='Reconnect']")
                .click();
    }

    public void abandonSession(String playerName) {
        page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText(playerName))
                .locator("button[title='Abandon']")
                .click();
    }

    public void refresh() {
        // First ensure any open dialog is closed
        waitForDialogToClose();
        page.locator("button[title='Refresh']").click();
    }

    private void waitForDialogToClose() {
        // Press Escape to close any open dialog
        Locator dialog = page.locator(".MuiDialog-root");
        if (dialog.count() > 0 && dialog.isVisible()) {
            page.keyboard().press("Escape");
            try {
                dialog.waitFor(new Locator.WaitForOptions()
                        .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN)
                        .setTimeout(5000));
            } catch (Exception e) {
                // Dialog might have already closed
            }
        }
    }

    public void waitForSuccessAlert() {
        page.locator(".MuiAlert-standardSuccess, .MuiAlert-filledSuccess").waitFor();
    }

    public boolean isNoSessionsMessageDisplayed() {
        return page.getByText("No sessions").isVisible();
    }

    public boolean isNoContainerSelectedMessageDisplayed() {
        return page.getByText("No Container Selected").isVisible() ||
               page.getByText("Select a container").isVisible();
    }
}
