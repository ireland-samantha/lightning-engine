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

package ca.samanthaireland.lightning.controlplane.autoscaler.service;

import ca.samanthaireland.lightning.controlplane.autoscaler.model.ScalingRecommendation;

import java.util.Optional;

/**
 * Service for analyzing cluster metrics and generating scaling recommendations.
 */
public interface AutoscalerService {

    /**
     * Analyzes current cluster state and generates a scaling recommendation.
     *
     * @return the scaling recommendation
     */
    ScalingRecommendation getRecommendation();

    /**
     * Gets the most recent scaling recommendation, if any.
     *
     * @return the last recommendation, or empty if none
     */
    Optional<ScalingRecommendation> getLastRecommendation();

    /**
     * Whether a scaling action is currently in cooldown.
     *
     * @return true if in cooldown period
     */
    boolean isInCooldown();

    /**
     * Records that a scaling action was taken, starting the cooldown timer.
     */
    void recordScalingAction();
}
