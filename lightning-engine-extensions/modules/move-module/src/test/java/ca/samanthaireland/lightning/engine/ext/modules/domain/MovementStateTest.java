package ca.samanthaireland.lightning.engine.ext.modules.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MovementState}.
 */
@DisplayName("MovementState")
class MovementStateTest {

    @Nested
    @DisplayName("applyVelocity")
    class ApplyVelocity {

        @Test
        @DisplayName("should add velocity to position")
        void shouldAddVelocityToPosition() {
            MovementState state = MovementState.of(
                    1L,
                    Position.of(10, 20, 30),
                    Velocity.of(5, -10, 15)
            );

            MovementState result = state.applyVelocity();

            assertThat(result.entityId()).isEqualTo(1L);
            assertThat(result.position()).isEqualTo(Position.of(15, 10, 45));
            assertThat(result.velocity()).isEqualTo(Velocity.of(5, -10, 15));
        }

        @Test
        @DisplayName("should not change position when velocity is zero")
        void shouldNotChangePositionWhenVelocityIsZero() {
            MovementState state = MovementState.of(
                    1L,
                    Position.of(100, 200, 300),
                    Velocity.zero()
            );

            MovementState result = state.applyVelocity();

            assertThat(result.position()).isEqualTo(Position.of(100, 200, 300));
        }

        @Test
        @DisplayName("should handle negative positions")
        void shouldHandleNegativePositions() {
            MovementState state = MovementState.of(
                    1L,
                    Position.of(0, 0, 0),
                    Velocity.of(-10, -20, -30)
            );

            MovementState result = state.applyVelocity();

            assertThat(result.position()).isEqualTo(Position.of(-10, -20, -30));
        }
    }

    @Nested
    @DisplayName("factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("atPosition should create state with zero velocity")
        void atPositionShouldCreateStateWithZeroVelocity() {
            Position position = Position.of(50, 60, 70);

            MovementState state = MovementState.atPosition(42L, position);

            assertThat(state.entityId()).isEqualTo(42L);
            assertThat(state.position()).isEqualTo(position);
            assertThat(state.velocity()).isEqualTo(Velocity.zero());
        }

        @Test
        @DisplayName("of should create state with given position and velocity")
        void ofShouldCreateStateWithGivenPositionAndVelocity() {
            Position position = Position.of(10, 20, 30);
            Velocity velocity = Velocity.of(1, 2, 3);

            MovementState state = MovementState.of(42L, position, velocity);

            assertThat(state.entityId()).isEqualTo(42L);
            assertThat(state.position()).isEqualTo(position);
            assertThat(state.velocity()).isEqualTo(velocity);
        }
    }
}
