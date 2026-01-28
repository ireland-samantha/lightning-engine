/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */

package ca.samanthaireland.lightning.engine.api.resource.adapter.dto;

/**
 * Query parameters for fetching history snapshots.
 *
 * @param fromTick starting tick (inclusive), defaults to 0
 * @param toTick ending tick (inclusive), defaults to Long.MAX_VALUE
 * @param limit maximum snapshots to return, defaults to 100
 */
public record HistoryQueryParams(
        long fromTick,
        long toTick,
        int limit
) {
    /**
     * Default query params (all snapshots, limit 100).
     */
    public static HistoryQueryParams defaults() {
        return new HistoryQueryParams(0, Long.MAX_VALUE, 100);
    }

    /**
     * Create query params with a specific limit.
     */
    public static HistoryQueryParams withLimit(int limit) {
        return new HistoryQueryParams(0, Long.MAX_VALUE, limit);
    }

    /**
     * Create query params for a tick range.
     */
    public static HistoryQueryParams forRange(long fromTick, long toTick) {
        return new HistoryQueryParams(fromTick, toTick, 100);
    }

    /**
     * Create query params for a tick range with limit.
     */
    public static HistoryQueryParams forRange(long fromTick, long toTick, int limit) {
        return new HistoryQueryParams(fromTick, toTick, limit);
    }
}
