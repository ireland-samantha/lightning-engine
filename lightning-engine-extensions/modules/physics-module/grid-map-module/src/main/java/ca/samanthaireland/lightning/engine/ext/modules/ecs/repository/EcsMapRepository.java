package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.modules.domain.GridMap;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.MapRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ca.samanthaireland.lightning.engine.ext.modules.GridMapModuleFactory.*;

/**
 * ECS-backed implementation of MapRepository.
 *
 * <p>Stores map data as entity components in the EntityComponentStore.
 *
 * <p>Note: Gets the store dynamically from context to avoid stale reference issues
 * when the module's JWT token is updated.
 */
public class EcsMapRepository implements MapRepository {

    private final ModuleContext context;

    public EcsMapRepository(ModuleContext context) {
        this.context = context;
    }

    private EntityComponentStore getStore() {
        return context.getEntityComponentStore();
    }

    @Override
    public GridMap save(long matchId, GridMap gridMap) {
        EntityComponentStore store = getStore();
        long mapEntityId = store.createEntityForMatch(matchId);
        store.attachComponents(mapEntityId, MAP_COMPONENTS,
                new float[]{gridMap.width(), gridMap.height(), gridMap.depth(), 1.0f});
        return new GridMap(mapEntityId, gridMap.width(), gridMap.height(), gridMap.depth());
    }

    @Override
    public Optional<GridMap> findById(long mapId) {
        EntityComponentStore store = getStore();
        Set<Long> mapEntities = store.getEntitiesWithComponents(List.of(MAP_ENTITY));
        if (!mapEntities.contains(mapId)) {
            return Optional.empty();
        }

        int width = (int) store.getComponent(mapId, MAP_WIDTH);
        int height = (int) store.getComponent(mapId, MAP_HEIGHT);
        int depth = (int) store.getComponent(mapId, MAP_DEPTH);

        return Optional.of(new GridMap(mapId, width, height, depth));
    }

    @Override
    public boolean exists(long mapId) {
        Set<Long> mapEntities = getStore().getEntitiesWithComponents(List.of(MAP_ENTITY));
        return mapEntities.contains(mapId);
    }
}
