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

package ca.samanthaireland.lightning.controlplane.config;

import java.util.Optional;

/**
 * Configuration for the Lightning Control Plane.
 *
 * <p>This is a pure domain abstraction with no framework dependencies.
 * Framework-specific implementations should adapt their configuration
 * systems to this interface.
 */
public interface ControlPlaneConfiguration {

    /**
     * TTL in seconds for node entries in the registry.
     * Nodes must send heartbeats more frequently than this value.
     *
     * @return TTL in seconds
     */
    int nodeTtlSeconds();

    /**
     * Shared secret token for node authentication.
     * If empty and requireAuth is true, all requests will be rejected.
     *
     * @return the auth token, if configured
     */
    Optional<String> authToken();

    /**
     * Whether to require authentication for node operations.
     *
     * @return true if authentication is required
     */
    boolean requireAuth();
}
