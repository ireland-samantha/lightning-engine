package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.GridMap;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Position;
import ca.samanthaireland.lightning.engine.ext.modules.domain.PositionOutOfBoundsException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.MapRepository;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.PositionRepository;

import java.util.Optional;

/**
 * Domain service for entity position operations.
 */
public class PositionService {

    private final MapRepository mapRepository;
    private final PositionRepository positionRepository;

    public PositionService(MapRepository mapRepository, PositionRepository positionRepository) {
        this.mapRepository = mapRepository;
        this.positionRepository = positionRepository;
    }

    /**
     * Set an entity's position on a map with bounds validation.
     *
     * @param entityId the entity to position
     * @param mapId the map to position on
     * @param position the target position
     * @throws EntityNotFoundException if map does not exist
     * @throws PositionOutOfBoundsException if position is outside map bounds
     */
    public void setPosition(long entityId, long mapId, Position position) {
        GridMap gridMap = mapRepository.findById(mapId)
                .orElseThrow(() -> new EntityNotFoundException("mapId"));

        gridMap.isWithinBounds(position);

        positionRepository.save(entityId, position);
    }

    /**
     * Get an entity's current position.
     *
     * @param entityId the entity ID
     * @return the position if the entity has one
     */
    public Optional<Position> getPosition(long entityId) {
        return positionRepository.findByEntityId(entityId);
    }
}
