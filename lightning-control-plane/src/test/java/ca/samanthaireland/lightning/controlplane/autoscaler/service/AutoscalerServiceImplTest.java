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

import ca.samanthaireland.lightning.controlplane.config.AutoscalerConfiguration;
import ca.samanthaireland.lightning.controlplane.autoscaler.model.ScalingAction;
import ca.samanthaireland.lightning.controlplane.autoscaler.model.ScalingRecommendation;
import ca.samanthaireland.lightning.controlplane.node.model.Node;
import ca.samanthaireland.lightning.controlplane.node.model.NodeCapacity;
import ca.samanthaireland.lightning.controlplane.node.model.NodeMetrics;
import ca.samanthaireland.lightning.controlplane.node.service.NodeRegistryService;
import ca.samanthaireland.lightning.controlplane.scheduler.service.SchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoscalerServiceImplTest {

    @Mock
    private NodeRegistryService nodeRegistryService;

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private AutoscalerConfiguration config;

    private AutoscalerServiceImpl autoscaler;

    @BeforeEach
    void setUp() {
        // Default config values
        lenient().when(config.enabled()).thenReturn(true);
        lenient().when(config.scaleUpThreshold()).thenReturn(0.8);
        lenient().when(config.scaleDownThreshold()).thenReturn(0.3);
        lenient().when(config.minNodes()).thenReturn(1);
        lenient().when(config.maxNodes()).thenReturn(100);
        lenient().when(config.cooldownSeconds()).thenReturn(300);
        lenient().when(config.targetSaturation()).thenReturn(0.6);

        autoscaler = new AutoscalerServiceImpl(nodeRegistryService, schedulerService, config);
    }

    @Test
    void getRecommendation_disabled_returnsNone() {
        // Arrange
        when(config.enabled()).thenReturn(false);

        // Act
        ScalingRecommendation result = autoscaler.getRecommendation();

        // Assert
        assertThat(result.action()).isEqualTo(ScalingAction.NONE);
        assertThat(result.reason()).contains("disabled");
    }

    @Test
    void getRecommendation_noNodes_recommendsScaleUp() {
        // Arrange
        when(nodeRegistryService.findAll()).thenReturn(List.of());
        when(schedulerService.getClusterSaturation()).thenReturn(1.0);

        // Act
        ScalingRecommendation result = autoscaler.getRecommendation();

        // Assert
        assertThat(result.action()).isEqualTo(ScalingAction.SCALE_UP);
        assertThat(result.currentNodes()).isEqualTo(0);
        assertThat(result.recommendedNodes()).isEqualTo(1); // minNodes
    }

    @Test
    void getRecommendation_highSaturation_recommendsScaleUp() {
        // Arrange
        Node node = createNodeWithLoad(80, 100); // 80% loaded
        when(nodeRegistryService.findAll()).thenReturn(List.of(node));
        when(schedulerService.getClusterSaturation()).thenReturn(0.85); // Above 0.8 threshold

        // Act
        ScalingRecommendation result = autoscaler.getRecommendation();

        // Assert
        assertThat(result.action()).isEqualTo(ScalingAction.SCALE_UP);
        assertThat(result.nodeDelta()).isGreaterThan(0);
    }

    @Test
    void getRecommendation_lowSaturation_recommendsScaleDown() {
        // Arrange
        Node node1 = createNodeWithLoad(10, 100);
        Node node2 = createNodeWithLoad(10, 100);
        when(nodeRegistryService.findAll()).thenReturn(List.of(node1, node2));
        when(schedulerService.getClusterSaturation()).thenReturn(0.1); // Below 0.3 threshold

        // Act
        ScalingRecommendation result = autoscaler.getRecommendation();

        // Assert
        assertThat(result.action()).isEqualTo(ScalingAction.SCALE_DOWN);
        assertThat(result.nodeDelta()).isLessThan(0);
    }

    @Test
    void getRecommendation_normalSaturation_recommendsNone() {
        // Arrange
        Node node = createNodeWithLoad(50, 100); // 50% loaded
        when(nodeRegistryService.findAll()).thenReturn(List.of(node));
        when(schedulerService.getClusterSaturation()).thenReturn(0.5); // Between thresholds

        // Act
        ScalingRecommendation result = autoscaler.getRecommendation();

        // Assert
        assertThat(result.action()).isEqualTo(ScalingAction.NONE);
        assertThat(result.reason()).contains("within target range");
    }

    @Test
    void getRecommendation_atMaxNodes_cannotScaleUp() {
        // Arrange
        when(config.maxNodes()).thenReturn(1);
        Node node = createNodeWithLoad(90, 100);
        when(nodeRegistryService.findAll()).thenReturn(List.of(node));
        when(schedulerService.getClusterSaturation()).thenReturn(0.9);

        // Act
        ScalingRecommendation result = autoscaler.getRecommendation();

        // Assert
        assertThat(result.action()).isEqualTo(ScalingAction.NONE);
        assertThat(result.reason()).contains("maximum node count");
    }

    @Test
    void getRecommendation_atMinNodes_cannotScaleDown() {
        // Arrange
        when(config.minNodes()).thenReturn(1);
        Node node = createNodeWithLoad(10, 100);
        when(nodeRegistryService.findAll()).thenReturn(List.of(node));
        when(schedulerService.getClusterSaturation()).thenReturn(0.1);

        // Act
        ScalingRecommendation result = autoscaler.getRecommendation();

        // Assert
        assertThat(result.action()).isEqualTo(ScalingAction.NONE);
        assertThat(result.reason()).contains("minimum node count");
    }

    @Test
    void getRecommendation_inCooldown_returnsNone() {
        // Arrange
        Node node = createNodeWithLoad(90, 100);
        when(nodeRegistryService.findAll()).thenReturn(List.of(node));
        when(schedulerService.getClusterSaturation()).thenReturn(0.9);

        // Start cooldown
        autoscaler.recordScalingAction();

        // Act
        ScalingRecommendation result = autoscaler.getRecommendation();

        // Assert
        assertThat(result.action()).isEqualTo(ScalingAction.NONE);
        assertThat(result.reason()).contains("cooldown");
    }

    @Test
    void isInCooldown_noPriorAction_returnsFalse() {
        // Act & Assert
        assertThat(autoscaler.isInCooldown()).isFalse();
    }

    @Test
    void isInCooldown_afterRecordingAction_returnsTrue() {
        // Act
        autoscaler.recordScalingAction();

        // Assert
        assertThat(autoscaler.isInCooldown()).isTrue();
    }

    @Test
    void getLastRecommendation_noRecommendation_returnsEmpty() {
        // Act & Assert
        assertThat(autoscaler.getLastRecommendation()).isEmpty();
    }

    @Test
    void getLastRecommendation_afterRecommendation_returnsLast() {
        // Arrange
        Node node = createNodeWithLoad(50, 100);
        when(nodeRegistryService.findAll()).thenReturn(List.of(node));
        when(schedulerService.getClusterSaturation()).thenReturn(0.5);

        // Act
        autoscaler.getRecommendation();

        // Assert
        assertThat(autoscaler.getLastRecommendation()).isPresent();
    }

    @Test
    void scaleDown_wouldExceedScaleUpThreshold_returnsNone() {
        // Arrange - 2 nodes at 35% total, scaling down to 1 would make it 70%
        // But if target saturation calculation results in exceeding scale-up threshold, should not scale down
        when(config.scaleUpThreshold()).thenReturn(0.7);
        when(config.targetSaturation()).thenReturn(0.6);

        Node node1 = createNodeWithLoad(35, 100);
        Node node2 = createNodeWithLoad(35, 100);
        when(nodeRegistryService.findAll()).thenReturn(List.of(node1, node2));
        when(schedulerService.getClusterSaturation()).thenReturn(0.25); // Below 0.3

        // Act
        ScalingRecommendation result = autoscaler.getRecommendation();

        // Assert - Should not scale down because it would put us above 0.7
        assertThat(result.action()).isEqualTo(ScalingAction.NONE);
    }

    private Node createNodeWithLoad(int containers, int maxContainers) {
        return Node.register("node-" + System.nanoTime(), "http://localhost:8080", new NodeCapacity(maxContainers))
                .withHeartbeat(new NodeMetrics(containers, 0, 0, 0, 0));
    }
}
