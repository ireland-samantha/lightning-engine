package ca.samanthaireland.lightning.engine.ext.modules.domain.repository;

import java.util.Optional;

/**
 * Repository interface for map-to-match associations.
 *
 * <p>Manages the relationship between matches and their assigned maps.
 * Each match can have at most one assigned map.
 */
public interface MapMatchRepository {

    /**
     * Assign a map to a match.
     *
     * <p>If the match already has an assigned map, it will be replaced.
     *
     * @param matchId the match ID
     * @param mapId the map entity ID to assign
     */
    void assignMapToMatch(long matchId, long mapId);

    /**
     * Find the map ID assigned to a match.
     *
     * @param matchId the match ID
     * @return the assigned map ID, or empty if no map is assigned
     */
    Optional<Long> findMapIdByMatchId(long matchId);

    /**
     * Check if a match has an assigned map.
     *
     * @param matchId the match ID
     * @return true if the match has an assigned map
     */
    default boolean hasAssignedMap(long matchId) {
        return findMapIdByMatchId(matchId).isPresent();
    }

    /**
     * Remove the map assignment for a match.
     *
     * @param matchId the match ID
     */
    void removeMapAssignment(long matchId);
}
