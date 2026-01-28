package ca.samanthaireland.lightning.engine.ext.modules.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link GridMap} domain entity.
 */
@DisplayName("Map")
class MapTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create map with valid dimensions")
        void shouldCreateMapWithValidDimensions() {
            GridMap gridMap = new GridMap(1L, 10, 20, 3);

            assertThat(gridMap.id()).isEqualTo(1L);
            assertThat(gridMap.width()).isEqualTo(10);
            assertThat(gridMap.height()).isEqualTo(20);
            assertThat(gridMap.depth()).isEqualTo(3);
        }

        @Test
        @DisplayName("should throw when width is zero")
        void shouldThrowWhenWidthIsZero() {
            assertThatThrownBy(() -> new GridMap(1L, 0, 10, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("width must be positive");
        }

        @Test
        @DisplayName("should throw when width is negative")
        void shouldThrowWhenWidthIsNegative() {
            assertThatThrownBy(() -> new GridMap(1L, -5, 10, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("width must be positive");
        }

        @Test
        @DisplayName("should throw when height is zero")
        void shouldThrowWhenHeightIsZero() {
            assertThatThrownBy(() -> new GridMap(1L, 10, 0, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("height must be positive");
        }

        @Test
        @DisplayName("should throw when height is negative")
        void shouldThrowWhenHeightIsNegative() {
            assertThatThrownBy(() -> new GridMap(1L, 10, -5, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("height must be positive");
        }

        @Test
        @DisplayName("should throw when depth is zero")
        void shouldThrowWhenDepthIsZero() {
            assertThatThrownBy(() -> new GridMap(1L, 10, 10, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("depth must be positive");
        }

        @Test
        @DisplayName("should throw when depth is negative")
        void shouldThrowWhenDepthIsNegative() {
            assertThatThrownBy(() -> new GridMap(1L, 10, 10, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("depth must be positive");
        }
    }

    @Nested
    @DisplayName("Map.create factory method")
    class CreateFactory {

        @Test
        @DisplayName("should create map with id 0")
        void shouldCreateMapWithIdZero() {
            GridMap gridMap = GridMap.create(10, 20, 3);

            assertThat(gridMap.id()).isEqualTo(0L);
            assertThat(gridMap.width()).isEqualTo(10);
            assertThat(gridMap.height()).isEqualTo(20);
            assertThat(gridMap.depth()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("contains")
    class Contains {

        private final GridMap gridMap = new GridMap(1L, 10, 10, 5);

        @Test
        @DisplayName("should return true for position at origin")
        void shouldReturnTrueForPositionAtOrigin() {
            Position position = new Position(0, 0, 0);
            assertThat(gridMap.contains(position)).isTrue();
        }

        @Test
        @DisplayName("should return true for position at max bounds")
        void shouldReturnTrueForPositionAtMaxBounds() {
            Position position = new Position(9, 9, 4);
            assertThat(gridMap.contains(position)).isTrue();
        }

        @Test
        @DisplayName("should return true for position in middle")
        void shouldReturnTrueForPositionInMiddle() {
            Position position = new Position(5, 5, 2);
            assertThat(gridMap.contains(position)).isTrue();
        }

        @Test
        @DisplayName("should return false for negative x")
        void shouldReturnFalseForNegativeX() {
            Position position = new Position(-1, 5, 0);
            assertThat(gridMap.contains(position)).isFalse();
        }

        @Test
        @DisplayName("should return false for negative y")
        void shouldReturnFalseForNegativeY() {
            Position position = new Position(5, -1, 0);
            assertThat(gridMap.contains(position)).isFalse();
        }

        @Test
        @DisplayName("should return false for negative z")
        void shouldReturnFalseForNegativeZ() {
            Position position = new Position(5, 5, -1);
            assertThat(gridMap.contains(position)).isFalse();
        }

        @Test
        @DisplayName("should return false for x equal to width")
        void shouldReturnFalseForXEqualToWidth() {
            Position position = new Position(10, 5, 0);
            assertThat(gridMap.contains(position)).isFalse();
        }

        @Test
        @DisplayName("should return false for y equal to height")
        void shouldReturnFalseForYEqualToHeight() {
            Position position = new Position(5, 10, 0);
            assertThat(gridMap.contains(position)).isFalse();
        }

        @Test
        @DisplayName("should return false for z equal to depth")
        void shouldReturnFalseForZEqualToDepth() {
            Position position = new Position(5, 5, 5);
            assertThat(gridMap.contains(position)).isFalse();
        }

        @Test
        @DisplayName("should return false for x exceeding width")
        void shouldReturnFalseForXExceedingWidth() {
            Position position = new Position(15, 5, 0);
            assertThat(gridMap.contains(position)).isFalse();
        }
    }

    @Nested
    @DisplayName("validatePosition")
    class ValidatePosition {

        private final GridMap gridMap = new GridMap(1L, 10, 10, 5);

        @Test
        @DisplayName("should not throw for valid position")
        void shouldNotThrowForValidPosition() {
            Position position = new Position(5, 5, 2);
            gridMap.isWithinBounds(position);
        }

        @Test
        @DisplayName("should throw PositionOutOfBoundsException for invalid position")
        void shouldThrowPositionOutOfBoundsExceptionForInvalidPosition() {
            Position position = new Position(15, 5, 0);

            assertThatThrownBy(() -> gridMap.isWithinBounds(position))
                    .isInstanceOf(PositionOutOfBoundsException.class)
                    .hasMessageContaining("out of bounds");
        }

        @Test
        @DisplayName("should include position and map in exception")
        void shouldIncludePositionAndMapInException() {
            Position position = new Position(-1, 5, 0);

            try {
                gridMap.isWithinBounds(position);
            } catch (PositionOutOfBoundsException e) {
                assertThat(e.getPosition()).isEqualTo(position);
                assertThat(e.getMap()).isEqualTo(gridMap);
            }
        }
    }
}
