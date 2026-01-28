package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.HealthService;
import ca.samanthaireland.lightning.engine.ext.modules.dto.DamagePayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Command to deal damage to an entity.
 */
@Slf4j
public final class DamageCommand {

    private DamageCommand() {}

    /**
     * Creates a new damage command.
     *
     * @param healthService the health service for health operations
     * @return the configured EngineCommand
     */
    public static EngineCommand create(HealthService healthService) {
        return CommandBuilder.newCommand()
                .withName("damage")
                .withSchema(Map.of(
                        "entityId", Long.class,
                        "amount", Float.class
                ))
                .withExecution(payload -> {
                    DamagePayload dto = PayloadMapper.convert(payload, DamagePayload.class);

                    if (dto.entityId() == 0 || dto.getAmount() <= 0) {
                        log.warn("damage: invalid entityId or amount");
                        return;
                    }

                    healthService.damage(dto.entityId(), dto.getAmount());
                })
                .build();
    }
}
