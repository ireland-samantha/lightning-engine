package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.core.match.Match;
import ca.samanthaireland.lightning.engine.core.match.MatchService;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.module.ModuleResolver;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Entity;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.EntityRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Domain service for entity spawn operations.
 *
 * <p>Note: Position management is now handled by GridMapModule.
 * Use GridMapExports.setPosition() to set entity positions after spawning.
 *
 * <p>Uses lazy store access via ModuleContext to ensure proper JWT permissions
 * are always applied when accessing the component store.
 */
@Slf4j
public class EntityService {

    private final EntityRepository entityRepository;
    private final MatchService matchService;
    private final ModuleResolver moduleResolver;
    private final ModuleContext context;

    public EntityService(
            EntityRepository entityRepository,
            MatchService matchService,
            ModuleResolver moduleResolver,
            ModuleContext context) {
        this.entityRepository = entityRepository;
        this.matchService = matchService;
        this.moduleResolver = moduleResolver;
        this.context = context;
    }

    private EntityComponentStore store() {
        return context.getEntityComponentStore();
    }

    /**
     * Spawn a new entity in the given match.
     *
     * <p>Note: Position is not set by this method. Use GridMapExports.setPosition()
     * to set the entity's position after spawning.
     *
     * @param matchId the match to spawn the entity in
     * @param entityType the type of entity
     * @param playerId the player who owns this entity
     * @return the spawned entity with assigned ID
     */
    public Entity spawn(long matchId, long entityType, long playerId) {
        Entity entity = Entity.create(entityType, playerId);
        Entity savedEntity = entityRepository.save(matchId, entity);

        attachModuleFlags(savedEntity.id(), matchId);

        log.info("Spawned entity {} for match {} with type {}",
                savedEntity.id(), matchId, entityType);

        return savedEntity;
    }

    /**
     * Find an entity by ID.
     *
     * @param entityId the entity ID
     * @return the entity if found
     */
    public Optional<Entity> findById(long entityId) {
        return entityRepository.findById(entityId);
    }

    private void attachModuleFlags(long entityId, long matchId) {
        if (matchService == null) {
            log.warn("No match service found");
            return;
        }
        Optional<Match> matchOpt = matchService.getMatch(matchId);
        if (matchOpt.isPresent()) {
            Match match = matchOpt.get();
            if (moduleResolver == null) {
                log.warn("No module resolver found");
                return;
            }
            for (String module : match.enabledModules()) {
                EngineModule moduleFound = moduleResolver.resolveModule(module);
                if (moduleFound != null) {
                    BaseComponent flagComponent = moduleFound.createFlagComponent();
                    if (flagComponent != null) {
                        store().attachComponent(entityId, flagComponent, 1.0f);
                        log.debug("Attached flag {} to entity {}", flagComponent.getName(), entityId);
                    }
                }
            }
        }
    }
}
