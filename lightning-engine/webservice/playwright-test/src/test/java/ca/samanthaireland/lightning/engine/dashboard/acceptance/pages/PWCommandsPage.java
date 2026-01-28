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
 * Playwright Page Object for the Commands page.
 * Commands are displayed in a table with Send buttons that open a dialog.
 */
public class PWCommandsPage {
    private final Page page;

    public PWCommandsPage(Page page) {
        this.page = page;
    }

    public boolean isDisplayed() {
        page.waitForLoadState();
        page.waitForTimeout(500);
        return page.getByText("Commands").first().isVisible() ||
               page.getByText("No Container Selected").isVisible();
    }

    public void waitForPageLoad() {
        page.waitForLoadState();
        // Wait for the table to load
        page.locator("table").first().waitFor(new Locator.WaitForOptions().setTimeout(10000));
        page.waitForTimeout(500);
    }

    public List<String> getAvailableCommands() {
        // Command names are in table cells with monospace font
        return page.locator("tbody tr td:first-child .MuiTypography-root").allTextContents();
    }

    /**
     * Click the Send button for a command to open the send dialog.
     * @param commandName the name of the command (e.g., "spawn")
     */
    public void openSendDialog(String commandName) {
        // Find the row containing the command name and click its Send button
        page.locator("tbody tr")
            .filter(new Locator.FilterOptions().setHasText(commandName))
            .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Send"))
            .click();
        // Wait for dialog to open
        page.locator(".MuiDialog-root").waitFor(new Locator.WaitForOptions().setTimeout(10000));
        page.waitForTimeout(300);
    }

    /**
     * Fill a parameter in the currently open send dialog.
     * Works for regular TextField inputs.
     * @param parameterName the label of the parameter field
     * @param value the value to enter
     */
    public void fillParameter(String parameterName, String value) {
        page.locator(".MuiDialog-root").getByLabel(parameterName).fill(value);
    }

    /**
     * Select the first option from an Autocomplete field in the dialog.
     * Used for matchId, playerId, entityId fields that use Autocomplete.
     * @param parameterName the label of the autocomplete field
     */
    public void selectFirstAutocompleteOption(String parameterName) {
        Locator dialog = page.locator(".MuiDialog-root");
        Locator input = dialog.getByLabel(parameterName);

        // Wait for data to be loaded
        page.waitForTimeout(2000);

        // Try multiple attempts to open and select from dropdown
        for (int attempt = 0; attempt < 3; attempt++) {
            // Click on the dropdown button to open
            Locator autocomplete = dialog.locator(".MuiAutocomplete-root")
                .filter(new Locator.FilterOptions().setHas(input));
            Locator expandButton = autocomplete.locator(".MuiAutocomplete-popupIndicator");

            if (expandButton.count() > 0) {
                expandButton.click();
            } else {
                // Fallback: click input and press ArrowDown
                input.click();
                page.waitForTimeout(200);
                page.keyboard().press("ArrowDown");
            }
            page.waitForTimeout(500);

            // Check for listbox
            Locator listbox = page.locator("[role='listbox']");
            if (listbox.count() > 0 && listbox.isVisible()) {
                // Check for "No options" message
                Locator noOptions = listbox.locator("text=No options");
                if (noOptions.count() > 0) {
                    System.out.println("Autocomplete " + parameterName + " attempt " + (attempt+1) + ": No options yet, retrying...");
                    page.keyboard().press("Escape");
                    page.waitForTimeout(1000);
                    continue;
                }

                // Select first option
                Locator options = listbox.locator("[role='option']");
                if (options.count() > 0) {
                    options.first().click();
                    page.waitForTimeout(200);
                    return;
                }
            }

            // Close dropdown if open and retry
            page.keyboard().press("Escape");
            page.waitForTimeout(1000);
        }

        // Final debug output
        page.screenshot(new com.microsoft.playwright.Page.ScreenshotOptions()
            .setPath(java.nio.file.Paths.get("target/autocomplete-failed-" + parameterName + ".png")));
        System.out.println("Autocomplete " + parameterName + ": Failed after 3 attempts, no options available");
        throw new RuntimeException("Could not select option for autocomplete field: " + parameterName);
    }

    /**
     * Type a value directly into an Autocomplete field (bypassing dropdown selection).
     * Use this when the dropdown has no options or as a fallback.
     * @param parameterName the label of the autocomplete field
     * @param value the value to type
     */
    public void typeInAutocomplete(String parameterName, String value) {
        Locator dialog = page.locator(".MuiDialog-root");
        Locator input = dialog.getByLabel(parameterName);
        input.fill(value);
        page.waitForTimeout(200);
    }

    /**
     * Click the Send button in the currently open dialog and wait for it to close.
     */
    public void clickSendInDialog() {
        page.locator(".MuiDialog-root")
            .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Send"))
            .click();
        // Wait for dialog to close
        page.locator(".MuiDialog-root").waitFor(
            new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN).setTimeout(10000));
    }

    /**
     * Send a command with the specified parameters.
     * @param commandName the command name (e.g., "spawn")
     * @param parameters alternating parameter names and values
     */
    public void sendCommandWithParams(String commandName, String... parameters) {
        openSendDialog(commandName);
        for (int i = 0; i < parameters.length; i += 2) {
            fillParameter(parameters[i], parameters[i + 1]);
        }
        clickSendInDialog();
        // Small wait for any success alert to process
        page.waitForTimeout(300);
    }

    public void selectModule(String moduleName) {
        page.getByLabel("Filter by Module").click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(moduleName)).click();
    }

    public void refresh() {
        page.locator("button[title='Refresh']").click();
    }

    public void waitForSuccessAlert() {
        page.locator(".MuiAlert-standardSuccess, .MuiAlert-filledSuccess").waitFor(
            new Locator.WaitForOptions().setTimeout(10000));
    }

    public boolean isNoCommandsMessageDisplayed() {
        return page.getByText("No commands available").isVisible();
    }

    public boolean isNoContainerSelectedMessageDisplayed() {
        return page.getByText("No Container Selected").isVisible() ||
               page.getByText("Select a container").isVisible();
    }

    /**
     * Expand a command to show its parameters and Send button.
     * Opens the send dialog for the command.
     */
    public void expandCommand(String commandName) {
        openSendDialog(commandName);
    }

    /**
     * Send the currently expanded command.
     */
    public void sendCommand(String commandName) {
        clickSendInDialog();
    }
}
