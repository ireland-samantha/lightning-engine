package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.ProjectileService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.DestroyProjectilePayload;

import java.util.Map;

/**
 * Command to destroy a projectile.
 *
 * <p>Marks a projectile for destruction, which will be cleaned up on the next tick.
 */
public final class DestroyProjectileCommand {

    private DestroyProjectileCommand() {}

    /**
     * Creates a new destroyProjectile command.
     *
     * @param projectileService the projectile service for destruction
     * @return the configured EngineCommand
     */
    public static EngineCommand create(ProjectileService projectileService) {
        return CommandBuilder.newCommand()
                .withName("destroyProjectile")
                .withSchema(Map.of("entityId", Long.class))
                .withExecution(payload -> {
                    DestroyProjectilePayload dto = PayloadMapper.convert(payload, DestroyProjectilePayload.class);
                    projectileService.destroyProjectile(dto.entityId());
                })
                .build();
    }
}
