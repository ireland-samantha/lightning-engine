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

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CommandTelemetry}.
 */
class CommandTelemetryTest {

    private CommandTelemetry telemetry;

    @BeforeEach
    void setUp() {
        telemetry = new CommandTelemetry();
    }

    @Nested
    @DisplayName("Command statistics")
    class CommandStatistics {

        @Test
        void shouldReturnZeroStatsForUnknownCommand() {
            var stats = telemetry.getCommandStats("unknown");

            assertThat(stats.commandName()).isEqualTo("unknown");
            assertThat(stats.successCount()).isZero();
            assertThat(stats.errorCount()).isZero();
            assertThat(stats.avgTimeMs()).isZero();
        }

        @Test
        void shouldTrackSuccessCount() {
            telemetry.recordCommand("spawn", 1, 1_000_000);
            telemetry.recordCommand("spawn", 1, 2_000_000);

            var stats = telemetry.getCommandStats("spawn");
            assertThat(stats.successCount()).isEqualTo(2);
        }

        @Test
        void shouldTrackErrorCount() {
            telemetry.recordError("spawn", 1);
            telemetry.recordError("spawn", 1);
            telemetry.recordError("spawn", 1);

            var stats = telemetry.getCommandStats("spawn");
            assertThat(stats.errorCount()).isEqualTo(3);
        }

        @Test
        void shouldCalculateAverageTime() {
            telemetry.recordCommand("spawn", 1, 10_000_000); // 10ms
            telemetry.recordCommand("spawn", 1, 20_000_000); // 20ms

            var stats = telemetry.getCommandStats("spawn");
            assertThat(stats.avgTimeMs()).isEqualTo(15.0);
        }

        @Test
        void shouldTrackMinTime() {
            telemetry.recordCommand("spawn", 1, 30_000_000); // 30ms
            telemetry.recordCommand("spawn", 1, 10_000_000); // 10ms
            telemetry.recordCommand("spawn", 1, 20_000_000); // 20ms

            var stats = telemetry.getCommandStats("spawn");
            assertThat(stats.minTimeMs()).isEqualTo(10.0);
        }

        @Test
        void shouldTrackMaxTime() {
            telemetry.recordCommand("spawn", 1, 10_000_000); // 10ms
            telemetry.recordCommand("spawn", 1, 30_000_000); // 30ms
            telemetry.recordCommand("spawn", 1, 20_000_000); // 20ms

            var stats = telemetry.getCommandStats("spawn");
            assertThat(stats.maxTimeMs()).isEqualTo(30.0);
        }

        @Test
        void shouldTrackMultipleCommandTypes() {
            telemetry.recordCommand("spawn", 1, 1_000_000);
            telemetry.recordCommand("move", 1, 2_000_000);
            telemetry.recordCommand("spawn", 1, 3_000_000);

            assertThat(telemetry.getCommandStats("spawn").successCount()).isEqualTo(2);
            assertThat(telemetry.getCommandStats("move").successCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Container command counts")
    class ContainerCommandCounts {

        @Test
        void shouldReturnZeroForUnknownContainer() {
            assertThat(telemetry.getContainerCommandCount(999)).isZero();
        }

        @Test
        void shouldTrackCommandsPerContainer() {
            telemetry.recordCommand("spawn", 1, 1_000_000);
            telemetry.recordCommand("spawn", 1, 1_000_000);
            telemetry.recordCommand("spawn", 2, 1_000_000);

            assertThat(telemetry.getContainerCommandCount(1)).isEqualTo(2);
            assertThat(telemetry.getContainerCommandCount(2)).isEqualTo(1);
        }

        @Test
        void shouldCountErrorsInContainerTotal() {
            telemetry.recordCommand("spawn", 1, 1_000_000);
            telemetry.recordError("spawn", 1);

            assertThat(telemetry.getContainerCommandCount(1)).isEqualTo(2);
        }

        @Test
        void shouldReturnAllContainerCounts() {
            telemetry.recordCommand("spawn", 1, 1_000_000);
            telemetry.recordCommand("spawn", 2, 1_000_000);
            telemetry.recordCommand("spawn", 3, 1_000_000);

            Map<Long, Long> counts = telemetry.getAllContainerCommandCounts();

            assertThat(counts).hasSize(3);
            assertThat(counts.get(1L)).isEqualTo(1);
            assertThat(counts.get(2L)).isEqualTo(1);
            assertThat(counts.get(3L)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Aggregate statistics")
    class AggregateStatistics {

        @Test
        void shouldReturnAllCommandStats() {
            telemetry.recordCommand("spawn", 1, 1_000_000);
            telemetry.recordCommand("move", 1, 2_000_000);
            telemetry.recordCommand("attack", 1, 3_000_000);

            Map<String, CommandTelemetry.CommandSnapshot> allStats = telemetry.getAllCommandStats();

            assertThat(allStats).hasSize(3);
            assertThat(allStats).containsKeys("spawn", "move", "attack");
        }

        @Test
        void shouldReturnTopCommands() {
            telemetry.recordCommand("spawn", 1, 1_000_000);
            telemetry.recordCommand("spawn", 1, 1_000_000);
            telemetry.recordCommand("spawn", 1, 1_000_000);
            telemetry.recordCommand("move", 1, 1_000_000);
            telemetry.recordCommand("move", 1, 1_000_000);
            telemetry.recordCommand("attack", 1, 1_000_000);

            Map<String, Long> topCommands = telemetry.getTopCommands(2);

            assertThat(topCommands).hasSize(2);
            assertThat(topCommands.keySet().iterator().next()).isEqualTo("spawn");
        }

        @Test
        void shouldReturnFewerThanLimitIfNotEnoughCommands() {
            telemetry.recordCommand("spawn", 1, 1_000_000);

            Map<String, Long> topCommands = telemetry.getTopCommands(10);

            assertThat(topCommands).hasSize(1);
        }
    }

    @Nested
    @DisplayName("CommandSnapshot")
    class CommandSnapshotTest {

        @Test
        void shouldCalculateTotalCount() {
            telemetry.recordCommand("spawn", 1, 1_000_000);
            telemetry.recordCommand("spawn", 1, 1_000_000);
            telemetry.recordError("spawn", 1);

            var stats = telemetry.getCommandStats("spawn");
            assertThat(stats.totalCount()).isEqualTo(3);
        }

        @Test
        void shouldCalculateErrorRate() {
            telemetry.recordCommand("spawn", 1, 1_000_000);
            telemetry.recordCommand("spawn", 1, 1_000_000);
            telemetry.recordCommand("spawn", 1, 1_000_000);
            telemetry.recordError("spawn", 1);

            var stats = telemetry.getCommandStats("spawn");
            assertThat(stats.errorRate()).isEqualTo(25.0);
        }

        @Test
        void shouldReturnZeroErrorRateWhenNoCommands() {
            var stats = telemetry.getCommandStats("spawn");
            assertThat(stats.errorRate()).isZero();
        }
    }
}
