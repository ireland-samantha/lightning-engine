package ca.samanthaireland.lightning.engine.ext.modules.domain;

/**
 * Domain entity representing an entity's health state.
 *
 * <p>Health consists of:
 * <ul>
 *   <li>currentHP - Current hit points</li>
 *   <li>maxHP - Maximum hit points</li>
 *   <li>damageTaken - Damage accumulated this tick (cleared after processing)</li>
 *   <li>isDead - Whether the entity has died (HP <= 0)</li>
 *   <li>invulnerable - Whether the entity is immune to damage</li>
 * </ul>
 */
public record Health(
        float currentHP,
        float maxHP,
        float damageTaken,
        boolean isDead,
        boolean invulnerable
) {

    /**
     * Creates a new Health instance with validation.
     *
     * @throws IllegalArgumentException if maxHP is not positive
     * @throws IllegalArgumentException if currentHP is negative
     */
    public Health {
        if (maxHP <= 0) {
            throw new IllegalArgumentException("Max HP must be positive, got: " + maxHP);
        }
        if (currentHP < 0) {
            throw new IllegalArgumentException("Current HP cannot be negative, got: " + currentHP);
        }
    }

    /**
     * Creates a new health state for attaching to an entity.
     *
     * @param maxHP the maximum HP
     * @param currentHP the starting HP
     * @return the new health state
     */
    public static Health create(float maxHP, float currentHP) {
        return new Health(currentHP, maxHP, 0, false, false);
    }

    /**
     * Creates a new health state with full HP.
     *
     * @param maxHP the maximum HP
     * @return the new health state
     */
    public static Health create(float maxHP) {
        return create(maxHP, maxHP);
    }

    /**
     * Creates a copy with updated current HP, clamped to [0, maxHP].
     *
     * @param newCurrentHP the new current HP
     * @return updated health state
     */
    public Health withCurrentHP(float newCurrentHP) {
        float clamped = Math.max(0, Math.min(newCurrentHP, maxHP));
        boolean dead = clamped <= 0;
        return new Health(clamped, maxHP, damageTaken, dead, invulnerable);
    }

    /**
     * Creates a copy with damage added to the damage queue.
     *
     * @param amount the amount of damage to queue
     * @return updated health state
     */
    public Health withDamage(float amount) {
        if (amount <= 0) {
            return this;
        }
        return new Health(currentHP, maxHP, damageTaken + amount, isDead, invulnerable);
    }

    /**
     * Creates a copy with healing applied, capped at maxHP.
     *
     * @param amount the amount to heal
     * @return updated health state
     */
    public Health withHealing(float amount) {
        if (amount <= 0 || isDead) {
            return this;
        }
        float newHP = Math.min(currentHP + amount, maxHP);
        return new Health(newHP, maxHP, damageTaken, isDead, invulnerable);
    }

    /**
     * Creates a copy with invulnerability set.
     *
     * @param isInvulnerable whether the entity is invulnerable
     * @return updated health state
     */
    public Health withInvulnerable(boolean isInvulnerable) {
        return new Health(currentHP, maxHP, damageTaken, isDead, isInvulnerable);
    }

    /**
     * Creates a copy with damage cleared (for end of tick).
     *
     * @return updated health state
     */
    public Health withDamageCleared() {
        return new Health(currentHP, maxHP, 0, isDead, invulnerable);
    }

    /**
     * Processes accumulated damage and returns the new health state.
     *
     * <p>If invulnerable, damage is ignored.
     * <p>If dead, no processing occurs.
     * <p>HP is clamped to 0 and isDead is set if HP reaches 0.
     *
     * @return the new health state after processing damage
     */
    public Health processDamage() {
        if (isDead || damageTaken <= 0 || invulnerable) {
            return withDamageCleared();
        }

        float newHP = currentHP - damageTaken;
        if (newHP <= 0) {
            return new Health(0, maxHP, 0, true, invulnerable);
        }

        return new Health(newHP, maxHP, 0, false, invulnerable);
    }

    /**
     * Checks if the entity has health attached (always true for a Health instance).
     */
    public boolean hasHealth() {
        return true;
    }
}
