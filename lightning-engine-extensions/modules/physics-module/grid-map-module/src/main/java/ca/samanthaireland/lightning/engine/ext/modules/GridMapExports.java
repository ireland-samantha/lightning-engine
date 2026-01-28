package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.ext.module.ModuleExports;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Position;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.PositionRepository;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.MapService;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.PositionService;

import java.util.Optional;

/**
 * Exports from GridMapModule for use by other modules.
 *
 * <p>Provides position management APIs that other modules (like RigidBodyModule)
 * can use to update entity positions.
 */
public class GridMapExports implements ModuleExports {
    private final PositionService positionService;
    private final PositionRepository positionRepository;
    private final MapService mapService;

    public GridMapExports(PositionService positionService, PositionRepository positionRepository, MapService mapService) {
        this.positionService = positionService;
        this.positionRepository = positionRepository;
        this.mapService = mapService;
    }

    /**
     * Set an entity's position with bounds validation against the match's map.
     *
     * @param matchId the match ID (used to look up the map for bounds validation)
     * @param entityId the entity to position
     * @param position the target position
     * @throws EntityNotFoundException if match has no assigned map
     */
    public void setPositionOnMap(long matchId, long entityId, Position position) {
        mapService.findMapByMatchId(matchId)
                .ifPresentOrElse(map ->
                    positionService.setPosition(entityId, map.id(), position),
                    () -> {
                        throw new EntityNotFoundException(String.format("Match %s does not have an assigned map.", matchId));
                    });
    }

    /**
     * Set an entity's position directly without bounds validation.
     *
     * @param entityId the entity to position
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     */
    public void setPosition(long entityId, float x, float y, float z) {
        positionRepository.save(entityId, new Position(x, y, z));
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

    /**
     * Get the x coordinate of an entity's position.
     *
     * @param entityId the entity ID
     * @return the x coordinate, or 0 if not found
     */
    public float getPositionX(long entityId) {
        return positionRepository.findByEntityId(entityId).map(Position::x).orElse(0f);
    }

    /**
     * Get the y coordinate of an entity's position.
     *
     * @param entityId the entity ID
     * @return the y coordinate, or 0 if not found
     */
    public float getPositionY(long entityId) {
        return positionRepository.findByEntityId(entityId).map(Position::y).orElse(0f);
    }

    /**
     * Get the z coordinate of an entity's position.
     *
     * @param entityId the entity ID
     * @return the z coordinate, or 0 if not found
     */
    public float getPositionZ(long entityId) {
        return positionRepository.findByEntityId(entityId).map(Position::z).orElse(0f);
    }
}
