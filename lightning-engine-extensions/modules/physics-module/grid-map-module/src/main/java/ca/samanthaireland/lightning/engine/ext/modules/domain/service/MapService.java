package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.core.exception.ConflictException;
import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.GridMap;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.MapMatchRepository;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.MapRepository;

import java.util.Optional;

/**
 * Domain service for map operations.
 */
public class MapService {

    private final MapRepository mapRepository;
    private final MapMatchRepository mapMatchRepository;

    public MapService(MapRepository mapRepository, MapMatchRepository mapMatchRepository) {
        this.mapRepository = mapRepository;
        this.mapMatchRepository = mapMatchRepository;
    }

    /**
     * Create a new map for the given match.
     */
    public GridMap createMap(long matchId, int width, int height, int depth) {
        GridMap gridMap = GridMap.create(width, height, depth);
        if (mapMatchRepository.hasAssignedMap(matchId)) {
            throw new ConflictException("Match already has assigned map.");
        }
        GridMap map = mapRepository.save(matchId, gridMap);
        mapMatchRepository.assignMapToMatch(matchId, matchId);
        return map;
    }

    /**
     * Find a map by ID.
     *
     * @param mapId the map entity ID
     * @return the map
     * @throws EntityNotFoundException if map does not exist
     */
    public GridMap findById(long mapId) {
        return mapRepository.findById(mapId)
                .orElseThrow(() -> new EntityNotFoundException("mapId"));
    }

    /**
     * Assign a map to a match.
     *
     * <p>Validates that the map exists before creating the assignment.
     * If the match already has an assigned map, it will be replaced.
     *
     * @param matchId the match ID
     * @param mapId the map entity ID to assign
     * @throws EntityNotFoundException if the map does not exist
     */
    public void assignMapToMatch(long matchId, long mapId) {
        if (!mapRepository.exists(mapId)) {
            throw new EntityNotFoundException("mapId");
        }
        mapMatchRepository.assignMapToMatch(matchId, mapId);
    }

    /**
     * Find the map assigned to a match.
     *
     * @param matchId the match ID
     * @return the assigned map, or empty if no map is assigned
     */
    public Optional<GridMap> findMapByMatchId(long matchId) {
        return mapMatchRepository.findMapIdByMatchId(matchId)
                .flatMap(mapRepository::findById);
    }

    /**
     * Check if a match has an assigned map.
     *
     * @param matchId the match ID
     * @return true if the match has an assigned map
     */
    public boolean hasAssignedMap(long matchId) {
        return mapMatchRepository.hasAssignedMap(matchId);
    }

    /**
     * Remove the map assignment for a match.
     *
     * @param matchId the match ID
     */
    public void removeMapAssignment(long matchId) {
        mapMatchRepository.removeMapAssignment(matchId);
    }
}
