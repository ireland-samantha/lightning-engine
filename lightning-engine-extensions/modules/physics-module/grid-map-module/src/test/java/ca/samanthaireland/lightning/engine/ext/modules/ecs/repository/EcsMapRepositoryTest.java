package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.modules.domain.GridMap;
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

import static ca.samanthaireland.lightning.engine.ext.modules.GridMapModuleFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EcsMapRepository}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EcsMapRepository")
class EcsMapRepositoryTest {

    @Mock
    private ModuleContext context;

    @Mock
    private EntityComponentStore store;

    private EcsMapRepository repository;

    @BeforeEach
    void setUp() {
        when(context.getEntityComponentStore()).thenReturn(store);
        repository = new EcsMapRepository(context);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should create entity with map components")
        void shouldCreateEntityWithMapComponents() {
            long matchId = 1L;
            long createdEntityId = 100L;
            GridMap gridMap = GridMap.create(20, 15, 3);

            when(store.createEntityForMatch(matchId)).thenReturn(createdEntityId);

            GridMap result = repository.save(matchId, gridMap);

            assertThat(result.id()).isEqualTo(createdEntityId);
            assertThat(result.width()).isEqualTo(20);
            assertThat(result.height()).isEqualTo(15);
            assertThat(result.depth()).isEqualTo(3);
        }

        @Test
        @DisplayName("should attach correct component values")
        void shouldAttachCorrectComponentValues() {
            long matchId = 1L;
            long createdEntityId = 100L;
            GridMap gridMap = GridMap.create(20, 15, 3);

            when(store.createEntityForMatch(matchId)).thenReturn(createdEntityId);

            repository.save(matchId, gridMap);

            ArgumentCaptor<float[]> valuesCaptor = ArgumentCaptor.forClass(float[].class);
            verify(store).attachComponents(eq(createdEntityId), eq(MAP_COMPONENTS), valuesCaptor.capture());

            float[] values = valuesCaptor.getValue();
            assertThat(values[0]).isEqualTo(20f);
            assertThat(values[1]).isEqualTo(15f);
            assertThat(values[2]).isEqualTo(3f);
            assertThat(values[3]).isEqualTo(1.0f); // MAP_ENTITY marker
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return map when entity exists")
        void shouldReturnMapWhenEntityExists() {
            long mapId = 100L;
            when(store.getEntitiesWithComponents(List.of(MAP_ENTITY)))
                    .thenReturn(Set.of(mapId));
            when(store.getComponent(mapId, MAP_WIDTH)).thenReturn(20f);
            when(store.getComponent(mapId, MAP_HEIGHT)).thenReturn(15f);
            when(store.getComponent(mapId, MAP_DEPTH)).thenReturn(3f);

            Optional<GridMap> result = repository.findById(mapId);

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(mapId);
            assertThat(result.get().width()).isEqualTo(20);
            assertThat(result.get().height()).isEqualTo(15);
            assertThat(result.get().depth()).isEqualTo(3);
        }

        @Test
        @DisplayName("should return empty when entity does not exist")
        void shouldReturnEmptyWhenEntityDoesNotExist() {
            long mapId = 100L;
            when(store.getEntitiesWithComponents(List.of(MAP_ENTITY)))
                    .thenReturn(Set.of(200L, 300L));

            Optional<GridMap> result = repository.findById(mapId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when no map entities exist")
        void shouldReturnEmptyWhenNoMapEntitiesExist() {
            long mapId = 100L;
            when(store.getEntitiesWithComponents(List.of(MAP_ENTITY)))
                    .thenReturn(Set.of());

            Optional<GridMap> result = repository.findById(mapId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("should return true when map exists")
        void shouldReturnTrueWhenMapExists() {
            long mapId = 100L;
            when(store.getEntitiesWithComponents(List.of(MAP_ENTITY)))
                    .thenReturn(Set.of(mapId, 200L));

            boolean result = repository.exists(mapId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when map does not exist")
        void shouldReturnFalseWhenMapDoesNotExist() {
            long mapId = 100L;
            when(store.getEntitiesWithComponents(List.of(MAP_ENTITY)))
                    .thenReturn(Set.of(200L, 300L));

            boolean result = repository.exists(mapId);

            assertThat(result).isFalse();
        }
    }
}
