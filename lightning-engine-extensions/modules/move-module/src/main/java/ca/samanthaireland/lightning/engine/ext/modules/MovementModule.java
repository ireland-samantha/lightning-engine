package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.system.EngineSystem;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.MovementRepository;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.MovementService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.AttachMovementCommand;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.DeleteMoveableCommand;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.repository.EcsMovementRepository;

import java.util.List;

import static ca.samanthaireland.lightning.engine.ext.modules.MoveModuleFactory.ALL_COMPONENTS;
import static ca.samanthaireland.lightning.engine.ext.modules.MoveModuleFactory.MODULE;

/**
 * Movement module implementation.
 *
 * <p>Provides velocity-based movement for entities with:
 * <ul>
 *   <li>Position components (POSITION_X, POSITION_Y, POSITION_Z)</li>
 *   <li>Velocity components (VELOCITY_X, VELOCITY_Y, VELOCITY_Z)</li>
 *   <li>Commands for attaching and removing movement</li>
 *   <li>Systems for applying velocity to position each tick</li>
 * </ul>
 *
 * @deprecated use RigidBodyModule
 */
@Deprecated
public class MovementModule implements EngineModule {
    private final MovementService movementService;

    public MovementModule(ModuleContext context) {
        MovementRepository movementRepository = new EcsMovementRepository(
                context.getEntityComponentStore()
        );

        this.movementService = new MovementService(movementRepository);
    }

    @Override
    public List<EngineSystem> createSystems() {
        return List.of(
                movementService::applyVelocities,
                movementService::processDeletions
        );
    }

    @Override
    public List<EngineCommand> createCommands() {
        return List.of(
                AttachMovementCommand.create(movementService),
                DeleteMoveableCommand.create(movementService)
        );
    }

    @Override
    public List<BaseComponent> createComponents() {
        return ALL_COMPONENTS;
    }

    @Override
    public BaseComponent createFlagComponent() {
        return MODULE;
    }

    @Override
    public String getName() {
        return "MoveModule";
    }
}
