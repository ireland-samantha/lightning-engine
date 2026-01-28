/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */

package ca.samanthaireland.lightning.engine.api.resource.adapter.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HistoryQueryParams")
class HistoryQueryParamsTest {

    @Test
    @DisplayName("defaults() should return params with sensible defaults")
    void defaultsReturnsCorrectValues() {
        HistoryQueryParams params = HistoryQueryParams.defaults();

        assertThat(params.fromTick()).isEqualTo(0);
        assertThat(params.toTick()).isEqualTo(Long.MAX_VALUE);
        assertThat(params.limit()).isEqualTo(100);
    }

    @Test
    @DisplayName("withLimit() should return params with specified limit")
    void withLimitReturnsCorrectValues() {
        HistoryQueryParams params = HistoryQueryParams.withLimit(50);

        assertThat(params.fromTick()).isEqualTo(0);
        assertThat(params.toTick()).isEqualTo(Long.MAX_VALUE);
        assertThat(params.limit()).isEqualTo(50);
    }

    @Test
    @DisplayName("forRange() should return params with specified tick range")
    void forRangeReturnsCorrectValues() {
        HistoryQueryParams params = HistoryQueryParams.forRange(10, 100);

        assertThat(params.fromTick()).isEqualTo(10);
        assertThat(params.toTick()).isEqualTo(100);
        assertThat(params.limit()).isEqualTo(100);
    }

    @Test
    @DisplayName("forRange() with limit should return params with specified range and limit")
    void forRangeWithLimitReturnsCorrectValues() {
        HistoryQueryParams params = HistoryQueryParams.forRange(5, 50, 25);

        assertThat(params.fromTick()).isEqualTo(5);
        assertThat(params.toTick()).isEqualTo(50);
        assertThat(params.limit()).isEqualTo(25);
    }

    @Test
    @DisplayName("constructor should set all values correctly")
    void constructorSetsAllValues() {
        HistoryQueryParams params = new HistoryQueryParams(1, 999, 10);

        assertThat(params.fromTick()).isEqualTo(1);
        assertThat(params.toTick()).isEqualTo(999);
        assertThat(params.limit()).isEqualTo(10);
    }
}
