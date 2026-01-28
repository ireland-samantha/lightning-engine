/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.dashboard.acceptance.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Playwright Page Object for the Matches page.
 */
public class PWMatchesPage {
    private final Page page;

    public PWMatchesPage(Page page) {
        this.page = page;
    }

    public boolean isDisplayed() {
        page.waitForLoadState();
        page.waitForTimeout(500);
        return page.getByText("Matches").first().isVisible() ||
               page.getByText("No Container Selected").isVisible();
    }

    public void waitForPageLoad() {
        page.waitForLoadState();
        page.waitForTimeout(500);
    }

    public void clickCreateMatch() {
        // Use first() to avoid matching both header button and dialog button
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create Match")).first().click();
    }

    public void createMatch(String... moduleNames) {
        clickCreateMatch();
        // Use MuiDialog-root to avoid matching the mobile drawer
        Locator dialog = page.locator(".MuiDialog-root");
        dialog.waitFor();
        page.waitForTimeout(500); // Wait for dialog to fully render

        // Select modules in autocomplete - use exact match to avoid matching dialog label
        Locator moduleInput = dialog.getByLabel("Modules", new Locator.GetByLabelOptions().setExact(true));
        for (String moduleName : moduleNames) {
            // Click input to open/focus dropdown
            moduleInput.click();
            // Wait for listbox to appear
            page.locator("[role='listbox']").waitFor();
            page.waitForTimeout(300); // Wait for options to populate
            // Select the option
            Locator option = page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(moduleName));
            option.waitFor();
            option.click();
            page.waitForTimeout(200); // Wait for selection to register
        }
        // Tab out of the autocomplete to close dropdown (Escape closes the dialog)
        page.keyboard().press("Tab");
        page.waitForTimeout(200);

        // Click Create button within the dialog
        dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Create")).click();

        // Wait a bit for the API call to process
        page.waitForTimeout(2000);

        // Check for error alert inside the dialog first (API errors are shown there)
        Locator dialogErrorAlert = dialog.locator(".MuiAlert-standardError, .MuiAlert-filledError");
        if (dialogErrorAlert.count() > 0 && dialogErrorAlert.isVisible()) {
            String errorText = dialogErrorAlert.textContent();
            throw new AssertionError("Match creation failed (dialog error): " + errorText);
        }

        // Wait for dialog to close (indicating the operation completed)
        try {
            dialog.waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN).setTimeout(10000));
        } catch (com.microsoft.playwright.TimeoutError e) {
            // Dialog didn't close - check for any error message on screen
            Locator anyError = page.locator(".MuiAlert-standardError, .MuiAlert-filledError");
            if (anyError.count() > 0) {
                String errorText = anyError.first().textContent();
                throw new AssertionError("Match creation failed (error visible): " + errorText, e);
            }
            // Take screenshot for debugging
            page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Paths.get("target/match-creation-failure.png")));
            throw new AssertionError("Match creation failed - dialog didn't close. Screenshot saved to target/match-creation-failure.png", e);
        }
        page.waitForTimeout(500); // Give time for success alert to appear

        // Check for success or error alert
        Locator successAlert = page.locator(".MuiAlert-standardSuccess, .MuiAlert-filledSuccess");
        Locator errorAlert = page.locator(".MuiAlert-standardError, .MuiAlert-filledError");

        // If error alert is visible, the operation failed
        if (errorAlert.isVisible()) {
            throw new AssertionError("Match creation failed: " + errorAlert.textContent());
        }

        // Success alert should be visible
        if (!successAlert.isVisible()) {
            // Wait a bit more and check again
            page.waitForTimeout(500);
            if (!successAlert.isVisible() && !errorAlert.isVisible()) {
                // Neither alert visible - operation may have succeeded anyway, proceed
                return;
            }
            if (errorAlert.isVisible()) {
                throw new AssertionError("Match creation failed: " + errorAlert.textContent());
            }
        }
    }

    public int getMatchCount() {
        // Matches are displayed as cards in a grid
        return page.locator(".MuiCard-root").count();
    }

    public List<String> getMatchIds() {
        // Match IDs are shown in Typography with format "Match X"
        // Extract just the ID number from "Match X" text
        java.util.List<String> ids = new java.util.ArrayList<>();
        java.util.List<String> texts = page.locator(".MuiCard-root .MuiTypography-h6")
            .allTextContents();
        for (String text : texts) {
            if (text.startsWith("Match ")) {
                ids.add(text.substring(6).trim()); // Remove "Match " prefix
            }
        }
        return ids;
    }

    public long getMatchTick(int matchId) {
        Locator card = page.locator(".MuiCard-root").filter(new Locator.FilterOptions().setHasText("Match " + matchId));
        String tickText = card.locator(".MuiChip-root").filter(new Locator.FilterOptions().setHasText("Tick")).textContent();
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(tickText);
        return matcher.find() ? Long.parseLong(matcher.group()) : 0;
    }

    public void selectMatch(int matchId) {
        page.locator(".MuiCard-root").filter(new Locator.FilterOptions().setHasText("Match " + matchId))
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Select"))
                .click();
    }

    public void deleteMatch(int matchId) {
        page.locator(".MuiCard-root").filter(new Locator.FilterOptions().setHasText("Match " + matchId))
                .locator("button[title='Delete']")
                .click();
        // Confirm
        page.locator(".MuiDialog-root")
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(Pattern.compile("Delete|Confirm")))
                .click();
    }

    public void refresh() {
        page.locator("button[title='Refresh']").click();
    }

    public void waitForSuccessAlert() {
        page.locator(".MuiAlert-standardSuccess, .MuiAlert-filledSuccess").waitFor();
    }

    public boolean isNoMatchesMessageDisplayed() {
        return page.getByText("No matches").isVisible();
    }
}
