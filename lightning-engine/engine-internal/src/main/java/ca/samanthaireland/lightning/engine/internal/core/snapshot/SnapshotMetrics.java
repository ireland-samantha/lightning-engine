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

package ca.samanthaireland.lightning.engine.internal.core.snapshot;

/**
 * Metrics for snapshot generation performance.
 *
 * <p>These metrics are collected by {@link CachingSnapshotProvider} and can be
 * exposed via the REST API for monitoring snapshot generation efficiency.
 *
 * @param totalGenerations total number of snapshot generations (includes all types)
 * @param cacheHits number of times a cached snapshot was reused (no changes)
 * @param cacheMisses number of times a cache miss required full/incremental generation
 * @param incrementalUpdates number of incremental snapshot updates performed
 * @param fullRebuilds number of full snapshot rebuilds performed
 * @param avgGenerationMs average generation time in milliseconds
 * @param lastGenerationMs last generation time in milliseconds
 * @param maxGenerationMs maximum generation time in milliseconds
 */
public record SnapshotMetrics(
        long totalGenerations,
        long cacheHits,
        long cacheMisses,
        long incrementalUpdates,
        long fullRebuilds,
        double avgGenerationMs,
        double lastGenerationMs,
        double maxGenerationMs
) {

    /**
     * Returns empty metrics (all zeros).
     *
     * @return empty metrics
     */
    public static SnapshotMetrics empty() {
        return new SnapshotMetrics(0, 0, 0, 0, 0, 0.0, 0.0, 0.0);
    }

    /**
     * Calculates the cache hit rate.
     *
     * @return cache hit rate between 0.0 and 1.0, or 0.0 if no generations
     */
    public double cacheHitRate() {
        if (totalGenerations == 0) {
            return 0.0;
        }
        return (double) cacheHits / totalGenerations;
    }

    /**
     * Calculates the incremental update rate (portion of non-cache-hit generations
     * that were incremental vs full).
     *
     * @return incremental rate between 0.0 and 1.0, or 0.0 if no cache misses
     */
    public double incrementalRate() {
        if (cacheMisses == 0) {
            return 0.0;
        }
        return (double) incrementalUpdates / cacheMisses;
    }

    /**
     * Returns the total time spent on snapshot generation.
     *
     * @return total generation time in milliseconds
     */
    public double totalGenerationTimeMs() {
        return avgGenerationMs * totalGenerations;
    }
}
