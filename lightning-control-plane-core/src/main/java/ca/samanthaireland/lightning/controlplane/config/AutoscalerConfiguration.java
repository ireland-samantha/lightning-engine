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

/**
 * Configuration for the autoscaler.
 *
 * <p>This is a pure domain abstraction with no framework dependencies.
 * Framework-specific implementations should adapt their configuration
 * systems to this interface.
 */
public interface AutoscalerConfiguration {

    /**
     * Whether the autoscaler is enabled.
     *
     * @return true if enabled
     */
    boolean enabled();

    /**
     * Cluster saturation threshold for scale-up recommendation.
     * When saturation exceeds this value, scale-up is recommended.
     *
     * @return threshold as decimal (e.g., 0.8 = 80%)
     */
    double scaleUpThreshold();

    /**
     * Cluster saturation threshold for scale-down recommendation.
     * When saturation is below this value, scale-down may be recommended.
     *
     * @return threshold as decimal (e.g., 0.3 = 30%)
     */
    double scaleDownThreshold();

    /**
     * Minimum number of healthy nodes to maintain.
     * Scale-down will not be recommended if it would go below this.
     *
     * @return minimum nodes
     */
    int minNodes();

    /**
     * Maximum number of nodes allowed.
     * Scale-up will not be recommended if at this limit.
     *
     * @return maximum nodes
     */
    int maxNodes();

    /**
     * Cooldown period in seconds between scaling actions.
     *
     * @return cooldown in seconds
     */
    int cooldownSeconds();

    /**
     * Target saturation to aim for when calculating scale recommendations.
     *
     * @return target saturation as decimal (e.g., 0.6 = 60%)
     */
    double targetSaturation();
}
