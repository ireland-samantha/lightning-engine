package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.ItemService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.SpawnItemPayload;

import java.util.Map;

/**
 * Command to spawn an item entity in the world.
 *
 * <p>Payload:
 * <ul>
 *   <li>matchId (long) - Target match</li>
 *   <li>itemTypeId (long) - Item type to spawn</li>
 *   <li>positionX/Y (float) - World position</li>
 *   <li>stackSize (float) - Number of items in stack (default 1)</li>
 * </ul>
 */
public final class SpawnItemCommand {

    private SpawnItemCommand() {}

    /**
     * Creates a new spawnItem command.
     *
     * @param itemService the item service
     * @return the configured EngineCommand
     */
    public static EngineCommand create(ItemService itemService) {
        return CommandBuilder.newCommand()
                .withName("spawnItem")
                .withSchema(Map.of(
                        "matchId", Long.class,
                        "itemTypeId", Long.class,
                        "positionX", Float.class,
                        "positionY", Float.class,
                        "stackSize", Float.class
                ))
                .withExecution(payload -> {
                    SpawnItemPayload dto = PayloadMapper.convert(payload, SpawnItemPayload.class);

                    itemService.spawnItem(
                            dto.getMatchId(),
                            dto.getItemTypeId(),
                            dto.getPositionX(),
                            dto.getPositionY(),
                            dto.getStackSize()
                    );
                })
                .build();
    }
}
