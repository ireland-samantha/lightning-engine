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


package ca.samanthaireland.lightning.engine.api.resource.adapter;

import java.time.Duration;

/**
 * Configuration for HTTP adapters.
 *
 * <p>Provides sensible defaults for HTTP client configuration
 * that can be overridden when creating adapter instances.
 */
public final class AdapterConfig {

    /**
     * Default HTTP connection timeout.
     */
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Default HTTP request timeout.
     */
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final String bearerToken;

    private AdapterConfig(Duration connectTimeout, Duration requestTimeout, String bearerToken) {
        this.connectTimeout = connectTimeout;
        this.requestTimeout = requestTimeout;
        this.bearerToken = bearerToken;
    }

    /**
     * Create configuration with default values.
     */
    public static AdapterConfig defaults() {
        return new AdapterConfig(DEFAULT_CONNECT_TIMEOUT, DEFAULT_REQUEST_TIMEOUT, null);
    }

    /**
     * Create configuration with custom connect timeout.
     */
    public static AdapterConfig withConnectTimeout(Duration connectTimeout) {
        return new AdapterConfig(connectTimeout, DEFAULT_REQUEST_TIMEOUT, null);
    }

    /**
     * Create configuration with custom timeouts.
     */
    public static AdapterConfig of(Duration connectTimeout, Duration requestTimeout) {
        return new AdapterConfig(connectTimeout, requestTimeout, null);
    }

    /**
     * Create configuration with custom timeouts and bearer token.
     */
    public static AdapterConfig of(Duration connectTimeout, Duration requestTimeout, String bearerToken) {
        return new AdapterConfig(connectTimeout, requestTimeout, bearerToken);
    }

    /**
     * Create a new config with a bearer token for authentication.
     */
    public AdapterConfig withBearerToken(String token) {
        return new AdapterConfig(connectTimeout, requestTimeout, token);
    }

    /**
     * Get the connection timeout.
     */
    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Get the request timeout.
     */
    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * Get the bearer token for authentication, or null if not set.
     */
    public String getBearerToken() {
        return bearerToken;
    }

    /**
     * Check if authentication is configured.
     */
    public boolean hasAuthentication() {
        return bearerToken != null && !bearerToken.isBlank();
    }
}
