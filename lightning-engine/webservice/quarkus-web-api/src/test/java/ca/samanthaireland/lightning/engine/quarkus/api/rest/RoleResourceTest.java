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

package ca.samanthaireland.lightning.engine.quarkus.api.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * Unit tests for {@link RoleResource}.
 */
@QuarkusTest
@DisplayName("RoleResource")
class RoleResourceTest {

    @Nested
    @DisplayName("GET /api/auth/roles")
    class GetAllRoles {

        @Test
        @DisplayName("should return list of roles")
        @TestSecurity(user = "admin", roles = "admin")
        void shouldReturnListOfRoles() {
            given()
                    .when().get("/api/auth/roles")
                    .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() {
            given()
                    .when().get("/api/auth/roles")
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("GET /api/auth/roles/{roleId}")
    class GetRole {

        @Test
        @DisplayName("should return 404 for non-existent role")
        @TestSecurity(user = "admin", roles = "admin")
        void shouldReturn404ForNonExistentRole() {
            given()
                    .when().get("/api/auth/roles/99999")
                    .then()
                    .statusCode(404);
        }
    }

    @Nested
    @DisplayName("GET /api/auth/roles/name/{roleName}")
    class GetRoleByName {

        @Test
        @DisplayName("should return 404 for non-existent role name")
        @TestSecurity(user = "admin", roles = "admin")
        void shouldReturn404ForNonExistentRoleName() {
            given()
                    .when().get("/api/auth/roles/name/nonexistentrole")
                    .then()
                    .statusCode(404);
        }
    }

    @Nested
    @DisplayName("POST /api/auth/roles")
    class CreateRole {

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"testrole\",\"description\":\"Test role\"}")
                    .when().post("/api/auth/roles")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("should return 403 for non-admin user")
        @TestSecurity(user = "viewer", roles = "view_only")
        void shouldReturn403ForNonAdminUser() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"testrole\",\"description\":\"Test role\"}")
                    .when().post("/api/auth/roles")
                    .then()
                    .statusCode(403);
        }
    }

    @Nested
    @DisplayName("DELETE /api/auth/roles/{roleId}")
    class DeleteRole {

        @Test
        @DisplayName("should return 404 for non-existent role")
        @TestSecurity(user = "admin", roles = "admin")
        void shouldReturn404ForNonExistentRole() {
            given()
                    .when().delete("/api/auth/roles/99999")
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() {
            given()
                    .when().delete("/api/auth/roles/1")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("should return 403 for non-admin user")
        @TestSecurity(user = "viewer", roles = "view_only")
        void shouldReturn403ForNonAdminUser() {
            given()
                    .when().delete("/api/auth/roles/1")
                    .then()
                    .statusCode(403);
        }
    }
}
