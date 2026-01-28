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

package ca.samanthaireland.lightning.controlplane.provider.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

class PagedResponseTest {

    @Test
    void of_firstPage_returnsCorrectItems() {
        // Arrange
        List<String> items = List.of("a", "b", "c", "d", "e");

        // Act
        PagedResponse<String> result = PagedResponse.of(items, 0, 2);

        // Assert
        assertThat(result.items()).containsExactly("a", "b");
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(2);
        assertThat(result.totalItems()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void of_middlePage_returnsCorrectItems() {
        // Arrange
        List<String> items = List.of("a", "b", "c", "d", "e");

        // Act
        PagedResponse<String> result = PagedResponse.of(items, 1, 2);

        // Assert
        assertThat(result.items()).containsExactly("c", "d");
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    void of_lastPage_returnsRemainingItems() {
        // Arrange
        List<String> items = List.of("a", "b", "c", "d", "e");

        // Act
        PagedResponse<String> result = PagedResponse.of(items, 2, 2);

        // Assert
        assertThat(result.items()).containsExactly("e");
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    void of_pageBeyondEnd_returnsEmptyList() {
        // Arrange
        List<String> items = List.of("a", "b", "c");

        // Act
        PagedResponse<String> result = PagedResponse.of(items, 10, 2);

        // Assert
        assertThat(result.items()).isEmpty();
        assertThat(result.totalItems()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void of_emptyList_returnsEmptyPage() {
        // Arrange
        List<String> items = List.of();

        // Act
        PagedResponse<String> result = PagedResponse.of(items, 0, 10);

        // Assert
        assertThat(result.items()).isEmpty();
        assertThat(result.totalItems()).isEqualTo(0);
        assertThat(result.totalPages()).isEqualTo(0);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void of_exactFit_calculatesCorrectly() {
        // Arrange
        List<String> items = List.of("a", "b", "c", "d");

        // Act
        PagedResponse<String> result = PagedResponse.of(items, 0, 4);

        // Assert
        assertThat(result.items()).containsExactly("a", "b", "c", "d");
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void of_largeList_paginatesCorrectly() {
        // Arrange
        List<Integer> items = IntStream.range(0, 100).boxed().toList();

        // Act
        PagedResponse<Integer> page5 = PagedResponse.of(items, 5, 10);

        // Assert
        assertThat(page5.items()).containsExactly(50, 51, 52, 53, 54, 55, 56, 57, 58, 59);
        assertThat(page5.totalPages()).isEqualTo(10);
        assertThat(page5.hasNext()).isTrue();
        assertThat(page5.hasPrevious()).isTrue();
    }
}
