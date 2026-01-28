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

package ca.samanthaireland.lightning.engine.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Vector2}.
 */
@DisplayName("Vector2")
class Vector2Test {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should store x and y values correctly")
        void shouldStoreXAndYValuesCorrectly() {
            Vector2 vector = new Vector2(10, 20);

            assertThat(vector.x()).isEqualTo(10);
            assertThat(vector.y()).isEqualTo(20);
        }

        @Test
        @DisplayName("should work with negative values")
        void shouldWorkWithNegativeValues() {
            Vector2 vector = new Vector2(-100, -200);

            assertThat(vector.x()).isEqualTo(-100);
            assertThat(vector.y()).isEqualTo(-200);
        }

        @Test
        @DisplayName("should work with zero values")
        void shouldWorkWithZeroValues() {
            Vector2 vector = new Vector2(0, 0);

            assertThat(vector.x()).isZero();
            assertThat(vector.y()).isZero();
        }

        @Test
        @DisplayName("should work with boundary values")
        void shouldWorkWithBoundaryValues() {
            Vector2 maxVector = new Vector2(Long.MAX_VALUE, Long.MAX_VALUE);
            Vector2 minVector = new Vector2(Long.MIN_VALUE, Long.MIN_VALUE);

            assertThat(maxVector.x()).isEqualTo(Long.MAX_VALUE);
            assertThat(maxVector.y()).isEqualTo(Long.MAX_VALUE);
            assertThat(minVector.x()).isEqualTo(Long.MIN_VALUE);
            assertThat(minVector.y()).isEqualTo(Long.MIN_VALUE);
        }
    }

    @Nested
    @DisplayName("zero")
    class Zero {

        @Test
        @DisplayName("should return vector with x=0 and y=0")
        void shouldReturnVectorWithZeroComponents() {
            Vector2 zero = Vector2.zero();

            assertThat(zero.x()).isZero();
            assertThat(zero.y()).isZero();
        }

        @Test
        @DisplayName("should return same instance on multiple calls")
        void shouldReturnSameInstanceOnMultipleCalls() {
            Vector2 first = Vector2.zero();
            Vector2 second = Vector2.zero();

            assertThat(first).isSameAs(second);
        }
    }

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("should create vector from int parameters")
        void shouldCreateVectorFromIntParameters() {
            Vector2 vector = Vector2.of(5, 10);

            assertThat(vector.x()).isEqualTo(5);
            assertThat(vector.y()).isEqualTo(10);
        }

        @Test
        @DisplayName("should correctly promote int to long")
        void shouldCorrectlyPromoteIntToLong() {
            Vector2 vector = Vector2.of(Integer.MAX_VALUE, Integer.MIN_VALUE);

            assertThat(vector.x()).isEqualTo(Integer.MAX_VALUE);
            assertThat(vector.y()).isEqualTo(Integer.MIN_VALUE);
        }

        @Test
        @DisplayName("should work with negative int values")
        void shouldWorkWithNegativeIntValues() {
            Vector2 vector = Vector2.of(-50, -75);

            assertThat(vector.x()).isEqualTo(-50);
            assertThat(vector.y()).isEqualTo(-75);
        }
    }

    @Nested
    @DisplayName("record behavior")
    class RecordBehavior {

        @Test
        @DisplayName("should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            Vector2 v1 = new Vector2(10, 20);
            Vector2 v2 = new Vector2(10, 20);
            Vector2 v3 = new Vector2(10, 30);

            assertThat(v1).isEqualTo(v2);
            assertThat(v1).isNotEqualTo(v3);
        }

        @Test
        @DisplayName("should implement hashCode correctly")
        void shouldImplementHashCodeCorrectly() {
            Vector2 v1 = new Vector2(10, 20);
            Vector2 v2 = new Vector2(10, 20);

            assertThat(v1.hashCode()).isEqualTo(v2.hashCode());
        }

        @Test
        @DisplayName("should implement toString correctly")
        void shouldImplementToStringCorrectly() {
            Vector2 vector = new Vector2(10, 20);

            assertThat(vector.toString()).contains("10").contains("20");
        }
    }
}
