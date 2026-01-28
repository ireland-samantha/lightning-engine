package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ca.samanthaireland.lightning.engine.ext.modules.EntityModuleFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EcsEntityRepository}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EcsEntityRepository")
class EcsEntityRepositoryTest {

    @Mock
    private ModuleContext moduleContext;

    @Mock
    private EntityComponentStore store;

    private EcsEntityRepository repository;

    @BeforeEach
    void setUp() {
        when(moduleContext.getEntityComponentStore()).thenReturn(store);
        repository = new EcsEntityRepository(moduleContext);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should create entity with core components")
        void shouldCreateEntityWithCoreComponents() {
            long matchId = 1L;
            long createdEntityId = 100L;
            Entity entity = Entity.create(200L, 42L);

            when(store.createEntityForMatch(matchId)).thenReturn(createdEntityId);

            Entity result = repository.save(matchId, entity);

            assertThat(result.id()).isEqualTo(createdEntityId);
            assertThat(result.entityType()).isEqualTo(200L);
            assertThat(result.playerId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("should attach correct component values")
        void shouldAttachCorrectComponentValues() {
            long matchId = 1L;
            long createdEntityId = 100L;
            Entity entity = Entity.create(200L, 42L);

            when(store.createEntityForMatch(matchId)).thenReturn(createdEntityId);

            repository.save(matchId, entity);

            ArgumentCaptor<float[]> valuesCaptor = ArgumentCaptor.forClass(float[].class);
            verify(store).attachComponents(eq(createdEntityId), eq(CORE_COMPONENTS), valuesCaptor.capture());

            float[] values = valuesCaptor.getValue();
            assertThat(values[0]).isEqualTo(200f); // entityType
            assertThat(values[1]).isEqualTo(42f);  // playerId (ownerId)
            assertThat(values[2]).isEqualTo(42f);  // playerId
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return entity when it exists")
        void shouldReturnEntityWhenItExists() {
            long entityId = 100L;
            when(store.getEntitiesWithComponents(List.of(ENTITY_TYPE)))
                    .thenReturn(Set.of(entityId));
            when(store.getComponent(entityId, ENTITY_TYPE)).thenReturn(200f);
            when(store.getComponent(entityId, PLAYER_ID)).thenReturn(42f);

            Optional<Entity> result = repository.findById(entityId);

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(entityId);
            assertThat(result.get().entityType()).isEqualTo(200L);
            assertThat(result.get().playerId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("should return empty when entity does not exist")
        void shouldReturnEmptyWhenEntityDoesNotExist() {
            long entityId = 100L;
            when(store.getEntitiesWithComponents(List.of(ENTITY_TYPE)))
                    .thenReturn(Set.of(200L, 300L));

            Optional<Entity> result = repository.findById(entityId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when no entities exist")
        void shouldReturnEmptyWhenNoEntitiesExist() {
            long entityId = 100L;
            when(store.getEntitiesWithComponents(List.of(ENTITY_TYPE)))
                    .thenReturn(Set.of());

            Optional<Entity> result = repository.findById(entityId);

            assertThat(result).isEmpty();
        }
    }
}
