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

package ca.samanthaireland.lightning.controlplane.node.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NodeMetricsTest {

    @Test
    void constructor_validMetrics_succeeds() {
        NodeMetrics metrics = new NodeMetrics(5, 10, 0.5, 256, 512);

        assertThat(metrics.containerCount()).isEqualTo(5);
        assertThat(metrics.matchCount()).isEqualTo(10);
        assertThat(metrics.cpuUsage()).isEqualTo(0.5);
        assertThat(metrics.memoryUsedMb()).isEqualTo(256);
        assertThat(metrics.memoryMaxMb()).isEqualTo(512);
    }

    @Test
    void empty_returnsZeroValues() {
        NodeMetrics metrics = NodeMetrics.empty();

        assertThat(metrics.containerCount()).isZero();
        assertThat(metrics.matchCount()).isZero();
        assertThat(metrics.cpuUsage()).isZero();
        assertThat(metrics.memoryUsedMb()).isZero();
        assertThat(metrics.memoryMaxMb()).isZero();
    }

    @Test
    void constructor_negativeContainerCount_throwsException() {
        assertThatThrownBy(() -> new NodeMetrics(-1, 10, 0.5, 256, 512))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("containerCount cannot be negative");
    }

    @Test
    void constructor_negativeMatchCount_throwsException() {
        assertThatThrownBy(() -> new NodeMetrics(5, -1, 0.5, 256, 512))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("matchCount cannot be negative");
    }

    @Test
    void constructor_cpuUsageAboveOne_throwsException() {
        assertThatThrownBy(() -> new NodeMetrics(5, 10, 1.5, 256, 512))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cpuUsage must be between 0.0 and 1.0");
    }

    @Test
    void constructor_cpuUsageBelowZero_throwsException() {
        assertThatThrownBy(() -> new NodeMetrics(5, 10, -0.1, 256, 512))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cpuUsage must be between 0.0 and 1.0");
    }

    @Test
    void constructor_negativeMemoryUsed_throwsException() {
        assertThatThrownBy(() -> new NodeMetrics(5, 10, 0.5, -1, 512))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("memoryUsedMb cannot be negative");
    }

    @Test
    void constructor_negativeMemoryMax_throwsException() {
        assertThatThrownBy(() -> new NodeMetrics(5, 10, 0.5, 256, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("memoryMaxMb cannot be negative");
    }
}
