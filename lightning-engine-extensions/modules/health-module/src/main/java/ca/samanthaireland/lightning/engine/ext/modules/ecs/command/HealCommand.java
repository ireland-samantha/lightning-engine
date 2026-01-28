package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.HealthService;
import ca.samanthaireland.lightning.engine.ext.modules.dto.HealPayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Command to heal an entity.
 */
@Slf4j
public final class HealCommand {

    private HealCommand() {}

    /**
     * Creates a new heal command.
     *
     * @param healthService the health service for health operations
     * @return the configured EngineCommand
     */
    public static EngineCommand create(HealthService healthService) {
        return CommandBuilder.newCommand()
                .withName("heal")
                .withSchema(Map.of(
                        "entityId", Long.class,
                        "amount", Float.class
                ))
                .withExecution(payload -> {
                    HealPayload dto = PayloadMapper.convert(payload, HealPayload.class);

                    if (dto.entityId() == 0 || dto.getAmount() <= 0) {
                        log.warn("heal: invalid entityId or amount");
                        return;
                    }

                    healthService.heal(dto.entityId(), dto.getAmount());
                })
                .build();
    }
}
