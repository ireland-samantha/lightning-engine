package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.core.exception.InvalidParameterException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.RigidBody;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Vector3;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.RigidBodyService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.AttachRigidBodyPayload;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Command to attach rigid body components to an entity.
 */
@Slf4j
public final class AttachRigidBodyCommand {

    private AttachRigidBodyCommand() {}

    /**
     * Creates a new attachRigidBody command.
     *
     * @param rigidBodyService the rigid body service
     * @return the configured EngineCommand
     */
    public static EngineCommand create(RigidBodyService rigidBodyService) {
        Map<String, Class<?>> schema = new HashMap<>();
        schema.put("entityId", Long.class);
        schema.put("positionX", Float.class);
        schema.put("positionY", Float.class);
        schema.put("positionZ", Float.class);
        schema.put("velocityX", Float.class);
        schema.put("velocityY", Float.class);
        schema.put("velocityZ", Float.class);
        schema.put("mass", Float.class);
        schema.put("linearDrag", Float.class);
        schema.put("angularDrag", Float.class);
        schema.put("inertia", Float.class);

        return CommandBuilder.newCommand()
                .withName("attachRigidBody")
                .withSchema(schema)
                .withExecution(payload -> {
                    AttachRigidBodyPayload dto = PayloadMapper.convert(payload, AttachRigidBodyPayload.class);

                    if (!dto.hasEntityId()) {
                        throw new InvalidParameterException("attachRigidBody: missing entityId");
                    }

                    Vector3 position = new Vector3(dto.getPositionX(), dto.getPositionY(), dto.getPositionZ());
                    Vector3 velocity = new Vector3(dto.getVelocityX(), dto.getVelocityY(), dto.getVelocityZ());

                    RigidBody rigidBody = rigidBodyService.attachRigidBody(
                            dto.entityId(),
                            position,
                            velocity,
                            dto.getMass(),
                            dto.getLinearDrag(),
                            dto.getAngularDrag(),
                            dto.getInertia()
                    );

                    log.info("Attached rigid body to entity {}: pos=({},{},{}), vel=({},{},{}), mass={}",
                            rigidBody.entityId(),
                            rigidBody.position().x(), rigidBody.position().y(), rigidBody.position().z(),
                            rigidBody.velocity().x(), rigidBody.velocity().y(), rigidBody.velocity().z(),
                            rigidBody.mass());
                })
                .build();
    }
}
