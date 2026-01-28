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


package ca.samanthaireland.lightning.engine.auth.match;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MatchAuthService")
class MatchAuthServiceTest {

    private MatchAuthService authService;

    @BeforeEach
    void setUp() {
        authService = new MatchAuthService();
    }

    @Nested
    @DisplayName("issueToken")
    class IssueToken {

        @Test
        @DisplayName("should issue token with correct player ID")
        void shouldIssueTokenWithCorrectPlayerId() {
            MatchAuthToken token = authService.issueToken(42L, 1L, "PlayerOne");

            assertThat(token.playerId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("should issue token with correct match ID")
        void shouldIssueTokenWithCorrectMatchId() {
            MatchAuthToken token = authService.issueToken(1L, 100L, "Player");

            assertThat(token.matchId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should issue token with correct player name")
        void shouldIssueTokenWithCorrectPlayerName() {
            MatchAuthToken token = authService.issueToken(1L, 1L, "TestPlayer");

            assertThat(token.playerName()).isEqualTo("TestPlayer");
        }

        @Test
        @DisplayName("should set expiration in the future")
        void shouldSetExpirationInFuture() {
            MatchAuthToken token = authService.issueToken(1L, 1L, "Player");

            assertThat(token.expiresAt()).isAfter(Instant.now());
            assertThat(token.isExpired()).isFalse();
        }

        @Test
        @DisplayName("should generate valid JWT string")
        void shouldGenerateValidJwtString() {
            MatchAuthToken token = authService.issueToken(1L, 1L, "Player");

            assertThat(token.jwtToken()).isNotBlank();
            assertThat(token.jwtToken().split("\\.")).hasSize(3);
        }
    }

    @Nested
    @DisplayName("verifyToken")
    class VerifyToken {

        @Test
        @DisplayName("should verify and return token with correct claims")
        void shouldVerifyAndReturnTokenWithCorrectClaims() {
            MatchAuthToken original = authService.issueToken(42L, 100L, "TestPlayer");

            MatchAuthToken verified = authService.verifyToken(original.jwtToken());

            assertThat(verified.playerId()).isEqualTo(42L);
            assertThat(verified.matchId()).isEqualTo(100L);
            assertThat(verified.playerName()).isEqualTo("TestPlayer");
        }

        @Test
        @DisplayName("should throw for invalid token")
        void shouldThrowForInvalidToken() {
            assertThatThrownBy(() -> authService.verifyToken("invalid.token"))
                    .isInstanceOf(MatchAuthException.class);
        }

        @Test
        @DisplayName("should throw for tampered token")
        void shouldThrowForTamperedToken() {
            MatchAuthToken token = authService.issueToken(1L, 1L, "Player");
            String tampered = token.jwtToken() + "tampered";

            assertThatThrownBy(() -> authService.verifyToken(tampered))
                    .isInstanceOf(MatchAuthException.class);
        }

        @Test
        @DisplayName("should throw for token from different service")
        void shouldThrowForTokenFromDifferentService() {
            MatchAuthService otherService = new MatchAuthService();
            MatchAuthToken otherToken = otherService.issueToken(1L, 1L, "Player");

            assertThatThrownBy(() -> authService.verifyToken(otherToken.jwtToken()))
                    .isInstanceOf(MatchAuthException.class);
        }
    }

    @Nested
    @DisplayName("verifyTokenForMatch")
    class VerifyTokenForMatch {

        @Test
        @DisplayName("should verify token for correct match")
        void shouldVerifyTokenForCorrectMatch() {
            MatchAuthToken original = authService.issueToken(1L, 100L, "Player");

            MatchAuthToken verified = authService.verifyTokenForMatch(original.jwtToken(), 100L);

            assertThat(verified.matchId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should throw for wrong match")
        void shouldThrowForWrongMatch() {
            MatchAuthToken token = authService.issueToken(1L, 100L, "Player");

            assertThatThrownBy(() -> authService.verifyTokenForMatch(token.jwtToken(), 200L))
                    .isInstanceOf(MatchAuthException.class)
                    .hasMessageContaining("match 100")
                    .hasMessageContaining("not match 200");
        }
    }

    @Nested
    @DisplayName("verifyTokenForPlayer")
    class VerifyTokenForPlayer {

        @Test
        @DisplayName("should verify token for correct player and match")
        void shouldVerifyTokenForCorrectPlayerAndMatch() {
            MatchAuthToken original = authService.issueToken(42L, 100L, "Player");

            MatchAuthToken verified = authService.verifyTokenForPlayer(original.jwtToken(), 42L, 100L);

            assertThat(verified.playerId()).isEqualTo(42L);
            assertThat(verified.matchId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should throw for wrong player")
        void shouldThrowForWrongPlayer() {
            MatchAuthToken token = authService.issueToken(42L, 100L, "Player");

            assertThatThrownBy(() -> authService.verifyTokenForPlayer(token.jwtToken(), 99L, 100L))
                    .isInstanceOf(MatchAuthException.class)
                    .hasMessageContaining("player 42")
                    .hasMessageContaining("not player 99");
        }

        @Test
        @DisplayName("should throw for wrong match")
        void shouldThrowForWrongMatch() {
            MatchAuthToken token = authService.issueToken(42L, 100L, "Player");

            assertThatThrownBy(() -> authService.verifyTokenForPlayer(token.jwtToken(), 42L, 200L))
                    .isInstanceOf(MatchAuthException.class)
                    .hasMessageContaining("match");
        }
    }

    @Nested
    @DisplayName("MatchAuthToken methods")
    class TokenMethods {

        @Test
        @DisplayName("isValidForMatch should return true for correct match and not expired")
        void isValidForMatchShouldReturnTrueForCorrectMatch() {
            MatchAuthToken token = authService.issueToken(1L, 100L, "Player");

            assertThat(token.isValidForMatch(100L)).isTrue();
            assertThat(token.isValidForMatch(200L)).isFalse();
        }

        @Test
        @DisplayName("isForPlayer should check player ID")
        void isForPlayerShouldCheckPlayerId() {
            MatchAuthToken token = authService.issueToken(42L, 1L, "Player");

            assertThat(token.isForPlayer(42L)).isTrue();
            assertThat(token.isForPlayer(99L)).isFalse();
        }

        @Test
        @DisplayName("isExpired should return false for fresh token")
        void isExpiredShouldReturnFalseForFreshToken() {
            MatchAuthToken token = authService.issueToken(1L, 1L, "Player");

            assertThat(token.isExpired()).isFalse();
        }
    }
}
