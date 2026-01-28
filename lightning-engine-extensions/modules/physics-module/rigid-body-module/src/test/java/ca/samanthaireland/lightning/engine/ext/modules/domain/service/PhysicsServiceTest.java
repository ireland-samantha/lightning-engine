package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.RigidBody;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Vector3;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.RigidBodyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PhysicsService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PhysicsService")
class PhysicsServiceTest {

    @Mock
    private RigidBodyRepository rigidBodyRepository;

    private PhysicsService physicsService;

    @BeforeEach
    void setUp() {
        physicsService = new PhysicsService(rigidBodyRepository);
    }

    @Nested
    @DisplayName("applyForce")
    class ApplyForce {

        @Test
        @DisplayName("should accumulate force on rigid body")
        void shouldAccumulateForceOnRigidBody() {
            long entityId = 42L;
            Vector3 currentForce = new Vector3(1, 2, 3);
            RigidBody rigidBody = new RigidBody(
                    entityId, Vector3.ZERO, Vector3.ZERO, Vector3.ZERO, currentForce,
                    1.0f, 0, 0, 0, 0, 0, 1.0f
            );
            when(rigidBodyRepository.findById(entityId)).thenReturn(Optional.of(rigidBody));

            Vector3 additionalForce = new Vector3(10, 20, 30);
            physicsService.applyForce(entityId, additionalForce);

            ArgumentCaptor<Vector3> forceCaptor = ArgumentCaptor.forClass(Vector3.class);
            verify(rigidBodyRepository).updateForce(eq(entityId), forceCaptor.capture());

            Vector3 newForce = forceCaptor.getValue();
            assertThat(newForce.x()).isEqualTo(11);
            assertThat(newForce.y()).isEqualTo(22);
            assertThat(newForce.z()).isEqualTo(33);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when entity not found")
        void shouldThrowEntityNotFoundExceptionWhenEntityNotFound() {
            long entityId = 42L;
            when(rigidBodyRepository.findById(entityId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> physicsService.applyForce(entityId, Vector3.ZERO))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("applyImpulse")
    class ApplyImpulse {

        @Test
        @DisplayName("should update velocity based on impulse and mass")
        void shouldUpdateVelocityBasedOnImpulseAndMass() {
            long entityId = 42L;
            Vector3 currentVelocity = new Vector3(1, 1, 1);
            float mass = 2.0f;
            RigidBody rigidBody = new RigidBody(
                    entityId, Vector3.ZERO, currentVelocity, Vector3.ZERO, Vector3.ZERO,
                    mass, 0, 0, 0, 0, 0, 1.0f
            );
            when(rigidBodyRepository.findById(entityId)).thenReturn(Optional.of(rigidBody));

            // Impulse of (4, 6, 8) with mass 2 should add (2, 3, 4) to velocity
            Vector3 impulse = new Vector3(4, 6, 8);
            physicsService.applyImpulse(entityId, impulse);

            ArgumentCaptor<Vector3> velocityCaptor = ArgumentCaptor.forClass(Vector3.class);
            verify(rigidBodyRepository).updateVelocity(eq(entityId), velocityCaptor.capture());

            Vector3 newVelocity = velocityCaptor.getValue();
            assertThat(newVelocity.x()).isEqualTo(3); // 1 + 4/2
            assertThat(newVelocity.y()).isEqualTo(4); // 1 + 6/2
            assertThat(newVelocity.z()).isEqualTo(5); // 1 + 8/2
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when entity not found")
        void shouldThrowEntityNotFoundExceptionWhenEntityNotFound() {
            long entityId = 42L;
            when(rigidBodyRepository.findById(entityId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> physicsService.applyImpulse(entityId, Vector3.ZERO))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("setVelocity")
    class SetVelocity {

        @Test
        @DisplayName("should update velocity directly")
        void shouldUpdateVelocityDirectly() {
            long entityId = 42L;
            when(rigidBodyRepository.exists(entityId)).thenReturn(true);

            Vector3 velocity = new Vector3(10, 20, 30);
            physicsService.setVelocity(entityId, velocity);

            verify(rigidBodyRepository).updateVelocity(entityId, velocity);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when entity not found")
        void shouldThrowEntityNotFoundExceptionWhenEntityNotFound() {
            long entityId = 42L;
            when(rigidBodyRepository.exists(entityId)).thenReturn(false);

            assertThatThrownBy(() -> physicsService.setVelocity(entityId, Vector3.ZERO))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("setPosition")
    class SetPosition {

        @Test
        @DisplayName("should update position directly")
        void shouldUpdatePositionDirectly() {
            long entityId = 42L;
            when(rigidBodyRepository.exists(entityId)).thenReturn(true);

            Vector3 position = new Vector3(100, 200, 300);
            physicsService.setPosition(entityId, position);

            verify(rigidBodyRepository).updatePosition(entityId, position);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when entity not found")
        void shouldThrowEntityNotFoundExceptionWhenEntityNotFound() {
            long entityId = 42L;
            when(rigidBodyRepository.exists(entityId)).thenReturn(false);

            assertThatThrownBy(() -> physicsService.setPosition(entityId, Vector3.ZERO))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("applyTorque")
    class ApplyTorque {

        @Test
        @DisplayName("should accumulate torque on rigid body")
        void shouldAccumulateTorqueOnRigidBody() {
            long entityId = 42L;
            float currentTorque = 5.0f;
            float currentAngularVelocity = 1.0f;
            float currentRotation = 0.5f;
            RigidBody rigidBody = new RigidBody(
                    entityId, Vector3.ZERO, Vector3.ZERO, Vector3.ZERO, Vector3.ZERO,
                    1.0f, 0, 0, currentAngularVelocity, currentRotation, currentTorque, 1.0f
            );
            when(rigidBodyRepository.findById(entityId)).thenReturn(Optional.of(rigidBody));

            float additionalTorque = 10.0f;
            physicsService.applyTorque(entityId, additionalTorque);

            verify(rigidBodyRepository).updateAngular(
                    entityId, currentAngularVelocity, currentRotation, 15.0f
            );
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when entity not found")
        void shouldThrowEntityNotFoundExceptionWhenEntityNotFound() {
            long entityId = 42L;
            when(rigidBodyRepository.findById(entityId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> physicsService.applyTorque(entityId, 10.0f))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }
}
