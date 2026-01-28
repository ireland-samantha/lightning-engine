package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.modules.domain.MovementState;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Position;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Velocity;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.MovementRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ca.samanthaireland.lightning.engine.ext.modules.MoveModuleFactory.*;

/**
 * ECS-backed implementation of MovementRepository.
 *
 * <p>Stores movement data as entity components in the EntityComponentStore.
 *
 * @deprecated use RigidBodyModule
 */
@Deprecated
public class EcsMovementRepository implements MovementRepository {

    private final EntityComponentStore store;

    public EcsMovementRepository(EntityComponentStore store) {
        this.store = store;
    }

    @Override
    public void save(long entityId, Position position, Velocity velocity) {
        store.attachComponents(entityId, MOVE_COMPONENTS,
                new float[]{position.x(), position.y(), position.z(),
                        velocity.x(), velocity.y(), velocity.z()});
        store.attachComponent(entityId, MODULE, 1.0f);
    }

    @Override
    public Optional<MovementState> findById(long entityId) {
        Set<Long> entitiesWithMovement = store.getEntitiesWithComponents(ALL_COMPONENTS);
        if (!entitiesWithMovement.contains(entityId)) {
            return Optional.empty();
        }

        float posX = store.getComponent(entityId, POSITION_X);
        float posY = store.getComponent(entityId, POSITION_Y);
        float posZ = store.getComponent(entityId, POSITION_Z);
        float velX = store.getComponent(entityId, VELOCITY_X);
        float velY = store.getComponent(entityId, VELOCITY_Y);
        float velZ = store.getComponent(entityId, VELOCITY_Z);

        return Optional.of(MovementState.of(
                entityId,
                Position.of(posX, posY, posZ),
                Velocity.of(velX, velY, velZ)
        ));
    }

    @Override
    public Set<Long> findAllMoveable() {
        return store.getEntitiesWithComponents(ALL_COMPONENTS);
    }

    @Override
    public void updatePosition(long entityId, Position position) {
        store.attachComponents(
                entityId,
                List.of(POSITION_X, POSITION_Y, POSITION_Z),
                new float[]{position.x(), position.y(), position.z()}
        );
    }

    @Override
    public void delete(long entityId) {
        for (BaseComponent component : MOVE_COMPONENTS) {
            store.removeComponent(entityId, component);
        }
        store.removeComponent(entityId, MODULE);
    }

    @Override
    public boolean exists(long entityId) {
        Set<Long> entitiesWithMovement = store.getEntitiesWithComponents(ALL_COMPONENTS);
        return entitiesWithMovement.contains(entityId);
    }
}
