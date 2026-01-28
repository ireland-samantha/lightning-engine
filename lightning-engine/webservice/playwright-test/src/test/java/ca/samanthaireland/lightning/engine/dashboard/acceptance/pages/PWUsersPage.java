/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.dashboard.acceptance.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Playwright Page Object for the Users page.
 */
public class PWUsersPage {
    private final Page page;

    public PWUsersPage(Page page) {
        this.page = page;
    }

    // ==================== Navigation ====================

    public boolean isDisplayed() {
        page.waitForLoadState();
        page.waitForTimeout(500);
        return page.getByText("User Management").first().isVisible() ||
               page.getByText("Add User").isVisible();
    }

    public void waitForPageLoad() {
        page.waitForLoadState();
        page.waitForTimeout(500);
    }

    // ==================== State Checks ====================

    public boolean isNoUsersMessageDisplayed() {
        return page.getByText("No users").isVisible();
    }

    // ==================== User Management ====================

    public void clickCreateUser() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add User")).click();
        page.locator(".MuiDialog-root").waitFor();
    }

    public void createUser(String username, String password) {
        clickCreateUser();
        page.getByLabel("Username").fill(username);
        page.getByLabel("Password").fill(password);
        page.locator(".MuiDialogTitle-root").click(); // Close any dropdowns
        page.locator(".MuiDialog-root")
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save")).click();
        waitForSuccessAlert();
    }

    public void createUser(String username, String password, String... roles) {
        clickCreateUser();
        page.getByLabel("Username").fill(username);
        page.getByLabel("Password").fill(password);

        // Select roles
        Locator rolesInput = page.getByLabel("Roles");
        for (String role : roles) {
            rolesInput.click();
            page.waitForTimeout(200);
            page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(role)).click();
        }
        page.keyboard().press("Escape"); // Close dropdown
        page.locator(".MuiDialogTitle-root").click(); // Ensure focus is outside

        page.locator(".MuiDialog-root")
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save")).click();
        waitForSuccessAlert();
    }

    public List<String> getUsernames() {
        return page.locator("tbody tr td:first-child").allTextContents();
    }

    public int getUserCount() {
        return page.locator("tbody tr").count();
    }

    public Locator getUserRow(String username) {
        return page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText(username));
    }

    public String getUserStatus(String username) {
        return getUserRow(username).locator(".MuiChip-root").first().textContent();
    }

    public void toggleUserEnabled(String username) {
        getUserRow(username)
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(Pattern.compile("Enable|Disable")))
                .click();
        waitForSuccessAlert();
    }

    public void deleteUser(String username) {
        getUserRow(username).locator("button[title='Delete']").click();
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
}
