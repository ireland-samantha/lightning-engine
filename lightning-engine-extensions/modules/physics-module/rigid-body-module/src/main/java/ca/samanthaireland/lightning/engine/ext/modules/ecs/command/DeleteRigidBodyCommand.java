package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.core.exception.InvalidParameterException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.RigidBodyService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.DeleteRigidBodyPayload;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Command to delete rigid body components from an entity.
 *
 * <p>This command queues deletion for the cleanup system.
 */
@Slf4j
public final class DeleteRigidBodyCommand {

    private DeleteRigidBodyCommand() {}

    /**
     * Creates a new deleteRigidBody command.
     *
     * @param rigidBodyService the rigid body service
     * @param deleteQueue the queue for entities pending deletion
     * @return the configured EngineCommand
     */
    public static EngineCommand create(RigidBodyService rigidBodyService, List<Long> deleteQueue) {
        return CommandBuilder.newCommand()
                .withName("deleteRigidBody")
                .withSchema(Map.of("entityId", Long.class))
                .withExecution(payload -> {
                    DeleteRigidBodyPayload dto = PayloadMapper.convert(payload, DeleteRigidBodyPayload.class);

                    if (!dto.hasEntityId()) {
                        throw new InvalidParameterException("deleteRigidBody: missing entityId");
                    }

                    deleteQueue.add(dto.entityId());
                    log.debug("Queued rigid body deletion for entity {}", dto.entityId());
                })
                .build();
    }
}
