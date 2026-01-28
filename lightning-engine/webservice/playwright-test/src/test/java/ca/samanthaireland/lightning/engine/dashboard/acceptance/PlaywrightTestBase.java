/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.dashboard.acceptance;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

/**
 * Base class for Playwright acceptance tests with Testcontainers.
 * Uses Testcontainers for the backend server and Playwright's built-in
 * browser management for UI testing.
 */
@Testcontainers
public abstract class PlaywrightTestBase {

    protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(2);
    protected static final Duration POLL_INTERVAL = Duration.ofMillis(500);

    // Playwright instances (shared across all tests in class)
    protected static Playwright playwright;
    protected static Browser browser;

    // Per-test instances
    protected BrowserContext context;
    protected Page page;

    // Base URL for the backend server
    protected static String baseUrl;

    // Backend server container (using pre-built image)
    @Container
    protected static GenericContainer<?> backend = TestContainers.backendContainer();

    @BeforeAll
    static void setupPlaywright() {
        // Check if we should use external backend (for local development)
        String externalUrl = System.getProperty("backend.url");
        if (externalUrl != null && !externalUrl.isEmpty()) {
            baseUrl = externalUrl;
        } else {
            // Use backend container URL accessible from host
            baseUrl = TestContainers.getBaseUrl(backend);
        }

        // Initialize Playwright
        playwright = Playwright.create();

        // Check for headed mode (debugging)
        boolean headed = System.getProperty("playwright.headed") != null;

        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(!headed)
                .setSlowMo(headed ? 100 : 0)); // Slow down for visibility when headed
    }

    @AfterAll
    static void teardownPlaywright() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void setupPage() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1920, 1080));
        page = context.newPage();
        page.setDefaultTimeout(DEFAULT_TIMEOUT.toMillis());
    }

    @AfterEach
    void cleanupPage() {
        if (context != null) {
            context.close();
        }
    }

    // ==================== Navigation Helpers ====================

    protected void navigateTo(String path) {
        page.navigate(baseUrl + path);
        page.waitForLoadState();
    }

    protected void navigateToHome() {
        navigateTo("/admin/dashboard/");
        // Wait for React app to render (either login form or dashboard)
        page.waitForSelector("form, .MuiDrawer-docked",
            new Page.WaitForSelectorOptions().setTimeout(30000));
    }

    protected void login(String username, String password) {
        navigateTo("/admin/dashboard/");

        // Wait for login form to be ready
        page.getByLabel("Username").waitFor();

        // Fill credentials
        page.getByLabel("Username").fill(username);
        page.getByLabel("Password").fill(password);

        // Click login and wait for response
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")).click();

        // Wait for dashboard to appear - the logout button should be visible
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Logout")).waitFor(
            new Locator.WaitForOptions().setTimeout(30000));
    }

    protected void loginAsAdmin() {
        login("admin", "admin");
    }

    // ==================== Sidebar Navigation ====================

    protected void clickSidebarItem(String itemText) {
        page.locator(".MuiDrawer-docked").getByText(itemText).click();
        page.waitForLoadState();
    }

    // ==================== Element Interaction Helpers ====================

    protected void clickButton(String buttonText) {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(buttonText)).click();
    }

    protected void typeInField(String labelText, String text) {
        page.getByLabel(labelText).fill(text);
    }

    protected void selectOption(String labelText, String optionText) {
        page.getByLabel(labelText).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(optionText)).click();
    }

    protected void selectAutocompleteOptions(String labelText, String... options) {
        Locator input = page.getByLabel(labelText);
        input.click();
        for (String option : options) {
            page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(option)).click();
        }
        // Click outside to close
        page.locator("body").click();
    }

    // ==================== Assertion Helpers ====================

    protected boolean elementWithTextExists(String text) {
        return page.getByText(text).count() > 0;
    }

    protected void waitForText(String text) {
        page.getByText(text).waitFor();
    }

    protected void waitForAlert(String messageContains) {
        page.locator(".MuiAlert-message").filter(new Locator.FilterOptions()
                .setHasText(messageContains)).waitFor();
    }

    protected void waitForSuccessAlert() {
        page.locator(".MuiAlert-standardSuccess, .MuiAlert-filledSuccess").waitFor();
    }

    protected void waitForErrorAlert() {
        page.locator(".MuiAlert-standardError, .MuiAlert-filledError").waitFor();
    }

    protected String getText(String selector) {
        return page.locator(selector).textContent();
    }

    protected boolean isVisible(String selector) {
        return page.locator(selector).isVisible();
    }

    // ==================== Table Helpers ====================

    protected Locator getTableRow(String cellContent) {
        return page.locator("tr").filter(new Locator.FilterOptions().setHasText(cellContent));
    }

    protected void clickRowAction(String rowIdentifier, String buttonTitle) {
        getTableRow(rowIdentifier).getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(buttonTitle)).click();
    }

    protected int getTableRowCount() {
        return page.locator("tbody tr").count();
    }

    // ==================== Card Helpers ====================

    protected Locator getCard(String cardTitle) {
        return page.locator(".MuiCard-root").filter(new Locator.FilterOptions().setHasText(cardTitle));
    }

    protected void clickCard(String cardTitle) {
        getCard(cardTitle).click();
    }

    // ==================== Dialog Helpers ====================

    protected Locator waitForDialog() {
        Locator dialog = page.locator(".MuiDialog-root");
        dialog.waitFor();
        return dialog;
    }

    protected void closeDialog() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Cancel")).click();
        page.locator(".MuiDialog-root").waitFor(new Locator.WaitForOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));
    }

    protected void confirmDialog() {
        // Try common confirm button texts
        Locator confirmButton = page.locator(".MuiDialog-root")
                .getByRole(AriaRole.BUTTON)
                .filter(new Locator.FilterOptions().setHasText(java.util.regex.Pattern.compile("OK|Confirm|Yes|Create|Save|Delete")));
        confirmButton.click();
    }

    protected boolean isDialogOpen() {
        return page.locator(".MuiDialog-root").isVisible();
    }

    // ==================== Utility Helpers ====================

    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void awaitCondition(Runnable assertion) {
        await().atMost(DEFAULT_TIMEOUT)
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(assertion::run);
    }

    protected void refresh() {
        page.reload();
        page.waitForLoadState();
    }

    protected String getCurrentUrl() {
        return page.url();
    }

    protected void takeScreenshot(String name) {
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(java.nio.file.Paths.get("target/screenshots/" + name + ".png")));
    }
}
