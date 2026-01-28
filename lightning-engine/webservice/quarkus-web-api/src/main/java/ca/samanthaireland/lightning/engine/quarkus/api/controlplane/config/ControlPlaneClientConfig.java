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

package ca.samanthaireland.lightning.engine.quarkus.api.controlplane.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.Optional;

/**
 * Configuration for the control plane client.
 * When control-plane.url is set, this node will register with the control plane.
 */
@ConfigMapping(prefix = "control-plane")
public interface ControlPlaneClientConfig {

    /**
     * URL of the control plane service (e.g., http://control-plane:8081).
     * If not set, control plane integration is disabled.
     *
     * @return control plane URL, if configured
     */
    @WithName("url")
    Optional<String> url();

    /**
     * Shared secret token for authenticating with the control plane.
     *
     * @return auth token, if configured
     */
    @WithName("token")
    Optional<String> token();

    /**
     * Interval in seconds between heartbeat requests.
     *
     * @return heartbeat interval (default: 10)
     */
    @WithName("heartbeat-interval-seconds")
    @WithDefault("10")
    int heartbeatIntervalSeconds();

    /**
     * Address that other services should use to reach this node.
     * Should include protocol and port (e.g., http://node1:8080).
     *
     * @return advertise address, if configured
     */
    @WithName("advertise-address")
    Optional<String> advertiseAddress();

    /**
     * Unique identifier for this node. If not set, a UUID will be generated.
     *
     * @return node ID, if configured
     */
    @WithName("node-id")
    Optional<String> nodeId();

    /**
     * Maximum number of containers this node can host.
     *
     * @return max containers (default: 100)
     */
    @WithName("max-containers")
    @WithDefault("100")
    int maxContainers();

    /**
     * Checks if control plane integration is enabled.
     *
     * @return true if URL is configured
     */
    default boolean isEnabled() {
        return url().isPresent() && !url().get().isBlank();
    }
}
