package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.ext.modules.domain.GridMap;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.MapService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.CreateGridMapPayload;
import lombok.extern.slf4j.Slf4j;

/**
 * Command to create a grid map entity.
 *
 * <p>Creates an entity that represents a grid map with specified dimensions.
 * This map entity can be used to validate grid positions.
 */
@Slf4j
public final class CreateMapCommand {

    private CreateMapCommand() {}

    /**
     * Creates a new createMap command.
     *
     * @param mapService the map service for map creation
     * @return the configured EngineCommand
     */
    public static EngineCommand create(MapService mapService) {
        return CommandBuilder.newCommand()
                .withName("createMap")
                .withSchema(java.util.Map.of(
                        "matchId", Long.class,
                        "width", Integer.class,
                        "height", Integer.class,
                        "depth", Integer.class
                ))
                .withExecution(payload -> {
                    CreateGridMapPayload dto = PayloadMapper.convert(payload, CreateGridMapPayload.class);

                    GridMap createdGridMap =
                            mapService.createMap(
                                dto.matchId(),
                                dto.getWidth(),
                                dto.getHeight(),
                                dto.getDepth());

                    log.info("Created grid map entity {} with dimensions {}x{}x{} for match {}",
                            createdGridMap.id(), createdGridMap.width(), createdGridMap.height(), createdGridMap.depth(), dto.matchId());
                })
                .build();
    }
}
