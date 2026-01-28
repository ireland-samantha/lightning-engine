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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Unit tests for {@link AuthResource}.
 */
@QuarkusTest
@DisplayName("AuthResource")
class AuthResourceTest {

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("should return token on valid credentials")
        void shouldReturnTokenOnValidCredentials() {
            // Uses deterministic test password configured in test/resources/application.properties
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"admin\",\"password\":\"testadminpassword123\"}")
                    .when().post("/api/auth/login")
                    .then()
                    .statusCode(200)
                    .body("token", notNullValue())
                    .body("username", equalTo("admin"));
        }

        @Test
        @DisplayName("should return 401 on invalid credentials")
        void shouldReturn401OnInvalidCredentials() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"admin\",\"password\":\"wrongpassword\"}")
                    .when().post("/api/auth/login")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("should return 401 for non-existent user")
        void shouldReturn401ForNonExistentUser() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"nonexistent\",\"password\":\"password\"}")
                    .when().post("/api/auth/login")
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("should return 401 for missing Authorization header")
        @TestSecurity(user = "admin", roles = "admin")
        void shouldReturn401ForMissingAuthHeader() {
            given()
                    .contentType(ContentType.JSON)
                    .when().post("/api/auth/refresh")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("should return 401 for invalid token format")
        @TestSecurity(user = "admin", roles = "admin")
        void shouldReturn401ForInvalidTokenFormat() {
            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "InvalidFormat")
                    .when().post("/api/auth/refresh")
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class GetCurrentUser {

        @Test
        @DisplayName("should return user info for authenticated user")
        @TestSecurity(user = "admin", roles = "admin")
        void shouldReturnUserInfoForAuthenticatedUser() {
            given()
                    .when().get("/api/auth/me")
                    .then()
                    .statusCode(200)
                    .body("username", equalTo("admin"));
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() {
            given()
                    .when().get("/api/auth/me")
                    .then()
                    .statusCode(401);
        }
    }
}
