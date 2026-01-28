package ca.samanthaireland.lightning.engine.ext.modules.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RigidBody}.
 */
@DisplayName("RigidBody")
class RigidBodyTest {

    @Nested
    @DisplayName("constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("should throw for zero mass")
        void shouldThrowForZeroMass() {
            assertThatThrownBy(() -> new RigidBody(
                    1L, Vector3.ZERO, Vector3.ZERO, Vector3.ZERO, Vector3.ZERO,
                    0, 0, 0, 0, 0, 0, 1.0f
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Mass must be positive");
        }

        @Test
        @DisplayName("should throw for negative mass")
        void shouldThrowForNegativeMass() {
            assertThatThrownBy(() -> new RigidBody(
                    1L, Vector3.ZERO, Vector3.ZERO, Vector3.ZERO, Vector3.ZERO,
                    -1, 0, 0, 0, 0, 0, 1.0f
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Mass must be positive");
        }

        @Test
        @DisplayName("should throw for zero inertia")
        void shouldThrowForZeroInertia() {
            assertThatThrownBy(() -> new RigidBody(
                    1L, Vector3.ZERO, Vector3.ZERO, Vector3.ZERO, Vector3.ZERO,
                    1.0f, 0, 0, 0, 0, 0, 0
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Inertia must be positive");
        }

        @Test
        @DisplayName("should throw for negative inertia")
        void shouldThrowForNegativeInertia() {
            assertThatThrownBy(() -> new RigidBody(
                    1L, Vector3.ZERO, Vector3.ZERO, Vector3.ZERO, Vector3.ZERO,
                    1.0f, 0, 0, 0, 0, 0, -1
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Inertia must be positive");
        }
    }

    @Nested
    @DisplayName("create factory method")
    class CreateFactoryMethod {

        @Test
        @DisplayName("should create rigid body with defaults")
        void shouldCreateRigidBodyWithDefaults() {
            RigidBody rb = RigidBody.create(42L, new Vector3(1, 2, 3), new Vector3(4, 5, 6), 2.0f);

            assertThat(rb.entityId()).isEqualTo(42L);
            assertThat(rb.position()).isEqualTo(new Vector3(1, 2, 3));
            assertThat(rb.velocity()).isEqualTo(new Vector3(4, 5, 6));
            assertThat(rb.mass()).isEqualTo(2.0f);
            assertThat(rb.acceleration()).isEqualTo(Vector3.ZERO);
            assertThat(rb.force()).isEqualTo(Vector3.ZERO);
            assertThat(rb.linearDrag()).isEqualTo(0);
            assertThat(rb.angularDrag()).isEqualTo(0);
            assertThat(rb.inertia()).isEqualTo(1.0f);
        }

        @Test
        @DisplayName("should create rigid body with all properties")
        void shouldCreateRigidBodyWithAllProperties() {
            RigidBody rb = RigidBody.create(
                    42L,
                    new Vector3(1, 2, 3),
                    new Vector3(4, 5, 6),
                    2.0f,
                    0.1f,
                    0.2f,
                    1.5f
            );

            assertThat(rb.entityId()).isEqualTo(42L);
            assertThat(rb.mass()).isEqualTo(2.0f);
            assertThat(rb.linearDrag()).isEqualTo(0.1f);
            assertThat(rb.angularDrag()).isEqualTo(0.2f);
            assertThat(rb.inertia()).isEqualTo(1.5f);
        }
    }

    @Nested
    @DisplayName("applyForce")
    class ApplyForce {

        @Test
        @DisplayName("should accumulate force")
        void shouldAccumulateForce() {
            RigidBody rb = new RigidBody(
                    1L, Vector3.ZERO, Vector3.ZERO, Vector3.ZERO,
                    new Vector3(1, 2, 3),
                    1.0f, 0, 0, 0, 0, 0, 1.0f
            );

            RigidBody result = rb.applyForce(new Vector3(10, 20, 30));

            assertThat(result.force().x()).isEqualTo(11);
            assertThat(result.force().y()).isEqualTo(22);
            assertThat(result.force().z()).isEqualTo(33);
        }

        @Test
        @DisplayName("should not modify original")
        void shouldNotModifyOriginal() {
            RigidBody rb = RigidBody.create(1L, Vector3.ZERO, Vector3.ZERO, 1.0f);

            rb.applyForce(new Vector3(10, 20, 30));

            assertThat(rb.force()).isEqualTo(Vector3.ZERO);
        }
    }

    @Nested
    @DisplayName("applyTorque")
    class ApplyTorque {

        @Test
        @DisplayName("should accumulate torque")
        void shouldAccumulateTorque() {
            RigidBody rb = new RigidBody(
                    1L, Vector3.ZERO, Vector3.ZERO, Vector3.ZERO, Vector3.ZERO,
                    1.0f, 0, 0, 0, 0, 5.0f, 1.0f
            );

            RigidBody result = rb.applyTorque(10.0f);

            assertThat(result.torque()).isEqualTo(15.0f);
        }
    }

    @Nested
    @DisplayName("applyImpulse")
    class ApplyImpulse {

        @Test
        @DisplayName("should update velocity based on mass")
        void shouldUpdateVelocityBasedOnMass() {
            RigidBody rb = new RigidBody(
                    1L, Vector3.ZERO, new Vector3(1, 1, 1), Vector3.ZERO, Vector3.ZERO,
                    2.0f, 0, 0, 0, 0, 0, 1.0f
            );

            // Impulse of (4, 6, 8) with mass 2 should add (2, 3, 4) to velocity
            RigidBody result = rb.applyImpulse(new Vector3(4, 6, 8));

            assertThat(result.velocity().x()).isEqualTo(3);
            assertThat(result.velocity().y()).isEqualTo(4);
            assertThat(result.velocity().z()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("withVelocity")
    class WithVelocity {

        @Test
        @DisplayName("should return new rigid body with updated velocity")
        void shouldReturnNewRigidBodyWithUpdatedVelocity() {
            RigidBody rb = RigidBody.create(1L, Vector3.ZERO, Vector3.ZERO, 1.0f);

            RigidBody result = rb.withVelocity(new Vector3(10, 20, 30));

            assertThat(result.velocity()).isEqualTo(new Vector3(10, 20, 30));
            assertThat(rb.velocity()).isEqualTo(Vector3.ZERO); // Original unchanged
        }
    }

    @Nested
    @DisplayName("withPosition")
    class WithPosition {

        @Test
        @DisplayName("should return new rigid body with updated position")
        void shouldReturnNewRigidBodyWithUpdatedPosition() {
            RigidBody rb = RigidBody.create(1L, Vector3.ZERO, Vector3.ZERO, 1.0f);

            RigidBody result = rb.withPosition(new Vector3(100, 200, 300));

            assertThat(result.position()).isEqualTo(new Vector3(100, 200, 300));
            assertThat(rb.position()).isEqualTo(Vector3.ZERO); // Original unchanged
        }
    }
}
