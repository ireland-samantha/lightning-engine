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

package ca.samanthaireland.lightning.engine.quarkus.api.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WebSocketMetrics}.
 */
class WebSocketMetricsTest {

    private WebSocketMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new WebSocketMetrics();
    }

    @Nested
    @DisplayName("Connection tracking")
    class ConnectionTracking {

        @Test
        void shouldStartWithZeroActiveConnections() {
            assertThat(metrics.getActiveConnectionCount()).isZero();
        }

        @Test
        void shouldIncrementActiveConnectionsOnOpen() {
            metrics.connectionOpened();

            assertThat(metrics.getActiveConnectionCount()).isEqualTo(1);
        }

        @Test
        void shouldDecrementActiveConnectionsOnClose() {
            metrics.connectionOpened();
            metrics.connectionOpened();
            metrics.connectionClosed();

            assertThat(metrics.getActiveConnectionCount()).isEqualTo(1);
        }

        @Test
        void shouldTrackTotalConnectionsOpened() {
            metrics.connectionOpened();
            metrics.connectionOpened();
            metrics.connectionClosed();
            metrics.connectionOpened();

            var snapshot = metrics.getSnapshot();
            assertThat(snapshot.connectionsOpened()).isEqualTo(3);
        }

        @Test
        void shouldTrackTotalConnectionsClosed() {
            metrics.connectionOpened();
            metrics.connectionOpened();
            metrics.connectionClosed();
            metrics.connectionClosed();

            var snapshot = metrics.getSnapshot();
            assertThat(snapshot.connectionsClosed()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Command tracking")
    class CommandTracking {

        @Test
        void shouldTrackCommandsProcessed() {
            metrics.commandProcessed();
            metrics.commandProcessed();
            metrics.commandProcessed();

            var snapshot = metrics.getSnapshot();
            assertThat(snapshot.commandsProcessed()).isEqualTo(3);
        }

        @Test
        void shouldTrackCommandsRateLimited() {
            metrics.commandRateLimited();
            metrics.commandRateLimited();

            var snapshot = metrics.getSnapshot();
            assertThat(snapshot.commandsRateLimited()).isEqualTo(2);
        }

        @Test
        void shouldTrackCommandErrors() {
            metrics.commandError();

            var snapshot = metrics.getSnapshot();
            assertThat(snapshot.commandErrors()).isEqualTo(1);
        }

        @Test
        void shouldTrackAuthFailures() {
            metrics.authFailure();
            metrics.authFailure();
            metrics.authFailure();

            var snapshot = metrics.getSnapshot();
            assertThat(snapshot.authFailures()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Time recording")
    class TimeRecording {

        @Test
        void shouldRecordExecutionTime() {
            metrics.recordTime(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "result";
            });

            var snapshot = metrics.getSnapshot();
            assertThat(snapshot.totalCommandTimeNanos()).isGreaterThan(0);
        }

        @Test
        void shouldReturnSupplierResult() {
            String result = metrics.recordTime(() -> "hello");

            assertThat(result).isEqualTo("hello");
        }

        @Test
        void shouldAccumulateTiming() {
            metrics.recordTime(() -> "a");
            metrics.recordTime(() -> "b");

            var snapshot = metrics.getSnapshot();
            assertThat(snapshot.totalCommandTimeNanos()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("MetricsSnapshot")
    class MetricsSnapshotTest {

        @Test
        void shouldCalculateAverageCommandTimeWhenNoCommands() {
            var snapshot = metrics.getSnapshot();

            assertThat(snapshot.averageCommandTimeMs()).isZero();
        }

        @Test
        void shouldCalculateAverageCommandTime() {
            // Record some time and commands
            metrics.recordTime(() -> {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
            metrics.commandProcessed();
            metrics.commandProcessed();

            var snapshot = metrics.getSnapshot();
            assertThat(snapshot.averageCommandTimeMs()).isGreaterThan(0);
        }

        @Test
        void shouldCaptureAllMetricsInSnapshot() {
            metrics.connectionOpened();
            metrics.connectionClosed();
            metrics.commandProcessed();
            metrics.commandRateLimited();
            metrics.commandError();
            metrics.authFailure();

            var snapshot = metrics.getSnapshot();

            assertThat(snapshot.activeConnections()).isZero();
            assertThat(snapshot.connectionsOpened()).isEqualTo(1);
            assertThat(snapshot.connectionsClosed()).isEqualTo(1);
            assertThat(snapshot.commandsProcessed()).isEqualTo(1);
            assertThat(snapshot.commandsRateLimited()).isEqualTo(1);
            assertThat(snapshot.commandErrors()).isEqualTo(1);
            assertThat(snapshot.authFailures()).isEqualTo(1);
        }
    }
}
