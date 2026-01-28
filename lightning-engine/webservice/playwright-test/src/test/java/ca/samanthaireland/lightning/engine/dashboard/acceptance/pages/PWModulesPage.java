/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.dashboard.acceptance.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.util.List;

/**
 * Playwright Page Object for the Modules page.
 */
public class PWModulesPage {
    private final Page page;

    public PWModulesPage(Page page) {
        this.page = page;
    }

    public boolean isDisplayed() {
        return page.getByText("Modules").first().isVisible();
    }

    public void waitForPageLoad() {
        page.getByText("Modules").first().waitFor();
    }

    public int getModuleCount() {
        return page.locator("tbody tr").count();
    }

    public List<String> getModuleNames() {
        return page.locator("tbody tr td:first-child").allTextContents();
    }

    public boolean hasModule(String moduleName) {
        return page.locator("tbody tr").filter(new com.microsoft.playwright.Locator.FilterOptions()
                .setHasText(moduleName)).count() > 0;
    }

    public void clickReloadAll() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Reload All")).click();
    }

    public void refresh() {
        page.locator("button[title='Refresh']").click();
    }

    public void waitForSuccessAlert() {
        page.locator(".MuiAlert-standardSuccess, .MuiAlert-filledSuccess").waitFor();
    }

    public boolean isNoModulesMessageDisplayed() {
        return page.getByText("No modules").isVisible();
    }

    public boolean isNoContainerSelectedMessageDisplayed() {
        return page.getByText("No Container Selected").isVisible() ||
               page.getByText("Select a container").isVisible();
    }
}
