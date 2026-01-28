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

import ca.samanthaireland.lightning.controlplane.autoscaler.model.ScalingAction;
import ca.samanthaireland.lightning.controlplane.autoscaler.model.ScalingRecommendation;
import ca.samanthaireland.lightning.controlplane.config.AutoscalerConfiguration;
import ca.samanthaireland.lightning.controlplane.node.model.Node;
import ca.samanthaireland.lightning.controlplane.node.model.NodeCapacity;
import ca.samanthaireland.lightning.controlplane.node.model.NodeMetrics;
import ca.samanthaireland.lightning.controlplane.node.service.NodeRegistryService;
import ca.samanthaireland.lightning.controlplane.scheduler.service.SchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AutoscalerServiceImpl}.
 *
 * <p>Tests verify scaling recommendation logic based on cluster saturation
 * thresholds and cooldown periods.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AutoscalerServiceImpl")
class AutoscalerServiceImplTest {

    @Mock
    private NodeRegistryService nodeRegistryService;

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private AutoscalerConfiguration config;

    private AutoscalerServiceImpl autoscalerService;

    @BeforeEach
    void setUp() {
        autoscalerService = new AutoscalerServiceImpl(nodeRegistryService, schedulerService, config);
        // Use lenient for cooldownSeconds since it may or may not be needed depending on code path
        lenient().when(config.cooldownSeconds()).thenReturn(300);
    }

    @Nested
    @DisplayName("getRecommendation")
    class GetRecommendation {

        @Test
        @DisplayName("should return NONE when autoscaler is disabled")
        void shouldReturnNoneWhenDisabled() {
            // Arrange
            when(config.enabled()).thenReturn(false);

            // Act
            ScalingRecommendation recommendation = autoscalerService.getRecommendation();

            // Assert
            assertThat(recommendation.action()).isEqualTo(ScalingAction.NONE);
            assertThat(recommendation.reason()).contains("disabled");
        }

        @Test
        @DisplayName("should recommend scale up when no healthy nodes")
        void shouldRecommendScaleUpWhenNoHealthyNodes() {
            // Arrange
            when(config.enabled()).thenReturn(true);
            when(config.minNodes()).thenReturn(2);
            when(nodeRegistryService.findAll()).thenReturn(List.of());
            when(schedulerService.getClusterSaturation()).thenReturn(1.0);

            // Act
            ScalingRecommendation recommendation = autoscalerService.getRecommendation();

            // Assert
            assertThat(recommendation.action()).isEqualTo(ScalingAction.SCALE_UP);
            assertThat(recommendation.currentNodes()).isZero();
            assertThat(recommendation.recommendedNodes()).isEqualTo(2);
            assertThat(recommendation.reason()).contains("No healthy nodes");
        }

        @Test
        @DisplayName("should recommend scale up when saturation exceeds threshold")
        void shouldRecommendScaleUpWhenSaturationExceedsThreshold() {
            // Arrange
            when(config.enabled()).thenReturn(true);
            when(config.scaleUpThreshold()).thenReturn(0.8);
            when(config.maxNodes()).thenReturn(10);
            when(config.targetSaturation()).thenReturn(0.6);

            Node node1 = createNodeWithMetrics("node-1", 90, 100); // 90% used
            Node node2 = createNodeWithMetrics("node-2", 85, 100); // 85% used
            when(nodeRegistryService.findAll()).thenReturn(List.of(node1, node2));
            when(schedulerService.getClusterSaturation()).thenReturn(0.875); // 175/200

            // Act
            ScalingRecommendation recommendation = autoscalerService.getRecommendation();

            // Assert
            assertThat(recommendation.action()).isEqualTo(ScalingAction.SCALE_UP);
            assertThat(recommendation.currentNodes()).isEqualTo(2);
            assertThat(recommendation.recommendedNodes()).isGreaterThan(2);
            assertThat(recommendation.reason()).contains("exceeds threshold");
        }

        @Test
        @DisplayName("should recommend scale down when saturation below threshold")
        void shouldRecommendScaleDownWhenSaturationBelowThreshold() {
            // Arrange
            when(config.enabled()).thenReturn(true);
            when(config.scaleUpThreshold()).thenReturn(0.8);
            when(config.scaleDownThreshold()).thenReturn(0.3);
            when(config.minNodes()).thenReturn(1);
            when(config.targetSaturation()).thenReturn(0.6);

            Node node1 = createNodeWithMetrics("node-1", 10, 100); // 10% used
            Node node2 = createNodeWithMetrics("node-2", 15, 100); // 15% used
            Node node3 = createNodeWithMetrics("node-3", 5, 100);  // 5% used
            when(nodeRegistryService.findAll()).thenReturn(List.of(node1, node2, node3));
            when(schedulerService.getClusterSaturation()).thenReturn(0.1); // 30/300

            // Act
            ScalingRecommendation recommendation = autoscalerService.getRecommendation();

            // Assert
            assertThat(recommendation.action()).isEqualTo(ScalingAction.SCALE_DOWN);
            assertThat(recommendation.currentNodes()).isEqualTo(3);
            assertThat(recommendation.recommendedNodes()).isLessThan(3);
            assertThat(recommendation.reason()).contains("below threshold");
        }

        @Test
        @DisplayName("should return NONE when saturation within acceptable range")
        void shouldReturnNoneWhenSaturationWithinRange() {
            // Arrange
            when(config.enabled()).thenReturn(true);
            when(config.scaleUpThreshold()).thenReturn(0.8);
            when(config.scaleDownThreshold()).thenReturn(0.3);

            Node node1 = createNodeWithMetrics("node-1", 50, 100);
            Node node2 = createNodeWithMetrics("node-2", 60, 100);
            when(nodeRegistryService.findAll()).thenReturn(List.of(node1, node2));
            when(schedulerService.getClusterSaturation()).thenReturn(0.55); // 110/200

            // Act
            ScalingRecommendation recommendation = autoscalerService.getRecommendation();

            // Assert
            assertThat(recommendation.action()).isEqualTo(ScalingAction.NONE);
            assertThat(recommendation.reason()).contains("within target range");
        }

        @Test
        @DisplayName("should not scale up when at max nodes")
        void shouldNotScaleUpWhenAtMaxNodes() {
            // Arrange
            when(config.enabled()).thenReturn(true);
            when(config.scaleUpThreshold()).thenReturn(0.8);
            when(config.maxNodes()).thenReturn(2);

            Node node1 = createNodeWithMetrics("node-1", 95, 100);
            Node node2 = createNodeWithMetrics("node-2", 90, 100);
            when(nodeRegistryService.findAll()).thenReturn(List.of(node1, node2));
            when(schedulerService.getClusterSaturation()).thenReturn(0.925);

            // Act
            ScalingRecommendation recommendation = autoscalerService.getRecommendation();

            // Assert
            assertThat(recommendation.action()).isEqualTo(ScalingAction.NONE);
            assertThat(recommendation.reason()).contains("maximum node count");
        }

        @Test
        @DisplayName("should not scale down when at min nodes")
        void shouldNotScaleDownWhenAtMinNodes() {
            // Arrange
            when(config.enabled()).thenReturn(true);
            when(config.scaleUpThreshold()).thenReturn(0.8);
            when(config.scaleDownThreshold()).thenReturn(0.3);
            when(config.minNodes()).thenReturn(2);

            Node node1 = createNodeWithMetrics("node-1", 10, 100);
            Node node2 = createNodeWithMetrics("node-2", 10, 100);
            when(nodeRegistryService.findAll()).thenReturn(List.of(node1, node2));
            when(schedulerService.getClusterSaturation()).thenReturn(0.1);

            // Act
            ScalingRecommendation recommendation = autoscalerService.getRecommendation();

            // Assert
            assertThat(recommendation.action()).isEqualTo(ScalingAction.NONE);
            assertThat(recommendation.reason()).contains("minimum node count");
        }

        @Test
        @DisplayName("should exclude draining nodes from healthy count")
        void shouldExcludeDrainingNodesFromHealthyCount() {
            // Arrange
            when(config.enabled()).thenReturn(true);
            when(config.scaleUpThreshold()).thenReturn(0.8);
            when(config.scaleDownThreshold()).thenReturn(0.3);

            Node healthyNode = createNodeWithMetrics("node-1", 50, 100);
            Node drainingNode = createNodeWithMetrics("node-2", 30, 100).drain();
            when(nodeRegistryService.findAll()).thenReturn(List.of(healthyNode, drainingNode));
            when(schedulerService.getClusterSaturation()).thenReturn(0.5);

            // Act
            ScalingRecommendation recommendation = autoscalerService.getRecommendation();

            // Assert
            assertThat(recommendation.currentNodes()).isEqualTo(1); // Only healthy node counted
        }
    }

    @Nested
    @DisplayName("getLastRecommendation")
    class GetLastRecommendation {

        @Test
        @DisplayName("should return empty when no recommendation made")
        void shouldReturnEmptyWhenNoRecommendationMade() {
            // Act
            Optional<ScalingRecommendation> result = autoscalerService.getLastRecommendation();

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return last recommendation after getRecommendation called")
        void shouldReturnLastRecommendationAfterCall() {
            // Arrange
            when(config.enabled()).thenReturn(false);

            // Act
            ScalingRecommendation firstCall = autoscalerService.getRecommendation();
            Optional<ScalingRecommendation> lastRecommendation = autoscalerService.getLastRecommendation();

            // Assert
            assertThat(lastRecommendation).isPresent();
            assertThat(lastRecommendation.get().action()).isEqualTo(firstCall.action());
        }
    }

    @Nested
    @DisplayName("isInCooldown")
    class IsInCooldown {

        @Test
        @DisplayName("should return false when no scaling action recorded")
        void shouldReturnFalseWhenNoScalingActionRecorded() {
            // Act
            boolean inCooldown = autoscalerService.isInCooldown();

            // Assert
            assertThat(inCooldown).isFalse();
        }

        @Test
        @DisplayName("should return true immediately after recording scaling action")
        void shouldReturnTrueAfterRecordingScalingAction() {
            // Act
            autoscalerService.recordScalingAction();
            boolean inCooldown = autoscalerService.isInCooldown();

            // Assert
            assertThat(inCooldown).isTrue();
        }
    }

    @Nested
    @DisplayName("recordScalingAction")
    class RecordScalingAction {

        @Test
        @DisplayName("should prevent scaling recommendations during cooldown")
        void shouldPreventScalingDuringCooldown() {
            // Arrange
            when(config.enabled()).thenReturn(true);

            Node node = createNodeWithMetrics("node-1", 95, 100);
            when(nodeRegistryService.findAll()).thenReturn(List.of(node));
            when(schedulerService.getClusterSaturation()).thenReturn(0.95);

            // First record a scaling action
            autoscalerService.recordScalingAction();

            // Act
            ScalingRecommendation recommendation = autoscalerService.getRecommendation();

            // Assert
            assertThat(recommendation.action()).isEqualTo(ScalingAction.NONE);
            assertThat(recommendation.reason()).contains("cooldown");
        }
    }

    @Nested
    @DisplayName("scaling calculations")
    class ScalingCalculations {

        @Test
        @DisplayName("should calculate proper node count for scale up")
        void shouldCalculateProperNodeCountForScaleUp() {
            // Arrange
            when(config.enabled()).thenReturn(true);
            when(config.scaleUpThreshold()).thenReturn(0.8);
            when(config.maxNodes()).thenReturn(10);
            when(config.targetSaturation()).thenReturn(0.6);

            // 2 nodes with 90% saturation, want to get to 60% target
            // Need more nodes to reduce saturation
            Node node1 = createNodeWithMetrics("node-1", 90, 100);
            Node node2 = createNodeWithMetrics("node-2", 90, 100);
            when(nodeRegistryService.findAll()).thenReturn(List.of(node1, node2));
            when(schedulerService.getClusterSaturation()).thenReturn(0.9);

            // Act
            ScalingRecommendation recommendation = autoscalerService.getRecommendation();

            // Assert
            assertThat(recommendation.action()).isEqualTo(ScalingAction.SCALE_UP);
            assertThat(recommendation.nodeDelta()).isPositive();
            assertThat(recommendation.recommendedNodes()).isGreaterThan(2);
        }

        @Test
        @DisplayName("should handle scale down without exceeding scale up threshold")
        void shouldHandleScaleDownWithoutExceedingScaleUpThreshold() {
            // Arrange
            when(config.enabled()).thenReturn(true);
            when(config.scaleUpThreshold()).thenReturn(0.8);
            when(config.scaleDownThreshold()).thenReturn(0.3);
            when(config.minNodes()).thenReturn(1);
            when(config.targetSaturation()).thenReturn(0.6);

            // 2 nodes at 25% each - below scale down threshold
            // Removing a node would put us at 50%, still below 80% threshold
            Node node1 = createNodeWithMetrics("node-1", 25, 100);
            Node node2 = createNodeWithMetrics("node-2", 25, 100);
            when(nodeRegistryService.findAll()).thenReturn(List.of(node1, node2));
            when(schedulerService.getClusterSaturation()).thenReturn(0.25);

            // Act
            ScalingRecommendation recommendation = autoscalerService.getRecommendation();

            // Assert
            assertThat(recommendation.action()).isEqualTo(ScalingAction.SCALE_DOWN);
            // Verify estimated saturation is reasonable
            assertThat(recommendation.targetSaturation()).isLessThan(config.scaleUpThreshold());
        }
    }

    // Helper methods

    private Node createNodeWithMetrics(String nodeId, int containerCount, int maxContainers) {
        return Node.register(nodeId, "http://" + nodeId + ":8080", new NodeCapacity(maxContainers))
                .withHeartbeat(new NodeMetrics(containerCount, containerCount * 2, 0.5, 256, 512));
    }
}
