package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.core.exception.InvalidParameterException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.BoxColliderService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.AttachBoxColliderPayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Command to attach a box collider to an entity.
 */
@Slf4j
public final class AttachBoxColliderCommand {

    private AttachBoxColliderCommand() {}

    /**
     * Creates a new attachBoxCollider command.
     *
     * @param service the box collider service
     * @return the configured EngineCommand
     */
    public static EngineCommand create(BoxColliderService service) {
        return CommandBuilder.newCommand()
                .withName("attachBoxCollider")
                .withSchema(Map.of(
                        "entityId", Long.class,
                        "width", Float.class,
                        "height", Float.class,
                        "depth", Float.class,
                        "offsetX", Float.class,
                        "offsetY", Float.class,
                        "offsetZ", Float.class,
                        "layer", Integer.class,
                        "mask", Integer.class,
                        "isTrigger", Boolean.class
                ))
                .withExecution(payload -> {
                    AttachBoxColliderPayload dto = PayloadMapper.convert(payload, AttachBoxColliderPayload.class);

                    if (!dto.hasEntityId()) {
                        throw new InvalidParameterException("attachBoxCollider: missing entityId");
                    }

                    service.attachCollider(
                            dto.getEntityId(),
                            dto.getWidth(),
                            dto.getHeight(),
                            dto.getDepth(),
                            dto.getOffsetX(),
                            dto.getOffsetY(),
                            dto.getOffsetZ(),
                            dto.getLayer(),
                            dto.getMask(),
                            dto.getIsTrigger()
                    );
                })
                .build();
    }
}
