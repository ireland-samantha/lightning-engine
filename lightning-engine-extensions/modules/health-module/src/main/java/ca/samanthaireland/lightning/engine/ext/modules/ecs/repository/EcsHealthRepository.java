package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Health;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.HealthRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ca.samanthaireland.lightning.engine.ext.modules.HealthModuleFactory.*;

/**
 * ECS-backed implementation of HealthRepository.
 *
 * <p>Stores health data as entity components in the EntityComponentStore.
 */
public class EcsHealthRepository implements HealthRepository {

    private final EntityComponentStore store;

    public EcsHealthRepository(EntityComponentStore store) {
        this.store = store;
    }

    @Override
    public void save(long entityId, Health health) {
        store.attachComponent(entityId, CURRENT_HP, health.currentHP());
        store.attachComponent(entityId, MAX_HP, health.maxHP());
        store.attachComponent(entityId, DAMAGE_TAKEN, health.damageTaken());
        store.attachComponent(entityId, IS_DEAD, health.isDead() ? 1.0f : 0);
        store.attachComponent(entityId, INVULNERABLE, health.invulnerable() ? 1.0f : 0);
        store.attachComponent(entityId, FLAG, 1.0f);
    }

    @Override
    public Optional<Health> findByEntityId(long entityId) {
        Set<Long> entities = store.getEntitiesWithComponents(List.of(FLAG));
        if (!entities.contains(entityId)) {
            return Optional.empty();
        }

        float currentHP = store.getComponent(entityId, CURRENT_HP);
        float maxHP = store.getComponent(entityId, MAX_HP);
        float damageTaken = store.getComponent(entityId, DAMAGE_TAKEN);
        boolean isDead = store.getComponent(entityId, IS_DEAD) > 0;
        boolean invulnerable = store.getComponent(entityId, INVULNERABLE) > 0;

        return Optional.of(new Health(currentHP, maxHP, damageTaken, isDead, invulnerable));
    }

    @Override
    public boolean hasHealth(long entityId) {
        Set<Long> entities = store.getEntitiesWithComponents(List.of(FLAG));
        return entities.contains(entityId);
    }

    @Override
    public Set<Long> findAllEntityIds() {
        return store.getEntitiesWithComponents(List.of(FLAG));
    }
}
