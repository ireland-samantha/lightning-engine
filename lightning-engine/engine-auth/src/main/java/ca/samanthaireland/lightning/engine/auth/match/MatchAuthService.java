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

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Service for issuing and verifying match authentication tokens.
 *
 * <p>Match tokens are issued when a player joins a match and provide
 * authentication for match-specific operations.
 *
 * <p>Thread-safe.
 */
@Slf4j
public class MatchAuthService {

    private static final String ISSUER = "lightningfirefly-match";
    private static final int DEFAULT_TOKEN_EXPIRY_HOURS = 8;

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final int tokenExpiryHours;

    /**
     * Create a MatchAuthService with a random secret key.
     */
    public MatchAuthService() {
        this(generateSecretKey(), DEFAULT_TOKEN_EXPIRY_HOURS);
    }

    /**
     * Create a MatchAuthService with a custom secret key.
     *
     * @param secretKey the secret key for signing JWTs
     * @param tokenExpiryHours hours until tokens expire
     */
    public MatchAuthService(String secretKey, int tokenExpiryHours) {
        this.algorithm = Algorithm.HMAC256(secretKey);
        this.verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
        this.tokenExpiryHours = tokenExpiryHours;
        log.info("MatchAuthService initialized with {}h token expiry", tokenExpiryHours);
    }

    /**
     * Issue a token for a player joining a match.
     *
     * @param playerId the player's unique ID
     * @param matchId the match the player is joining
     * @param playerName the player's display name
     * @return the match authentication token
     */
    public MatchAuthToken issueToken(long playerId, long matchId, String playerName) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(tokenExpiryHours, ChronoUnit.HOURS);

        String jwt = JWT.create()
                .withIssuer(ISSUER)
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .withClaim(MatchAuthToken.CLAIM_PLAYER_ID, playerId)
                .withClaim(MatchAuthToken.CLAIM_MATCH_ID, matchId)
                .withClaim(MatchAuthToken.CLAIM_PLAYER_NAME, playerName)
                .sign(algorithm);

        log.info("Issued match token for player {} (ID: {}) in match {}",
                playerName, playerId, matchId);

        return new MatchAuthToken(playerId, matchId, playerName, expiresAt, jwt);
    }

    /**
     * Verify a match token and extract its claims.
     *
     * @param token the JWT token string
     * @return the verified match token
     * @throws MatchAuthException if the token is invalid or expired
     */
    public MatchAuthToken verifyToken(String token) {
        try {
            DecodedJWT decoded = verifier.verify(token);

            long playerId = decoded.getClaim(MatchAuthToken.CLAIM_PLAYER_ID).asLong();
            long matchId = decoded.getClaim(MatchAuthToken.CLAIM_MATCH_ID).asLong();
            String playerName = decoded.getClaim(MatchAuthToken.CLAIM_PLAYER_NAME).asString();
            Instant expiresAt = decoded.getExpiresAtAsInstant();

            return new MatchAuthToken(playerId, matchId, playerName, expiresAt, token);

        } catch (JWTVerificationException e) {
            log.warn("Match token verification failed: {}", e.getMessage());
            throw new MatchAuthException("Invalid match token: " + e.getMessage(), e);
        }
    }

    /**
     * Verify that a token is valid for a specific match.
     *
     * @param token the JWT token string
     * @param expectedMatchId the expected match ID
     * @return the verified match token
     * @throws MatchAuthException if the token is invalid or not for the expected match
     */
    public MatchAuthToken verifyTokenForMatch(String token, long expectedMatchId) {
        MatchAuthToken matchToken = verifyToken(token);

        if (matchToken.matchId() != expectedMatchId) {
            throw new MatchAuthException(
                    "Token is for match " + matchToken.matchId() +
                            ", not match " + expectedMatchId);
        }

        return matchToken;
    }

    /**
     * Verify that a token is valid for a specific player in a match.
     *
     * @param token the JWT token string
     * @param expectedPlayerId the expected player ID
     * @param expectedMatchId the expected match ID
     * @return the verified match token
     * @throws MatchAuthException if the token is invalid or doesn't match expectations
     */
    public MatchAuthToken verifyTokenForPlayer(
            String token, long expectedPlayerId, long expectedMatchId) {
        MatchAuthToken matchToken = verifyTokenForMatch(token, expectedMatchId);

        if (matchToken.playerId() != expectedPlayerId) {
            throw new MatchAuthException(
                    "Token is for player " + matchToken.playerId() +
                            ", not player " + expectedPlayerId);
        }

        return matchToken;
    }

    /**
     * Generate a cryptographically secure random secret key.
     *
     * @return a base64-encoded secret key
     */
    private static String generateSecretKey() {
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }
}
