package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.HealthService;
import ca.samanthaireland.lightning.engine.ext.modules.dto.SetInvulnerablePayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Command to set invulnerability on an entity.
 */
@Slf4j
public final class SetInvulnerableCommand {

    private SetInvulnerableCommand() {}

    /**
     * Creates a new setInvulnerable command.
     *
     * @param healthService the health service for health operations
     * @return the configured EngineCommand
     */
    public static EngineCommand create(HealthService healthService) {
        return CommandBuilder.newCommand()
                .withName("setInvulnerable")
                .withSchema(Map.of(
                        "entityId", Long.class,
                        "invulnerable", Float.class
                ))
                .withExecution(payload -> {
                    SetInvulnerablePayload dto = PayloadMapper.convert(payload, SetInvulnerablePayload.class);

                    if (dto.entityId() == 0) {
                        log.warn("setInvulnerable: missing entityId");
                        return;
                    }

                    healthService.setInvulnerable(dto.entityId(), dto.isInvulnerable());
                })
                .build();
    }
}
