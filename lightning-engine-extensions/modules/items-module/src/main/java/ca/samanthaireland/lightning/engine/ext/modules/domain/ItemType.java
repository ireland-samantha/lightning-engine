package ca.samanthaireland.lightning.engine.ext.modules.domain;

/**
 * Domain entity representing an item type definition.
 *
 * <p>An item type defines the blueprint for items: their name, stacking behavior,
 * rarity, and various stats. Item instances reference these types.
 */
public record ItemType(
        long id,
        String name,
        int maxStack,
        int rarity,
        float value,
        float weight,
        float healAmount,
        float damageBonus,
        float armorValue
) {

    /**
     * Creates a new item type with validated properties.
     *
     * @throws IllegalArgumentException if name is null or empty, or maxStack is not positive
     */
    public ItemType {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Item type name cannot be null or empty");
        }
        if (maxStack <= 0) {
            throw new IllegalArgumentException("Max stack must be positive, got: " + maxStack);
        }
    }

    /**
     * Creates an item type without an assigned ID (for creation).
     */
    public static ItemType create(String name, int maxStack, int rarity, float value,
                                   float weight, float healAmount, float damageBonus, float armorValue) {
        return new ItemType(0, name, maxStack, rarity, value, weight, healAmount, damageBonus, armorValue);
    }
}
