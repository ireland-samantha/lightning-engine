package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;

import java.util.List;

/**
 * Module factory for the Projectile module.
 *
 * <p>Provides projectile management:
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
 *
 * <p>Commands:
 * <ul>
 *   <li>spawnProjectile - Create a new projectile</li>
 *   <li>destroyProjectile - Manually destroy a projectile</li>
 * </ul>
 *
 * <p>Note: Uses EntityModule for positions, RigidBodyModule for velocity if available.
 *
 * @see ProjectileComponents for component constants
 */
public class ProjectileModuleFactory implements ModuleFactory {

    // Delegated constants for backwards compatibility
    public static final BaseComponent OWNER_ENTITY_ID = ProjectileComponents.OWNER_ENTITY_ID;
    public static final BaseComponent DAMAGE = ProjectileComponents.DAMAGE;
    public static final BaseComponent SPEED = ProjectileComponents.SPEED;
    public static final BaseComponent DIRECTION_X = ProjectileComponents.DIRECTION_X;
    public static final BaseComponent DIRECTION_Y = ProjectileComponents.DIRECTION_Y;
    public static final BaseComponent LIFETIME = ProjectileComponents.LIFETIME;
    public static final BaseComponent TICKS_ALIVE = ProjectileComponents.TICKS_ALIVE;
    public static final BaseComponent PIERCE_COUNT = ProjectileComponents.PIERCE_COUNT;
    public static final BaseComponent HITS_REMAINING = ProjectileComponents.HITS_REMAINING;
    public static final BaseComponent PROJECTILE_TYPE = ProjectileComponents.PROJECTILE_TYPE;
    public static final BaseComponent PENDING_DESTROY = ProjectileComponents.PENDING_DESTROY;
    public static final BaseComponent FLAG = ProjectileComponents.FLAG;
    public static final List<BaseComponent> CORE_COMPONENTS = ProjectileComponents.CORE_COMPONENTS;
    public static final List<BaseComponent> ALL_COMPONENTS = ProjectileComponents.ALL_COMPONENTS;

    @Override
    public EngineModule create(ModuleContext context) {
        return new ProjectileModule(context);
    }
}
