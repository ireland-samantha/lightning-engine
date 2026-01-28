package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.MapMatchRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of MapMatchRepository.
 *
 * <p>Stores map-to-match associations in a thread-safe concurrent map.
 * This implementation is suitable for single-server deployments.
 */
public class EcsMapMatchRepository implements MapMatchRepository {

    private final Map<Long, Long> matchToMapAssignments = new ConcurrentHashMap<>();

    @Override
    public void assignMapToMatch(long matchId, long mapId) {
        matchToMapAssignments.put(matchId, mapId);
    }

    @Override
    public Optional<Long> findMapIdByMatchId(long matchId) {
        return Optional.ofNullable(matchToMapAssignments.get(matchId));
    }

    @Override
    public void removeMapAssignment(long matchId) {
        matchToMapAssignments.remove(matchId);
    }
}
