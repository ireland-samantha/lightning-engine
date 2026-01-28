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
 * Unit tests for {@link UserResource}.
 */
@QuarkusTest
@DisplayName("UserResource")
class UserResourceTest {

    @Nested
    @DisplayName("GET /api/auth/users")
    class GetAllUsers {

        @Test
        @DisplayName("should return list of users")
        @TestSecurity(user = "admin", roles = "admin")
        void shouldReturnListOfUsers() {
            given()
                    .when().get("/api/auth/users")
                    .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() {
            given()
                    .when().get("/api/auth/users")
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("GET /api/auth/users/{userId}")
    class GetUser {

        @Test
        @DisplayName("should return 404 for non-existent user")
        @TestSecurity(user = "admin", roles = "admin")
        void shouldReturn404ForNonExistentUser() {
            given()
                    .when().get("/api/auth/users/99999")
                    .then()
                    .statusCode(404);
        }
    }

    @Nested
    @DisplayName("GET /api/auth/users/username/{username}")
    class GetUserByUsername {

        @Test
        @DisplayName("should return 404 for non-existent username")
        @TestSecurity(user = "admin", roles = "admin")
        void shouldReturn404ForNonExistentUsername() {
            given()
                    .when().get("/api/auth/users/username/nonexistentuser")
                    .then()
                    .statusCode(404);
        }
    }

    @Nested
    @DisplayName("POST /api/auth/users")
    class CreateUser {

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"testuser\",\"password\":\"password\",\"roles\":[]}")
                    .when().post("/api/auth/users")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("should return 403 for non-admin user")
        @TestSecurity(user = "viewer", roles = "view_only")
        void shouldReturn403ForNonAdminUser() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"testuser\",\"password\":\"password\",\"roles\":[]}")
                    .when().post("/api/auth/users")
                    .then()
                    .statusCode(403);
        }
    }

    @Nested
    @DisplayName("DELETE /api/auth/users/{userId}")
    class DeleteUser {

        @Test
        @DisplayName("should return 404 for non-existent user")
        @TestSecurity(user = "admin", roles = "admin")
        void shouldReturn404ForNonExistentUser() {
            given()
                    .when().delete("/api/auth/users/99999")
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() {
            given()
                    .when().delete("/api/auth/users/1")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("should return 403 for non-admin user")
        @TestSecurity(user = "viewer", roles = "view_only")
        void shouldReturn403ForNonAdminUser() {
            given()
                    .when().delete("/api/auth/users/1")
                    .then()
                    .statusCode(403);
        }
    }

    @Nested
    @DisplayName("PUT /api/auth/users/{userId}/enabled")
    class SetEnabled {

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() {
            given()
                    .contentType(ContentType.JSON)
                    .body("true")
                    .when().put("/api/auth/users/1/enabled")
                    .then()
                    .statusCode(401);
        }
    }
}
