package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;

import java.util.List;

/**
 * Module factory for the Health module.
 *
 * <p>Provides health/damage management:
 * <ul>
 *   <li>CURRENT_HP - Current hit points</li>
 *   <li>MAX_HP - Maximum hit points</li>
 *   <li>DAMAGE_TAKEN - Damage accumulated this tick (cleared after processing)</li>
 *   <li>IS_DEAD - Flag indicating entity has died (HP <= 0)</li>
 *   <li>INVULNERABLE - Flag to prevent damage</li>
 * </ul>
 *
 * <p>Commands:
 * <ul>
 *   <li>attachHealth - Attach health to an entity with maxHP</li>
 *   <li>damage - Deal damage to an entity</li>
 *   <li>heal - Restore health to an entity</li>
 *   <li>setInvulnerable - Toggle invulnerability</li>
 * </ul>
 *
 * @see HealthComponents for component constants
 */
public class HealthModuleFactory implements ModuleFactory {

    // Delegated constants for backwards compatibility
    public static final BaseComponent CURRENT_HP = HealthComponents.CURRENT_HP;
    public static final BaseComponent MAX_HP = HealthComponents.MAX_HP;
    public static final BaseComponent DAMAGE_TAKEN = HealthComponents.DAMAGE_TAKEN;
    public static final BaseComponent IS_DEAD = HealthComponents.IS_DEAD;
    public static final BaseComponent INVULNERABLE = HealthComponents.INVULNERABLE;
    public static final BaseComponent FLAG = HealthComponents.FLAG;
    public static final List<BaseComponent> CORE_COMPONENTS = HealthComponents.CORE_COMPONENTS;
    public static final List<BaseComponent> ALL_COMPONENTS = HealthComponents.ALL_COMPONENTS;

    @Override
    public EngineModule create(ModuleContext context) {
        return new HealthModule(context);
    }
}
