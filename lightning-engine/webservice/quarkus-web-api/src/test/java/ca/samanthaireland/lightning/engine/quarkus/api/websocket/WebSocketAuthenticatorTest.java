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

package ca.samanthaireland.lightning.engine.quarkus.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.quarkus.websockets.next.HandshakeRequest;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;

/**
 * Unit tests for {@link WebSocketAuthenticator}.
 */
class WebSocketAuthenticatorTest {

    private WebSocketAuthenticator authenticator;
    private JWTParser mockJwtParser;

    @BeforeEach
    void setUp() throws Exception {
        authenticator = new WebSocketAuthenticator();
        mockJwtParser = mock(JWTParser.class);
        setField(authenticator, "jwtParser", mockJwtParser);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private WebSocketConnection mockConnection(String subprotocol, String query) {
        WebSocketConnection connection = mock(WebSocketConnection.class);
        HandshakeRequest request = mock(HandshakeRequest.class);
        when(connection.handshakeRequest()).thenReturn(request);
        when(request.header("Sec-WebSocket-Protocol")).thenReturn(subprotocol);
        when(request.query()).thenReturn(query);
        return connection;
    }

    private JsonWebToken mockJwt(String subject, Set<String> groups) {
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(jwt.getSubject()).thenReturn(subject);
        when(jwt.getGroups()).thenReturn(groups);
        return jwt;
    }

    @Nested
    @DisplayName("Token extraction")
    class TokenExtraction {

        @Test
        void shouldExtractTokenFromSubprotocol() throws Exception {
            WebSocketConnection conn = mockConnection("Bearer.my-jwt-token", null);
            JsonWebToken jwt = mockJwt("user1", Set.of("admin"));
            when(mockJwtParser.parse("my-jwt-token")).thenReturn(jwt);

            var result = authenticator.authenticate(conn);

            assertThat(result).isInstanceOf(WebSocketAuthenticator.AuthResult.Success.class);
        }

        @Test
        void shouldExtractTokenFromQueryParam() throws Exception {
            WebSocketConnection conn = mockConnection(null, "token=my-jwt-token");
            JsonWebToken jwt = mockJwt("user1", Set.of("admin"));
            when(mockJwtParser.parse("my-jwt-token")).thenReturn(jwt);

            var result = authenticator.authenticate(conn);

            assertThat(result).isInstanceOf(WebSocketAuthenticator.AuthResult.Success.class);
        }

        @Test
        void shouldPreferSubprotocolOverQueryParam() throws Exception {
            WebSocketConnection conn = mockConnection("Bearer.subprotocol-token", "token=query-token");
            JsonWebToken jwt = mockJwt("user1", Set.of("admin"));
            when(mockJwtParser.parse("subprotocol-token")).thenReturn(jwt);

            var result = authenticator.authenticate(conn);

            assertThat(result).isInstanceOf(WebSocketAuthenticator.AuthResult.Success.class);
        }

        @Test
        void shouldExtractTokenFromQueryWithMultipleParams() throws Exception {
            WebSocketConnection conn = mockConnection(null, "foo=bar&token=my-jwt-token&baz=qux");
            JsonWebToken jwt = mockJwt("user1", Set.of("admin"));
            when(mockJwtParser.parse("my-jwt-token")).thenReturn(jwt);

            var result = authenticator.authenticate(conn);

            assertThat(result).isInstanceOf(WebSocketAuthenticator.AuthResult.Success.class);
        }
    }

    @Nested
    @DisplayName("Authentication failures")
    class AuthenticationFailures {

        @Test
        void shouldFailWhenNoToken() {
            WebSocketConnection conn = mockConnection(null, null);

            var result = authenticator.authenticate(conn);

            assertThat(result).isInstanceOf(WebSocketAuthenticator.AuthResult.Failure.class);
            var failure = (WebSocketAuthenticator.AuthResult.Failure) result;
            assertThat(failure.message()).contains("Authentication required");
        }

        @Test
        void shouldFailWhenEmptyQuery() {
            WebSocketConnection conn = mockConnection(null, "");

            var result = authenticator.authenticate(conn);

            assertThat(result).isInstanceOf(WebSocketAuthenticator.AuthResult.Failure.class);
        }

        @Test
        void shouldFailWhenQueryHasNoToken() {
            WebSocketConnection conn = mockConnection(null, "foo=bar&baz=qux");

            var result = authenticator.authenticate(conn);

            assertThat(result).isInstanceOf(WebSocketAuthenticator.AuthResult.Failure.class);
        }

        @Test
        void shouldFailWhenTokenIsInvalid() throws Exception {
            WebSocketConnection conn = mockConnection(null, "token=invalid-token");
            when(mockJwtParser.parse("invalid-token")).thenThrow(new ParseException("Invalid token"));

            var result = authenticator.authenticate(conn);

            assertThat(result).isInstanceOf(WebSocketAuthenticator.AuthResult.Failure.class);
            var failure = (WebSocketAuthenticator.AuthResult.Failure) result;
            assertThat(failure.message()).contains("invalid token");
        }

        @Test
        void shouldFailWhenSubprotocolPrefixOnly() {
            WebSocketConnection conn = mockConnection("Bearer.", null);

            var result = authenticator.authenticate(conn);

            assertThat(result).isInstanceOf(WebSocketAuthenticator.AuthResult.Failure.class);
        }
    }

    @Nested
    @DisplayName("Role-based authorization")
    class RoleBasedAuthorization {

        @Test
        void shouldSucceedWithAdminRole() throws Exception {
            WebSocketConnection conn = mockConnection(null, "token=jwt");
            JsonWebToken jwt = mockJwt("admin-user", Set.of("admin"));
            when(mockJwtParser.parse("jwt")).thenReturn(jwt);

            var result = authenticator.authenticate(conn);

            assertThat(result).isInstanceOf(WebSocketAuthenticator.AuthResult.Success.class);
            var success = (WebSocketAuthenticator.AuthResult.Success) result;
            assertThat(success.subject()).isEqualTo("admin-user");
            assertThat(success.roles()).contains("admin");
        }

        @Test
        void shouldSucceedWithCommandManagerRole() throws Exception {
            WebSocketConnection conn = mockConnection(null, "token=jwt");
            JsonWebToken jwt = mockJwt("cmd-manager", Set.of("command_manager"));
            when(mockJwtParser.parse("jwt")).thenReturn(jwt);

            var result = authenticator.authenticate(conn);

            assertThat(result).isInstanceOf(WebSocketAuthenticator.AuthResult.Success.class);
        }

        @Test
        void shouldFailWithInsufficientRole() throws Exception {
            WebSocketConnection conn = mockConnection(null, "token=jwt");
            JsonWebToken jwt = mockJwt("viewer", Set.of("view_only"));
            when(mockJwtParser.parse("jwt")).thenReturn(jwt);

            var result = authenticator.authenticate(conn);

            assertThat(result).isInstanceOf(WebSocketAuthenticator.AuthResult.Failure.class);
            var failure = (WebSocketAuthenticator.AuthResult.Failure) result;
            assertThat(failure.message()).contains("Authorization failed");
        }

        @Test
        void shouldSucceedWithViewRoleWhenAllowed() throws Exception {
            WebSocketConnection conn = mockConnection(null, "token=jwt");
            JsonWebToken jwt = mockJwt("viewer", Set.of("view_only"));
            when(mockJwtParser.parse("jwt")).thenReturn(jwt);

            var result = authenticator.authenticate(conn, WebSocketAuthenticator.VIEW_ROLES);

            assertThat(result).isInstanceOf(WebSocketAuthenticator.AuthResult.Success.class);
        }

        @Test
        void shouldSucceedWithMultipleRoles() throws Exception {
            WebSocketConnection conn = mockConnection(null, "token=jwt");
            JsonWebToken jwt = mockJwt("power-user", Set.of("user", "admin", "other"));
            when(mockJwtParser.parse("jwt")).thenReturn(jwt);

            var result = authenticator.authenticate(conn);

            assertThat(result).isInstanceOf(WebSocketAuthenticator.AuthResult.Success.class);
        }
    }

    @Nested
    @DisplayName("Role constants")
    class RoleConstants {

        @Test
        void commandRolesShouldContainAdminAndCommandManager() {
            assertThat(WebSocketAuthenticator.COMMAND_ROLES)
                    .containsExactlyInAnyOrder("admin", "command_manager");
        }

        @Test
        void viewRolesShouldContainAllReadRoles() {
            assertThat(WebSocketAuthenticator.VIEW_ROLES)
                    .containsExactlyInAnyOrder("admin", "command_manager", "view_only");
        }

        @Test
        void shouldExposeCommandRolesViaGetter() {
            assertThat(authenticator.getCommandRoles()).isEqualTo(WebSocketAuthenticator.COMMAND_ROLES);
        }

        @Test
        void shouldExposeViewRolesViaGetter() {
            assertThat(authenticator.getViewRoles()).isEqualTo(WebSocketAuthenticator.VIEW_ROLES);
        }
    }
}
