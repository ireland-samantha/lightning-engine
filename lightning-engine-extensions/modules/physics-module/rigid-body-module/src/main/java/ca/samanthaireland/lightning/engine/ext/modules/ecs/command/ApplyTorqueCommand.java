package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.core.exception.InvalidParameterException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.PhysicsService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.ApplyTorquePayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Command to apply torque to a rigid body.
 */
@Slf4j
public final class ApplyTorqueCommand {

    private ApplyTorqueCommand() {}

    /**
     * Creates a new applyTorque command.
     *
     * @param physicsService the physics service
     * @return the configured EngineCommand
     */
    public static EngineCommand create(PhysicsService physicsService) {
        return CommandBuilder.newCommand()
                .withName("applyTorque")
                .withSchema(Map.of(
                        "entityId", Long.class,
                        "torque", Float.class
                ))
                .withExecution(payload -> {
                    ApplyTorquePayload dto = PayloadMapper.convert(payload, ApplyTorquePayload.class);

                    if (!dto.hasEntityId()) {
                        throw new InvalidParameterException("applyTorque: missing entityId");
                    }

                    physicsService.applyTorque(dto.entityId(), dto.getTorque());

                    log.debug("Applied torque {} to entity {}", dto.getTorque(), dto.entityId());
                })
                .build();
    }
}
