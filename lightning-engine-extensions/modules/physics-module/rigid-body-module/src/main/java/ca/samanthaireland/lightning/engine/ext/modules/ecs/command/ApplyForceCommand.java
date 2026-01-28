package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.core.exception.InvalidParameterException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Vector3;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.PhysicsService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.ApplyForcePayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Command to apply a force to a rigid body.
 */
@Slf4j
public final class ApplyForceCommand {

    private ApplyForceCommand() {}

    /**
     * Creates a new applyForce command.
     *
     * @param physicsService the physics service
     * @return the configured EngineCommand
     */
    public static EngineCommand create(PhysicsService physicsService) {
        return CommandBuilder.newCommand()
                .withName("applyForce")
                .withSchema(Map.of(
                        "entityId", Long.class,
                        "forceX", Float.class,
                        "forceY", Float.class,
                        "forceZ", Float.class
                ))
                .withExecution(payload -> {
                    ApplyForcePayload dto = PayloadMapper.convert(payload, ApplyForcePayload.class);

                    if (!dto.hasEntityId()) {
                        throw new InvalidParameterException("applyForce: missing entityId");
                    }

                    Vector3 force = new Vector3(dto.getForceX(), dto.getForceY(), dto.getForceZ());
                    physicsService.applyForce(dto.entityId(), force);

                    log.debug("Applied force ({},{},{}) to entity {}",
                            force.x(), force.y(), force.z(), dto.entityId());
                })
                .build();
    }
}
