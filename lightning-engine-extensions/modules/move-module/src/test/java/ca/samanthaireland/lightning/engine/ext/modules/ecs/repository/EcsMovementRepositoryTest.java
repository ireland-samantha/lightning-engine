package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.modules.domain.MovementState;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Position;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Velocity;
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

import static ca.samanthaireland.lightning.engine.ext.modules.MoveModuleFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EcsMovementRepository}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EcsMovementRepository")
class EcsMovementRepositoryTest {

    @Mock
    private EntityComponentStore store;

    private EcsMovementRepository repository;

    @BeforeEach
    void setUp() {
        repository = new EcsMovementRepository(store);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should attach movement components to entity")
        void shouldAttachMovementComponentsToEntity() {
            long entityId = 42L;
            Position position = Position.of(10, 20, 30);
            Velocity velocity = Velocity.of(1, 2, 3);

            repository.save(entityId, position, velocity);

            verify(store).attachComponents(eq(entityId), eq(MOVE_COMPONENTS),
                    eq(new float[]{10, 20, 30, 1, 2, 3}));
            verify(store).attachComponent(entityId, MODULE, 1.0f);
        }

        @Test
        @DisplayName("should save movement at origin with zero velocity")
        void shouldSaveMovementAtOriginWithZeroVelocity() {
            long entityId = 42L;
            Position position = Position.origin();
            Velocity velocity = Velocity.zero();

            repository.save(entityId, position, velocity);

            verify(store).attachComponents(eq(entityId), eq(MOVE_COMPONENTS),
                    eq(new float[]{0, 0, 0, 0, 0, 0}));
            verify(store).attachComponent(entityId, MODULE, 1.0f);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return movement state when entity has movement components")
        void shouldReturnMovementStateWhenEntityHasMovementComponents() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(ALL_COMPONENTS)).thenReturn(Set.of(entityId));
            when(store.getComponent(entityId, POSITION_X)).thenReturn(10f);
            when(store.getComponent(entityId, POSITION_Y)).thenReturn(20f);
            when(store.getComponent(entityId, POSITION_Z)).thenReturn(30f);
            when(store.getComponent(entityId, VELOCITY_X)).thenReturn(1f);
            when(store.getComponent(entityId, VELOCITY_Y)).thenReturn(2f);
            when(store.getComponent(entityId, VELOCITY_Z)).thenReturn(3f);

            Optional<MovementState> result = repository.findById(entityId);

            assertThat(result).isPresent();
            assertThat(result.get().entityId()).isEqualTo(entityId);
            assertThat(result.get().position()).isEqualTo(Position.of(10, 20, 30));
            assertThat(result.get().velocity()).isEqualTo(Velocity.of(1, 2, 3));
        }

        @Test
        @DisplayName("should return empty when entity has no movement components")
        void shouldReturnEmptyWhenEntityHasNoMovementComponents() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(ALL_COMPONENTS)).thenReturn(Set.of(100L, 200L));

            Optional<MovementState> result = repository.findById(entityId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when no entities have movement components")
        void shouldReturnEmptyWhenNoEntitiesHaveMovementComponents() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(ALL_COMPONENTS)).thenReturn(Set.of());

            Optional<MovementState> result = repository.findById(entityId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllMoveable")
    class FindAllMoveable {

        @Test
        @DisplayName("should return all entities with movement components")
        void shouldReturnAllEntitiesWithMovementComponents() {
            when(store.getEntitiesWithComponents(ALL_COMPONENTS)).thenReturn(Set.of(1L, 2L, 3L));

            Set<Long> result = repository.findAllMoveable();

            assertThat(result).containsExactlyInAnyOrder(1L, 2L, 3L);
        }

        @Test
        @DisplayName("should return empty set when no moveable entities")
        void shouldReturnEmptySetWhenNoMoveableEntities() {
            when(store.getEntitiesWithComponents(ALL_COMPONENTS)).thenReturn(Set.of());

            Set<Long> result = repository.findAllMoveable();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updatePosition")
    class UpdatePosition {

        @Test
        @DisplayName("should attach position components to entity")
        void shouldAttachPositionComponentsToEntity() {
            long entityId = 42L;
            Position position = Position.of(100, 200, 300);

            repository.updatePosition(entityId, position);

            verify(store).attachComponents(
                    eq(entityId),
                    eq(List.of(POSITION_X, POSITION_Y, POSITION_Z)),
                    eq(new float[]{100, 200, 300})
            );
        }

        @Test
        @DisplayName("should update position to origin")
        void shouldUpdatePositionToOrigin() {
            long entityId = 42L;
            Position position = Position.origin();

            repository.updatePosition(entityId, position);

            verify(store).attachComponents(
                    eq(entityId),
                    eq(List.of(POSITION_X, POSITION_Y, POSITION_Z)),
                    eq(new float[]{0, 0, 0})
            );
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should remove all movement components from entity")
        void shouldRemoveAllMovementComponentsFromEntity() {
            long entityId = 42L;

            repository.delete(entityId);

            verify(store).removeComponent(entityId, POSITION_X);
            verify(store).removeComponent(entityId, POSITION_Y);
            verify(store).removeComponent(entityId, POSITION_Z);
            verify(store).removeComponent(entityId, VELOCITY_X);
            verify(store).removeComponent(entityId, VELOCITY_Y);
            verify(store).removeComponent(entityId, VELOCITY_Z);
            verify(store).removeComponent(entityId, MODULE);
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("should return true when entity has movement components")
        void shouldReturnTrueWhenEntityHasMovementComponents() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(ALL_COMPONENTS)).thenReturn(Set.of(entityId));

            boolean result = repository.exists(entityId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when entity does not have movement components")
        void shouldReturnFalseWhenEntityDoesNotHaveMovementComponents() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(ALL_COMPONENTS)).thenReturn(Set.of(100L, 200L));

            boolean result = repository.exists(entityId);

            assertThat(result).isFalse();
        }
    }
}
