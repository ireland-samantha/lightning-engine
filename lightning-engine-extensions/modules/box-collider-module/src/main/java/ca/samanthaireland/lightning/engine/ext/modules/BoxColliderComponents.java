package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionLevel;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.component.ColliderComponent;
import ca.samanthaireland.lightning.engine.util.IdGeneratorV2;

import java.util.List;

/**
 * Component constants for the BoxCollider module.
 *
 * <p>Provides axis-aligned bounding box (AABB) collision detection components:
 * <ul>
 *   <li>Box dimensions (width, height, depth)</li>
 *   <li>Offset from entity position</li>
 *   <li>Layer-based collision filtering</li>
 *   <li>Trigger vs solid colliders</li>
 *   <li>Collision events (entity IDs of colliding objects)</li>
 * </ul>
 */
public final class BoxColliderComponents {

    private BoxColliderComponents() {
        // Constants class
    }

    // Box dimensions
    public static final BaseComponent BOX_WIDTH = new ColliderComponent(
            IdGeneratorV2.newId(), "BOX_WIDTH");
    public static final BaseComponent BOX_HEIGHT = new ColliderComponent(
            IdGeneratorV2.newId(), "BOX_HEIGHT");
    public static final BaseComponent BOX_DEPTH = new ColliderComponent(
            IdGeneratorV2.newId(), "BOX_DEPTH");

    // Offset from entity position (collider center = position + offset)
    public static final BaseComponent OFFSET_X = new ColliderComponent(
            IdGeneratorV2.newId(), "OFFSET_X");
    public static final BaseComponent OFFSET_Y = new ColliderComponent(
            IdGeneratorV2.newId(), "OFFSET_Y");
    public static final BaseComponent OFFSET_Z = new ColliderComponent(
            IdGeneratorV2.newId(), "OFFSET_Z");

    // Collision filtering
    public static final BaseComponent COLLISION_LAYER = new ColliderComponent(
            IdGeneratorV2.newId(), "COLLISION_LAYER");
    public static final BaseComponent COLLISION_MASK = new ColliderComponent(
            IdGeneratorV2.newId(), "COLLISION_MASK");

    // Trigger mode (1 = trigger, 0 = solid)
    public static final BaseComponent IS_TRIGGER = new ColliderComponent(
            IdGeneratorV2.newId(), "IS_TRIGGER");

    // Collision state (updated each tick)
    public static final BaseComponent IS_COLLIDING = new ColliderComponent(
            IdGeneratorV2.newId(), "IS_COLLIDING");
    public static final BaseComponent COLLISION_COUNT = new ColliderComponent(
            IdGeneratorV2.newId(), "COLLISION_COUNT");
    public static final BaseComponent LAST_COLLISION_ENTITY = new ColliderComponent(
            IdGeneratorV2.newId(), "LAST_COLLISION_ENTITY");

    // Collision normal (direction to push out)
    public static final BaseComponent COLLISION_NORMAL_X = new ColliderComponent(
            IdGeneratorV2.newId(), "COLLISION_NORMAL_X");
    public static final BaseComponent COLLISION_NORMAL_Y = new ColliderComponent(
            IdGeneratorV2.newId(), "COLLISION_NORMAL_Y");

    // Penetration depth
    public static final BaseComponent PENETRATION_DEPTH = new ColliderComponent(
            IdGeneratorV2.newId(), "PENETRATION_DEPTH");

    // Collision handler components
    public static final BaseComponent COLLISION_HANDLER_TYPE = new ColliderComponent(
            IdGeneratorV2.newId(), "COLLISION_HANDLER_TYPE");
    public static final BaseComponent COLLISION_HANDLER_PARAM1 = new ColliderComponent(
            IdGeneratorV2.newId(), "COLLISION_HANDLER_PARAM1");
    public static final BaseComponent COLLISION_HANDLER_PARAM2 = new ColliderComponent(
            IdGeneratorV2.newId(), "COLLISION_HANDLER_PARAM2");
    public static final BaseComponent COLLISION_HANDLED_TICK = new ColliderComponent(
            IdGeneratorV2.newId(), "COLLISION_HANDLED_TICK");

    // Module flag (PRIVATE - only EntityModule with superuser can attach during spawn)
    public static final BaseComponent FLAG = new PermissionComponent(
            IdGeneratorV2.newId(), "boxCollider", PermissionLevel.PRIVATE);

    // Collision handler type constant
    public static final int HANDLER_NONE = 0;

    // Component groups
    public static final List<BaseComponent> DIMENSION_COMPONENTS =
            List.of(BOX_WIDTH, BOX_HEIGHT, BOX_DEPTH);

    public static final List<BaseComponent> OFFSET_COMPONENTS =
            List.of(OFFSET_X, OFFSET_Y, OFFSET_Z);

    public static final List<BaseComponent> COLLISION_STATE_COMPONENTS =
            List.of(IS_COLLIDING, COLLISION_COUNT, LAST_COLLISION_ENTITY,
                    COLLISION_NORMAL_X, COLLISION_NORMAL_Y, PENETRATION_DEPTH);

    public static final List<BaseComponent> HANDLER_COMPONENTS =
            List.of(COLLISION_HANDLER_TYPE, COLLISION_HANDLER_PARAM1,
                    COLLISION_HANDLER_PARAM2, COLLISION_HANDLED_TICK);

    public static final List<BaseComponent> ALL_COMPONENTS = List.of(
            BOX_WIDTH, BOX_HEIGHT, BOX_DEPTH,
            OFFSET_X, OFFSET_Y, OFFSET_Z,
            COLLISION_LAYER, COLLISION_MASK,
            IS_TRIGGER,
            IS_COLLIDING, COLLISION_COUNT, LAST_COLLISION_ENTITY,
            COLLISION_NORMAL_X, COLLISION_NORMAL_Y, PENETRATION_DEPTH,
            COLLISION_HANDLER_TYPE, COLLISION_HANDLER_PARAM1,
            COLLISION_HANDLER_PARAM2, COLLISION_HANDLED_TICK,
            FLAG
    );
}
