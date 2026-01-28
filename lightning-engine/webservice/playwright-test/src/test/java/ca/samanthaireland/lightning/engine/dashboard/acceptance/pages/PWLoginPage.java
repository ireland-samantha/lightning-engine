/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.dashboard.acceptance.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright Page Object for the Login page.
 */
public class PWLoginPage {
    private final Page page;

    public PWLoginPage(Page page) {
        this.page = page;
    }

    public PWLoginPage enterUsername(String username) {
        page.getByLabel("Username").fill(username);
        return this;
    }

    public PWLoginPage enterPassword(String password) {
        page.getByLabel("Password").fill(password);
        return this;
    }

    public void clickLogin() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")).click();
    }

    public PWDashboardPage loginAs(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
        waitForLoginToComplete();
        return new PWDashboardPage(page);
    }

    public PWDashboardPage loginAsAdmin() {
        return loginAs("admin", "admin");
    }

    public boolean isErrorDisplayed() {
        return page.locator(".MuiAlert-standardError, [role='alert']").isVisible();
    }

    public String getErrorMessage() {
        return page.locator(".MuiAlert-standardError, [role='alert']").textContent();
    }

    public boolean isLoginButtonEnabled() {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")).isEnabled();
    }

    public boolean isLoading() {
        return page.locator(".MuiCircularProgress-root").isVisible();
    }

    private void waitForLoginToComplete() {
        // Wait for dashboard Logout button which should be visible after login
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Logout")).waitFor();
    }

    public boolean isDisplayed() {
        return page.getByLabel("Username").isVisible();
    }
}
