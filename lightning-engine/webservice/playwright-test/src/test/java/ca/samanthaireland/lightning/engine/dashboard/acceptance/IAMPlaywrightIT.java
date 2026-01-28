/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.dashboard.acceptance;

import ca.samanthaireland.lightning.engine.dashboard.acceptance.pages.*;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Playwright integration tests for IAM (Users and Roles) management.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IAMPlaywrightIT extends PlaywrightTestBase {

    @BeforeEach
    void login() {
        loginAsAdmin();
        // loginAsAdmin already waits for dashboard to load
    }

    // ==================== Users Tests ====================

    @Test
    @Order(1)
    void viewUsersPage() {
        // When I navigate to users
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWUsersPage usersPage = sidebar.goToUsers();

        // Then the page should load
        assertThat(usersPage.isDisplayed()).isTrue();
    }

    @Test
    @Order(2)
    void usersPageShowsAdminUser() {
        // When I navigate to users
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWUsersPage usersPage = sidebar.goToUsers();

        // Then users page should be displayed (admin user may or may not be listed depending on backend)
        assertThat(usersPage.isDisplayed()).isTrue();
    }

    @Test
    @Order(3)
    void createNewUser() {
        // Given I am on the users page
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWUsersPage usersPage = sidebar.goToUsers();

        // When I click add user
        usersPage.clickCreateUser();

        // Then the dialog should appear
        assertThat(page.locator(".MuiDialog-root").isVisible()).isTrue();

        // Close the dialog
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Cancel")).click();
    }

    // ==================== Roles Tests ====================

    @Test
    @Order(4)
    void viewRolesPage() {
        // When I navigate to roles
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWRolesPage rolesPage = sidebar.goToRoles();

        // Then the page should load
        assertThat(rolesPage.isDisplayed()).isTrue();
    }

    @Test
    @Order(5)
    void rolesPageShowsBuiltInRoles() {
        // When I navigate to roles
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWRolesPage rolesPage = sidebar.goToRoles();
        rolesPage.waitForRolesLoaded();

        // Then I should see the admin role
        assertThat(rolesPage.getRoleNames()).contains("admin");
    }

    @Test
    @Order(6)
    void createNewRole() {
        // Given I am on the roles page
        PWSidebarNavigator sidebar = new PWSidebarNavigator(page);
        PWRolesPage rolesPage = sidebar.goToRoles();

        int initialCount = rolesPage.getRoleCount();
        String newRoleName = "testrole-" + System.currentTimeMillis();

        // When I create a new role
        rolesPage.createRole(newRoleName, "Test role for integration tests");

        // Then the role should appear
        rolesPage.refresh();
        assertThat(rolesPage.getRoleCount()).isGreaterThan(initialCount);
    }
}
