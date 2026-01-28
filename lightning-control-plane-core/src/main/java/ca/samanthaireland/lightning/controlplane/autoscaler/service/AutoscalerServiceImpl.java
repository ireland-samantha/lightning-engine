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
import ca.samanthaireland.lightning.controlplane.config.AutoscalerConfiguration;
import ca.samanthaireland.lightning.controlplane.node.model.Node;
import ca.samanthaireland.lightning.controlplane.node.model.NodeStatus;
import ca.samanthaireland.lightning.controlplane.node.service.NodeRegistryService;
import ca.samanthaireland.lightning.controlplane.scheduler.service.SchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of AutoscalerService that analyzes cluster metrics
 * and generates scaling recommendations based on saturation thresholds.
 *
 * <p>This is a pure domain implementation with no framework dependencies.
 * Dependencies are provided via constructor injection.
 */
public class AutoscalerServiceImpl implements AutoscalerService {
    private static final Logger log = LoggerFactory.getLogger(AutoscalerServiceImpl.class);

    private final NodeRegistryService nodeRegistryService;
    private final SchedulerService schedulerService;
    private final AutoscalerConfiguration config;

    private final AtomicReference<ScalingRecommendation> lastRecommendation = new AtomicReference<>();
    private final AtomicReference<Instant> lastScalingAction = new AtomicReference<>();

    /**
     * Creates a new AutoscalerServiceImpl.
     *
     * @param nodeRegistryService the node registry service
     * @param schedulerService    the scheduler service
     * @param config              the autoscaler configuration
     */
    public AutoscalerServiceImpl(
            NodeRegistryService nodeRegistryService,
            SchedulerService schedulerService,
            AutoscalerConfiguration config
    ) {
        this.nodeRegistryService = nodeRegistryService;
        this.schedulerService = schedulerService;
        this.config = config;
    }

    @Override
    public ScalingRecommendation getRecommendation() {
        if (!config.enabled()) {
            ScalingRecommendation recommendation = ScalingRecommendation.none(
                    0, 0.0, "Autoscaler is disabled"
            );
            lastRecommendation.set(recommendation);
            return recommendation;
        }

        List<Node> allNodes = nodeRegistryService.findAll();
        List<Node> healthyNodes = allNodes.stream()
                .filter(n -> n.status() == NodeStatus.HEALTHY)
                .toList();

        int currentNodeCount = healthyNodes.size();
        double currentSaturation = schedulerService.getClusterSaturation();

        // Calculate total capacity and usage for recommendations
        int totalCapacity = healthyNodes.stream()
                .mapToInt(n -> n.capacity().maxContainers())
                .sum();
        int totalUsed = healthyNodes.stream()
                .mapToInt(n -> n.metrics().containerCount())
                .sum();

        ScalingRecommendation recommendation = calculateRecommendation(
                currentNodeCount,
                currentSaturation,
                totalCapacity,
                totalUsed
        );

        lastRecommendation.set(recommendation);
        log.debug("Scaling recommendation: {} (saturation: {:.1f}%, nodes: {})",
                recommendation.action(), currentSaturation * 100, currentNodeCount);

        return recommendation;
    }

    @Override
    public Optional<ScalingRecommendation> getLastRecommendation() {
        return Optional.ofNullable(lastRecommendation.get());
    }

    @Override
    public boolean isInCooldown() {
        Instant lastAction = lastScalingAction.get();
        if (lastAction == null) {
            return false;
        }

        Instant cooldownEnd = lastAction.plusSeconds(config.cooldownSeconds());
        return Instant.now().isBefore(cooldownEnd);
    }

    @Override
    public void recordScalingAction() {
        lastScalingAction.set(Instant.now());
        log.info("Scaling action recorded, cooldown period started ({} seconds)", config.cooldownSeconds());
    }

    private ScalingRecommendation calculateRecommendation(
            int currentNodeCount,
            double currentSaturation,
            int totalCapacity,
            int totalUsed
    ) {
        // Check if in cooldown
        if (isInCooldown()) {
            return ScalingRecommendation.none(
                    currentNodeCount,
                    currentSaturation,
                    "In cooldown period, no scaling recommended"
            );
        }

        // No nodes - must scale up
        if (currentNodeCount == 0) {
            return ScalingRecommendation.scaleUp(
                    0,
                    config.minNodes(),
                    1.0,
                    0.0,
                    "No healthy nodes available, scaling to minimum"
            );
        }

        // Check scale-up threshold
        if (currentSaturation >= config.scaleUpThreshold()) {
            return calculateScaleUp(currentNodeCount, currentSaturation, totalCapacity, totalUsed);
        }

        // Check scale-down threshold
        if (currentSaturation <= config.scaleDownThreshold()) {
            return calculateScaleDown(currentNodeCount, currentSaturation, totalCapacity, totalUsed);
        }

        // Within acceptable range
        return ScalingRecommendation.none(
                currentNodeCount,
                currentSaturation,
                String.format("Saturation (%.1f%%) is within target range (%.0f%%-%.0f%%)",
                        currentSaturation * 100,
                        config.scaleDownThreshold() * 100,
                        config.scaleUpThreshold() * 100)
        );
    }

    private ScalingRecommendation calculateScaleUp(
            int currentNodeCount,
            double currentSaturation,
            int totalCapacity,
            int totalUsed
    ) {
        // Already at max nodes
        if (currentNodeCount >= config.maxNodes()) {
            return ScalingRecommendation.none(
                    currentNodeCount,
                    currentSaturation,
                    "At maximum node count (" + config.maxNodes() + "), cannot scale up"
            );
        }

        // Calculate nodes needed to reach target saturation
        // targetSaturation = totalUsed / newTotalCapacity
        // newTotalCapacity = totalUsed / targetSaturation
        // Assume average capacity per node
        int avgCapacityPerNode = currentNodeCount > 0 ? totalCapacity / currentNodeCount : 100;
        double targetCapacity = totalUsed / config.targetSaturation();
        int targetNodes = (int) Math.ceil(targetCapacity / avgCapacityPerNode);

        // Clamp to max nodes
        targetNodes = Math.min(targetNodes, config.maxNodes());

        // Ensure at least one node is added
        targetNodes = Math.max(targetNodes, currentNodeCount + 1);

        double estimatedSaturation = avgCapacityPerNode * targetNodes > 0
                ? (double) totalUsed / (avgCapacityPerNode * targetNodes)
                : 0.0;

        return ScalingRecommendation.scaleUp(
                currentNodeCount,
                targetNodes,
                currentSaturation,
                estimatedSaturation,
                String.format("Saturation (%.1f%%) exceeds threshold (%.0f%%), recommending %d additional node(s)",
                        currentSaturation * 100,
                        config.scaleUpThreshold() * 100,
                        targetNodes - currentNodeCount)
        );
    }

    private ScalingRecommendation calculateScaleDown(
            int currentNodeCount,
            double currentSaturation,
            int totalCapacity,
            int totalUsed
    ) {
        // Already at minimum nodes
        if (currentNodeCount <= config.minNodes()) {
            return ScalingRecommendation.none(
                    currentNodeCount,
                    currentSaturation,
                    "At minimum node count (" + config.minNodes() + "), cannot scale down"
            );
        }

        // Calculate nodes needed to reach target saturation
        int avgCapacityPerNode = totalCapacity / currentNodeCount;
        double targetCapacity = totalUsed / config.targetSaturation();
        int targetNodes = (int) Math.ceil(targetCapacity / avgCapacityPerNode);

        // Clamp to min nodes
        targetNodes = Math.max(targetNodes, config.minNodes());

        // Ensure at least one node is removed
        targetNodes = Math.min(targetNodes, currentNodeCount - 1);

        double estimatedSaturation = avgCapacityPerNode * targetNodes > 0
                ? (double) totalUsed / (avgCapacityPerNode * targetNodes)
                : 0.0;

        // Don't scale down if it would exceed scale-up threshold
        if (estimatedSaturation >= config.scaleUpThreshold()) {
            return ScalingRecommendation.none(
                    currentNodeCount,
                    currentSaturation,
                    "Scale-down would exceed scale-up threshold, staying at current size"
            );
        }

        return ScalingRecommendation.scaleDown(
                currentNodeCount,
                targetNodes,
                currentSaturation,
                estimatedSaturation,
                String.format("Saturation (%.1f%%) below threshold (%.0f%%), recommending removal of %d node(s)",
                        currentSaturation * 100,
                        config.scaleDownThreshold() * 100,
                        currentNodeCount - targetNodes)
        );
    }
}
