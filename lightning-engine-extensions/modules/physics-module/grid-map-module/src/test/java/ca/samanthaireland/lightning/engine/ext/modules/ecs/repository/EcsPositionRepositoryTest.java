package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Position;
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

import static ca.samanthaireland.lightning.engine.ext.modules.GridMapModuleFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EcsPositionRepository}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EcsPositionRepository")
class EcsPositionRepositoryTest {

    @Mock
    private ModuleContext context;

    @Mock
    private EntityComponentStore store;

    private EcsPositionRepository repository;

    @BeforeEach
    void setUp() {
        when(context.getEntityComponentStore()).thenReturn(store);
        repository = new EcsPositionRepository(context);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should attach position components to entity")
        void shouldAttachPositionComponentsToEntity() {
            long entityId = 42L;
            Position position = new Position(5f, 10f, 2f);

            repository.save(entityId, position);

            verify(store).attachComponent(entityId, POSITION_X, 5f);
            verify(store).attachComponent(entityId, POSITION_Y, 10f);
            verify(store).attachComponent(entityId, POSITION_Z, 2f);
        }

        @Test
        @DisplayName("should save position at origin")
        void shouldSavePositionAtOrigin() {
            long entityId = 42L;
            Position position = new Position(0f, 0f, 0f);

            repository.save(entityId, position);

            verify(store).attachComponent(entityId, POSITION_X, 0f);
            verify(store).attachComponent(entityId, POSITION_Y, 0f);
            verify(store).attachComponent(entityId, POSITION_Z, 0f);
        }
    }

    @Nested
    @DisplayName("findByEntityId")
    class FindByEntityId {

        @Test
        @DisplayName("should return position when entity has position components")
        void shouldReturnPositionWhenEntityHasPositionComponents() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(POSITION_X, POSITION_Y, POSITION_Z)))
                    .thenReturn(Set.of(entityId));
            when(store.getComponent(entityId, POSITION_X)).thenReturn(5f);
            when(store.getComponent(entityId, POSITION_Y)).thenReturn(10f);
            when(store.getComponent(entityId, POSITION_Z)).thenReturn(2f);

            Optional<Position> result = repository.findByEntityId(entityId);

            assertThat(result).isPresent();
            assertThat(result.get().x()).isEqualTo(5f);
            assertThat(result.get().y()).isEqualTo(10f);
            assertThat(result.get().z()).isEqualTo(2f);
        }

        @Test
        @DisplayName("should return empty when entity has no position components")
        void shouldReturnEmptyWhenEntityHasNoPositionComponents() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(POSITION_X, POSITION_Y, POSITION_Z)))
                    .thenReturn(Set.of(100L, 200L));

            Optional<Position> result = repository.findByEntityId(entityId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when no entities have position components")
        void shouldReturnEmptyWhenNoEntitiesHavePositionComponents() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(POSITION_X, POSITION_Y, POSITION_Z)))
                    .thenReturn(Set.of());

            Optional<Position> result = repository.findByEntityId(entityId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle position at origin")
        void shouldHandlePositionAtOrigin() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(POSITION_X, POSITION_Y, POSITION_Z)))
                    .thenReturn(Set.of(entityId));
            when(store.getComponent(entityId, POSITION_X)).thenReturn(0f);
            when(store.getComponent(entityId, POSITION_Y)).thenReturn(0f);
            when(store.getComponent(entityId, POSITION_Z)).thenReturn(0f);

            Optional<Position> result = repository.findByEntityId(entityId);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(new Position(0f, 0f, 0f));
        }
    }
}
