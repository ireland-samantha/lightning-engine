package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.ItemService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.DropItemPayload;

import java.util.Map;

/**
 * Command to drop an item from inventory.
 *
 * <p>Payload:
 * <ul>
 *   <li>itemEntityId (long) - Item entity to drop</li>
 *   <li>positionX/Y (float) - World position to drop at</li>
 * </ul>
 */
public final class DropItemCommand {

    private DropItemCommand() {}

    /**
     * Creates a new dropItem command.
     *
     * @param itemService the item service
     * @return the configured EngineCommand
     */
    public static EngineCommand create(ItemService itemService) {
        return CommandBuilder.newCommand()
                .withName("dropItem")
                .withSchema(Map.of(
                        "itemEntityId", Long.class,
                        "positionX", Float.class,
                        "positionY", Float.class
                ))
                .withExecution(payload -> {
                    DropItemPayload dto = PayloadMapper.convert(payload, DropItemPayload.class);

                    itemService.dropItem(
                            dto.getItemEntityId(),
                            dto.getPositionX(),
                            dto.getPositionY()
                    );
                })
                .build();
    }
}
