package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.ItemService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.UseItemPayload;

import java.util.Map;

/**
 * Command to use an item (consume or activate).
 *
 * <p>Payload:
 * <ul>
 *   <li>itemEntityId (long) - Item entity to use</li>
 *   <li>userEntityId (long) - Entity using the item</li>
 * </ul>
 */
public final class UseItemCommand {

    private UseItemCommand() {}

    /**
     * Creates a new useItem command.
     *
     * @param itemService the item service
     * @return the configured EngineCommand
     */
    public static EngineCommand create(ItemService itemService) {
        return CommandBuilder.newCommand()
                .withName("useItem")
                .withSchema(Map.of(
                        "itemEntityId", Long.class,
                        "userEntityId", Long.class
                ))
                .withExecution(payload -> {
                    UseItemPayload dto = PayloadMapper.convert(payload, UseItemPayload.class);

                    itemService.useItem(
                            dto.getItemEntityId(),
                            dto.getUserEntityId()
                    );
                })
                .build();
    }
}
