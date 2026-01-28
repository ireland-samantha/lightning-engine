package ca.samanthaireland.lightning.engine.ext.modules.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Vector3}.
 */
@DisplayName("Vector3")
class Vector3Test {

    @Nested
    @DisplayName("ZERO constant")
    class ZeroConstant {

        @Test
        @DisplayName("should have all zero components")
        void shouldHaveAllZeroComponents() {
            assertThat(Vector3.ZERO.x()).isEqualTo(0);
            assertThat(Vector3.ZERO.y()).isEqualTo(0);
            assertThat(Vector3.ZERO.z()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("add")
    class Add {

        @Test
        @DisplayName("should add two vectors")
        void shouldAddTwoVectors() {
            Vector3 a = new Vector3(1, 2, 3);
            Vector3 b = new Vector3(4, 5, 6);

            Vector3 result = a.add(b);

            assertThat(result.x()).isEqualTo(5);
            assertThat(result.y()).isEqualTo(7);
            assertThat(result.z()).isEqualTo(9);
        }

        @Test
        @DisplayName("should not modify original vectors")
        void shouldNotModifyOriginalVectors() {
            Vector3 a = new Vector3(1, 2, 3);
            Vector3 b = new Vector3(4, 5, 6);

            a.add(b);

            assertThat(a.x()).isEqualTo(1);
            assertThat(a.y()).isEqualTo(2);
            assertThat(a.z()).isEqualTo(3);
        }

        @Test
        @DisplayName("should handle negative values")
        void shouldHandleNegativeValues() {
            Vector3 a = new Vector3(5, 5, 5);
            Vector3 b = new Vector3(-3, -3, -3);

            Vector3 result = a.add(b);

            assertThat(result.x()).isEqualTo(2);
            assertThat(result.y()).isEqualTo(2);
            assertThat(result.z()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("multiply")
    class Multiply {

        @Test
        @DisplayName("should multiply by scalar")
        void shouldMultiplyByScalar() {
            Vector3 v = new Vector3(2, 3, 4);

            Vector3 result = v.multiply(2);

            assertThat(result.x()).isEqualTo(4);
            assertThat(result.y()).isEqualTo(6);
            assertThat(result.z()).isEqualTo(8);
        }

        @Test
        @DisplayName("should handle zero scalar")
        void shouldHandleZeroScalar() {
            Vector3 v = new Vector3(2, 3, 4);

            Vector3 result = v.multiply(0);

            assertThat(result).isEqualTo(Vector3.ZERO);
        }

        @Test
        @DisplayName("should handle negative scalar")
        void shouldHandleNegativeScalar() {
            Vector3 v = new Vector3(2, 3, 4);

            Vector3 result = v.multiply(-1);

            assertThat(result.x()).isEqualTo(-2);
            assertThat(result.y()).isEqualTo(-3);
            assertThat(result.z()).isEqualTo(-4);
        }
    }

    @Nested
    @DisplayName("divide")
    class Divide {

        @Test
        @DisplayName("should divide by scalar")
        void shouldDivideByScalar() {
            Vector3 v = new Vector3(6, 9, 12);

            Vector3 result = v.divide(3);

            assertThat(result.x()).isEqualTo(2);
            assertThat(result.y()).isEqualTo(3);
            assertThat(result.z()).isEqualTo(4);
        }

        @Test
        @DisplayName("should throw for zero divisor")
        void shouldThrowForZeroDivisor() {
            Vector3 v = new Vector3(1, 2, 3);

            assertThatThrownBy(() -> v.divide(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("divide by zero");
        }

        @Test
        @DisplayName("should handle fractional results")
        void shouldHandleFractionalResults() {
            Vector3 v = new Vector3(1, 2, 3);

            Vector3 result = v.divide(2);

            assertThat(result.x()).isEqualTo(0.5f);
            assertThat(result.y()).isEqualTo(1f);
            assertThat(result.z()).isEqualTo(1.5f);
        }
    }
}
