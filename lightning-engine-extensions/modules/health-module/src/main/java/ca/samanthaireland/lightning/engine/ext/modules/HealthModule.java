package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.system.EngineSystem;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.HealthRepository;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.HealthService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.AttachHealthCommand;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.DamageCommand;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.HealCommand;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.SetInvulnerableCommand;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.repository.EcsHealthRepository;

import java.util.List;

import static ca.samanthaireland.lightning.engine.ext.modules.HealthModuleFactory.ALL_COMPONENTS;
import static ca.samanthaireland.lightning.engine.ext.modules.HealthModuleFactory.FLAG;

/**
 * Health module implementation.
 *
 * <p>Provides health/damage management:
 * <ul>
 *   <li>CURRENT_HP - Current hit points</li>
 *   <li>MAX_HP - Maximum hit points</li>
 *   <li>DAMAGE_TAKEN - Damage accumulated this tick (cleared after processing)</li>
 *   <li>IS_DEAD - Flag indicating entity has died (HP <= 0)</li>
 *   <li>INVULNERABLE - Flag to prevent damage</li>
 * </ul>
 */
public class HealthModule implements EngineModule {
    private final HealthService healthService;

    public HealthModule(ModuleContext context) {
        HealthRepository healthRepository = new EcsHealthRepository(
                context.getEntityComponentStore()
        );

        this.healthService = new HealthService(healthRepository);
    }

    @Override
    public List<EngineSystem> createSystems() {
        return List.of(healthService::processDamage);
    }

    @Override
    public List<EngineCommand> createCommands() {
        return List.of(
                AttachHealthCommand.create(healthService),
                DamageCommand.create(healthService),
                HealCommand.create(healthService),
                SetInvulnerableCommand.create(healthService)
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
        return "HealthModule";
    }
}
