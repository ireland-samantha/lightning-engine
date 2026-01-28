package ca.samanthaireland.lightning.engine.ext.modules.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Position}.
 */
@DisplayName("Position")
class PositionTest {

    @Nested
    @DisplayName("add")
    class Add {

        @Test
        @DisplayName("should add velocity components to position")
        void shouldAddVelocityComponentsToPosition() {
            Position position = Position.of(10, 20, 30);
            Velocity velocity = Velocity.of(5, 10, 15);

            Position result = position.add(velocity);

            assertThat(result.x()).isEqualTo(15);
            assertThat(result.y()).isEqualTo(30);
            assertThat(result.z()).isEqualTo(45);
        }

        @Test
        @DisplayName("should handle negative velocity")
        void shouldHandleNegativeVelocity() {
            Position position = Position.of(100, 100, 100);
            Velocity velocity = Velocity.of(-50, -25, -10);

            Position result = position.add(velocity);

            assertThat(result.x()).isEqualTo(50);
            assertThat(result.y()).isEqualTo(75);
            assertThat(result.z()).isEqualTo(90);
        }

        @Test
        @DisplayName("should not modify original position")
        void shouldNotModifyOriginalPosition() {
            Position original = Position.of(10, 20, 30);
            Velocity velocity = Velocity.of(5, 10, 15);

            original.add(velocity);

            assertThat(original.x()).isEqualTo(10);
            assertThat(original.y()).isEqualTo(20);
            assertThat(original.z()).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("origin should create position at 0,0,0")
        void originShouldCreatePositionAtZero() {
            Position position = Position.origin();

            assertThat(position.x()).isEqualTo(0);
            assertThat(position.y()).isEqualTo(0);
            assertThat(position.z()).isEqualTo(0);
        }

        @Test
        @DisplayName("of should create position with given coordinates")
        void ofShouldCreatePositionWithGivenCoordinates() {
            Position position = Position.of(10.5f, 20.5f, 30.5f);

            assertThat(position.x()).isEqualTo(10.5f);
            assertThat(position.y()).isEqualTo(20.5f);
            assertThat(position.z()).isEqualTo(30.5f);
        }
    }
}
