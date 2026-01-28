package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.modules.domain.BoxCollider;
import ca.samanthaireland.lightning.engine.ext.modules.domain.CollisionHandlerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ca.samanthaireland.lightning.engine.ext.modules.BoxColliderModuleFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EcsBoxColliderRepository}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EcsBoxColliderRepository")
class EcsBoxColliderRepositoryTest {

    @Mock
    private EntityComponentStore store;

    private EcsBoxColliderRepository repository;

    @BeforeEach
    void setUp() {
        repository = new EcsBoxColliderRepository(store);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should attach all collider components")
        void shouldAttachAllColliderComponents() {
            long entityId = 42L;
            BoxCollider collider = BoxCollider.create(10f, 20f, 5f, 1f, 2f, 3f, 2, 7, true);

            repository.save(entityId, collider);

            verify(store).attachComponents(eq(entityId), eq(DIMENSION_COMPONENTS),
                    eq(new float[]{10f, 20f, 5f}));
            verify(store).attachComponents(eq(entityId), eq(OFFSET_COMPONENTS),
                    eq(new float[]{1f, 2f, 3f}));
            verify(store).attachComponent(entityId, COLLISION_LAYER, 2);
            verify(store).attachComponent(entityId, COLLISION_MASK, 7);
            verify(store).attachComponent(entityId, IS_TRIGGER, 1);
            verify(store).attachComponent(entityId, FLAG, 1.0f);
        }

        @Test
        @DisplayName("should initialize collision state")
        void shouldInitializeCollisionState() {
            long entityId = 42L;
            BoxCollider collider = BoxCollider.createSimple(10f, 20f, 5f);

            repository.save(entityId, collider);

            verify(store).attachComponent(entityId, IS_COLLIDING, 0);
            verify(store).attachComponent(entityId, COLLISION_COUNT, 0);
            verify(store).attachComponent(entityId, LAST_COLLISION_ENTITY, 0);
        }
    }

    @Nested
    @DisplayName("findByEntityId")
    class FindByEntityId {

        @Test
        @DisplayName("should return collider when entity exists")
        void shouldReturnColliderWhenEntityExists() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(FLAG))).thenReturn(Set.of(entityId));
            when(store.getComponent(entityId, BOX_WIDTH)).thenReturn(10f);
            when(store.getComponent(entityId, BOX_HEIGHT)).thenReturn(20f);
            when(store.getComponent(entityId, BOX_DEPTH)).thenReturn(5f);
            when(store.getComponent(entityId, OFFSET_X)).thenReturn(1f);
            when(store.getComponent(entityId, OFFSET_Y)).thenReturn(2f);
            when(store.getComponent(entityId, OFFSET_Z)).thenReturn(3f);
            when(store.getComponent(entityId, COLLISION_LAYER)).thenReturn(2f);
            when(store.getComponent(entityId, COLLISION_MASK)).thenReturn(7f);
            when(store.getComponent(entityId, IS_TRIGGER)).thenReturn(1f);

            Optional<BoxCollider> result = repository.findByEntityId(entityId);

            assertThat(result).isPresent();
            BoxCollider collider = result.get();
            assertThat(collider.entityId()).isEqualTo(entityId);
            assertThat(collider.width()).isEqualTo(10f);
            assertThat(collider.height()).isEqualTo(20f);
            assertThat(collider.depth()).isEqualTo(5f);
            assertThat(collider.offsetX()).isEqualTo(1f);
            assertThat(collider.offsetY()).isEqualTo(2f);
            assertThat(collider.offsetZ()).isEqualTo(3f);
            assertThat(collider.layer()).isEqualTo(2);
            assertThat(collider.mask()).isEqualTo(7);
            assertThat(collider.isTrigger()).isTrue();
        }

        @Test
        @DisplayName("should return empty when entity does not exist")
        void shouldReturnEmptyWhenEntityDoesNotExist() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(FLAG))).thenReturn(Set.of(100L, 200L));

            Optional<BoxCollider> result = repository.findByEntityId(entityId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("should return true when collider exists")
        void shouldReturnTrueWhenColliderExists() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(FLAG))).thenReturn(Set.of(entityId));

            boolean result = repository.exists(entityId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when collider does not exist")
        void shouldReturnFalseWhenColliderDoesNotExist() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(FLAG))).thenReturn(Set.of());

            boolean result = repository.exists(entityId);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("updateSize")
    class UpdateSize {

        @Test
        @DisplayName("should update width and height components")
        void shouldUpdateWidthAndHeightComponents() {
            long entityId = 42L;

            repository.updateSize(entityId, 15f, 25f);

            verify(store).attachComponent(entityId, BOX_WIDTH, 15f);
            verify(store).attachComponent(entityId, BOX_HEIGHT, 25f);
        }
    }

    @Nested
    @DisplayName("updateLayerMask")
    class UpdateLayerMask {

        @Test
        @DisplayName("should update layer and mask components")
        void shouldUpdateLayerAndMaskComponents() {
            long entityId = 42L;

            repository.updateLayerMask(entityId, 4, 12);

            verify(store).attachComponent(entityId, COLLISION_LAYER, 4);
            verify(store).attachComponent(entityId, COLLISION_MASK, 12);
        }
    }

    @Nested
    @DisplayName("saveHandlerConfig")
    class SaveHandlerConfig {

        @Test
        @DisplayName("should save handler config components")
        void shouldSaveHandlerConfigComponents() {
            long entityId = 42L;
            CollisionHandlerConfig config = CollisionHandlerConfig.create(1, 10f, 20f);

            repository.saveHandlerConfig(entityId, config);

            verify(store).attachComponent(entityId, COLLISION_HANDLER_TYPE, 1);
            verify(store).attachComponent(entityId, COLLISION_HANDLER_PARAM1, 10f);
            verify(store).attachComponent(entityId, COLLISION_HANDLER_PARAM2, 20f);
            verify(store).attachComponent(entityId, COLLISION_HANDLED_TICK, 0);
        }
    }

    @Nested
    @DisplayName("findAllColliderEntities")
    class FindAllColliderEntities {

        @Test
        @DisplayName("should return all entities with colliders")
        void shouldReturnAllEntitiesWithColliders() {
            Set<Long> entities = Set.of(1L, 2L, 3L);
            when(store.getEntitiesWithComponents(List.of(FLAG))).thenReturn(entities);

            Set<Long> result = repository.findAllColliderEntities();

            assertThat(result).containsExactlyInAnyOrderElementsOf(entities);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should remove all collider components")
        void shouldRemoveAllColliderComponents() {
            long entityId = 42L;

            repository.delete(entityId);

            for (var component : ALL_COMPONENTS) {
                verify(store).removeComponent(entityId, component);
            }
        }
    }

    @Nested
    @DisplayName("updateCollisionState")
    class UpdateCollisionState {

        @Test
        @DisplayName("should update all collision state components")
        void shouldUpdateAllCollisionStateComponents() {
            long entityId = 42L;

            repository.updateCollisionState(entityId, true, 3, 100L, 1f, 0f, 2.5f);

            verify(store).attachComponent(entityId, IS_COLLIDING, 1);
            verify(store).attachComponent(entityId, COLLISION_COUNT, 3);
            verify(store).attachComponent(entityId, LAST_COLLISION_ENTITY, 100L);
            verify(store).attachComponent(entityId, COLLISION_NORMAL_X, 1f);
            verify(store).attachComponent(entityId, COLLISION_NORMAL_Y, 0f);
            verify(store).attachComponent(entityId, PENETRATION_DEPTH, 2.5f);
        }
    }

    @Nested
    @DisplayName("resetCollisionState")
    class ResetCollisionState {

        @Test
        @DisplayName("should reset all collision state components to zero")
        void shouldResetAllCollisionStateComponentsToZero() {
            long entityId = 42L;

            repository.resetCollisionState(entityId);

            verify(store).attachComponent(entityId, IS_COLLIDING, 0);
            verify(store).attachComponent(entityId, COLLISION_COUNT, 0);
            verify(store).attachComponent(entityId, LAST_COLLISION_ENTITY, 0);
            verify(store).attachComponent(entityId, COLLISION_NORMAL_X, 0);
            verify(store).attachComponent(entityId, COLLISION_NORMAL_Y, 0);
            verify(store).attachComponent(entityId, PENETRATION_DEPTH, 0);
        }
    }
}
