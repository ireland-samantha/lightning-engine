package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionLevel;
import ca.samanthaireland.lightning.engine.util.IdGeneratorV2;

import java.util.List;

/**
 * Component constants for the Health module.
 *
 * <p>Provides health/damage management components:
 * <ul>
 *   <li>CURRENT_HP - Current hit points</li>
 *   <li>MAX_HP - Maximum hit points</li>
 *   <li>DAMAGE_TAKEN - Damage accumulated this tick (cleared after processing)</li>
 *   <li>IS_DEAD - Flag indicating entity has died (HP <= 0)</li>
 *   <li>INVULNERABLE - Flag to prevent damage</li>
 * </ul>
 */
public final class HealthComponents {

    private HealthComponents() {
        // Constants class
    }

    // Health components
    public static final BaseComponent CURRENT_HP = new HealthComponent(
            IdGeneratorV2.newId(), "CURRENT_HP");
    public static final BaseComponent MAX_HP = new HealthComponent(
            IdGeneratorV2.newId(), "MAX_HP");
    public static final BaseComponent DAMAGE_TAKEN = new HealthComponent(
            IdGeneratorV2.newId(), "DAMAGE_TAKEN");
    public static final BaseComponent IS_DEAD = new HealthComponent(
            IdGeneratorV2.newId(), "IS_DEAD");
    public static final BaseComponent INVULNERABLE = new HealthComponent(
            IdGeneratorV2.newId(), "INVULNERABLE");

    // Module flag (PRIVATE - only EntityModule with superuser can attach during spawn)
    public static final BaseComponent FLAG = new PermissionComponent(
            IdGeneratorV2.newId(), "health", PermissionLevel.PRIVATE);

    /**
     * Core components for health tracking.
     */
    public static final List<BaseComponent> CORE_COMPONENTS = List.of(
            CURRENT_HP, MAX_HP, DAMAGE_TAKEN, IS_DEAD, INVULNERABLE
    );

    /**
     * Components for snapshot export.
     */
    public static final List<BaseComponent> ALL_COMPONENTS = CORE_COMPONENTS;
}
