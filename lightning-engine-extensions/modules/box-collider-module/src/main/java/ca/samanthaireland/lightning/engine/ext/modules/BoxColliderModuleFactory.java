package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;

import java.util.List;

/**
 * Module factory for the BoxCollider module.
 *
 * <p>Provides axis-aligned bounding box (AABB) collision detection:
 * <ul>
 *   <li>Box dimensions (width, height, depth)</li>
 *   <li>Offset from entity position</li>
 *   <li>Layer-based collision filtering</li>
 *   <li>Trigger vs solid colliders</li>
 *   <li>Collision events (entity IDs of colliding objects)</li>
 * </ul>
 *
 * <p>Collision detection is O(n^2) - suitable for small numbers of entities.
 * For larger numbers, consider spatial partitioning (quadtree, spatial hash).
 *
 * <p>Uses EntityModule's shared position components (POSITION_X/Y/Z) for
 * determining entity positions.
 *
 * @see BoxColliderComponents for component constants
 */
public class BoxColliderModuleFactory implements ModuleFactory {

    // Delegated constants for backwards compatibility
    public static final BaseComponent BOX_WIDTH = BoxColliderComponents.BOX_WIDTH;
    public static final BaseComponent BOX_HEIGHT = BoxColliderComponents.BOX_HEIGHT;
    public static final BaseComponent BOX_DEPTH = BoxColliderComponents.BOX_DEPTH;
    public static final BaseComponent OFFSET_X = BoxColliderComponents.OFFSET_X;
    public static final BaseComponent OFFSET_Y = BoxColliderComponents.OFFSET_Y;
    public static final BaseComponent OFFSET_Z = BoxColliderComponents.OFFSET_Z;
    public static final BaseComponent COLLISION_LAYER = BoxColliderComponents.COLLISION_LAYER;
    public static final BaseComponent COLLISION_MASK = BoxColliderComponents.COLLISION_MASK;
    public static final BaseComponent IS_TRIGGER = BoxColliderComponents.IS_TRIGGER;
    public static final BaseComponent IS_COLLIDING = BoxColliderComponents.IS_COLLIDING;
    public static final BaseComponent COLLISION_COUNT = BoxColliderComponents.COLLISION_COUNT;
    public static final BaseComponent LAST_COLLISION_ENTITY = BoxColliderComponents.LAST_COLLISION_ENTITY;
    public static final BaseComponent COLLISION_NORMAL_X = BoxColliderComponents.COLLISION_NORMAL_X;
    public static final BaseComponent COLLISION_NORMAL_Y = BoxColliderComponents.COLLISION_NORMAL_Y;
    public static final BaseComponent PENETRATION_DEPTH = BoxColliderComponents.PENETRATION_DEPTH;
    public static final BaseComponent COLLISION_HANDLER_TYPE = BoxColliderComponents.COLLISION_HANDLER_TYPE;
    public static final BaseComponent COLLISION_HANDLER_PARAM1 = BoxColliderComponents.COLLISION_HANDLER_PARAM1;
    public static final BaseComponent COLLISION_HANDLER_PARAM2 = BoxColliderComponents.COLLISION_HANDLER_PARAM2;
    public static final BaseComponent COLLISION_HANDLED_TICK = BoxColliderComponents.COLLISION_HANDLED_TICK;
    public static final BaseComponent FLAG = BoxColliderComponents.FLAG;
    public static final int HANDLER_NONE = BoxColliderComponents.HANDLER_NONE;
    public static final List<BaseComponent> DIMENSION_COMPONENTS = BoxColliderComponents.DIMENSION_COMPONENTS;
    public static final List<BaseComponent> OFFSET_COMPONENTS = BoxColliderComponents.OFFSET_COMPONENTS;
    public static final List<BaseComponent> COLLISION_STATE_COMPONENTS = BoxColliderComponents.COLLISION_STATE_COMPONENTS;
    public static final List<BaseComponent> HANDLER_COMPONENTS = BoxColliderComponents.HANDLER_COMPONENTS;
    public static final List<BaseComponent> ALL_COMPONENTS = BoxColliderComponents.ALL_COMPONENTS;

    @Override
    public EngineModule create(ModuleContext context) {
        return new BoxColliderModule(context);
    }
}
