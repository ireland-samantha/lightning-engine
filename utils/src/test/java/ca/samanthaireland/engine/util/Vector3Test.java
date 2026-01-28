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
 * Unit tests for {@link Vector3}.
 */
@DisplayName("Vector3")
class Vector3Test {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should store x, y, and z values correctly")
        void shouldStoreXYZValuesCorrectly() {
            Vector3 vector = new Vector3(10, 20, 30);

            assertThat(vector.x()).isEqualTo(10);
            assertThat(vector.y()).isEqualTo(20);
            assertThat(vector.z()).isEqualTo(30);
        }

        @Test
        @DisplayName("should work with negative values")
        void shouldWorkWithNegativeValues() {
            Vector3 vector = new Vector3(-100, -200, -300);

            assertThat(vector.x()).isEqualTo(-100);
            assertThat(vector.y()).isEqualTo(-200);
            assertThat(vector.z()).isEqualTo(-300);
        }

        @Test
        @DisplayName("should work with zero values")
        void shouldWorkWithZeroValues() {
            Vector3 vector = new Vector3(0, 0, 0);

            assertThat(vector.x()).isZero();
            assertThat(vector.y()).isZero();
            assertThat(vector.z()).isZero();
        }

        @Test
        @DisplayName("should work with boundary values")
        void shouldWorkWithBoundaryValues() {
            Vector3 maxVector = new Vector3(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
            Vector3 minVector = new Vector3(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE);

            assertThat(maxVector.x()).isEqualTo(Long.MAX_VALUE);
            assertThat(maxVector.y()).isEqualTo(Long.MAX_VALUE);
            assertThat(maxVector.z()).isEqualTo(Long.MAX_VALUE);
            assertThat(minVector.x()).isEqualTo(Long.MIN_VALUE);
            assertThat(minVector.y()).isEqualTo(Long.MIN_VALUE);
            assertThat(minVector.z()).isEqualTo(Long.MIN_VALUE);
        }
    }

    @Nested
    @DisplayName("zero")
    class Zero {

        @Test
        @DisplayName("should return vector with x=0, y=0, z=0")
        void shouldReturnVectorWithZeroComponents() {
            Vector3 zero = Vector3.zero();

            assertThat(zero.x()).isZero();
            assertThat(zero.y()).isZero();
            assertThat(zero.z()).isZero();
        }

        @Test
        @DisplayName("should return same instance on multiple calls")
        void shouldReturnSameInstanceOnMultipleCalls() {
            Vector3 first = Vector3.zero();
            Vector3 second = Vector3.zero();

            assertThat(first).isSameAs(second);
        }
    }

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("should create vector from int x, int y, long z parameters")
        void shouldCreateVectorFromMixedParameters() {
            Vector3 vector = Vector3.of(5, 10, 15L);

            assertThat(vector.x()).isEqualTo(5);
            assertThat(vector.y()).isEqualTo(10);
            assertThat(vector.z()).isEqualTo(15);
        }

        @Test
        @DisplayName("should correctly handle mixed int/long promotion")
        void shouldCorrectlyHandleMixedPromotion() {
            Vector3 vector = Vector3.of(Integer.MAX_VALUE, Integer.MIN_VALUE, Long.MAX_VALUE);

            assertThat(vector.x()).isEqualTo(Integer.MAX_VALUE);
            assertThat(vector.y()).isEqualTo(Integer.MIN_VALUE);
            assertThat(vector.z()).isEqualTo(Long.MAX_VALUE);
        }

        @Test
        @DisplayName("should work with negative values")
        void shouldWorkWithNegativeValues() {
            Vector3 vector = Vector3.of(-50, -75, -100L);

            assertThat(vector.x()).isEqualTo(-50);
            assertThat(vector.y()).isEqualTo(-75);
            assertThat(vector.z()).isEqualTo(-100);
        }
    }

    @Nested
    @DisplayName("record behavior")
    class RecordBehavior {

        @Test
        @DisplayName("should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            Vector3 v1 = new Vector3(10, 20, 30);
            Vector3 v2 = new Vector3(10, 20, 30);
            Vector3 v3 = new Vector3(10, 20, 40);

            assertThat(v1).isEqualTo(v2);
            assertThat(v1).isNotEqualTo(v3);
        }

        @Test
        @DisplayName("should implement hashCode correctly")
        void shouldImplementHashCodeCorrectly() {
            Vector3 v1 = new Vector3(10, 20, 30);
            Vector3 v2 = new Vector3(10, 20, 30);

            assertThat(v1.hashCode()).isEqualTo(v2.hashCode());
        }

        @Test
        @DisplayName("should implement toString correctly")
        void shouldImplementToStringCorrectly() {
            Vector3 vector = new Vector3(10, 20, 30);

            assertThat(vector.toString()).contains("10").contains("20").contains("30");
        }
    }
}
