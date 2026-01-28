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

package ca.samanthaireland.lightning.controlplane.autoscaler.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A scaling recommendation from the autoscaler.
 *
 * @param action            the recommended scaling action
 * @param currentNodes      current number of healthy nodes
 * @param recommendedNodes  recommended number of nodes after scaling
 * @param nodeDelta         number of nodes to add (positive) or remove (negative)
 * @param currentSaturation current cluster saturation (0.0 to 1.0)
 * @param targetSaturation  target saturation after scaling
 * @param reason            human-readable explanation
 * @param timestamp         when this recommendation was generated
 */
public record ScalingRecommendation(
        ScalingAction action,
        int currentNodes,
        int recommendedNodes,
        int nodeDelta,
        double currentSaturation,
        double targetSaturation,
        String reason,
        Instant timestamp
) {

    public ScalingRecommendation {
        Objects.requireNonNull(action, "action cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");

        if (currentNodes < 0) {
            throw new IllegalArgumentException("currentNodes cannot be negative");
        }
        if (recommendedNodes < 0) {
            throw new IllegalArgumentException("recommendedNodes cannot be negative");
        }
        if (currentSaturation < 0 || currentSaturation > 1) {
            throw new IllegalArgumentException("currentSaturation must be between 0 and 1");
        }
        if (targetSaturation < 0 || targetSaturation > 1) {
            throw new IllegalArgumentException("targetSaturation must be between 0 and 1");
        }
    }

    /**
     * Creates a recommendation to do nothing.
     *
     * @param currentNodes      current node count
     * @param currentSaturation current saturation
     * @param reason            explanation
     * @return a NONE recommendation
     */
    public static ScalingRecommendation none(int currentNodes, double currentSaturation, String reason) {
        return new ScalingRecommendation(
                ScalingAction.NONE,
                currentNodes,
                currentNodes,
                0,
                currentSaturation,
                currentSaturation,
                reason,
                Instant.now()
        );
    }

    /**
     * Creates a scale-up recommendation.
     *
     * @param currentNodes      current node count
     * @param recommendedNodes  recommended node count
     * @param currentSaturation current saturation
     * @param targetSaturation  expected saturation after scaling
     * @param reason            explanation
     * @return a SCALE_UP recommendation
     */
    public static ScalingRecommendation scaleUp(
            int currentNodes,
            int recommendedNodes,
            double currentSaturation,
            double targetSaturation,
            String reason
    ) {
        return new ScalingRecommendation(
                ScalingAction.SCALE_UP,
                currentNodes,
                recommendedNodes,
                recommendedNodes - currentNodes,
                currentSaturation,
                targetSaturation,
                reason,
                Instant.now()
        );
    }

    /**
     * Creates a scale-down recommendation.
     *
     * @param currentNodes      current node count
     * @param recommendedNodes  recommended node count
     * @param currentSaturation current saturation
     * @param targetSaturation  expected saturation after scaling
     * @param reason            explanation
     * @return a SCALE_DOWN recommendation
     */
    public static ScalingRecommendation scaleDown(
            int currentNodes,
            int recommendedNodes,
            double currentSaturation,
            double targetSaturation,
            String reason
    ) {
        return new ScalingRecommendation(
                ScalingAction.SCALE_DOWN,
                currentNodes,
                recommendedNodes,
                recommendedNodes - currentNodes,
                currentSaturation,
                targetSaturation,
                reason,
                Instant.now()
        );
    }

    /**
     * Whether this recommendation suggests any scaling action.
     *
     * @return true if scaling is recommended
     */
    public boolean requiresAction() {
        return action != ScalingAction.NONE;
    }
}
