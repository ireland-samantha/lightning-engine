package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.ext.modules.domain.ItemType;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.ItemTypeRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Optional;

/**
 * Domain service for item type operations.
 */
@Slf4j
public class ItemTypeService {

    private final ItemTypeRepository itemTypeRepository;

    public ItemTypeService(ItemTypeRepository itemTypeRepository) {
        this.itemTypeRepository = itemTypeRepository;
    }

    /**
     * Create a new item type in the given match.
     *
     * @param matchId the match to create the item type in
     * @param name item type name
     * @param maxStack maximum stack size
     * @param rarity rarity tier
     * @param value base value
     * @param weight item weight
     * @param healAmount HP restored when used
     * @param damageBonus damage bonus when equipped
     * @param armorValue armor value when equipped
     * @return the created item type with assigned ID
     * @throws IllegalArgumentException if name is invalid or maxStack is not positive
     */
    public ItemType createItemType(long matchId, String name, int maxStack, int rarity,
                                    float value, float weight, float healAmount,
                                    float damageBonus, float armorValue) {
        ItemType itemType = ItemType.create(name, maxStack, rarity, value, weight,
                healAmount, damageBonus, armorValue);
        ItemType savedType = itemTypeRepository.save(matchId, itemType);
        log.info("Created item type '{}' with ID {} for match {}", name, savedType.id(), matchId);
        return savedType;
    }

    /**
     * Find an item type by ID.
     *
     * @param matchId the match ID
     * @param itemTypeId the item type ID
     * @return the item type if found
     */
    public Optional<ItemType> findById(long matchId, long itemTypeId) {
        return itemTypeRepository.findById(matchId, itemTypeId);
    }

    /**
     * Get all item types for a match.
     *
     * @param matchId the match ID
     * @return collection of all item types
     */
    public Collection<ItemType> getAllItemTypes(long matchId) {
        Collection<ItemType> types = itemTypeRepository.findAllByMatchId(matchId);
        log.info("Item types registered for match {}: {}", matchId, types.size());
        for (ItemType type : types) {
            log.info("  Type {}: '{}' (maxStack={}, rarity={})",
                    type.id(), type.name(), type.maxStack(), type.rarity());
        }
        return types;
    }
}
