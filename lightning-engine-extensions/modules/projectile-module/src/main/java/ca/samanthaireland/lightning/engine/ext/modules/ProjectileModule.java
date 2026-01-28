package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.system.EngineSystem;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.ProjectileRepository;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.ProjectileService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.DestroyProjectileCommand;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.SpawnProjectileCommand;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.repository.EcsProjectileRepository;

import java.util.List;

import static ca.samanthaireland.lightning.engine.ext.modules.ProjectileModuleFactory.ALL_COMPONENTS;
import static ca.samanthaireland.lightning.engine.ext.modules.ProjectileModuleFactory.FLAG;

/**
 * Projectile module implementation.
 *
 * <p>Provides projectile management with:
 * <ul>
 *   <li>Projectile spawning and destruction commands</li>
 *   <li>Movement system based on speed and direction</li>
 *   <li>Lifetime management with auto-destroy</li>
 *   <li>Cleanup system for destroyed projectiles</li>
 * </ul>
 */
public class ProjectileModule implements EngineModule {
    private final ProjectileService projectileService;

    public ProjectileModule(ModuleContext context) {
        ProjectileRepository projectileRepository = new EcsProjectileRepository(
                context.getEntityComponentStore()
        );

        this.projectileService = new ProjectileService(projectileRepository);
    }

    /**
     * Constructor with injected service for testing.
     *
     * @param projectileService the projectile service
     */
    ProjectileModule(ProjectileService projectileService) {
        this.projectileService = projectileService;
    }

    @Override
    public List<EngineSystem> createSystems() {
        return List.of(
                createMovementSystem(),
                createLifetimeSystem(),
                createCleanupSystem()
        );
    }

    @Override
    public List<EngineCommand> createCommands() {
        return List.of(
                SpawnProjectileCommand.create(projectileService),
                DestroyProjectileCommand.create(projectileService)
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
        return "ProjectileModule";
    }

    /**
     * System to move projectiles based on speed and direction.
     */
    private EngineSystem createMovementSystem() {
        return () -> projectileService.processMovement();
    }

    /**
     * System to manage projectile lifetime and auto-destroy.
     */
    private EngineSystem createLifetimeSystem() {
        return () -> projectileService.processLifetime();
    }

    /**
     * System to clean up destroyed projectiles.
     */
    private EngineSystem createCleanupSystem() {
        return () -> projectileService.processCleanup();
    }
}
