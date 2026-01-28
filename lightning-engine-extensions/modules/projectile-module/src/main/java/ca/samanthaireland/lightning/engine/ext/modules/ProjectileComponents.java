package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionLevel;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.component.ProjectileComponent;
import ca.samanthaireland.lightning.engine.util.IdGeneratorV2;

import java.util.List;

/**
 * Component constants for the Projectile module.
 *
 * <p>Provides projectile management components:
 * <ul>
 *   <li>OWNER_ENTITY_ID - Entity that fired the projectile</li>
 *   <li>DAMAGE - Damage dealt on hit</li>
 *   <li>SPEED - Projectile speed</li>
 *   <li>DIRECTION_X/Y - Normalized direction vector</li>
 *   <li>LIFETIME - Ticks until auto-destroy (0 = no limit)</li>
 *   <li>TICKS_ALIVE - Current age in ticks</li>
 *   <li>PIERCE_COUNT - Number of targets to pierce (0 = destroy on first hit)</li>
 *   <li>HITS_REMAINING - Remaining pierce count</li>
 * </ul>
 */
public final class ProjectileComponents {

    private ProjectileComponents() {
        // Constants class
    }

    // Projectile metadata
    public static final BaseComponent OWNER_ENTITY_ID = new ProjectileComponent(
            IdGeneratorV2.newId(), "OWNER_ENTITY_ID");
    public static final BaseComponent DAMAGE = new ProjectileComponent(
            IdGeneratorV2.newId(), "DAMAGE");

    // Movement
    public static final BaseComponent SPEED = new ProjectileComponent(
            IdGeneratorV2.newId(), "SPEED");
    public static final BaseComponent DIRECTION_X = new ProjectileComponent(
            IdGeneratorV2.newId(), "DIRECTION_X");
    public static final BaseComponent DIRECTION_Y = new ProjectileComponent(
            IdGeneratorV2.newId(), "DIRECTION_Y");

    // Lifetime management
    public static final BaseComponent LIFETIME = new ProjectileComponent(
            IdGeneratorV2.newId(), "LIFETIME");
    public static final BaseComponent TICKS_ALIVE = new ProjectileComponent(
            IdGeneratorV2.newId(), "TICKS_ALIVE");

    // Piercing
    public static final BaseComponent PIERCE_COUNT = new ProjectileComponent(
            IdGeneratorV2.newId(), "PIERCE_COUNT");
    public static final BaseComponent HITS_REMAINING = new ProjectileComponent(
            IdGeneratorV2.newId(), "HITS_REMAINING");

    // Projectile type (for different projectile behaviors)
    public static final BaseComponent PROJECTILE_TYPE = new ProjectileComponent(
            IdGeneratorV2.newId(), "PROJECTILE_TYPE");

    // Pending destroy flag
    public static final BaseComponent PENDING_DESTROY = new ProjectileComponent(
            IdGeneratorV2.newId(), "PENDING_DESTROY");

    // Module flag (PRIVATE - only EntityModule with superuser can attach during spawn)
    public static final BaseComponent FLAG = new PermissionComponent(
            IdGeneratorV2.newId(), "projectile", PermissionLevel.PRIVATE);

    /**
     * Core components for projectile tracking.
     */
    public static final List<BaseComponent> CORE_COMPONENTS = List.of(
            OWNER_ENTITY_ID, DAMAGE, SPEED,
            DIRECTION_X, DIRECTION_Y,
            LIFETIME, TICKS_ALIVE,
            PIERCE_COUNT, HITS_REMAINING,
            PROJECTILE_TYPE, PENDING_DESTROY
    );

    /**
     * Components for snapshot export.
     */
    public static final List<BaseComponent> ALL_COMPONENTS = CORE_COMPONENTS;
}
