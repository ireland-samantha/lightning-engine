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

import ca.samanthaireland.lightning.engine.auth.module.ModuleAuthToken.ComponentPermission;
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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for issuing and verifying JWT tokens for module authentication.
 *
 * <p>This service generates a random secret on initialization and uses HMAC256
 * for signing tokens. Tokens are issued during module initialization and verified
 * on each ECS operation.
 *
 * <p>Token claims use the format {@code moduleName.componentName} with values of
 * {@code owner}, {@code read}, or {@code write}:
 * <ul>
 *   <li>module_name - The module's name (subject)</li>
 *   <li>component_permissions - Map of permission keys to permission levels</li>
 *   <li>superuser - Boolean flag for elevated privileges (e.g., EntityModule)</li>
 * </ul>
 *
 * <p>Thread Safety: This class is thread-safe. The secret and verifier are immutable
 * after construction.
 */
@Slf4j
public class ModuleAuthService {

    private static final String ISSUER = "lightning-engine";
    private static final int SECRET_LENGTH_BYTES = 32;
    private static final long TOKEN_VALIDITY_DAYS = 365; // Long-lived for in-process use

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    /**
     * Create a new ModuleAuthService with a randomly generated secret.
     */
    public ModuleAuthService() {
        String secret = generateSecret();
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
        log.info("ModuleAuthService initialized with new secret");
    }

    /**
     * Create a ModuleAuthService with a specific secret (for testing).
     *
     * @param secret the secret to use for signing/verifying
     */
    public ModuleAuthService(String secret) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
        log.debug("ModuleAuthService initialized with provided secret");
    }

    /**
     * Issue a JWT token for a module with component permissions.
     *
     * @param moduleName the name of the module
     * @param componentPermissions map of "moduleName.componentName" to permission level
     * @param superuser whether this module has superuser privileges
     * @return the module auth token containing the JWT
     */
    public ModuleAuthToken issueToken(
            String moduleName,
            Map<String, ComponentPermission> componentPermissions,
            boolean superuser) {

        Instant now = Instant.now();
        Instant expiresAt = now.plus(TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS);

        // Convert permissions to strings for JWT claim
        Map<String, String> permissionStrings = componentPermissions.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().name().toLowerCase()
                ));

        String token = JWT.create()
                .withIssuer(ISSUER)
                .withSubject(moduleName)
                .withClaim(ModuleAuthToken.CLAIM_MODULE_NAME, moduleName)
                .withClaim(ModuleAuthToken.CLAIM_COMPONENT_PERMISSIONS, permissionStrings)
                .withClaim(ModuleAuthToken.CLAIM_SUPERUSER, superuser)
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .sign(algorithm);

        return new ModuleAuthToken(moduleName, componentPermissions, superuser, token);
    }

    /**
     * Verify a JWT token and extract the module auth claims.
     *
     * @param token the JWT token string
     * @return the verified module auth token
     * @throws ModuleAuthException if the token is invalid or expired
     */
    public ModuleAuthToken verifyToken(String token) {
        try {
            DecodedJWT jwt = verifier.verify(token);

            String moduleName = jwt.getClaim(ModuleAuthToken.CLAIM_MODULE_NAME).asString();
            Map<String, Object> permissionStrings = jwt.getClaim(ModuleAuthToken.CLAIM_COMPONENT_PERMISSIONS).asMap();
            Boolean superuser = jwt.getClaim(ModuleAuthToken.CLAIM_SUPERUSER).asBoolean();

            Map<String, ComponentPermission> componentPermissions = new HashMap<>();
            if (permissionStrings != null) {
                for (Map.Entry<String, Object> entry : permissionStrings.entrySet()) {
                    String permissionStr = entry.getValue().toString().toUpperCase();
                    componentPermissions.put(entry.getKey(), ComponentPermission.valueOf(permissionStr));
                }
            }

            return new ModuleAuthToken(
                    moduleName,
                    Map.copyOf(componentPermissions),
                    superuser != null && superuser,
                    token
            );
        } catch (JWTVerificationException e) {
            throw new ModuleAuthException("Invalid or expired module token: " + e.getMessage(), e);
        }
    }

    /**
     * Issue a superuser token for system modules like EntityModule.
     *
     * @param moduleName the name of the system module
     * @param componentPermissions map of component permissions
     * @return the module auth token with superuser privileges
     */
    public ModuleAuthToken issueSuperuserToken(String moduleName, Map<String, ComponentPermission> componentPermissions) {
        return issueToken(moduleName, componentPermissions, true);
    }

    /**
     * Issue a regular (non-superuser) token for a module.
     *
     * @param moduleName the name of the module
     * @param componentPermissions map of component permissions
     * @return the module auth token without superuser privileges
     */
    public ModuleAuthToken issueRegularToken(String moduleName, Map<String, ComponentPermission> componentPermissions) {
        return issueToken(moduleName, componentPermissions, false);
    }

    /**
     * Refresh a module's token with new component permissions.
     *
     * <p>This method re-issues a JWT token with updated permission claims.
     * Use this when a new module is installed and existing modules need
     * access to its components.
     *
     * @param existingToken the module's current token (used to preserve superuser status)
     * @param newPermissions the updated permission claims
     * @return a new auth token with the updated permissions
     */
    public ModuleAuthToken refreshToken(ModuleAuthToken existingToken, Map<String, ComponentPermission> newPermissions) {
        return issueToken(existingToken.moduleName(), newPermissions, existingToken.superuser());
    }

    /**
     * Generate a cryptographically secure random secret.
     */
    private static String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] secretBytes = new byte[SECRET_LENGTH_BYTES];
        random.nextBytes(secretBytes);
        return Base64.getEncoder().encodeToString(secretBytes);
    }
}
