package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.ext.modules.domain.Item;
import ca.samanthaireland.lightning.engine.ext.modules.domain.ItemType;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.ItemRepository;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.ItemTypeRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Domain service for item operations.
 */
@Slf4j
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemTypeRepository itemTypeRepository;

    public ItemService(ItemRepository itemRepository, ItemTypeRepository itemTypeRepository) {
        this.itemRepository = itemRepository;
        this.itemTypeRepository = itemTypeRepository;
    }

    /**
     * Spawn an item entity in the world.
     *
     * @param matchId the match to spawn the item in
     * @param itemTypeId the item type to spawn
     * @param positionX world X position
     * @param positionY world Y position
     * @param stackSize number of items in stack
     * @return the spawned item with assigned entity ID
     */
    public Item spawnItem(long matchId, long itemTypeId, float positionX, float positionY, int stackSize) {
        log.info("spawnItem: looking up itemTypeId={} for matchId={}", itemTypeId, matchId);

        Optional<ItemType> typeOpt = itemTypeRepository.findById(matchId, itemTypeId);
        Item item;

        if (typeOpt.isEmpty()) {
            log.warn("spawnItem: unknown item type {} for match {}", itemTypeId, matchId);
            item = Item.createDefault(itemTypeId, stackSize, positionX, positionY);
        } else {
            ItemType typeDef = typeOpt.get();
            item = Item.createFromType(typeDef, stackSize, positionX, positionY);
            log.info("Spawned item '{}' (type {}) at ({}, {})",
                    typeDef.name(), itemTypeId, positionX, positionY);
        }

        return itemRepository.save(matchId, item);
    }

    /**
     * Pick up an item (add to inventory).
     *
     * @param itemEntityId item entity to pick up
     * @param pickerEntityId entity picking up the item
     * @param slotIndex inventory slot to place item
     * @return true if pickup was successful
     */
    public boolean pickupItem(long itemEntityId, long pickerEntityId, int slotIndex) {
        Optional<Item> itemOpt = itemRepository.findById(itemEntityId);
        if (itemOpt.isEmpty()) {
            log.warn("pickupItem: entity {} is not an item", itemEntityId);
            return false;
        }

        Item item = itemOpt.get();
        if (item.isInInventory()) {
            log.warn("pickupItem: item {} is already owned by {}", itemEntityId, item.ownerEntityId());
            return false;
        }

        itemRepository.updateOwnership(itemEntityId, pickerEntityId, slotIndex);
        log.info("Entity {} picked up item {}", pickerEntityId, itemEntityId);
        return true;
    }

    /**
     * Drop an item from inventory.
     *
     * @param itemEntityId item entity to drop
     * @param positionX world X position to drop at
     * @param positionY world Y position to drop at
     * @return true if drop was successful
     */
    public boolean dropItem(long itemEntityId, float positionX, float positionY) {
        Optional<Item> itemOpt = itemRepository.findById(itemEntityId);
        if (itemOpt.isEmpty()) {
            log.warn("dropItem: entity {} is not an item", itemEntityId);
            return false;
        }

        itemRepository.updateOwnership(itemEntityId, 0, -1);
        itemRepository.updatePosition(itemEntityId, positionX, positionY);
        log.info("Dropped item {} at ({}, {})", itemEntityId, positionX, positionY);
        return true;
    }

    /**
     * Use an item (consume or activate).
     *
     * @param itemEntityId item entity to use
     * @param userEntityId entity using the item
     * @return the heal amount from the item (for external handling), or 0 if use failed
     */
    public float useItem(long itemEntityId, long userEntityId) {
        log.info("useItem: itemEntityId={}, userEntityId={}", itemEntityId, userEntityId);

        Optional<Item> itemOpt = itemRepository.findById(itemEntityId);
        if (itemOpt.isEmpty()) {
            log.warn("useItem: entity {} is not an item", itemEntityId);
            return 0;
        }

        Item item = itemOpt.get();
        float healAmount = item.healAmount();
        log.info("useItem: item HEAL_AMOUNT={}", healAmount);

        // Consume stack
        int newStackSize = item.stackSize() - 1;
        if (newStackSize <= 0) {
            itemRepository.delete(itemEntityId);
            log.info("Item {} consumed and destroyed (healAmount={} available for external handling)",
                    itemEntityId, healAmount);
        } else {
            itemRepository.updateStackSize(itemEntityId, newStackSize);
            log.info("Item {} used, stack now {} (healAmount={} available for external handling)",
                    itemEntityId, newStackSize, healAmount);
        }

        return healAmount;
    }

    /**
     * Find an item by its entity ID.
     *
     * @param itemEntityId the item entity ID
     * @return the item if found
     */
    public Optional<Item> findById(long itemEntityId) {
        return itemRepository.findById(itemEntityId);
    }
}
