package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Position;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Velocity;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.MovementService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.AttachMovementPayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Command to attach movement components to an entity.
 *
 * @deprecated use RigidBodyModule
 */
@Slf4j
@Deprecated
public final class AttachMovementCommand {

    private AttachMovementCommand() {}

    /**
     * Creates a new attachMovement command.
     *
     * @param movementService the movement service for attaching movement
     * @return the configured EngineCommand
     */
    public static EngineCommand create(MovementService movementService) {
        return CommandBuilder.newCommand()
                .withName("attachMovement")
                .withSchema(Map.of(
                        "entityId", Long.class,
                        "positionX", Long.class,
                        "positionY", Long.class,
                        "positionZ", Long.class,
                        "velocityX", Long.class,
                        "velocityY", Long.class,
                        "velocityZ", Long.class
                ))
                .withExecution(payload -> {
                    AttachMovementPayload dto = PayloadMapper.convert(payload, AttachMovementPayload.class);

                    if (!dto.hasEntityId()) {
                        log.warn("attachMovement command missing required entityId");
                        return;
                    }

                    Position position = Position.of(
                            dto.positionXOrDefault(),
                            dto.positionYOrDefault(),
                            dto.positionZOrDefault()
                    );

                    Velocity velocity = Velocity.of(
                            dto.velocityXOrDefault(),
                            dto.velocityYOrDefault(),
                            dto.velocityZOrDefault()
                    );

                    movementService.attachMovement(dto.entityIdOrDefault(), position, velocity);
                })
                .build();
    }
}
