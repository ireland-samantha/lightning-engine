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

package ca.samanthaireland.lightning.engine.auth.module;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static ca.samanthaireland.lightning.engine.auth.module.ModuleAuthToken.ComponentPermission.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ModuleAuthService")
class ModuleAuthServiceTest {

    private ModuleAuthService authService;

    @BeforeEach
    void setUp() {
        // Use a fixed secret for predictable tests
        authService = new ModuleAuthService("test-secret-key-for-testing");
    }

    @Nested
    @DisplayName("issueToken")
    class IssueToken {

        @Test
        @DisplayName("should issue token with correct module name")
        void shouldIssueTokenWithCorrectModuleName() {
            Map<String, ModuleAuthToken.ComponentPermission> permissions = Map.of(
                    "TestModule.COMP_A", OWNER
            );
            ModuleAuthToken token = authService.issueToken("TestModule", permissions, false);

            assertThat(token.moduleName()).isEqualTo("TestModule");
        }

        @Test
        @DisplayName("should include component permissions")
        void shouldIncludeComponentPermissions() {
            Map<String, ModuleAuthToken.ComponentPermission> permissions = Map.of(
                    "TestModule.COMP_A", OWNER,
                    "OtherModule.COMP_B", READ,
                    "AnotherModule.COMP_C", WRITE
            );

            ModuleAuthToken token = authService.issueToken("TestModule", permissions, false);

            assertThat(token.componentPermissions()).containsEntry("TestModule.COMP_A", OWNER);
            assertThat(token.componentPermissions()).containsEntry("OtherModule.COMP_B", READ);
            assertThat(token.componentPermissions()).containsEntry("AnotherModule.COMP_C", WRITE);
        }

        @Test
        @DisplayName("should set superuser flag correctly")
        void shouldSetSuperuserFlagCorrectly() {
            Map<String, ModuleAuthToken.ComponentPermission> permissions = Map.of("Test.COMP", OWNER);

            ModuleAuthToken regularToken = authService.issueToken("Regular", permissions, false);
            ModuleAuthToken superToken = authService.issueToken("Super", permissions, true);

            assertThat(regularToken.superuser()).isFalse();
            assertThat(superToken.superuser()).isTrue();
        }

        @Test
        @DisplayName("should generate valid JWT string")
        void shouldGenerateValidJwtString() {
            ModuleAuthToken token = authService.issueToken("TestModule", Map.of("Test.A", OWNER), false);

            assertThat(token.jwtToken()).isNotBlank();
            assertThat(token.jwtToken().split("\\.")).hasSize(3); // JWT has 3 parts
        }
    }

    @Nested
    @DisplayName("issueRegularToken")
    class IssueRegularToken {

        @Test
        @DisplayName("should issue non-superuser token")
        void shouldIssueNonSuperuserToken() {
            Map<String, ModuleAuthToken.ComponentPermission> permissions = Map.of(
                    "RegularModule.COMP_A", OWNER
            );
            ModuleAuthToken token = authService.issueRegularToken("RegularModule", permissions);

            assertThat(token.superuser()).isFalse();
            assertThat(token.moduleName()).isEqualTo("RegularModule");
        }
    }

    @Nested
    @DisplayName("issueSuperuserToken")
    class IssueSuperuserToken {

        @Test
        @DisplayName("should issue superuser token")
        void shouldIssueSuperuserToken() {
            Map<String, ModuleAuthToken.ComponentPermission> permissions = Map.of(
                    "EntityModule.ENTITY_TYPE", OWNER
            );
            ModuleAuthToken token = authService.issueSuperuserToken("EntityModule", permissions);

            assertThat(token.superuser()).isTrue();
            assertThat(token.moduleName()).isEqualTo("EntityModule");
        }
    }

    @Nested
    @DisplayName("verifyToken")
    class VerifyToken {

        @Test
        @DisplayName("should verify and return token with correct claims")
        void shouldVerifyAndReturnTokenWithCorrectClaims() {
            Map<String, ModuleAuthToken.ComponentPermission> permissions = Map.of(
                    "TestModule.COMP_A", OWNER,
                    "OtherModule.COMP_B", READ
            );
            ModuleAuthToken original = authService.issueToken("TestModule", permissions, true);

            ModuleAuthToken verified = authService.verifyToken(original.jwtToken());

            assertThat(verified.moduleName()).isEqualTo("TestModule");
            assertThat(verified.componentPermissions()).containsEntry("TestModule.COMP_A", OWNER);
            assertThat(verified.componentPermissions()).containsEntry("OtherModule.COMP_B", READ);
            assertThat(verified.superuser()).isTrue();
        }

        @Test
        @DisplayName("should throw for invalid token")
        void shouldThrowForInvalidToken() {
            assertThatThrownBy(() -> authService.verifyToken("invalid.token.here"))
                    .isInstanceOf(ModuleAuthException.class);
        }

        @Test
        @DisplayName("should throw for tampered token")
        void shouldThrowForTamperedToken() {
            ModuleAuthToken token = authService.issueToken("Test", Map.of("Test.A", OWNER), false);
            String tampered = token.jwtToken() + "x";

            assertThatThrownBy(() -> authService.verifyToken(tampered))
                    .isInstanceOf(ModuleAuthException.class);
        }

        @Test
        @DisplayName("should throw for token from different service")
        void shouldThrowForTokenFromDifferentService() {
            ModuleAuthService otherService = new ModuleAuthService("different-secret");
            ModuleAuthToken otherToken = otherService.issueToken("Test", Map.of("Test.A", OWNER), false);

            assertThatThrownBy(() -> authService.verifyToken(otherToken.jwtToken()))
                    .isInstanceOf(ModuleAuthException.class);
        }
    }

    @Nested
    @DisplayName("ModuleAuthToken permission methods")
    class TokenMethods {

        @Test
        @DisplayName("ownsComponent should return true for owned component")
        void ownsComponentShouldReturnTrueForOwnedComponent() {
            Map<String, ModuleAuthToken.ComponentPermission> permissions = Map.of(
                    "TestModule.POSITION_X", OWNER,
                    "TestModule.POSITION_Y", OWNER,
                    "OtherModule.VELOCITY", READ
            );
            ModuleAuthToken token = authService.issueToken("TestModule", permissions, false);

            assertThat(token.ownsComponent("TestModule", "POSITION_X")).isTrue();
            assertThat(token.ownsComponent("TestModule", "POSITION_Y")).isTrue();
            assertThat(token.ownsComponent("OtherModule", "VELOCITY")).isFalse();
        }

        @Test
        @DisplayName("canRead should return true for superuser")
        void canReadShouldReturnTrueForSuperuser() {
            ModuleAuthToken superToken = authService.issueToken("Super", Map.of(), true);

            // Superuser can read anything
            assertThat(superToken.canRead("AnyModule", "ANY_COMPONENT")).isTrue();
        }

        @Test
        @DisplayName("canRead should check permissions when not superuser")
        void canReadShouldCheckPermissionsWhenNotSuperuser() {
            Map<String, ModuleAuthToken.ComponentPermission> permissions = Map.of(
                    "ModuleA.COMP_A", OWNER,
                    "ModuleB.COMP_B", READ,
                    "ModuleC.COMP_C", WRITE
            );
            ModuleAuthToken token = authService.issueToken("Test", permissions, false);

            // OWNER, READ, and WRITE all allow reading
            assertThat(token.canRead("ModuleA", "COMP_A")).isTrue();
            assertThat(token.canRead("ModuleB", "COMP_B")).isTrue();
            assertThat(token.canRead("ModuleC", "COMP_C")).isTrue();
            // No permission = cannot read
            assertThat(token.canRead("ModuleD", "COMP_D")).isFalse();
        }

        @Test
        @DisplayName("canWrite should return true for superuser")
        void canWriteShouldReturnTrueForSuperuser() {
            ModuleAuthToken superToken = authService.issueToken("Super", Map.of(), true);

            // Superuser can write anything
            assertThat(superToken.canWrite("AnyModule", "ANY_COMPONENT")).isTrue();
        }

        @Test
        @DisplayName("canWrite should check permissions when not superuser")
        void canWriteShouldCheckPermissionsWhenNotSuperuser() {
            Map<String, ModuleAuthToken.ComponentPermission> permissions = Map.of(
                    "ModuleA.COMP_A", OWNER,
                    "ModuleB.COMP_B", READ,
                    "ModuleC.COMP_C", WRITE
            );
            ModuleAuthToken token = authService.issueToken("Test", permissions, false);

            // OWNER and WRITE allow writing, READ does not
            assertThat(token.canWrite("ModuleA", "COMP_A")).isTrue();
            assertThat(token.canWrite("ModuleB", "COMP_B")).isFalse();  // READ only
            assertThat(token.canWrite("ModuleC", "COMP_C")).isTrue();
            // No permission = cannot write
            assertThat(token.canWrite("ModuleD", "COMP_D")).isFalse();
        }
    }

    @Nested
    @DisplayName("Empty permissions")
    class EmptyPermissions {

        @Test
        @DisplayName("should handle empty permissions map")
        void shouldHandleEmptyPermissionsMap() {
            ModuleAuthToken token = authService.issueToken("EmptyModule", Map.of(), false);

            assertThat(token.componentPermissions()).isEmpty();
            assertThat(token.formatPermissions()).isEqualTo("(no permissions)");
        }

        @Test
        @DisplayName("verified token should preserve empty permissions")
        void verifiedTokenShouldPreserveEmptyPermissions() {
            ModuleAuthToken original = authService.issueToken("EmptyModule", Map.of(), false);
            ModuleAuthToken verified = authService.verifyToken(original.jwtToken());

            assertThat(verified.componentPermissions()).isEmpty();
        }
    }
}
