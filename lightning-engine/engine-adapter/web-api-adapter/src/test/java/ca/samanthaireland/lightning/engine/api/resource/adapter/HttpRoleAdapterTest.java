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

@DisplayName("HttpRoleAdapter")
class HttpRoleAdapterTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> stringResponse;

    @Mock
    private HttpResponse<Void> voidResponse;

    private RoleAdapter.HttpRoleAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new RoleAdapter.HttpRoleAdapter("http://localhost:8080", httpClient);
    }

    @Nested
    @DisplayName("createRole")
    class CreateRole {

        @Test
        @DisplayName("should create role successfully")
        void shouldCreateRoleSuccessfully() throws Exception {
            when(stringResponse.statusCode()).thenReturn(201);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"name\":\"admin\",\"description\":\"Administrator role\",\"includedRoles\":[\"user\",\"moderator\"]}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            RoleAdapter.RoleResponse result = adapter.createRole("admin", "Administrator role", Set.of("user", "moderator"));

            assertThat(result.id()).isEqualTo(1);
            assertThat(result.name()).isEqualTo("admin");
            assertThat(result.description()).isEqualTo("Administrator role");
            assertThat(result.includedRoles()).containsExactlyInAnyOrder("user", "moderator");
        }

        @Test
        @DisplayName("should create role without included roles")
        void shouldCreateRoleWithoutIncludedRoles() throws Exception {
            when(stringResponse.statusCode()).thenReturn(201);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"name\":\"viewer\",\"description\":\"View only\",\"includedRoles\":[]}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            RoleAdapter.RoleResponse result = adapter.createRole("viewer", "View only", null);

            assertThat(result.name()).isEqualTo("viewer");
            assertThat(result.includedRoles()).isEmpty();
        }

        @Test
        @DisplayName("should throw IOException on failure")
        void shouldThrowIOExceptionOnFailure() throws Exception {
            when(stringResponse.statusCode()).thenReturn(400);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            assertThatThrownBy(() -> adapter.createRole("role", "desc", null))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("status: 400");
        }

        @Test
        @DisplayName("should handle interruption")
        void shouldHandleInterruption() throws Exception {
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new InterruptedException("Interrupted"));

            assertThatThrownBy(() -> adapter.createRole("role", "desc", null))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("interrupted");
        }
    }

    @Nested
    @DisplayName("getAllRoles")
    class GetAllRoles {

        @Test
        @DisplayName("should return all roles")
        void shouldReturnAllRoles() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn(
                    "[{\"id\":1,\"name\":\"admin\",\"description\":\"Admin\",\"includedRoles\":[\"user\"]}," +
                    "{\"id\":2,\"name\":\"user\",\"description\":\"User\",\"includedRoles\":[]}]"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            List<RoleAdapter.RoleResponse> result = adapter.getAllRoles();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("admin");
            assertThat(result.get(1).name()).isEqualTo("user");
        }

        @Test
        @DisplayName("should return empty list when no roles")
        void shouldReturnEmptyListWhenNoRoles() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn("[]");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            List<RoleAdapter.RoleResponse> result = adapter.getAllRoles();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw IOException on failure")
        void shouldThrowIOExceptionOnFailure() throws Exception {
            when(stringResponse.statusCode()).thenReturn(500);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            assertThatThrownBy(() -> adapter.getAllRoles())
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("status: 500");
        }
    }

    @Nested
    @DisplayName("getRole")
    class GetRole {

        @Test
        @DisplayName("should return role when found")
        void shouldReturnRoleWhenFound() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"name\":\"admin\",\"description\":\"Administrator\",\"includedRoles\":[\"user\"]}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            Optional<RoleAdapter.RoleResponse> result = adapter.getRole(1L);

            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo("admin");
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() throws Exception {
            when(stringResponse.statusCode()).thenReturn(404);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            Optional<RoleAdapter.RoleResponse> result = adapter.getRole(999L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw IOException on server error")
        void shouldThrowIOExceptionOnServerError() throws Exception {
            when(stringResponse.statusCode()).thenReturn(500);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            assertThatThrownBy(() -> adapter.getRole(1L))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("status: 500");
        }
    }

    @Nested
    @DisplayName("getRoleByName")
    class GetRoleByName {

        @Test
        @DisplayName("should return role when found")
        void shouldReturnRoleWhenFound() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"name\":\"admin\",\"description\":\"Administrator\",\"includedRoles\":[]}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            Optional<RoleAdapter.RoleResponse> result = adapter.getRoleByName("admin");

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() throws Exception {
            when(stringResponse.statusCode()).thenReturn(404);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            Optional<RoleAdapter.RoleResponse> result = adapter.getRoleByName("nonexistent");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateDescription")
    class UpdateDescription {

        @Test
        @DisplayName("should update description successfully")
        void shouldUpdateDescriptionSuccessfully() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"name\":\"admin\",\"description\":\"Updated description\",\"includedRoles\":[]}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            RoleAdapter.RoleResponse result = adapter.updateDescription(1L, "Updated description");

            assertThat(result.description()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("should throw IOException on failure")
        void shouldThrowIOExceptionOnFailure() throws Exception {
            when(stringResponse.statusCode()).thenReturn(404);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            assertThatThrownBy(() -> adapter.updateDescription(999L, "desc"))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("status: 404");
        }
    }

    @Nested
    @DisplayName("updateIncludedRoles")
    class UpdateIncludedRoles {

        @Test
        @DisplayName("should update included roles successfully")
        void shouldUpdateIncludedRolesSuccessfully() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"name\":\"admin\",\"description\":\"Admin\",\"includedRoles\":[\"user\",\"moderator\"]}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            RoleAdapter.RoleResponse result = adapter.updateIncludedRoles(1L, Set.of("user", "moderator"));

            assertThat(result.includedRoles()).containsExactlyInAnyOrder("user", "moderator");
        }

        @Test
        @DisplayName("should throw IOException on failure")
        void shouldThrowIOExceptionOnFailure() throws Exception {
            when(stringResponse.statusCode()).thenReturn(400);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            assertThatThrownBy(() -> adapter.updateIncludedRoles(1L, Set.of("nonexistent")))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("status: 400");
        }
    }

    @Nested
    @DisplayName("deleteRole")
    class DeleteRole {

        @Test
        @DisplayName("should return true when role deleted")
        void shouldReturnTrueWhenRoleDeleted() throws Exception {
            when(voidResponse.statusCode()).thenReturn(204);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(voidResponse);

            boolean result = adapter.deleteRole(1L);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when role not found")
        void shouldReturnFalseWhenRoleNotFound() throws Exception {
            when(voidResponse.statusCode()).thenReturn(404);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(voidResponse);

            boolean result = adapter.deleteRole(999L);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should handle interruption")
        void shouldHandleInterruption() throws Exception {
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new InterruptedException("Interrupted"));

            assertThatThrownBy(() -> adapter.deleteRole(1L))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("interrupted");
        }
    }

    @Nested
    @DisplayName("hasRole")
    class HasRole {

        @Test
        @DisplayName("should return true when role exists")
        void shouldReturnTrueWhenRoleExists() throws Exception {
            when(stringResponse.statusCode()).thenReturn(200);
            when(stringResponse.body()).thenReturn(
                    "{\"id\":1,\"name\":\"admin\",\"description\":\"Admin\",\"includedRoles\":[]}"
            );
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            boolean result = adapter.hasRole("admin");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when role does not exist")
        void shouldReturnFalseWhenRoleDoesNotExist() throws Exception {
            when(stringResponse.statusCode()).thenReturn(404);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(stringResponse);

            boolean result = adapter.hasRole("nonexistent");

            assertThat(result).isFalse();
        }
    }
}
