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


package ca.samanthaireland.lightning.engine.quarkus.api.auth;

import ca.samanthaireland.auth.AuthToken;
import ca.samanthaireland.auth.TokenIssuer;
import ca.samanthaireland.auth.User;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Quarkus-specific JWT issuer that uses smallrye-jwt for RSA signing.
 *
 * <p>This service generates JWTs that are compatible with Quarkus
 * security filters (@RolesAllowed) by using the same RSA keys and
 * issuer configured in application.properties.
 */
@ApplicationScoped
public class QuarkusTokenIssuer implements TokenIssuer {
    private static final Logger log = LoggerFactory.getLogger(QuarkusTokenIssuer.class);

    private static final int DEFAULT_TOKEN_EXPIRY_HOURS = 24;

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    /**
     * Issue a JWT token for a user using RSA signing.
     *
     * @param user the user to issue a token for
     * @return the authentication token
     */
    @Override
    public AuthToken issueToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofHours(DEFAULT_TOKEN_EXPIRY_HOURS));

        List<String> rolesList = new ArrayList<>(user.roles());

        // Use smallrye-jwt to build the token with RSA signing
        // This automatically uses the private key from smallrye.jwt.sign.key.location
        String jwt = Jwt.issuer(issuer)
                .upn(user.username())
                .groups(user.roles())
                .claim(AuthToken.CLAIM_USER_ID, user.id())
                .claim(AuthToken.CLAIM_USERNAME, user.username())
                .claim(AuthToken.CLAIM_ROLES, rolesList)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .sign();

        log.debug("Issued RSA-signed JWT for user '{}' with roles {}", user.username(), user.roles());

        return new AuthToken(user.id(), user.username(), user.roles(), expiresAt, jwt);
    }
}
