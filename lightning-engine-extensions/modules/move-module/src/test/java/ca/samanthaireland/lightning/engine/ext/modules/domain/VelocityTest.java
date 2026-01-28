package ca.samanthaireland.lightning.engine.ext.modules.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Velocity}.
 */
@DisplayName("Velocity")
class VelocityTest {

    @Nested
    @DisplayName("factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("zero should create velocity with all components zero")
        void zeroShouldCreateVelocityWithAllComponentsZero() {
            Velocity velocity = Velocity.zero();

            assertThat(velocity.x()).isEqualTo(0);
            assertThat(velocity.y()).isEqualTo(0);
            assertThat(velocity.z()).isEqualTo(0);
        }

        @Test
        @DisplayName("of should create velocity with given components")
        void ofShouldCreateVelocityWithGivenComponents() {
            Velocity velocity = Velocity.of(1.5f, 2.5f, 3.5f);

            assertThat(velocity.x()).isEqualTo(1.5f);
            assertThat(velocity.y()).isEqualTo(2.5f);
            assertThat(velocity.z()).isEqualTo(3.5f);
        }

        @Test
        @DisplayName("of should create velocity with negative components")
        void ofShouldCreateVelocityWithNegativeComponents() {
            Velocity velocity = Velocity.of(-10, -20, -30);

            assertThat(velocity.x()).isEqualTo(-10);
            assertThat(velocity.y()).isEqualTo(-20);
            assertThat(velocity.z()).isEqualTo(-30);
        }
    }

    @Nested
    @DisplayName("record equality")
    class RecordEquality {

        @Test
        @DisplayName("velocities with same components should be equal")
        void velocitiesWithSameComponentsShouldBeEqual() {
            Velocity v1 = Velocity.of(10, 20, 30);
            Velocity v2 = Velocity.of(10, 20, 30);

            assertThat(v1).isEqualTo(v2);
        }

        @Test
        @DisplayName("velocities with different components should not be equal")
        void velocitiesWithDifferentComponentsShouldNotBeEqual() {
            Velocity v1 = Velocity.of(10, 20, 30);
            Velocity v2 = Velocity.of(10, 20, 31);

            assertThat(v1).isNotEqualTo(v2);
        }
    }
}
