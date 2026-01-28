package ca.samanthaireland.lightning.engine.ext.modules.domain.repository;

import ca.samanthaireland.lightning.engine.ext.modules.domain.Item;

import java.util.Optional;

/**
 * Repository interface for Item entities.
 */
public interface ItemRepository {

    /**
     * Save an item entity and return the saved instance with assigned ID.
     *
     * @param matchId the match to create the item in
     * @param item the item to save
     * @return the saved item with assigned ID
     */
    Item save(long matchId, Item item);

    /**
     * Find an item by its entity ID.
     *
     * @param itemEntityId the item entity ID
     * @return the item if found
     */
    Optional<Item> findById(long itemEntityId);

    /**
     * Update an existing item's ownership.
     *
     * @param itemEntityId the item entity ID
     * @param ownerEntityId the new owner entity ID (0 for on ground)
     * @param slotIndex the new slot index (-1 for not in inventory)
     */
    void updateOwnership(long itemEntityId, long ownerEntityId, int slotIndex);

    /**
     * Update an existing item's position (for dropped items).
     *
     * @param itemEntityId the item entity ID
     * @param positionX the new X position
     * @param positionY the new Y position
     */
    void updatePosition(long itemEntityId, float positionX, float positionY);

    /**
     * Update an item's stack size.
     *
     * @param itemEntityId the item entity ID
     * @param stackSize the new stack size
     */
    void updateStackSize(long itemEntityId, int stackSize);

    /**
     * Delete an item entity (when consumed completely).
     *
     * @param itemEntityId the item entity ID
     */
    void delete(long itemEntityId);

    /**
     * Check if an item entity exists and is valid.
     *
     * @param itemEntityId the item entity ID
     * @return true if the item exists
     */
    boolean exists(long itemEntityId);
}
