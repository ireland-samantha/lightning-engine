package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.EntityService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.SpawnPayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Command to spawn a new entity.
 *
 * <p>Note: Position is not set by this command. Use GridMapModule's
 * SetEntityPositionCommand to set the entity's position after spawning.
 */
@Slf4j
public final class SpawnCommand {

    private SpawnCommand() {}

    /**
     * Creates a new spawn command.
     *
     * @param entityService the entity service for spawn operations
     * @return the configured EngineCommand
     */
    public static EngineCommand create(EntityService entityService) {
        return CommandBuilder.newCommand()
                .withName("spawn")
                .withSchema(Map.of(
                        "matchId", Long.class,
                        "playerId", Long.class,
                        "entityType", Long.class
                ))
                .withExecution(payload -> {
                    SpawnPayload dto = PayloadMapper.convert(payload, SpawnPayload.class);

                    entityService.spawn(
                            dto.matchId(),
                            dto.entityType(),
                            dto.playerId()
                    );
                })
                .build();
    }
}
