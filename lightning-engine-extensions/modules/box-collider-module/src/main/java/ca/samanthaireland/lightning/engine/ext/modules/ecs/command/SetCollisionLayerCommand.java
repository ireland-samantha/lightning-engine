package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.core.exception.InvalidParameterException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.BoxColliderService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.SetCollisionLayerPayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Command to set the collision layer and mask.
 */
@Slf4j
public final class SetCollisionLayerCommand {

    private SetCollisionLayerCommand() {}

    /**
     * Creates a new setCollisionLayer command.
     *
     * @param service the box collider service
     * @return the configured EngineCommand
     */
    public static EngineCommand create(BoxColliderService service) {
        return CommandBuilder.newCommand()
                .withName("setCollisionLayer")
                .withSchema(Map.of(
                        "entityId", Long.class,
                        "layer", Integer.class,
                        "mask", Integer.class
                ))
                .withExecution(payload -> {
                    SetCollisionLayerPayload dto = PayloadMapper.convert(payload, SetCollisionLayerPayload.class);

                    if (!dto.hasEntityId()) {
                        throw new InvalidParameterException("setCollisionLayer: missing entityId");
                    }

                    service.setLayerMask(dto.getEntityId(), dto.getLayer(), dto.getMask());
                })
                .build();
    }
}
