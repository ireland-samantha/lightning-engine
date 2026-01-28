package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.core.exception.InvalidParameterException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.BoxColliderService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.AttachCollisionHandlerPayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Command to attach a collision handler to an entity.
 */
@Slf4j
public final class AttachCollisionHandlerCommand {

    private AttachCollisionHandlerCommand() {}

    /**
     * Creates a new attachCollisionHandler command.
     *
     * @param service the box collider service
     * @return the configured EngineCommand
     */
    public static EngineCommand create(BoxColliderService service) {
        return CommandBuilder.newCommand()
                .withName("attachCollisionHandler")
                .withSchema(Map.of(
                        "entityId", Long.class,
                        "handlerType", Integer.class,
                        "param1", Float.class,
                        "param2", Float.class
                ))
                .withExecution(payload -> {
                    AttachCollisionHandlerPayload dto = PayloadMapper.convert(payload, AttachCollisionHandlerPayload.class);

                    if (!dto.hasEntityId()) {
                        throw new InvalidParameterException("attachCollisionHandler: missing entityId");
                    }

                    service.attachHandler(
                            dto.getEntityId(),
                            dto.getHandlerType(),
                            dto.getParam1(),
                            dto.getParam2()
                    );
                })
                .build();
    }
}
