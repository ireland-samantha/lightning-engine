package ca.samanthaireland.lightning.engine.ext.modules.domain.repository;

import ca.samanthaireland.lightning.engine.ext.modules.domain.ItemType;

import java.util.Collection;
import java.util.Optional;

/**
 * Repository interface for ItemType entities.
 */
public interface ItemTypeRepository {

    /**
     * Save an item type and return the saved instance with assigned ID.
     *
     * @param matchId the match to create the item type in
     * @param itemType the item type to save
     * @return the saved item type with assigned ID
     */
    ItemType save(long matchId, ItemType itemType);

    /**
     * Find an item type by its ID within a match.
     *
     * @param matchId the match ID
     * @param itemTypeId the item type ID
     * @return the item type if found
     */
    Optional<ItemType> findById(long matchId, long itemTypeId);

    /**
     * Get all item types for a match.
     *
     * @param matchId the match ID
     * @return collection of all item types in the match
     */
    Collection<ItemType> findAllByMatchId(long matchId);

    /**
     * Check if an item type exists.
     *
     * @param matchId the match ID
     * @param itemTypeId the item type ID
     * @return true if the item type exists
     */
    boolean exists(long matchId, long itemTypeId);
}
