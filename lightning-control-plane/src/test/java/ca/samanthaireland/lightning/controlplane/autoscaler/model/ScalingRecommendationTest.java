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

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class ScalingRecommendationTest {

    @Test
    void none_createsNoneRecommendation() {
        // Act
        ScalingRecommendation rec = ScalingRecommendation.none(5, 0.5, "All good");

        // Assert
        assertThat(rec.action()).isEqualTo(ScalingAction.NONE);
        assertThat(rec.currentNodes()).isEqualTo(5);
        assertThat(rec.recommendedNodes()).isEqualTo(5);
        assertThat(rec.nodeDelta()).isEqualTo(0);
        assertThat(rec.currentSaturation()).isEqualTo(0.5);
        assertThat(rec.targetSaturation()).isEqualTo(0.5);
        assertThat(rec.reason()).isEqualTo("All good");
        assertThat(rec.requiresAction()).isFalse();
    }

    @Test
    void scaleUp_createsScaleUpRecommendation() {
        // Act
        ScalingRecommendation rec = ScalingRecommendation.scaleUp(2, 4, 0.9, 0.6, "High load");

        // Assert
        assertThat(rec.action()).isEqualTo(ScalingAction.SCALE_UP);
        assertThat(rec.currentNodes()).isEqualTo(2);
        assertThat(rec.recommendedNodes()).isEqualTo(4);
        assertThat(rec.nodeDelta()).isEqualTo(2);
        assertThat(rec.currentSaturation()).isEqualTo(0.9);
        assertThat(rec.targetSaturation()).isEqualTo(0.6);
        assertThat(rec.requiresAction()).isTrue();
    }

    @Test
    void scaleDown_createsScaleDownRecommendation() {
        // Act
        ScalingRecommendation rec = ScalingRecommendation.scaleDown(4, 2, 0.2, 0.5, "Low load");

        // Assert
        assertThat(rec.action()).isEqualTo(ScalingAction.SCALE_DOWN);
        assertThat(rec.currentNodes()).isEqualTo(4);
        assertThat(rec.recommendedNodes()).isEqualTo(2);
        assertThat(rec.nodeDelta()).isEqualTo(-2);
        assertThat(rec.currentSaturation()).isEqualTo(0.2);
        assertThat(rec.targetSaturation()).isEqualTo(0.5);
        assertThat(rec.requiresAction()).isTrue();
    }

    @Test
    void constructor_negativeCurrentNodes_throwsException() {
        assertThatThrownBy(() -> new ScalingRecommendation(
                ScalingAction.NONE, -1, 1, 0, 0.5, 0.5, "test", Instant.now()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currentNodes");
    }

    @Test
    void constructor_negativeRecommendedNodes_throwsException() {
        assertThatThrownBy(() -> new ScalingRecommendation(
                ScalingAction.NONE, 1, -1, 0, 0.5, 0.5, "test", Instant.now()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recommendedNodes");
    }

    @Test
    void constructor_saturationOutOfRange_throwsException() {
        assertThatThrownBy(() -> new ScalingRecommendation(
                ScalingAction.NONE, 1, 1, 0, 1.5, 0.5, "test", Instant.now()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currentSaturation");

        assertThatThrownBy(() -> new ScalingRecommendation(
                ScalingAction.NONE, 1, 1, 0, 0.5, -0.1, "test", Instant.now()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetSaturation");
    }

    @Test
    void constructor_nullAction_throwsException() {
        assertThatThrownBy(() -> new ScalingRecommendation(
                null, 1, 1, 0, 0.5, 0.5, "test", Instant.now()
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_nullReason_throwsException() {
        assertThatThrownBy(() -> new ScalingRecommendation(
                ScalingAction.NONE, 1, 1, 0, 0.5, 0.5, null, Instant.now()
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void timestamp_isSetAutomatically() {
        // Arrange
        Instant before = Instant.now();

        // Act
        ScalingRecommendation rec = ScalingRecommendation.none(1, 0.5, "test");

        // Assert
        assertThat(rec.timestamp()).isAfterOrEqualTo(before);
        assertThat(rec.timestamp()).isBeforeOrEqualTo(Instant.now());
    }
}
