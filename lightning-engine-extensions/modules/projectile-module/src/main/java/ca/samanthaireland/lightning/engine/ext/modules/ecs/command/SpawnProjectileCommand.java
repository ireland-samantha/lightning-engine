package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.ProjectileService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.SpawnProjectilePayload;

import java.util.HashMap;
import java.util.Map;

/**
 * Command to spawn a new projectile.
 *
 * <p>Creates a projectile entity with the specified properties.
 */
public final class SpawnProjectileCommand {

    private SpawnProjectileCommand() {}

    /**
     * Creates a new spawnProjectile command.
     *
     * @param projectileService the projectile service for spawning
     * @return the configured EngineCommand
     */
    public static EngineCommand create(ProjectileService projectileService) {
        Map<String, Class<?>> schema = new HashMap<>();
        schema.put("matchId", Long.class);
        schema.put("ownerEntityId", Long.class);
        schema.put("positionX", Float.class);
        schema.put("positionY", Float.class);
        schema.put("directionX", Float.class);
        schema.put("directionY", Float.class);
        schema.put("speed", Float.class);
        schema.put("damage", Float.class);
        schema.put("lifetime", Float.class);
        schema.put("pierceCount", Float.class);
        schema.put("projectileType", Float.class);

        return CommandBuilder.newCommand()
                .withName("spawnProjectile")
                .withSchema(schema)
                .withExecution(payload -> {
                    SpawnProjectilePayload dto = PayloadMapper.convert(payload, SpawnProjectilePayload.class);

                    projectileService.spawnProjectile(
                            dto.matchId(),
                            dto.ownerEntityId(),
                            dto.getPositionX(),
                            dto.getPositionY(),
                            dto.getDirectionX(),
                            dto.getDirectionY(),
                            dto.getSpeed(),
                            dto.getDamage(),
                            dto.getLifetime(),
                            dto.getPierceCount(),
                            dto.getProjectileType()
                    );
                })
                .build();
    }
}
