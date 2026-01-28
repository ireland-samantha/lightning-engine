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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RigidBodyService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RigidBodyService")
class RigidBodyServiceTest {

    @Mock
    private RigidBodyRepository rigidBodyRepository;

    private RigidBodyService rigidBodyService;

    @BeforeEach
    void setUp() {
        rigidBodyService = new RigidBodyService(rigidBodyRepository);
    }

    @Nested
    @DisplayName("attachRigidBody")
    class AttachRigidBody {

        @Test
        @DisplayName("should create rigid body with specified properties")
        void shouldCreateRigidBodyWithSpecifiedProperties() {
            long entityId = 42L;
            Vector3 position = new Vector3(1, 2, 3);
            Vector3 velocity = new Vector3(4, 5, 6);
            float mass = 2.0f;
            float linearDrag = 0.1f;
            float angularDrag = 0.2f;
            float inertia = 1.5f;

            RigidBody result = rigidBodyService.attachRigidBody(
                    entityId, position, velocity, mass, linearDrag, angularDrag, inertia
            );

            assertThat(result.entityId()).isEqualTo(entityId);
            assertThat(result.position()).isEqualTo(position);
            assertThat(result.velocity()).isEqualTo(velocity);
            assertThat(result.mass()).isEqualTo(mass);
            assertThat(result.linearDrag()).isEqualTo(linearDrag);
            assertThat(result.angularDrag()).isEqualTo(angularDrag);
            assertThat(result.inertia()).isEqualTo(inertia);

            verify(rigidBodyRepository).save(any(RigidBody.class));
        }

        @Test
        @DisplayName("should save rigid body to repository")
        void shouldSaveRigidBodyToRepository() {
            long entityId = 42L;
            Vector3 position = new Vector3(0, 0, 0);
            Vector3 velocity = new Vector3(0, 0, 0);

            rigidBodyService.attachRigidBody(entityId, position, velocity, 1.0f, 0, 0, 1.0f);

            ArgumentCaptor<RigidBody> captor = ArgumentCaptor.forClass(RigidBody.class);
            verify(rigidBodyRepository).save(captor.capture());

            RigidBody saved = captor.getValue();
            assertThat(saved.entityId()).isEqualTo(entityId);
        }

        @Test
        @DisplayName("should throw for invalid mass")
        void shouldThrowForInvalidMass() {
            Vector3 position = Vector3.ZERO;
            Vector3 velocity = Vector3.ZERO;

            assertThatThrownBy(() ->
                rigidBodyService.attachRigidBody(42L, position, velocity, 0, 0, 0, 1.0f))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Mass must be positive");
        }

        @Test
        @DisplayName("should throw for invalid inertia")
        void shouldThrowForInvalidInertia() {
            Vector3 position = Vector3.ZERO;
            Vector3 velocity = Vector3.ZERO;

            assertThatThrownBy(() ->
                rigidBodyService.attachRigidBody(42L, position, velocity, 1.0f, 0, 0, -1.0f))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Inertia must be positive");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return rigid body when found")
        void shouldReturnRigidBodyWhenFound() {
            long entityId = 42L;
            RigidBody rigidBody = RigidBody.create(entityId, Vector3.ZERO, Vector3.ZERO, 1.0f);
            when(rigidBodyRepository.findById(entityId)).thenReturn(Optional.of(rigidBody));

            RigidBody result = rigidBodyService.findById(entityId);

            assertThat(result).isEqualTo(rigidBody);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when not found")
        void shouldThrowEntityNotFoundExceptionWhenNotFound() {
            long entityId = 42L;
            when(rigidBodyRepository.findById(entityId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> rigidBodyService.findById(entityId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delegate to repository")
        void shouldDelegateToRepository() {
            long entityId = 42L;

            rigidBodyService.delete(entityId);

            verify(rigidBodyRepository).delete(entityId);
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("should return true when entity exists")
        void shouldReturnTrueWhenEntityExists() {
            long entityId = 42L;
            when(rigidBodyRepository.exists(entityId)).thenReturn(true);

            boolean result = rigidBodyService.exists(entityId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when entity does not exist")
        void shouldReturnFalseWhenEntityDoesNotExist() {
            long entityId = 42L;
            when(rigidBodyRepository.exists(entityId)).thenReturn(false);

            boolean result = rigidBodyService.exists(entityId);

            assertThat(result).isFalse();
        }
    }
}
