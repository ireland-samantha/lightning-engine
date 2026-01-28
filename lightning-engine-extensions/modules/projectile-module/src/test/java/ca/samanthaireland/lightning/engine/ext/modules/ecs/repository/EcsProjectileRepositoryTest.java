package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.modules.EntityModuleFactory;
import ca.samanthaireland.lightning.engine.ext.modules.GridMapModuleFactory;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Projectile;
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

import static ca.samanthaireland.lightning.engine.ext.modules.ProjectileModuleFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EcsProjectileRepository}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EcsProjectileRepository")
class EcsProjectileRepositoryTest {

    @Mock
    private EntityComponentStore store;

    private EcsProjectileRepository repository;

    @BeforeEach
    void setUp() {
        repository = new EcsProjectileRepository(store);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should create entity and attach components")
        void shouldCreateEntityAndAttachComponents() {
            long matchId = 1L;
            long createdEntityId = 100L;
            Projectile projectile = Projectile.create(
                    5L, 10f, 20f, 1f, 0f, 15f, 25f, 100f, 2f, 1f
            );

            when(store.createEntityForMatch(matchId)).thenReturn(createdEntityId);

            Projectile result = repository.save(matchId, projectile);

            assertThat(result.id()).isEqualTo(createdEntityId);
            verify(store).createEntityForMatch(matchId);
            verify(store).attachComponent(createdEntityId, FLAG, 1.0f);
        }

        @Test
        @DisplayName("should preserve projectile properties")
        void shouldPreserveProjectileProperties() {
            long matchId = 1L;
            long createdEntityId = 100L;
            Projectile projectile = Projectile.create(
                    5L, 10f, 20f, 1f, 0f, 15f, 25f, 100f, 2f, 1f
            );

            when(store.createEntityForMatch(matchId)).thenReturn(createdEntityId);

            Projectile result = repository.save(matchId, projectile);

            assertThat(result.ownerEntityId()).isEqualTo(5L);
            assertThat(result.positionX()).isEqualTo(10f);
            assertThat(result.positionY()).isEqualTo(20f);
            assertThat(result.speed()).isEqualTo(15f);
            assertThat(result.damage()).isEqualTo(25f);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update position components")
        void shouldUpdatePositionComponents() {
            Projectile projectile = new Projectile(
                    100L, 5L, 15f, 25f, 1f, 0f, 15f, 25f, 100f, 50f, 2f, 1f, 1f, false
            );

            repository.update(projectile);

            verify(store).attachComponent(100L, GridMapModuleFactory.POSITION_X, 15f);
            verify(store).attachComponent(100L, GridMapModuleFactory.POSITION_Y, 25f);
        }

        @Test
        @DisplayName("should update ticksAlive")
        void shouldUpdateTicksAlive() {
            Projectile projectile = new Projectile(
                    100L, 5L, 15f, 25f, 1f, 0f, 15f, 25f, 100f, 50f, 2f, 1f, 1f, false
            );

            repository.update(projectile);

            verify(store).attachComponent(100L, TICKS_ALIVE, 50f);
        }

        @Test
        @DisplayName("should update pendingDestroy flag")
        void shouldUpdatePendingDestroyFlag() {
            Projectile projectile = new Projectile(
                    100L, 5L, 15f, 25f, 1f, 0f, 15f, 25f, 100f, 50f, 2f, 1f, 1f, true
            );

            repository.update(projectile);

            verify(store).attachComponent(100L, PENDING_DESTROY, 1.0f);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return projectile when entity exists")
        void shouldReturnProjectileWhenEntityExists() {
            long projectileId = 100L;
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(Set.of(projectileId));
            when(store.getComponent(projectileId, OWNER_ENTITY_ID)).thenReturn(5f);
            when(store.getComponent(projectileId, GridMapModuleFactory.POSITION_X)).thenReturn(10f);
            when(store.getComponent(projectileId, GridMapModuleFactory.POSITION_Y)).thenReturn(20f);
            when(store.getComponent(projectileId, DIRECTION_X)).thenReturn(1f);
            when(store.getComponent(projectileId, DIRECTION_Y)).thenReturn(0f);
            when(store.getComponent(projectileId, SPEED)).thenReturn(15f);
            when(store.getComponent(projectileId, DAMAGE)).thenReturn(25f);
            when(store.getComponent(projectileId, LIFETIME)).thenReturn(100f);
            when(store.getComponent(projectileId, TICKS_ALIVE)).thenReturn(50f);
            when(store.getComponent(projectileId, PIERCE_COUNT)).thenReturn(2f);
            when(store.getComponent(projectileId, HITS_REMAINING)).thenReturn(1f);
            when(store.getComponent(projectileId, PROJECTILE_TYPE)).thenReturn(1f);
            when(store.getComponent(projectileId, PENDING_DESTROY)).thenReturn(0f);

            Optional<Projectile> result = repository.findById(projectileId);

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(projectileId);
            assertThat(result.get().ownerEntityId()).isEqualTo(5L);
            assertThat(result.get().positionX()).isEqualTo(10f);
            assertThat(result.get().positionY()).isEqualTo(20f);
            assertThat(result.get().speed()).isEqualTo(15f);
            assertThat(result.get().damage()).isEqualTo(25f);
        }

        @Test
        @DisplayName("should return empty when entity does not exist")
        void shouldReturnEmptyWhenEntityDoesNotExist() {
            long projectileId = 100L;
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(Set.of(200L, 300L));

            Optional<Projectile> result = repository.findById(projectileId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllIds")
    class FindAllIds {

        @Test
        @DisplayName("should return all projectile entity IDs")
        void shouldReturnAllProjectileEntityIds() {
            Set<Long> expectedIds = Set.of(100L, 200L, 300L);
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(expectedIds);

            Set<Long> result = repository.findAllIds();

            assertThat(result).isEqualTo(expectedIds);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should remove all projectile components")
        void shouldRemoveAllProjectileComponents() {
            long projectileId = 100L;

            repository.delete(projectileId);

            for (var component : CORE_COMPONENTS) {
                verify(store).removeComponent(projectileId, component);
            }
            verify(store).removeComponent(projectileId, FLAG);
        }
    }

    @Nested
    @DisplayName("markPendingDestroy")
    class MarkPendingDestroy {

        @Test
        @DisplayName("should set pending destroy flag")
        void shouldSetPendingDestroyFlag() {
            long projectileId = 100L;

            repository.markPendingDestroy(projectileId);

            verify(store).attachComponent(projectileId, PENDING_DESTROY, 1.0f);
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("should return true when projectile exists")
        void shouldReturnTrueWhenProjectileExists() {
            long projectileId = 100L;
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(Set.of(projectileId, 200L));

            boolean result = repository.exists(projectileId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when projectile does not exist")
        void shouldReturnFalseWhenProjectileDoesNotExist() {
            long projectileId = 100L;
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(Set.of(200L, 300L));

            boolean result = repository.exists(projectileId);

            assertThat(result).isFalse();
        }
    }
}
