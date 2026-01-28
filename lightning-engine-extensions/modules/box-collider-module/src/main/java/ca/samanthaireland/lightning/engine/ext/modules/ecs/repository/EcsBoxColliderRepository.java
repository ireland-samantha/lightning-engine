package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.modules.domain.BoxCollider;
import ca.samanthaireland.lightning.engine.ext.modules.domain.CollisionHandlerConfig;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.BoxColliderRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ca.samanthaireland.lightning.engine.ext.modules.BoxColliderModuleFactory.*;

/**
 * ECS-backed implementation of BoxColliderRepository.
 *
 * <p>Stores box collider data as entity components in the EntityComponentStore.
 */
public class EcsBoxColliderRepository implements BoxColliderRepository {

    private final EntityComponentStore store;

    public EcsBoxColliderRepository(EntityComponentStore store) {
        this.store = store;
    }

    @Override
    public void save(long entityId, BoxCollider collider) {
        store.attachComponents(entityId, DIMENSION_COMPONENTS,
                new float[]{collider.width(), collider.height(), collider.depth()});
        store.attachComponents(entityId, OFFSET_COMPONENTS,
                new float[]{collider.offsetX(), collider.offsetY(), collider.offsetZ()});
        store.attachComponent(entityId, COLLISION_LAYER, collider.layer());
        store.attachComponent(entityId, COLLISION_MASK, collider.mask());
        store.attachComponent(entityId, IS_TRIGGER, collider.isTrigger() ? 1 : 0);

        // Initialize collision state
        resetCollisionState(entityId);
        store.attachComponent(entityId, FLAG, 1.0f);
    }

    @Override
    public Optional<BoxCollider> findByEntityId(long entityId) {
        Set<Long> colliderEntities = store.getEntitiesWithComponents(List.of(FLAG));
        if (!colliderEntities.contains(entityId)) {
            return Optional.empty();
        }

        float width = store.getComponent(entityId, BOX_WIDTH);
        float height = store.getComponent(entityId, BOX_HEIGHT);
        float depth = store.getComponent(entityId, BOX_DEPTH);
        float offsetX = store.getComponent(entityId, OFFSET_X);
        float offsetY = store.getComponent(entityId, OFFSET_Y);
        float offsetZ = store.getComponent(entityId, OFFSET_Z);
        int layer = (int) store.getComponent(entityId, COLLISION_LAYER);
        int mask = (int) store.getComponent(entityId, COLLISION_MASK);
        boolean isTrigger = store.getComponent(entityId, IS_TRIGGER) != 0;

        return Optional.of(new BoxCollider(entityId, width, height, depth,
                offsetX, offsetY, offsetZ, layer, mask, isTrigger));
    }

    @Override
    public boolean exists(long entityId) {
        Set<Long> colliderEntities = store.getEntitiesWithComponents(List.of(FLAG));
        return colliderEntities.contains(entityId);
    }

    @Override
    public void updateSize(long entityId, float width, float height) {
        store.attachComponent(entityId, BOX_WIDTH, width);
        store.attachComponent(entityId, BOX_HEIGHT, height);
    }

    @Override
    public void updateLayerMask(long entityId, int layer, int mask) {
        store.attachComponent(entityId, COLLISION_LAYER, layer);
        store.attachComponent(entityId, COLLISION_MASK, mask);
    }

    @Override
    public void saveHandlerConfig(long entityId, CollisionHandlerConfig config) {
        store.attachComponent(entityId, COLLISION_HANDLER_TYPE, config.handlerType());
        store.attachComponent(entityId, COLLISION_HANDLER_PARAM1, config.param1());
        store.attachComponent(entityId, COLLISION_HANDLER_PARAM2, config.param2());
        store.attachComponent(entityId, COLLISION_HANDLED_TICK, 0);
    }

    @Override
    public Set<Long> findAllColliderEntities() {
        return store.getEntitiesWithComponents(List.of(FLAG));
    }

    @Override
    public void delete(long entityId) {
        for (var component : ALL_COMPONENTS) {
            store.removeComponent(entityId, component);
        }
    }

    @Override
    public float getPositionX(long entityId) {
        return store.getComponent(entityId, ca.samanthaireland.lightning.engine.ext.modules.GridMapModuleFactory.POSITION_X);
    }

    @Override
    public float getPositionY(long entityId) {
        return store.getComponent(entityId, ca.samanthaireland.lightning.engine.ext.modules.GridMapModuleFactory.POSITION_Y);
    }

    @Override
    public int getHandlerType(long entityId) {
        return (int) store.getComponent(entityId, COLLISION_HANDLER_TYPE);
    }

    @Override
    public float getHandlerParam1(long entityId) {
        return store.getComponent(entityId, COLLISION_HANDLER_PARAM1);
    }

    @Override
    public float getHandlerParam2(long entityId) {
        return store.getComponent(entityId, COLLISION_HANDLER_PARAM2);
    }

    @Override
    public long getLastHandledTick(long entityId) {
        return (long) store.getComponent(entityId, COLLISION_HANDLED_TICK);
    }

    @Override
    public void updateHandledTick(long entityId, long tick) {
        store.attachComponent(entityId, COLLISION_HANDLED_TICK, tick);
    }

    @Override
    public void updateCollisionState(long entityId, boolean isColliding, int collisionCount,
                                     long lastCollisionEntity, float normalX, float normalY,
                                     float penetrationDepth) {
        store.attachComponent(entityId, IS_COLLIDING, isColliding ? 1 : 0);
        store.attachComponent(entityId, COLLISION_COUNT, collisionCount);
        store.attachComponent(entityId, LAST_COLLISION_ENTITY, lastCollisionEntity);
        store.attachComponent(entityId, COLLISION_NORMAL_X, normalX);
        store.attachComponent(entityId, COLLISION_NORMAL_Y, normalY);
        store.attachComponent(entityId, PENETRATION_DEPTH, penetrationDepth);
    }

    @Override
    public void resetCollisionState(long entityId) {
        store.attachComponent(entityId, IS_COLLIDING, 0);
        store.attachComponent(entityId, COLLISION_COUNT, 0);
        store.attachComponent(entityId, LAST_COLLISION_ENTITY, 0);
        store.attachComponent(entityId, COLLISION_NORMAL_X, 0);
        store.attachComponent(entityId, COLLISION_NORMAL_Y, 0);
        store.attachComponent(entityId, PENETRATION_DEPTH, 0);
    }
}
