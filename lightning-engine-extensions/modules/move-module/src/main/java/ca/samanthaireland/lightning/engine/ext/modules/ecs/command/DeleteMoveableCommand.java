package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.MovementService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.DeleteMoveablePayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Command to queue an entity for movement removal.
 *
 * @deprecated use RigidBodyModule
 */
@Slf4j
@Deprecated
public final class DeleteMoveableCommand {

    private DeleteMoveableCommand() {}

    /**
     * Creates a new DeleteMoveable command.
     *
     * @param movementService the movement service for queuing deletions
     * @return the configured EngineCommand
     */
    public static EngineCommand create(MovementService movementService) {
        return CommandBuilder.newCommand()
                .withName("DeleteMoveable")
                .withSchema(Map.of("id", Long.class))
                .withExecution(payload -> {
                    DeleteMoveablePayload dto = PayloadMapper.convert(payload, DeleteMoveablePayload.class);
                    movementService.queueForDeletion(dto.idOrDefault());
                })
                .build();
    }
}
