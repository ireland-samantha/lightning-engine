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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Detailed telemetry for command execution.
 *
 * <p>Provides per-command-name metrics including:
 * <ul>
 *   <li>Execution counts by command type</li>
 *   <li>Timing statistics per command type</li>
 *   <li>Error counts per command type</li>
 *   <li>Per-container command statistics</li>
 * </ul>
 *
 * <p>This complements {@link WebSocketMetrics} which provides aggregate metrics.
 */
@ApplicationScoped
public class CommandTelemetry {

    // Per-command metrics
    private final ConcurrentHashMap<String, CommandStats> commandStats = new ConcurrentHashMap<>();

    // Per-container command counts
    private final ConcurrentHashMap<Long, LongAdder> containerCommandCounts = new ConcurrentHashMap<>();

    /**
     * Record a successful command execution.
     *
     * @param commandName the command that was executed
     * @param containerId the container it executed in
     * @param durationNanos execution duration in nanoseconds
     */
    public void recordCommand(String commandName, long containerId, long durationNanos) {
        getOrCreateStats(commandName).recordSuccess(durationNanos);
        containerCommandCounts.computeIfAbsent(containerId, k -> new LongAdder()).increment();
    }

    /**
     * Record a command execution error.
     *
     * @param commandName the command that failed
     * @param containerId the container it was attempted in
     */
    public void recordError(String commandName, long containerId) {
        getOrCreateStats(commandName).recordError();
        containerCommandCounts.computeIfAbsent(containerId, k -> new LongAdder()).increment();
    }

    /**
     * Get statistics for a specific command.
     */
    public CommandSnapshot getCommandStats(String commandName) {
        CommandStats stats = commandStats.get(commandName);
        return stats != null ? stats.snapshot(commandName) : new CommandSnapshot(commandName, 0, 0, 0, 0, 0);
    }

    /**
     * Get statistics for all commands.
     */
    public Map<String, CommandSnapshot> getAllCommandStats() {
        return commandStats.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().snapshot(e.getKey())
                ));
    }

    /**
     * Get total commands executed for a container.
     */
    public long getContainerCommandCount(long containerId) {
        LongAdder count = containerCommandCounts.get(containerId);
        return count != null ? count.sum() : 0;
    }

    /**
     * Get command counts for all containers.
     */
    public Map<Long, Long> getAllContainerCommandCounts() {
        return containerCommandCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().sum()
                ));
    }

    /**
     * Get top N commands by execution count.
     */
    public Map<String, Long> getTopCommands(int limit) {
        return commandStats.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().successCount.sum(), a.getValue().successCount.sum()))
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().successCount.sum(),
                        (a, b) -> a,
                        java.util.LinkedHashMap::new
                ));
    }

    private CommandStats getOrCreateStats(String commandName) {
        return commandStats.computeIfAbsent(commandName, k -> new CommandStats());
    }

    /**
     * Internal mutable stats tracker for a single command type.
     */
    private static class CommandStats {
        final LongAdder successCount = new LongAdder();
        final LongAdder errorCount = new LongAdder();
        final LongAdder totalTimeNanos = new LongAdder();
        volatile long minTimeNanos = Long.MAX_VALUE;
        volatile long maxTimeNanos = 0;

        void recordSuccess(long durationNanos) {
            successCount.increment();
            totalTimeNanos.add(durationNanos);
            updateMinMax(durationNanos);
        }

        void recordError() {
            errorCount.increment();
        }

        private synchronized void updateMinMax(long durationNanos) {
            if (durationNanos < minTimeNanos) {
                minTimeNanos = durationNanos;
            }
            if (durationNanos > maxTimeNanos) {
                maxTimeNanos = durationNanos;
            }
        }

        CommandSnapshot snapshot(String commandName) {
            long count = successCount.sum();
            long total = totalTimeNanos.sum();
            double avgMs = count > 0 ? (total / 1_000_000.0) / count : 0.0;
            double minMs = minTimeNanos == Long.MAX_VALUE ? 0.0 : minTimeNanos / 1_000_000.0;
            double maxMs = maxTimeNanos / 1_000_000.0;
            return new CommandSnapshot(commandName, count, errorCount.sum(), avgMs, minMs, maxMs);
        }
    }

    /**
     * Immutable snapshot of statistics for a single command type.
     */
    public record CommandSnapshot(
            String commandName,
            long successCount,
            long errorCount,
            double avgTimeMs,
            double minTimeMs,
            double maxTimeMs
    ) {
        /**
         * Get total execution count (successes + errors).
         */
        public long totalCount() {
            return successCount + errorCount;
        }

        /**
         * Get error rate as a percentage.
         */
        public double errorRate() {
            long total = totalCount();
            return total > 0 ? (errorCount * 100.0) / total : 0.0;
        }
    }
}
