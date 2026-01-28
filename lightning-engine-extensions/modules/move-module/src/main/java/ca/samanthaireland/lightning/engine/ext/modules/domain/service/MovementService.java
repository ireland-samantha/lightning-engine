package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.ext.modules.domain.MovementState;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Position;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Velocity;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.MovementRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Domain service for movement operations.
 *
 * @deprecated use RigidBodyModule
 */
@Slf4j
@Deprecated
public class MovementService {

    private final MovementRepository movementRepository;
    private final List<Long> pendingDeletions = new ArrayList<>();

    public MovementService(MovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    /**
     * Attach movement to an entity with the given position and velocity.
     *
     * @param entityId the entity ID
     * @param position the initial position
     * @param velocity the initial velocity
     */
    public void attachMovement(long entityId, Position position, Velocity velocity) {
        movementRepository.save(entityId, position, velocity);
        log.info("Attached movement to entity {}: pos=({},{},{}), vel=({},{},{})",
                entityId, position.x(), position.y(), position.z(),
                velocity.x(), velocity.y(), velocity.z());
    }

    /**
     * Get the movement state of an entity.
     *
     * @param entityId the entity ID
     * @return the movement state if exists
     */
    public Optional<MovementState> getMovementState(long entityId) {
        return movementRepository.findById(entityId);
    }

    /**
     * Queue an entity for movement removal.
     *
     * @param entityId the entity ID to queue for deletion
     */
    public void queueForDeletion(long entityId) {
        log.trace("Request delete moveable: {}", entityId);
        pendingDeletions.add(entityId);
    }

    /**
     * Apply velocity to all moveable entities, updating their positions.
     */
    public void applyVelocities() {
        log.debug("Running move system");

        Set<Long> entities = movementRepository.findAllMoveable();

        for (long entityId : entities) {
            Optional<MovementState> stateOpt = movementRepository.findById(entityId);
            if (stateOpt.isPresent()) {
                MovementState state = stateOpt.get();
                MovementState newState = state.applyVelocity();
                movementRepository.updatePosition(entityId, newState.position());

                log.debug("Move entity {} from {},{},{}->{},{},{}",
                        entityId,
                        state.position().x(), state.position().y(), state.position().z(),
                        newState.position().x(), newState.position().y(), newState.position().z());
            }
        }
    }

    /**
     * Process pending deletions, removing movement from queued entities.
     */
    public void processDeletions() {
        for (Long entityId : pendingDeletions) {
            log.trace("Delete moveable: {}", entityId);
            movementRepository.delete(entityId);
        }
        pendingDeletions.clear();
    }

    /**
     * Get the list of entities pending deletion.
     * For testing purposes.
     */
    List<Long> getPendingDeletions() {
        return new ArrayList<>(pendingDeletions);
    }
}
