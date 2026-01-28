package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.system.EngineSystem;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.module.ModuleExports;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.MapMatchRepository;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.MapRepository;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.PositionRepository;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.MapService;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.PositionService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.AssignMapToMatchCommand;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.CreateMapCommand;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.SetEntityPositionCommand;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.repository.EcsMapMatchRepository;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.repository.EcsMapRepository;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.repository.EcsPositionRepository;

import java.util.List;

import static ca.samanthaireland.lightning.engine.ext.modules.GridMapModuleFactory.ALL_COMPONENTS;
import static ca.samanthaireland.lightning.engine.ext.modules.GridMapModuleFactory.FLAG;

/**
 * Grid map module implementation.
 *
 * <p>Provides grid-based positioning for tile-based games with:
 * <ul>
 *   <li>Grid position components (GRID_POS_X, GRID_POS_Y, GRID_POS_Z)</li>
 *   <li>Map dimension tracking (MAP_WIDTH, MAP_HEIGHT, MAP_DEPTH)</li>
 *   <li>Commands for map creation and position management</li>
 * </ul>
 */
public class GridMapModule implements EngineModule {
    private final ModuleContext context;
    private final MapService mapService;
    private final PositionService positionService;
    private final PositionRepository positionRepository;

    public GridMapModule(ModuleContext context) {
        this.context = context;
        // Repositories now get store dynamically from context to avoid stale reference issues
        MapRepository mapRepository = new EcsMapRepository(context);
        MapMatchRepository mapMatchRepository = new EcsMapMatchRepository();
        this.positionRepository = new EcsPositionRepository(context);

        this.mapService = new MapService(mapRepository, mapMatchRepository);
        this.positionService = new PositionService(mapRepository, positionRepository);
    }

    @Override
    public List<EngineSystem> createSystems() {
        return List.of();
    }

    @Override
    public List<EngineCommand> createCommands() {
        return List.of(
                CreateMapCommand.create(mapService),
                SetEntityPositionCommand.create(positionService),
                AssignMapToMatchCommand.create(mapService)
        );
    }

    @Override
    public List<BaseComponent> createComponents() {
        return ALL_COMPONENTS;
    }

    @Override
    public BaseComponent createFlagComponent() {
        return FLAG;
    }

    @Override
    public String getName() {
        return "GridMapModule";
    }

    @Override
    public List<ModuleExports> getExports() {
        return List.of(new GridMapExports(positionService, positionRepository, mapService));
    }
}
