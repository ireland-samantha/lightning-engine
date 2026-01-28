package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionLevel;
import ca.samanthaireland.lightning.engine.util.IdGeneratorV2;

import java.util.List;

/**
 * Component constants for the Items module.
 *
 * <p>Provides item type definitions and inventory management components:
 * <ul>
 *   <li>ITEM_TYPE_ID - Reference to the item type definition</li>
 *   <li>STACK_SIZE - Current stack count</li>
 *   <li>MAX_STACK - Maximum stack size for this type</li>
 *   <li>OWNER_ENTITY_ID - Entity carrying this item (0 if on ground)</li>
 *   <li>SLOT_INDEX - Inventory slot position</li>
 * </ul>
 *
 * <p>Item type properties (stored per item type in registry):
 * <ul>
 *   <li>ITEM_NAME_HASH - Hash of item name for lookup</li>
 *   <li>ITEM_RARITY - Rarity level</li>
 *   <li>ITEM_VALUE - Base value</li>
 *   <li>ITEM_WEIGHT - Weight for inventory limits</li>
 * </ul>
 *
 * <p>Item effects (for consumables/equipment):
 * <ul>
 *   <li>HEAL_AMOUNT - HP restored on use</li>
 *   <li>DAMAGE_BONUS - Damage added when equipped</li>
 *   <li>ARMOR_VALUE - Damage reduction when equipped</li>
 * </ul>
 */
public final class ItemComponents {

    private ItemComponents() {
        // Constants class
    }

    // Item instance components
    public static final BaseComponent ITEM_TYPE_ID = new ItemComponent(
            IdGeneratorV2.newId(), "ITEM_TYPE_ID");
    public static final BaseComponent STACK_SIZE = new ItemComponent(
            IdGeneratorV2.newId(), "STACK_SIZE");
    public static final BaseComponent MAX_STACK = new ItemComponent(
            IdGeneratorV2.newId(), "MAX_STACK");
    public static final BaseComponent OWNER_ENTITY_ID = new ItemComponent(
            IdGeneratorV2.newId(), "OWNER_ENTITY_ID");
    public static final BaseComponent SLOT_INDEX = new ItemComponent(
            IdGeneratorV2.newId(), "SLOT_INDEX");

    // Item type properties (stored per item type in registry)
    public static final BaseComponent ITEM_NAME_HASH = new ItemComponent(
            IdGeneratorV2.newId(), "ITEM_NAME_HASH");
    public static final BaseComponent ITEM_RARITY = new ItemComponent(
            IdGeneratorV2.newId(), "ITEM_RARITY");
    public static final BaseComponent ITEM_VALUE = new ItemComponent(
            IdGeneratorV2.newId(), "ITEM_VALUE");
    public static final BaseComponent ITEM_WEIGHT = new ItemComponent(
            IdGeneratorV2.newId(), "ITEM_WEIGHT");

    // Item effects (for consumables/equipment)
    public static final BaseComponent HEAL_AMOUNT = new ItemComponent(
            IdGeneratorV2.newId(), "HEAL_AMOUNT");
    public static final BaseComponent DAMAGE_BONUS = new ItemComponent(
            IdGeneratorV2.newId(), "DAMAGE_BONUS");
    public static final BaseComponent ARMOR_VALUE = new ItemComponent(
            IdGeneratorV2.newId(), "ARMOR_VALUE");

    // Module flag (PRIVATE - only EntityModule with superuser can attach during spawn)
    public static final BaseComponent FLAG = new PermissionComponent(
            IdGeneratorV2.newId(), "item", PermissionLevel.PRIVATE);

    /**
     * Core components for item instances.
     */
    public static final List<BaseComponent> CORE_COMPONENTS = List.of(
            ITEM_TYPE_ID, STACK_SIZE, MAX_STACK, OWNER_ENTITY_ID, SLOT_INDEX
    );

    /**
     * Extended components including item properties.
     */
    public static final List<BaseComponent> ALL_COMPONENTS = List.of(
            ITEM_TYPE_ID, STACK_SIZE, MAX_STACK, OWNER_ENTITY_ID, SLOT_INDEX,
            ITEM_NAME_HASH, ITEM_RARITY, ITEM_VALUE, ITEM_WEIGHT,
            HEAL_AMOUNT, DAMAGE_BONUS, ARMOR_VALUE
    );
}
