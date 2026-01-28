package ca.samanthaireland.lightning.engine.ext.modules.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Position} domain entity.
 */
@DisplayName("Position")
class PositionTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create position with all coordinates")
        void shouldCreatePositionWithAllCoordinates() {
            Position position = new Position(1.5f, 2.5f, 3.5f);

            assertThat(position.x()).isEqualTo(1.5f);
            assertThat(position.y()).isEqualTo(2.5f);
            assertThat(position.z()).isEqualTo(3.5f);
        }

        @Test
        @DisplayName("should handle zero coordinates")
        void shouldHandleZeroCoordinates() {
            Position position = new Position(0f, 0f, 0f);

            assertThat(position.x()).isEqualTo(0f);
            assertThat(position.y()).isEqualTo(0f);
            assertThat(position.z()).isEqualTo(0f);
        }

        @Test
        @DisplayName("should handle negative coordinates")
        void shouldHandleNegativeCoordinates() {
            Position position = new Position(-1.5f, -2.5f, -3.5f);

            assertThat(position.x()).isEqualTo(-1.5f);
            assertThat(position.y()).isEqualTo(-2.5f);
            assertThat(position.z()).isEqualTo(-3.5f);
        }
    }

    @Nested
    @DisplayName("Position.origin factory method")
    class OriginFactory {

        @Test
        @DisplayName("should create position at origin")
        void shouldCreatePositionAtOrigin() {
            Position position = Position.origin();

            assertThat(position.x()).isEqualTo(0f);
            assertThat(position.y()).isEqualTo(0f);
            assertThat(position.z()).isEqualTo(0f);
        }
    }

    @Nested
    @DisplayName("Position.of factory method")
    class OfFactory {

        @Test
        @DisplayName("should create position with x and y, z defaults to 0")
        void shouldCreatePositionWithXAndY() {
            Position position = Position.of(5.5f, 10.5f);

            assertThat(position.x()).isEqualTo(5.5f);
            assertThat(position.y()).isEqualTo(10.5f);
            assertThat(position.z()).isEqualTo(0f);
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when all coordinates match")
        void shouldBeEqualWhenAllCoordinatesMatch() {
            Position position1 = new Position(1.5f, 2.5f, 3.5f);
            Position position2 = new Position(1.5f, 2.5f, 3.5f);

            assertThat(position1).isEqualTo(position2);
        }

        @Test
        @DisplayName("should not be equal when x differs")
        void shouldNotBeEqualWhenXDiffers() {
            Position position1 = new Position(1.5f, 2.5f, 3.5f);
            Position position2 = new Position(9.9f, 2.5f, 3.5f);

            assertThat(position1).isNotEqualTo(position2);
        }

        @Test
        @DisplayName("should not be equal when y differs")
        void shouldNotBeEqualWhenYDiffers() {
            Position position1 = new Position(1.5f, 2.5f, 3.5f);
            Position position2 = new Position(1.5f, 9.9f, 3.5f);

            assertThat(position1).isNotEqualTo(position2);
        }

        @Test
        @DisplayName("should not be equal when z differs")
        void shouldNotBeEqualWhenZDiffers() {
            Position position1 = new Position(1.5f, 2.5f, 3.5f);
            Position position2 = new Position(1.5f, 2.5f, 9.9f);

            assertThat(position1).isNotEqualTo(position2);
        }
    }
}
