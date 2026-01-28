package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.ItemService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.PickupItemPayload;

import java.util.Map;

/**
 * Command to pick up an item (add to inventory).
 *
 * <p>Payload:
 * <ul>
 *   <li>itemEntityId (long) - Item entity to pick up</li>
 *   <li>pickerEntityId (long) - Entity picking up the item</li>
 *   <li>slotIndex (float) - Inventory slot to place item</li>
 * </ul>
 */
public final class PickupItemCommand {

    private PickupItemCommand() {}

    /**
     * Creates a new pickupItem command.
     *
     * @param itemService the item service
     * @return the configured EngineCommand
     */
    public static EngineCommand create(ItemService itemService) {
        return CommandBuilder.newCommand()
                .withName("pickupItem")
                .withSchema(Map.of(
                        "itemEntityId", Long.class,
                        "pickerEntityId", Long.class,
                        "slotIndex", Float.class
                ))
                .withExecution(payload -> {
                    PickupItemPayload dto = PayloadMapper.convert(payload, PickupItemPayload.class);

                    itemService.pickupItem(
                            dto.getItemEntityId(),
                            dto.getPickerEntityId(),
                            dto.getSlotIndex()
                    );
                })
                .build();
    }
}
