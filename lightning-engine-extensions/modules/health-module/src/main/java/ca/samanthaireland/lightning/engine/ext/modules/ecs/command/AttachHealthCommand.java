package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.HealthService;
import ca.samanthaireland.lightning.engine.ext.modules.dto.AttachHealthPayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Command to attach health to an entity.
 */
@Slf4j
public final class AttachHealthCommand {

    private AttachHealthCommand() {}

    /**
     * Creates a new attachHealth command.
     *
     * @param healthService the health service for health operations
     * @return the configured EngineCommand
     */
    public static EngineCommand create(HealthService healthService) {
        return CommandBuilder.newCommand()
                .withName("attachHealth")
                .withSchema(Map.of(
                        "entityId", Long.class,
                        "maxHP", Float.class,
                        "currentHP", Float.class
                ))
                .withExecution(payload -> {
                    AttachHealthPayload dto = PayloadMapper.convert(payload, AttachHealthPayload.class);

                    if (dto.entityId() == 0) {
                        log.warn("attachHealth: missing entityId");
                        return;
                    }

                    healthService.attachHealth(dto.entityId(), dto.getMaxHP(), dto.getCurrentHP());
                })
                .build();
    }
}
