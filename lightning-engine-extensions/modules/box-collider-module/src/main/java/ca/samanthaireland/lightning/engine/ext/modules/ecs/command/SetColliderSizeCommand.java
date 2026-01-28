package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.core.exception.InvalidParameterException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.BoxColliderService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.SetColliderSizePayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Command to set the size of a box collider.
 */
@Slf4j
public final class SetColliderSizeCommand {

    private SetColliderSizeCommand() {}

    /**
     * Creates a new setColliderSize command.
     *
     * @param service the box collider service
     * @return the configured EngineCommand
     */
    public static EngineCommand create(BoxColliderService service) {
        return CommandBuilder.newCommand()
                .withName("setColliderSize")
                .withSchema(Map.of(
                        "entityId", Long.class,
                        "width", Float.class,
                        "height", Float.class
                ))
                .withExecution(payload -> {
                    SetColliderSizePayload dto = PayloadMapper.convert(payload, SetColliderSizePayload.class);

                    if (!dto.hasEntityId()) {
                        throw new InvalidParameterException("setColliderSize: missing entityId");
                    }

                    service.setSize(dto.getEntityId(), dto.getWidth(), dto.getHeight());
                })
                .build();
    }
}
