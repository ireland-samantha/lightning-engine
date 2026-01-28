package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.core.exception.InvalidParameterException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Vector3;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.PhysicsService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.SetPositionPayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Command to set the position of a rigid body directly (teleport).
 */
@Slf4j
public final class SetPositionCommand {

    private SetPositionCommand() {}

    /**
     * Creates a new setPosition command.
     *
     * @param physicsService the physics service
     * @return the configured EngineCommand
     */
    public static EngineCommand create(PhysicsService physicsService) {
        return CommandBuilder.newCommand()
                .withName("setPosition")
                .withSchema(Map.of(
                        "entityId", Long.class,
                        "positionX", Float.class,
                        "positionY", Float.class,
                        "positionZ", Float.class
                ))
                .withExecution(payload -> {
                    SetPositionPayload dto = PayloadMapper.convert(payload, SetPositionPayload.class);

                    if (!dto.hasEntityId()) {
                        throw new InvalidParameterException("setPosition: missing entityId");
                    }

                    Vector3 position = new Vector3(dto.getPositionX(), dto.getPositionY(), dto.getPositionZ());
                    physicsService.setPosition(dto.entityId(), position);

                    log.debug("Set position ({},{},{}) for entity {}",
                            position.x(), position.y(), position.z(), dto.entityId());
                })
                .build();
    }
}
