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
 * Playwright Page Object for the Roles page.
 */
public class PWRolesPage {
    private final Page page;

    public PWRolesPage(Page page) {
        this.page = page;
    }

    // ==================== Navigation ====================

    public boolean isDisplayed() {
        page.waitForLoadState();
        page.waitForTimeout(500);
        return page.getByText("Role Management").first().isVisible() ||
               page.getByText("Add Role").isVisible();
    }

    public void waitForPageLoad() {
        page.waitForLoadState();
        // Wait for table to be visible
        page.locator("table").first().waitFor(new Locator.WaitForOptions().setTimeout(10000));
        page.waitForTimeout(500);
    }

    /**
     * Wait for the table to have at least one row of data.
     */
    public void waitForRolesLoaded() {
        page.waitForLoadState();
        // Wait for table body with at least one row
        page.locator("tbody tr").first().waitFor(new Locator.WaitForOptions().setTimeout(10000));
        page.waitForTimeout(300);
    }

    // ==================== State Checks ====================

    public boolean isNoRolesMessageDisplayed() {
        return page.getByText("No roles").isVisible();
    }

    // ==================== Role Management ====================

    public void clickCreateRole() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add Role")).click();
        page.locator(".MuiDialog-root").waitFor();
    }

    public void createRole(String name) {
        clickCreateRole();
        page.getByLabel("Name").fill(name);
        page.locator(".MuiDialogTitle-root").click(); // Close any dropdowns
        page.locator(".MuiDialog-root")
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save")).click();
        waitForSuccessAlert();
    }

    public void createRole(String name, String description) {
        clickCreateRole();
        page.getByLabel("Name").fill(name);
        page.getByLabel("Description").fill(description);
        page.locator(".MuiDialogTitle-root").click(); // Close any dropdowns
        page.locator(".MuiDialog-root")
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save")).click();
        waitForSuccessAlert();
    }

    public void createRoleWithIncludes(String name, String... includedRoles) {
        clickCreateRole();
        page.getByLabel("Name").fill(name);

        // Select included roles
        Locator includesInput = page.getByLabel("Includes Roles");
        for (String role : includedRoles) {
            includesInput.click();
            page.waitForTimeout(200);
            page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(role)).click();
        }
        page.keyboard().press("Escape"); // Close dropdown
        page.locator(".MuiDialogTitle-root").click(); // Ensure focus is outside

        page.locator(".MuiDialog-root")
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save")).click();
        waitForSuccessAlert();
    }

    public List<String> getRoleNames() {
        return page.locator("tbody tr td:first-child .MuiChip-root").allTextContents();
    }

    public int getRoleCount() {
        return page.locator("tbody tr").count();
    }

    public Locator getRoleRow(String roleName) {
        return page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText(roleName));
    }

    public void editRole(String roleName) {
        getRoleRow(roleName).locator("button[title='Edit']").click();
        page.locator(".MuiDialog-root").waitFor();
    }

    public void deleteRole(String roleName) {
        getRoleRow(roleName).locator("button[title='Delete']").click();
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
