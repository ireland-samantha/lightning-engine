package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Entity;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.EntityRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ca.samanthaireland.lightning.engine.ext.modules.EntityModuleFactory.*;

/**
 * ECS-backed implementation of EntityRepository.
 *
 * <p>Stores entity data as components in the EntityComponentStore.
 *
 * <p>Uses lazy store access via ModuleContext to ensure the repository
 * always uses the properly configured store with JWT permissions.
 */
public class EcsEntityRepository implements EntityRepository {

    private final ModuleContext context;

    public EcsEntityRepository(ModuleContext context) {
        this.context = context;
    }

    private EntityComponentStore store() {
        return context.getEntityComponentStore();
    }

    @Override
    public Entity save(long matchId, Entity entity) {
        long entityId = store().createEntityForMatch(matchId);
        store().attachComponents(entityId, CORE_COMPONENTS,
                new float[]{entity.entityType(), entity.playerId(), entity.playerId()});
        return new Entity(entityId, entity.entityType(), entity.playerId());
    }

    @Override
    public Optional<Entity> findById(long entityId) {
        Set<Long> entities = store().getEntitiesWithComponents(List.of(ENTITY_TYPE));
        if (!entities.contains(entityId)) {
            return Optional.empty();
        }

        long entityType = (long) store().getComponent(entityId, ENTITY_TYPE);
        long playerId = (long) store().getComponent(entityId, PLAYER_ID);

        return Optional.of(new Entity(entityId, entityType, playerId));
    }
}
