package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.core.exception.InvalidParameterException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Position;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.PositionService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.SetGridPositionPayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Command to set grid position on an entity with bounds checking.
 */
@Slf4j
public final class SetEntityPositionCommand {

    private SetEntityPositionCommand() {}

    /**
     * Creates a new setEntityPosition command.
     *
     * @param positionService the position service for position operations
     * @return the configured EngineCommand
     */
    public static EngineCommand create(PositionService positionService) {
        return CommandBuilder.newCommand()
                .withName("setEntityPosition")
                .withSchema(Map.of(
                        "entityId", Long.class,
                        "mapId", Long.class,
                        "gridX", Integer.class,
                        "gridY", Integer.class,
                        "gridZ", Integer.class
                ))
                .withExecution(payload -> {
                    SetGridPositionPayload dto = PayloadMapper.convert(payload, SetGridPositionPayload.class);

                    validateRequest(dto);

                    Position position = new Position(
                            dto.xOrDefault(),
                            dto.yOrDefault(),
                            dto.zOrDefault()
                    );

                    positionService.setPosition(dto.entityId(), dto.mapId(), position);

                    log.debug("Set grid position ({}, {}, {}) for entity {}",
                            position.x(), position.y(), position.z(), dto.entityId());
                })
                .build();
    }

    private static void validateRequest(SetGridPositionPayload dto) {
        if (!dto.hasEntityId()) {
            throw new InvalidParameterException("setEntityPosition: missing entityId");
        }

        if (!dto.hasMapId()) {
            throw new InvalidParameterException("setEntityPosition: missing mapId");
        }
    }
}
