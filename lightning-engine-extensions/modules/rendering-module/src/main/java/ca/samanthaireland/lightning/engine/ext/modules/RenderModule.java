package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.system.EngineSystem;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.SpriteRepository;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.SpriteService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.AttachSpriteCommand;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.repository.EcsSpriteRepository;

import java.util.List;

import static ca.samanthaireland.lightning.engine.ext.modules.RenderingModuleFactory.ALL_SPRITE_COMPONENTS;
import static ca.samanthaireland.lightning.engine.ext.modules.RenderingModuleFactory.FLAG;

/**
 * Render module implementation.
 *
 * <p>Provides sprite rendering capabilities for entities with:
 * <ul>
 *   <li>Sprite components (resource ID, dimensions, rotation, z-index, visibility)</li>
 *   <li>Commands for attaching sprites to entities</li>
 * </ul>
 */
public class RenderModule implements EngineModule {
    private final SpriteService spriteService;

    public RenderModule(ModuleContext context) {
        SpriteRepository spriteRepository = new EcsSpriteRepository(
                context.getEntityComponentStore()
        );

        this.spriteService = new SpriteService(spriteRepository);
    }

    @Override
    public List<EngineSystem> createSystems() {
        return List.of();
    }

    @Override
    public List<EngineCommand> createCommands() {
        return List.of(
                AttachSpriteCommand.create(spriteService)
        );
    }

    @Override
    public List<BaseComponent> createComponents() {
        return ALL_SPRITE_COMPONENTS;
    }

    @Override
    public BaseComponent createFlagComponent() {
        return FLAG;
    }

    @Override
    public String getName() {
        return "RenderModule";
    }
}
