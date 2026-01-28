package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.command.CommandPayload;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GridMapModuleFactory}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GridMapModuleFactory")
class GridMapModuleFactoryTest {

    @Mock
    private ModuleContext context;

    @Mock
    private EntityComponentStore entityComponentStore;

    private GridMapModuleFactory factory;
    private EngineModule module;

    @BeforeEach
    void setUp() {
        factory = new GridMapModuleFactory();
        lenient().when(context.getEntityComponentStore()).thenReturn(entityComponentStore);
        module = factory.create(context);
    }

    @Nested
    @DisplayName("Module Creation")
    class ModuleCreation {

        @Test
        @DisplayName("should create module with correct name")
        void shouldCreateModuleWithCorrectName() {
            assertThat(module.getName()).isEqualTo("GridMapModule");
        }

        @Test
        @DisplayName("should provide all grid position components")
        void shouldProvideAllGridPositionComponents() {
            List<BaseComponent> components = module.createComponents();

            assertThat(components).contains(
                    GridMapModuleFactory.GRID_POS_X,
                    GridMapModuleFactory.GRID_POS_Y,
                    GridMapModuleFactory.GRID_POS_Z
            );
        }

        @Test
        @DisplayName("should provide all map components")
        void shouldProvideAllMapComponents() {
            List<BaseComponent> components = module.createComponents();

            assertThat(components).contains(
                    GridMapModuleFactory.MAP_WIDTH,
                    GridMapModuleFactory.MAP_HEIGHT,
                    GridMapModuleFactory.MAP_DEPTH,
                    GridMapModuleFactory.MAP_ENTITY
            );
        }

        @Test
        @DisplayName("should provide flag component")
        void shouldProvideFlagComponent() {
            BaseComponent flag = module.createFlagComponent();

            assertThat(flag).isEqualTo(GridMapModuleFactory.FLAG);
            assertThat(flag.getName()).isEqualTo("gridmap");
        }

        @Test
        @DisplayName("should create three commands")
        void shouldCreateThreeCommands() {
            List<EngineCommand> commands = module.createCommands();

            assertThat(commands).hasSize(3);
            assertThat(commands.stream().map(EngineCommand::getName))
                    .containsExactlyInAnyOrder("createMap", "setEntityPosition", "assignMapToMatch");
        }

        @Test
        @DisplayName("should create no systems")
        void shouldCreateNoSystems() {
            assertThat(module.createSystems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Component Definitions")
    class ComponentDefinitions {

        @Test
        @DisplayName("grid position components should have unique IDs")
        void gridPositionComponentsShouldHaveUniqueIds() {
            assertThat(GridMapModuleFactory.GRID_POS_X.getId())
                    .isNotEqualTo(GridMapModuleFactory.GRID_POS_Y.getId())
                    .isNotEqualTo(GridMapModuleFactory.GRID_POS_Z.getId());
            assertThat(GridMapModuleFactory.GRID_POS_Y.getId())
                    .isNotEqualTo(GridMapModuleFactory.GRID_POS_Z.getId());
        }

        @Test
        @DisplayName("map components should have unique IDs")
        void mapComponentsShouldHaveUniqueIds() {
            assertThat(GridMapModuleFactory.MAP_WIDTH.getId())
                    .isNotEqualTo(GridMapModuleFactory.MAP_HEIGHT.getId())
                    .isNotEqualTo(GridMapModuleFactory.MAP_DEPTH.getId());
            assertThat(GridMapModuleFactory.MAP_HEIGHT.getId())
                    .isNotEqualTo(GridMapModuleFactory.MAP_DEPTH.getId());
        }

        @Test
        @DisplayName("GRID_POSITION_COMPONENTS list should be immutable")
        void gridPositionComponentsListShouldBeImmutable() {
            List<BaseComponent> components = GridMapModuleFactory.GRID_POSITION_COMPONENTS;
            assertThat(components).hasSize(3);
        }
    }

    @Nested
    @DisplayName("createMap Command")
    class CreateGridMapCommandTests {

        private EngineCommand command;

        @BeforeEach
        void setUp() {
            command = module.createCommands().stream()
                    .filter(c -> c.getName().equals("createMap"))
                    .findFirst()
                    .orElseThrow();
        }

        @Test
        @DisplayName("should create map entity with specified dimensions")
        void shouldCreateMapEntityWithSpecifiedDimensions() {
            long createdEntityId = 100L;
            when(entityComponentStore.createEntityForMatch(1L)).thenReturn(createdEntityId);

            CommandPayload payload = createPayload(Map.of(
                    "matchId", 1L,
                    "width", 20,
                    "height", 15,
                    "depth", 3
            ));

            command.executeCommand(payload);

            verify(entityComponentStore).createEntityForMatch(1L);
            ArgumentCaptor<float[]> valuesCaptor = ArgumentCaptor.forClass(float[].class);
            verify(entityComponentStore).attachComponents(eq(createdEntityId), eq(GridMapModuleFactory.MAP_COMPONENTS), valuesCaptor.capture());

            float[] values = valuesCaptor.getValue();
            assertThat(values[0]).isEqualTo(20f);
            assertThat(values[1]).isEqualTo(15f);
            assertThat(values[2]).isEqualTo(3f);
            assertThat(values[3]).isEqualTo(1.0f);
        }
    }

    @Nested
    @DisplayName("setEntityPosition Command")
    class SetEntityPositionCommandTests {

        private EngineCommand command;

        @BeforeEach
        void setUp() {
            command = module.createCommands().stream()
                    .filter(c -> c.getName().equals("setEntityPosition"))
                    .findFirst()
                    .orElseThrow();
        }

        @Test
        @DisplayName("should set position when map exists and position is valid")
        void shouldSetPositionWhenMapExistsAndPositionIsValid() {
            long mapEntityId = 100L;
            when(entityComponentStore.getEntitiesWithComponents(List.of(GridMapModuleFactory.MAP_ENTITY)))
                    .thenReturn(Set.of(mapEntityId));
            when(entityComponentStore.getComponent(mapEntityId, GridMapModuleFactory.MAP_WIDTH)).thenReturn(10f);
            when(entityComponentStore.getComponent(mapEntityId, GridMapModuleFactory.MAP_HEIGHT)).thenReturn(10f);
            when(entityComponentStore.getComponent(mapEntityId, GridMapModuleFactory.MAP_DEPTH)).thenReturn(5f);

            CommandPayload payload = createPayload(Map.of(
                    "entityId", 42L,
                    "mapId", mapEntityId,
                    "gridX", 5,
                    "gridY", 5,
                    "gridZ", 2
            ));

            command.executeCommand(payload);

            verify(entityComponentStore).attachComponent(42L, GridMapModuleFactory.POSITION_X, 5f);
            verify(entityComponentStore).attachComponent(42L, GridMapModuleFactory.POSITION_Y, 5f);
            verify(entityComponentStore).attachComponent(42L, GridMapModuleFactory.POSITION_Z, 2f);
        }

        @Test
        @DisplayName("should allow position at origin")
        void shouldAllowPositionAtOrigin() {
            long mapEntityId = 100L;
            when(entityComponentStore.getEntitiesWithComponents(List.of(GridMapModuleFactory.MAP_ENTITY)))
                    .thenReturn(Set.of(mapEntityId));
            when(entityComponentStore.getComponent(mapEntityId, GridMapModuleFactory.MAP_WIDTH)).thenReturn(10f);
            when(entityComponentStore.getComponent(mapEntityId, GridMapModuleFactory.MAP_HEIGHT)).thenReturn(10f);
            when(entityComponentStore.getComponent(mapEntityId, GridMapModuleFactory.MAP_DEPTH)).thenReturn(5f);

            CommandPayload payload = createPayload(Map.of(
                    "entityId", 42L,
                    "mapId", mapEntityId,
                    "gridX", 0,
                    "gridY", 0,
                    "gridZ", 0
            ));

            command.executeCommand(payload);

            verify(entityComponentStore).attachComponent(42L, GridMapModuleFactory.POSITION_X, 0f);
            verify(entityComponentStore).attachComponent(42L, GridMapModuleFactory.POSITION_Y, 0f);
            verify(entityComponentStore).attachComponent(42L, GridMapModuleFactory.POSITION_Z, 0f);
        }
    }

    @Nested
    @DisplayName("Command Schema")
    class CommandSchema {

        @Test
        @DisplayName("createMap should have correct schema")
        void createMapShouldHaveCorrectSchema() {
            EngineCommand command = module.createCommands().stream()
                    .filter(c -> c.getName().equals("createMap"))
                    .findFirst()
                    .orElseThrow();

            Map<String, Class<?>> schema = command.schema();
            assertThat(schema).containsKeys("matchId", "width", "height", "depth");
        }

        @Test
        @DisplayName("setEntityPosition should have correct schema")
        void setEntityPositionShouldHaveCorrectSchema() {
            EngineCommand command = module.createCommands().stream()
                    .filter(c -> c.getName().equals("setEntityPosition"))
                    .findFirst()
                    .orElseThrow();

            Map<String, Class<?>> schema = command.schema();
            assertThat(schema).containsKeys("entityId", "mapId", "gridX", "gridY", "gridZ");
        }
    }

    private CommandPayload createPayload(Map<String, Object> data) {
        return new CommandPayload() {
            @Override
            public Map<String, Object> getPayload() {
                return data;
            }
        };
    }
}
