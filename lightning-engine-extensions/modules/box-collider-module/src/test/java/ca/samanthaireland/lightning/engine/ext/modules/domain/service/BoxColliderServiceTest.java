package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.ext.modules.domain.BoxCollider;
import ca.samanthaireland.lightning.engine.ext.modules.domain.CollisionHandlerConfig;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.BoxColliderRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BoxColliderService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BoxColliderService")
class BoxColliderServiceTest {

    @Mock
    private BoxColliderRepository repository;

    private BoxColliderService service;

    @BeforeEach
    void setUp() {
        service = new BoxColliderService(repository);
    }

    @Nested
    @DisplayName("attachCollider")
    class AttachCollider {

        @Test
        @DisplayName("should save box collider with all parameters")
        void shouldSaveBoxColliderWithAllParameters() {
            long entityId = 42L;

            service.attachCollider(entityId, 10f, 20f, 5f, 1f, 2f, 3f, 2, 7, true);

            ArgumentCaptor<BoxCollider> captor = ArgumentCaptor.forClass(BoxCollider.class);
            verify(repository).save(eq(entityId), captor.capture());

            BoxCollider saved = captor.getValue();
            assertThat(saved.width()).isEqualTo(10f);
            assertThat(saved.height()).isEqualTo(20f);
            assertThat(saved.depth()).isEqualTo(5f);
            assertThat(saved.offsetX()).isEqualTo(1f);
            assertThat(saved.offsetY()).isEqualTo(2f);
            assertThat(saved.offsetZ()).isEqualTo(3f);
            assertThat(saved.layer()).isEqualTo(2);
            assertThat(saved.mask()).isEqualTo(7);
            assertThat(saved.isTrigger()).isTrue();
        }

        @Test
        @DisplayName("should throw for invalid dimensions")
        void shouldThrowForInvalidDimensions() {
            assertThatThrownBy(() -> service.attachCollider(1L, 0, 10f, 5f, 0, 0, 0, 1, -1, false))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(repository, never()).save(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("attachHandler")
    class AttachHandler {

        @Test
        @DisplayName("should attach handler when entity has collider")
        void shouldAttachHandlerWhenEntityHasCollider() {
            long entityId = 42L;
            when(repository.exists(entityId)).thenReturn(true);

            boolean result = service.attachHandler(entityId, 1, 10f, 20f);

            assertThat(result).isTrue();

            ArgumentCaptor<CollisionHandlerConfig> captor = ArgumentCaptor.forClass(CollisionHandlerConfig.class);
            verify(repository).saveHandlerConfig(eq(entityId), captor.capture());

            CollisionHandlerConfig config = captor.getValue();
            assertThat(config.handlerType()).isEqualTo(1);
            assertThat(config.param1()).isEqualTo(10f);
            assertThat(config.param2()).isEqualTo(20f);
        }

        @Test
        @DisplayName("should return false when entity has no collider")
        void shouldReturnFalseWhenEntityHasNoCollider() {
            long entityId = 42L;
            when(repository.exists(entityId)).thenReturn(false);

            boolean result = service.attachHandler(entityId, 1, 10f, 20f);

            assertThat(result).isFalse();
            verify(repository, never()).saveHandlerConfig(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("setSize")
    class SetSize {

        @Test
        @DisplayName("should update size in repository")
        void shouldUpdateSizeInRepository() {
            long entityId = 42L;

            service.setSize(entityId, 15f, 25f);

            verify(repository).updateSize(entityId, 15f, 25f);
        }
    }

    @Nested
    @DisplayName("setLayerMask")
    class SetLayerMask {

        @Test
        @DisplayName("should update layer and mask in repository")
        void shouldUpdateLayerMaskInRepository() {
            long entityId = 42L;

            service.setLayerMask(entityId, 4, 12);

            verify(repository).updateLayerMask(entityId, 4, 12);
        }
    }

    @Nested
    @DisplayName("queueDelete and processDeleteQueue")
    class DeleteQueue {

        @Test
        @DisplayName("should queue and process deletions")
        void shouldQueueAndProcessDeletions() {
            service.queueDelete(1L);
            service.queueDelete(2L);
            service.queueDelete(3L);

            verify(repository, never()).delete(anyLong());

            service.processDeleteQueue();

            verify(repository).delete(1L);
            verify(repository).delete(2L);
            verify(repository).delete(3L);
        }

        @Test
        @DisplayName("should clear queue after processing")
        void shouldClearQueueAfterProcessing() {
            service.queueDelete(1L);
            service.processDeleteQueue();
            service.processDeleteQueue();

            verify(repository, times(1)).delete(1L);
        }
    }

    @Nested
    @DisplayName("findByEntityId")
    class FindByEntityId {

        @Test
        @DisplayName("should return collider when found")
        void shouldReturnColliderWhenFound() {
            long entityId = 42L;
            BoxCollider collider = BoxCollider.createSimple(10f, 20f, 5f);
            when(repository.findByEntityId(entityId)).thenReturn(Optional.of(collider));

            Optional<BoxCollider> result = service.findByEntityId(entityId);

            assertThat(result).contains(collider);
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            long entityId = 42L;
            when(repository.findByEntityId(entityId)).thenReturn(Optional.empty());

            Optional<BoxCollider> result = service.findByEntityId(entityId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasCollider")
    class HasCollider {

        @Test
        @DisplayName("should return true when collider exists")
        void shouldReturnTrueWhenColliderExists() {
            long entityId = 42L;
            when(repository.exists(entityId)).thenReturn(true);

            assertThat(service.hasCollider(entityId)).isTrue();
        }

        @Test
        @DisplayName("should return false when collider does not exist")
        void shouldReturnFalseWhenColliderDoesNotExist() {
            long entityId = 42L;
            when(repository.exists(entityId)).thenReturn(false);

            assertThat(service.hasCollider(entityId)).isFalse();
        }
    }
}
