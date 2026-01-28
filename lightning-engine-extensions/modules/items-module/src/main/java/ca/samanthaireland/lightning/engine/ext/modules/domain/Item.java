package ca.samanthaireland.lightning.engine.ext.modules.domain;

/**
 * Domain entity representing an item instance.
 *
 * <p>An item is a concrete instance of an ItemType that exists in the game world.
 * It can be on the ground (ownerEntityId = 0) or in an entity's inventory.
 */
public record Item(
        long id,
        long itemTypeId,
        int stackSize,
        int maxStack,
        long ownerEntityId,
        int slotIndex,
        float positionX,
        float positionY,
        int nameHash,
        int rarity,
        float value,
        float weight,
        float healAmount,
        float damageBonus,
        float armorValue
) {

    /**
     * Creates a new item with validated properties.
     *
     * @throws IllegalArgumentException if stackSize is not positive or exceeds maxStack
     */
    public Item {
        if (stackSize <= 0) {
            throw new IllegalArgumentException("Stack size must be positive, got: " + stackSize);
        }
        if (maxStack <= 0) {
            throw new IllegalArgumentException("Max stack must be positive, got: " + maxStack);
        }
        if (stackSize > maxStack) {
            throw new IllegalArgumentException("Stack size " + stackSize + " exceeds max stack " + maxStack);
        }
    }

    /**
     * Creates an item without an assigned ID (for creation).
     */
    public static Item createFromType(ItemType type, int stackSize, float positionX, float positionY) {
        return new Item(
                0,
                type.id(),
                Math.min(stackSize, type.maxStack()),
                type.maxStack(),
                0, // on ground
                -1, // no slot
                positionX,
                positionY,
                type.name().hashCode(),
                type.rarity(),
                type.value(),
                type.weight(),
                type.healAmount(),
                type.damageBonus(),
                type.armorValue()
        );
    }

    /**
     * Creates an item with default properties (for unknown types).
     */
    public static Item createDefault(long itemTypeId, int stackSize, float positionX, float positionY) {
        return new Item(
                0,
                itemTypeId,
                stackSize,
                100, // default max stack
                0, // on ground
                -1, // no slot
                positionX,
                positionY,
                0, // no name hash
                0, // common rarity
                0, 0, 0, 0, 0 // no stats
        );
    }

    /**
     * Check if this item is on the ground (not owned by any entity).
     */
    public boolean isOnGround() {
        return ownerEntityId == 0;
    }

    /**
     * Check if this item is in an inventory.
     */
    public boolean isInInventory() {
        return ownerEntityId > 0;
    }

    /**
     * Create a copy with updated owner and slot.
     */
    public Item withOwner(long newOwnerEntityId, int newSlotIndex) {
        return new Item(id, itemTypeId, stackSize, maxStack, newOwnerEntityId, newSlotIndex,
                positionX, positionY, nameHash, rarity, value, weight, healAmount, damageBonus, armorValue);
    }

    /**
     * Create a copy dropped at a position.
     */
    public Item dropped(float newPositionX, float newPositionY) {
        return new Item(id, itemTypeId, stackSize, maxStack, 0, -1,
                newPositionX, newPositionY, nameHash, rarity, value, weight, healAmount, damageBonus, armorValue);
    }

    /**
     * Create a copy with reduced stack size.
     */
    public Item withReducedStack(int amount) {
        int newStackSize = stackSize - amount;
        if (newStackSize <= 0) {
            return null; // item consumed
        }
        return new Item(id, itemTypeId, newStackSize, maxStack, ownerEntityId, slotIndex,
                positionX, positionY, nameHash, rarity, value, weight, healAmount, damageBonus, armorValue);
    }
}
