package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.MapService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.AssignMapToMatchPayload;
import lombok.extern.slf4j.Slf4j;

/**
 * Command to assign a map to a match.
 *
 * <p>Associates an existing map entity with a match. Each match can have
 * at most one assigned map. If the match already has an assigned map,
 * it will be replaced.
 */
@Slf4j
public final class AssignMapToMatchCommand {

    private AssignMapToMatchCommand() {}

    /**
     * Creates a new assignMapToMatch command.
     *
     * @param mapService the map service for map assignment
     * @return the configured EngineCommand
     */
    public static EngineCommand create(MapService mapService) {
        return CommandBuilder.newCommand()
                .withName("assignMapToMatch")
                .withSchema(java.util.Map.of(
                        "matchId", Long.class,
                        "mapId", Long.class
                ))
                .withExecution(payload -> {
                    AssignMapToMatchPayload dto = PayloadMapper.convert(payload, AssignMapToMatchPayload.class);

                    mapService.assignMapToMatch(dto.matchId(), dto.mapId());

                    log.info("Assigned map {} to match {}", dto.mapId(), dto.matchId());
                })
                .build();
    }
}
