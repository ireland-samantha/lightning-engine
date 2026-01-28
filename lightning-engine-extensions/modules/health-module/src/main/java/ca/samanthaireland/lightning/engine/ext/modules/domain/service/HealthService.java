package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.ext.modules.domain.Health;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.HealthRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;

/**
 * Domain service for health operations.
 */
@Slf4j
public class HealthService {

    private final HealthRepository healthRepository;

    public HealthService(HealthRepository healthRepository) {
        this.healthRepository = healthRepository;
    }

    /**
     * Attach health to an entity.
     *
     * @param entityId the entity to attach health to
     * @param maxHP the maximum HP
     * @param currentHP the starting HP
     * @throws IllegalArgumentException if maxHP is not positive
     * @throws IllegalArgumentException if currentHP is negative
     */
    public void attachHealth(long entityId, float maxHP, float currentHP) {
        Health health = Health.create(maxHP, currentHP);
        healthRepository.save(entityId, health);
        log.info("Attached health to entity {}: {}/{} HP", entityId, currentHP, maxHP);
    }

    /**
     * Queue damage to an entity.
     *
     * <p>Damage is accumulated and processed at end of tick.
     *
     * @param entityId the entity to damage
     * @param amount the amount of damage (must be positive)
     * @return true if damage was queued, false if entity has no health
     */
    public boolean damage(long entityId, float amount) {
        if (amount <= 0) {
            log.warn("damage: invalid amount {}", amount);
            return false;
        }

        Optional<Health> healthOpt = healthRepository.findByEntityId(entityId);
        if (healthOpt.isEmpty()) {
            log.warn("damage: entity {} does not have health", entityId);
            return false;
        }

        Health health = healthOpt.get();
        Health updated = health.withDamage(amount);
        healthRepository.save(entityId, updated);

        log.debug("Queued {} damage to entity {}", amount, entityId);
        return true;
    }

    /**
     * Heal an entity.
     *
     * @param entityId the entity to heal
     * @param amount the amount to heal (must be positive)
     * @return true if healing was applied, false if entity has no health or is dead
     */
    public boolean heal(long entityId, float amount) {
        if (amount <= 0) {
            log.warn("heal: invalid amount {}", amount);
            return false;
        }

        Optional<Health> healthOpt = healthRepository.findByEntityId(entityId);
        if (healthOpt.isEmpty()) {
            log.warn("heal: entity {} does not have health", entityId);
            return false;
        }

        Health health = healthOpt.get();
        if (health.isDead()) {
            log.warn("heal: entity {} is dead", entityId);
            return false;
        }

        Health updated = health.withHealing(amount);
        healthRepository.save(entityId, updated);

        log.debug("Healed entity {} by {}, HP now {}/{}", entityId, amount, updated.currentHP(), updated.maxHP());
        return true;
    }

    /**
     * Set invulnerability on an entity.
     *
     * @param entityId the entity to modify
     * @param invulnerable whether the entity should be invulnerable
     * @return true if set successfully, false if entity has no health
     */
    public boolean setInvulnerable(long entityId, boolean invulnerable) {
        Optional<Health> healthOpt = healthRepository.findByEntityId(entityId);
        if (healthOpt.isEmpty()) {
            log.warn("setInvulnerable: entity {} does not have health", entityId);
            return false;
        }

        Health health = healthOpt.get();
        Health updated = health.withInvulnerable(invulnerable);
        healthRepository.save(entityId, updated);

        log.debug("Set entity {} invulnerable={}", entityId, invulnerable);
        return true;
    }

    /**
     * Process damage for all entities with health.
     *
     * <p>For each entity:
     * <ol>
     *   <li>Skip if dead</li>
     *   <li>Skip damage if invulnerable</li>
     *   <li>Apply accumulated damage</li>
     *   <li>Set dead flag if HP reaches 0</li>
     *   <li>Clear damage queue</li>
     * </ol>
     */
    public void processDamage() {
        Set<Long> entities = healthRepository.findAllEntityIds();

        for (Long entityId : entities) {
            Optional<Health> healthOpt = healthRepository.findByEntityId(entityId);
            if (healthOpt.isEmpty()) {
                continue;
            }

            Health health = healthOpt.get();
            if (health.isDead()) {
                continue;
            }

            Health processed = health.processDamage();
            healthRepository.save(entityId, processed);

            if (processed.isDead() && !health.isDead()) {
                log.info("Entity {} has died", entityId);
            } else if (health.damageTaken() > 0 && !health.invulnerable()) {
                log.debug("Entity {} took {} damage, HP now {}/{}",
                        entityId, health.damageTaken(), processed.currentHP(), processed.maxHP());
            }
        }
    }

    /**
     * Get the health state of an entity.
     *
     * @param entityId the entity ID
     * @return the health state if the entity has health attached
     */
    public Optional<Health> getHealth(long entityId) {
        return healthRepository.findByEntityId(entityId);
    }

    /**
     * Check if an entity has health attached.
     *
     * @param entityId the entity ID
     * @return true if the entity has health
     */
    public boolean hasHealth(long entityId) {
        return healthRepository.hasHealth(entityId);
    }
}
