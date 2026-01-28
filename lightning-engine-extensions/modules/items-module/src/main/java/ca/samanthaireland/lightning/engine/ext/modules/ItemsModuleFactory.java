package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;

import java.util.List;

/**
 * Module factory for the Items module.
 *
 * <p>Provides item type definitions and inventory management:
 * <ul>
 *   <li>createItemType - Define a new item type with properties</li>
 *   <li>spawnItem - Create an item entity in the world</li>
 *   <li>pickupItem - Add item to entity's inventory</li>
 *   <li>dropItem - Remove item from inventory and place in world</li>
 *   <li>useItem - Trigger item use effect</li>
 * </ul>
 *
 * <p>Item properties:
 * <ul>
 *   <li>ITEM_TYPE_ID - Reference to the item type definition</li>
 *   <li>STACK_SIZE - Current stack count</li>
 *   <li>MAX_STACK - Maximum stack size for this type</li>
 *   <li>OWNER_ENTITY_ID - Entity carrying this item (0 if on ground)</li>
 *   <li>SLOT_INDEX - Inventory slot position</li>
 * </ul>
 *
 * <p>Item types are stored in a registry (not in ECS) and referenced by ID.
 *
 * @see ItemComponents for component constants
 */
public class ItemsModuleFactory implements ModuleFactory {

    // Delegated constants for backwards compatibility
    public static final BaseComponent ITEM_TYPE_ID = ItemComponents.ITEM_TYPE_ID;
    public static final BaseComponent STACK_SIZE = ItemComponents.STACK_SIZE;
    public static final BaseComponent MAX_STACK = ItemComponents.MAX_STACK;
    public static final BaseComponent OWNER_ENTITY_ID = ItemComponents.OWNER_ENTITY_ID;
    public static final BaseComponent SLOT_INDEX = ItemComponents.SLOT_INDEX;
    public static final BaseComponent ITEM_NAME_HASH = ItemComponents.ITEM_NAME_HASH;
    public static final BaseComponent ITEM_RARITY = ItemComponents.ITEM_RARITY;
    public static final BaseComponent ITEM_VALUE = ItemComponents.ITEM_VALUE;
    public static final BaseComponent ITEM_WEIGHT = ItemComponents.ITEM_WEIGHT;
    public static final BaseComponent HEAL_AMOUNT = ItemComponents.HEAL_AMOUNT;
    public static final BaseComponent DAMAGE_BONUS = ItemComponents.DAMAGE_BONUS;
    public static final BaseComponent ARMOR_VALUE = ItemComponents.ARMOR_VALUE;
    public static final BaseComponent FLAG = ItemComponents.FLAG;
    public static final List<BaseComponent> CORE_COMPONENTS = ItemComponents.CORE_COMPONENTS;
    public static final List<BaseComponent> ALL_COMPONENTS = ItemComponents.ALL_COMPONENTS;

    @Override
    public EngineModule create(ModuleContext context) {
        return new ItemsModule(context);
    }
}
