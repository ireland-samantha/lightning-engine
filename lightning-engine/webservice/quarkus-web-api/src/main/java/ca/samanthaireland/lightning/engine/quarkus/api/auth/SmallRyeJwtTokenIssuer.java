/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.quarkus.api.auth;

import ca.samanthaireland.auth.AuthToken;
import ca.samanthaireland.auth.TokenIssuer;
import ca.samanthaireland.auth.User;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;

/**
 * TokenIssuer implementation that uses SmallRye JWT for RSA-based token signing.
 * This ensures tokens are signed with the same algorithm and keys that SmallRye JWT
 * uses for verification.
 */
@Slf4j
public class SmallRyeJwtTokenIssuer implements TokenIssuer {

    private static final String ISSUER = "https://lightningfirefly.com";
    private final int tokenExpiryHours;

    public SmallRyeJwtTokenIssuer(int tokenExpiryHours) {
        this.tokenExpiryHours = tokenExpiryHours;
    }

    @Override
    public AuthToken issueToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(tokenExpiryHours, ChronoUnit.HOURS);

        JwtClaimsBuilder builder = Jwt.issuer(ISSUER)
                .subject(user.username())
                .claim("user_id", user.id())
                .claim("username", user.username())
                .groups(new HashSet<>(user.roles()))  // This sets the 'groups' claim
                .issuedAt(now)
                .expiresAt(expiresAt);

        // Sign with the configured private key (from smallrye.jwt.sign.key.location)
        String jwt = builder.sign();

        log.debug("Issued JWT token for user '{}' with roles {}", user.username(), user.roles());
        return new AuthToken(user.id(), user.username(), user.roles(), expiresAt, jwt);
    }
}
