package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.core.exception.InvalidParameterException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.BoxColliderService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.DeleteBoxColliderPayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Command to delete a box collider from an entity.
 */
@Slf4j
public final class DeleteBoxColliderCommand {

    private DeleteBoxColliderCommand() {}

    /**
     * Creates a new deleteBoxCollider command.
     *
     * @param service the box collider service
     * @return the configured EngineCommand
     */
    public static EngineCommand create(BoxColliderService service) {
        return CommandBuilder.newCommand()
                .withName("deleteBoxCollider")
                .withSchema(Map.of("entityId", Long.class))
                .withExecution(payload -> {
                    DeleteBoxColliderPayload dto = PayloadMapper.convert(payload, DeleteBoxColliderPayload.class);

                    if (!dto.hasEntityId()) {
                        throw new InvalidParameterException("deleteBoxCollider: missing entityId");
                    }

                    service.queueDelete(dto.getEntityId());
                })
                .build();
    }
}
