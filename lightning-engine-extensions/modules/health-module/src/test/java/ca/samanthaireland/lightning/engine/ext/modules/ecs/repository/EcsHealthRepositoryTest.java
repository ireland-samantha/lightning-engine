package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Health;
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

import static ca.samanthaireland.lightning.engine.ext.modules.HealthModuleFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EcsHealthRepository}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EcsHealthRepository")
class EcsHealthRepositoryTest {

    @Mock
    private EntityComponentStore store;

    private EcsHealthRepository repository;

    @BeforeEach
    void setUp() {
        repository = new EcsHealthRepository(store);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should attach all health components")
        void shouldAttachAllHealthComponents() {
            long entityId = 42L;
            Health health = new Health(75f, 100f, 10f, false, true);

            repository.save(entityId, health);

            verify(store).attachComponent(entityId, CURRENT_HP, 75f);
            verify(store).attachComponent(entityId, MAX_HP, 100f);
            verify(store).attachComponent(entityId, DAMAGE_TAKEN, 10f);
            verify(store).attachComponent(entityId, IS_DEAD, 0f);
            verify(store).attachComponent(entityId, INVULNERABLE, 1.0f);
            verify(store).attachComponent(entityId, FLAG, 1.0f);
        }

        @Test
        @DisplayName("should set IS_DEAD to 1 when dead")
        void shouldSetIsDeadTo1WhenDead() {
            long entityId = 42L;
            Health health = new Health(0, 100f, 0, true, false);

            repository.save(entityId, health);

            verify(store).attachComponent(entityId, IS_DEAD, 1.0f);
        }

        @Test
        @DisplayName("should set INVULNERABLE to 0 when not invulnerable")
        void shouldSetInvulnerableTo0WhenNotInvulnerable() {
            long entityId = 42L;
            Health health = new Health(100f, 100f, 0, false, false);

            repository.save(entityId, health);

            verify(store).attachComponent(entityId, INVULNERABLE, 0f);
        }
    }

    @Nested
    @DisplayName("findByEntityId")
    class FindByEntityId {

        @Test
        @DisplayName("should return health when entity exists")
        void shouldReturnHealthWhenEntityExists() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(Set.of(entityId));
            when(store.getComponent(entityId, CURRENT_HP)).thenReturn(80f);
            when(store.getComponent(entityId, MAX_HP)).thenReturn(100f);
            when(store.getComponent(entityId, DAMAGE_TAKEN)).thenReturn(5f);
            when(store.getComponent(entityId, IS_DEAD)).thenReturn(0f);
            when(store.getComponent(entityId, INVULNERABLE)).thenReturn(1f);

            Optional<Health> result = repository.findByEntityId(entityId);

            assertThat(result).isPresent();
            Health health = result.get();
            assertThat(health.currentHP()).isEqualTo(80f);
            assertThat(health.maxHP()).isEqualTo(100f);
            assertThat(health.damageTaken()).isEqualTo(5f);
            assertThat(health.isDead()).isFalse();
            assertThat(health.invulnerable()).isTrue();
        }

        @Test
        @DisplayName("should return empty when entity does not exist")
        void shouldReturnEmptyWhenEntityDoesNotExist() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(Set.of(100L, 200L));

            Optional<Health> result = repository.findByEntityId(entityId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should correctly map IS_DEAD > 0 to isDead true")
        void shouldCorrectlyMapIsDeadToTrue() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(Set.of(entityId));
            when(store.getComponent(entityId, CURRENT_HP)).thenReturn(0f);
            when(store.getComponent(entityId, MAX_HP)).thenReturn(100f);
            when(store.getComponent(entityId, DAMAGE_TAKEN)).thenReturn(0f);
            when(store.getComponent(entityId, IS_DEAD)).thenReturn(1f);
            when(store.getComponent(entityId, INVULNERABLE)).thenReturn(0f);

            Optional<Health> result = repository.findByEntityId(entityId);

            assertThat(result).isPresent();
            assertThat(result.get().isDead()).isTrue();
        }
    }

    @Nested
    @DisplayName("hasHealth")
    class HasHealth {

        @Test
        @DisplayName("should return true when entity has health")
        void shouldReturnTrueWhenEntityHasHealth() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(Set.of(entityId, 100L));

            boolean result = repository.hasHealth(entityId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when entity does not have health")
        void shouldReturnFalseWhenEntityDoesNotHaveHealth() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(Set.of(100L, 200L));

            boolean result = repository.hasHealth(entityId);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when no entities have health")
        void shouldReturnFalseWhenNoEntitiesHaveHealth() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(Set.of());

            boolean result = repository.hasHealth(entityId);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("findAllEntityIds")
    class FindAllEntityIds {

        @Test
        @DisplayName("should return all entity IDs with health")
        void shouldReturnAllEntityIdsWithHealth() {
            Set<Long> entities = Set.of(1L, 2L, 3L);
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(entities);

            Set<Long> result = repository.findAllEntityIds();

            assertThat(result).containsExactlyInAnyOrderElementsOf(entities);
        }

        @Test
        @DisplayName("should return empty set when no entities have health")
        void shouldReturnEmptySetWhenNoEntitiesHaveHealth() {
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(Set.of());

            Set<Long> result = repository.findAllEntityIds();

            assertThat(result).isEmpty();
        }
    }
}
