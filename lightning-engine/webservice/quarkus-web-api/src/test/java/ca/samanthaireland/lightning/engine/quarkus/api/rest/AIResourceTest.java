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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * Unit tests for {@link AIResource}.
 */
@QuarkusTest
@DisplayName("AIResource")
class AIResourceTest {

    @Nested
    @DisplayName("GET /api/ai")
    class GetAllAI {

        @Test
        @DisplayName("should return list of AI")
        @TestSecurity(user = "admin", roles = "admin")
        void shouldReturnListOfAI() {
            given()
                    .when().get("/api/ai")
                    .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() {
            given()
                    .when().get("/api/ai")
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("GET /api/ai/{aiName}")
    class GetAI {

        @Test
        @DisplayName("should return 404 for non-existent AI")
        @TestSecurity(user = "admin", roles = "admin")
        void shouldReturn404ForNonExistentAI() {
            given()
                    .when().get("/api/ai/NonExistentAI")
                    .then()
                    .statusCode(404);
        }
    }

    @Nested
    @DisplayName("DELETE /api/ai/{aiName}")
    class DeleteAI {

        @Test
        @DisplayName("should return 404 for non-existent AI")
        @TestSecurity(user = "admin", roles = "admin")
        void shouldReturn404ForNonExistentAI() {
            given()
                    .when().delete("/api/ai/NonExistentAI")
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() {
            given()
                    .when().delete("/api/ai/SomeAI")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("should return 403 for non-admin user")
        @TestSecurity(user = "viewer", roles = "view_only")
        void shouldReturn403ForNonAdminUser() {
            given()
                    .when().delete("/api/ai/SomeAI")
                    .then()
                    .statusCode(403);
        }
    }

    @Nested
    @DisplayName("POST /api/ai/reload")
    class ReloadAI {

        @Test
        @DisplayName("should return list of AI after reload")
        @TestSecurity(user = "admin", roles = "admin")
        void shouldReturnAIAfterReload() {
            given()
                    .when().post("/api/ai/reload")
                    .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticatedRequest() {
            given()
                    .when().post("/api/ai/reload")
                    .then()
                    .statusCode(401);
        }
    }
}
