package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.modules.EntityModuleFactory;
import ca.samanthaireland.lightning.engine.ext.modules.GridMapModuleFactory;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Projectile;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.ProjectileRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ca.samanthaireland.lightning.engine.ext.modules.ProjectileModuleFactory.*;

/**
 * ECS-backed implementation of ProjectileRepository.
 *
 * <p>Stores projectile data as entity components in the EntityComponentStore.
 */
public class EcsProjectileRepository implements ProjectileRepository {

    private final EntityComponentStore store;

    public EcsProjectileRepository(EntityComponentStore store) {
        this.store = store;
    }

    @Override
    public Projectile save(long matchId, Projectile projectile) {
        // Create entity with match ID automatically attached
        long entityId = store.createEntityForMatch(matchId);

        // Attach entity type components
        store.attachComponents(entityId, EntityModuleFactory.CORE_COMPONENTS,
                new float[]{projectile.projectileType(), projectile.ownerEntityId(), projectile.ownerEntityId()});

        // Attach position components
        store.attachComponents(entityId, GridMapModuleFactory.POSITION_COMPONENTS,
                new float[]{projectile.positionX(), projectile.positionY(), 0});

        // Attach projectile-specific components
        store.attachComponent(entityId, OWNER_ENTITY_ID, projectile.ownerEntityId());
        store.attachComponent(entityId, DAMAGE, projectile.damage());
        store.attachComponent(entityId, SPEED, projectile.speed());
        store.attachComponent(entityId, DIRECTION_X, projectile.directionX());
        store.attachComponent(entityId, DIRECTION_Y, projectile.directionY());
        store.attachComponent(entityId, LIFETIME, projectile.lifetime());
        store.attachComponent(entityId, TICKS_ALIVE, projectile.ticksAlive());
        store.attachComponent(entityId, PIERCE_COUNT, projectile.pierceCount());
        store.attachComponent(entityId, HITS_REMAINING, projectile.hitsRemaining());
        store.attachComponent(entityId, PROJECTILE_TYPE, projectile.projectileType());
        store.attachComponent(entityId, PENDING_DESTROY, projectile.pendingDestroy() ? 1.0f : 0.0f);
        store.attachComponent(entityId, FLAG, 1.0f);

        return projectile.withId(entityId);
    }

    @Override
    public void update(Projectile projectile) {
        long entityId = projectile.id();

        // Update position via EntityModule's position components
        store.attachComponent(entityId, GridMapModuleFactory.POSITION_X, projectile.positionX());
        store.attachComponent(entityId, GridMapModuleFactory.POSITION_Y, projectile.positionY());

        // Update projectile-specific components
        store.attachComponent(entityId, DIRECTION_X, projectile.directionX());
        store.attachComponent(entityId, DIRECTION_Y, projectile.directionY());
        store.attachComponent(entityId, TICKS_ALIVE, projectile.ticksAlive());
        store.attachComponent(entityId, HITS_REMAINING, projectile.hitsRemaining());
        store.attachComponent(entityId, PENDING_DESTROY, projectile.pendingDestroy() ? 1.0f : 0.0f);
    }

    @Override
    public Optional<Projectile> findById(long projectileId) {
        Set<Long> projectileEntities = store.getEntitiesWithComponents(List.of(FLAG));
        if (!projectileEntities.contains(projectileId)) {
            return Optional.empty();
        }

        float ownerEntityId = store.getComponent(projectileId, OWNER_ENTITY_ID);
        float posX = store.getComponent(projectileId, GridMapModuleFactory.POSITION_X);
        float posY = store.getComponent(projectileId, GridMapModuleFactory.POSITION_Y);
        float dirX = store.getComponent(projectileId, DIRECTION_X);
        float dirY = store.getComponent(projectileId, DIRECTION_Y);
        float speed = store.getComponent(projectileId, SPEED);
        float damage = store.getComponent(projectileId, DAMAGE);
        float lifetime = store.getComponent(projectileId, LIFETIME);
        float ticksAlive = store.getComponent(projectileId, TICKS_ALIVE);
        float pierceCount = store.getComponent(projectileId, PIERCE_COUNT);
        float hitsRemaining = store.getComponent(projectileId, HITS_REMAINING);
        float projectileType = store.getComponent(projectileId, PROJECTILE_TYPE);
        float pendingDestroy = store.getComponent(projectileId, PENDING_DESTROY);

        return Optional.of(new Projectile(
                projectileId,
                (long) ownerEntityId,
                posX,
                posY,
                dirX,
                dirY,
                speed,
                damage,
                lifetime,
                ticksAlive,
                pierceCount,
                hitsRemaining,
                projectileType,
                pendingDestroy > 0
        ));
    }

    @Override
    public Set<Long> findAllIds() {
        return store.getEntitiesWithComponents(List.of(FLAG));
    }

    @Override
    public void delete(long projectileId) {
        // Remove all projectile components
        for (var component : CORE_COMPONENTS) {
            store.removeComponent(projectileId, component);
        }
        store.removeComponent(projectileId, FLAG);
    }

    @Override
    public void markPendingDestroy(long projectileId) {
        store.attachComponent(projectileId, PENDING_DESTROY, 1.0f);
    }

    @Override
    public boolean exists(long projectileId) {
        Set<Long> projectileEntities = store.getEntitiesWithComponents(List.of(FLAG));
        return projectileEntities.contains(projectileId);
    }
}
