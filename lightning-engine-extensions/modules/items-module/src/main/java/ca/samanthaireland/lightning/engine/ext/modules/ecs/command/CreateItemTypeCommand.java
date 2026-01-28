package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.ItemTypeService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.CreateItemTypePayload;

import java.util.HashMap;
import java.util.Map;

/**
 * Command to create a new item type.
 *
 * <p>Creates an item type definition with properties:
 * <ul>
 *   <li>name (String) - Item type name</li>
 *   <li>maxStack (float) - Maximum stack size (default 1)</li>
 *   <li>rarity (float) - Rarity tier (0=common, 1=uncommon, 2=rare, etc.)</li>
 *   <li>value (float) - Base value in currency</li>
 *   <li>weight (float) - Item weight</li>
 *   <li>healAmount (float) - HP restored when used (consumables)</li>
 *   <li>damageBonus (float) - Damage bonus when equipped</li>
 *   <li>armorValue (float) - Armor value when equipped</li>
 * </ul>
 */
public final class CreateItemTypeCommand {

    private CreateItemTypeCommand() {}

    /**
     * Creates a new createItemType command.
     *
     * @param itemTypeService the item type service
     * @return the configured EngineCommand
     */
    public static EngineCommand create(ItemTypeService itemTypeService) {
        Map<String, Class<?>> schema = new HashMap<>();
        schema.put("matchId", Long.class);
        schema.put("name", String.class);
        schema.put("maxStack", Float.class);
        schema.put("rarity", Float.class);
        schema.put("value", Float.class);
        schema.put("weight", Float.class);
        schema.put("healAmount", Float.class);
        schema.put("damageBonus", Float.class);
        schema.put("armorValue", Float.class);

        return CommandBuilder.newCommand()
                .withName("createItemType")
                .withSchema(schema)
                .withExecution(payload -> {
                    CreateItemTypePayload dto = PayloadMapper.convert(payload, CreateItemTypePayload.class);

                    itemTypeService.createItemType(
                            dto.getMatchId(),
                            dto.getName(),
                            dto.getMaxStack(),
                            dto.getRarity(),
                            dto.getValue(),
                            dto.getWeight(),
                            dto.getHealAmount(),
                            dto.getDamageBonus(),
                            dto.getArmorValue()
                    );
                })
                .build();
    }
}
