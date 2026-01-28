package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;

import java.util.List;

/**
 * Module factory for the RigidBody physics module.
 *
 * <p>Provides physics simulation with:
 * <ul>
 *   <li>Velocity tracking (vx, vy, vz)</li>
 *   <li>Acceleration (ax, ay, az)</li>
 *   <li>Mass-based force calculations (F = ma)</li>
 *   <li>Angular velocity and rotation</li>
 *   <li>Drag/damping for realistic movement</li>
 * </ul>
 *
 * <p>The physics system runs each tick:
 * <ol>
 *   <li>Apply forces to calculate acceleration (a = F/m)</li>
 *   <li>Apply acceleration to velocity (v += a * dt)</li>
 *   <li>Apply velocity to position (p += v * dt)</li>
 *   <li>Apply drag to velocity (v *= (1 - drag))</li>
 *   <li>Clear accumulated forces</li>
 * </ol>
 *
 * <p>Note: Position operations use GridMapExports (position components are in GridMapModule).
 *
 * @see RigidBodyComponents for component constants
 */
public class RigidBodyModuleFactory implements ModuleFactory {

    // Delegated constants for backwards compatibility
    public static final BaseComponent VELOCITY_X = RigidBodyComponents.VELOCITY_X;
    public static final BaseComponent VELOCITY_Y = RigidBodyComponents.VELOCITY_Y;
    public static final BaseComponent VELOCITY_Z = RigidBodyComponents.VELOCITY_Z;
    public static final BaseComponent ACCELERATION_X = RigidBodyComponents.ACCELERATION_X;
    public static final BaseComponent ACCELERATION_Y = RigidBodyComponents.ACCELERATION_Y;
    public static final BaseComponent ACCELERATION_Z = RigidBodyComponents.ACCELERATION_Z;
    public static final BaseComponent FORCE_X = RigidBodyComponents.FORCE_X;
    public static final BaseComponent FORCE_Y = RigidBodyComponents.FORCE_Y;
    public static final BaseComponent FORCE_Z = RigidBodyComponents.FORCE_Z;
    public static final BaseComponent MASS = RigidBodyComponents.MASS;
    public static final BaseComponent ANGULAR_VELOCITY = RigidBodyComponents.ANGULAR_VELOCITY;
    public static final BaseComponent ROTATION = RigidBodyComponents.ROTATION;
    public static final BaseComponent TORQUE = RigidBodyComponents.TORQUE;
    public static final BaseComponent INERTIA = RigidBodyComponents.INERTIA;
    public static final BaseComponent LINEAR_DRAG = RigidBodyComponents.LINEAR_DRAG;
    public static final BaseComponent ANGULAR_DRAG = RigidBodyComponents.ANGULAR_DRAG;
    public static final BaseComponent FLAG = RigidBodyComponents.FLAG;
    public static final List<BaseComponent> VELOCITY_COMPONENTS = RigidBodyComponents.VELOCITY_COMPONENTS;
    public static final List<BaseComponent> ACCELERATION_COMPONENTS = RigidBodyComponents.ACCELERATION_COMPONENTS;
    public static final List<BaseComponent> FORCE_COMPONENTS = RigidBodyComponents.FORCE_COMPONENTS;
    public static final List<BaseComponent> CORE_COMPONENTS = RigidBodyComponents.CORE_COMPONENTS;
    public static final List<BaseComponent> ALL_COMPONENTS = RigidBodyComponents.ALL_COMPONENTS;

    @Override
    public EngineModule create(ModuleContext context) {
        return new RigidBodyModule(context);
    }
}
