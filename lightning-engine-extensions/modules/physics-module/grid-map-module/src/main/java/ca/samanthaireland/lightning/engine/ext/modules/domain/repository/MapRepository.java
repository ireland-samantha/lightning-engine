package ca.samanthaireland.lightning.engine.ext.modules.domain.repository;

import ca.samanthaireland.lightning.engine.ext.modules.domain.GridMap;

import java.util.Optional;

/**
 * Repository interface for Map entities.
 */
public interface MapRepository {

    /**
     * Save a map and return the saved instance with assigned ID.
     *
     * @param matchId the match to create the map in
     * @param gridMap the map to save
     * @return the saved map with assigned ID
     */
    GridMap save(long matchId, GridMap gridMap);

    /**
     * Find a map by its entity ID.
     *
     * @param mapId the map entity ID
     * @return the map if found
     */
    Optional<GridMap> findById(long mapId);

    /**
     * Check if a map entity exists.
     *
     * @param mapId the map entity ID
     * @return true if the map exists
     */
    boolean exists(long mapId);
}
