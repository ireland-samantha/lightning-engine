/*
 * Copyright (c) 2026 Samantha Ireland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ca.samanthaireland.lightning.engine.api.resource.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("HttpUserAdapter")
class HttpUserAdapterTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> stringResponse;

    @Mock
    private HttpResponse<Void> voidResponse;

    private UserAdapter.HttpUserAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new UserAdapter.HttpUserAdapter("http://localhost:8080", httpClient);
    }

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("should create user successfully")
        void shouldCreateUserSuccessfully() throws Exception {
            when(stringResponse.statusCode()).thenReturn(201);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"username\":\"newuser\",\"roles\":[\"user\"],\"createdAt\":\"2026-01-01T00:00:00Z\",\"enabled\":true}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            UserAdapter.UserResponse result = adapter.createUser("newuser", "password123", Set.of("user"));

            assertThat(result.id()).isEqualTo(1);
            assertThat(result.username()).isEqualTo("newuser");
            assertThat(result.roles()).contains("user");
            assertThat(result.enabled()).isTrue();
        }

        @Test
        @DisplayName("should throw IOException on failure")
        void shouldThrowIOExceptionOnFailure() throws Exception {
            when(stringResponse.statusCode()).thenReturn(400);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            assertThatThrownBy(() -> adapter.createUser("user", "pass", null))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("status: 400");
        }

        @Test
        @DisplayName("should handle interruption")
        void shouldHandleInterruption() throws Exception {
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new InterruptedException("Interrupted"));

            assertThatThrownBy(() -> adapter.createUser("user", "pass", null))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("interrupted");
        }
    }

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {

        @Test
        @DisplayName("should return all users")
        void shouldReturnAllUsers() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            // Note: createdAt is omitted since JsonMapper doesn't have JSR310 module for Instant deserialization
            when(stringResponse.body()).thenReturn(
                    "[{\"id\":1,\"username\":\"user1\",\"roles\":[\"admin\"],\"enabled\":true}," +
                    "{\"id\":2,\"username\":\"user2\",\"roles\":[\"user\"],\"enabled\":false}]"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            List<UserAdapter.UserResponse> result = adapter.getAllUsers();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).username()).isEqualTo("user1");
            assertThat(result.get(1).username()).isEqualTo("user2");
        }

        @Test
        @DisplayName("should return empty list when no users")
        void shouldReturnEmptyListWhenNoUsers() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn("[]");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            List<UserAdapter.UserResponse> result = adapter.getAllUsers();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUser")
    class GetUser {

        @Test
        @DisplayName("should return user when found")
        void shouldReturnUserWhenFound() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"username\":\"admin\",\"roles\":[\"admin\"],\"createdAt\":\"2026-01-01T00:00:00Z\",\"enabled\":true}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            Optional<UserAdapter.UserResponse> result = adapter.getUser(1L);

            assertThat(result).isPresent();
            assertThat(result.get().username()).isEqualTo("admin");
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() throws Exception {
            when(stringResponse.statusCode()).thenReturn(404);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            Optional<UserAdapter.UserResponse> result = adapter.getUser(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUserByUsername")
    class GetUserByUsername {

        @Test
        @DisplayName("should return user when found")
        void shouldReturnUserWhenFound() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"username\":\"admin\",\"roles\":[\"admin\"],\"createdAt\":\"2026-01-01T00:00:00Z\",\"enabled\":true}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            Optional<UserAdapter.UserResponse> result = adapter.getUserByUsername("admin");

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() throws Exception {
            when(stringResponse.statusCode()).thenReturn(404);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            Optional<UserAdapter.UserResponse> result = adapter.getUserByUsername("nonexistent");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updatePassword")
    class UpdatePassword {

        @Test
        @DisplayName("should update password successfully")
        void shouldUpdatePasswordSuccessfully() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"username\":\"user\",\"roles\":[\"user\"],\"createdAt\":\"2026-01-01T00:00:00Z\",\"enabled\":true}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            UserAdapter.UserResponse result = adapter.updatePassword(1L, "newpassword");

            assertThat(result.id()).isEqualTo(1);
        }

        @Test
        @DisplayName("should throw IOException on failure")
        void shouldThrowIOExceptionOnFailure() throws Exception {
            when(stringResponse.statusCode()).thenReturn(404);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            assertThatThrownBy(() -> adapter.updatePassword(999L, "newpass"))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("status: 404");
        }
    }

    @Nested
    @DisplayName("updateRoles")
    class UpdateRoles {

        @Test
        @DisplayName("should update roles successfully")
        void shouldUpdateRolesSuccessfully() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"username\":\"user\",\"roles\":[\"admin\",\"user\"],\"createdAt\":\"2026-01-01T00:00:00Z\",\"enabled\":true}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            UserAdapter.UserResponse result = adapter.updateRoles(1L, Set.of("admin", "user"));

            assertThat(result.roles()).containsExactlyInAnyOrder("admin", "user");
        }
    }

    @Nested
    @DisplayName("addRole")
    class AddRole {

        @Test
        @DisplayName("should add role successfully")
        void shouldAddRoleSuccessfully() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"username\":\"user\",\"roles\":[\"user\",\"moderator\"],\"createdAt\":\"2026-01-01T00:00:00Z\",\"enabled\":true}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            UserAdapter.UserResponse result = adapter.addRole(1L, "moderator");

            assertThat(result.roles()).contains("moderator");
        }
    }

    @Nested
    @DisplayName("removeRole")
    class RemoveRole {

        @Test
        @DisplayName("should remove role successfully")
        void shouldRemoveRoleSuccessfully() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"username\":\"user\",\"roles\":[\"user\"],\"createdAt\":\"2026-01-01T00:00:00Z\",\"enabled\":true}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            UserAdapter.UserResponse result = adapter.removeRole(1L, "admin");

            assertThat(result.roles()).doesNotContain("admin");
        }
    }

    @Nested
    @DisplayName("setEnabled")
    class SetEnabled {

        @Test
        @DisplayName("should enable user")
        void shouldEnableUser() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"username\":\"user\",\"roles\":[\"user\"],\"createdAt\":\"2026-01-01T00:00:00Z\",\"enabled\":true}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            UserAdapter.UserResponse result = adapter.setEnabled(1L, true);

            assertThat(result.enabled()).isTrue();
        }

        @Test
        @DisplayName("should disable user")
        void shouldDisableUser() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"username\":\"user\",\"roles\":[\"user\"],\"createdAt\":\"2026-01-01T00:00:00Z\",\"enabled\":false}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            UserAdapter.UserResponse result = adapter.setEnabled(1L, false);

            assertThat(result.enabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("should return true when user deleted")
        void shouldReturnTrueWhenUserDeleted() throws Exception {
            when(voidResponse.statusCode()).thenReturn(204);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(voidResponse);

            boolean result = adapter.deleteUser(1L);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when user not found")
        void shouldReturnFalseWhenUserNotFound() throws Exception {
            when(voidResponse.statusCode()).thenReturn(404);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(voidResponse);

            boolean result = adapter.deleteUser(999L);

            assertThat(result).isFalse();
        }
    }
}
