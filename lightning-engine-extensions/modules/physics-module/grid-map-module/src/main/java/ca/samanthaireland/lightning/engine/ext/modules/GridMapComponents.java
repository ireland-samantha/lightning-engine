package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionLevel;
import ca.samanthaireland.lightning.engine.util.IdGeneratorV2;

import java.util.List;

/**
 * Component constants for the GridMap module.
 *
 * <p>Provides grid position components for tile-based games:
 * <ul>
 *   <li>GRID_POS_X, GRID_POS_Y, GRID_POS_Z - Entity grid coordinates</li>
 *   <li>POSITION_X, POSITION_Y, POSITION_Z - Continuous world coordinates</li>
 *   <li>MAP_WIDTH, MAP_HEIGHT, MAP_DEPTH - Grid map dimensions</li>
 * </ul>
 */
public final class GridMapComponents {

    private GridMapComponents() {
        // Constants class
    }

    // Grid position components for entities
    public static final BaseComponent GRID_POS_X = new PermissionComponent(
            IdGeneratorV2.newId(), "GRID_POS_X", PermissionLevel.READ);
    public static final BaseComponent GRID_POS_Y = new PermissionComponent(
            IdGeneratorV2.newId(), "GRID_POS_Y", PermissionLevel.READ);
    public static final BaseComponent GRID_POS_Z = new PermissionComponent(
            IdGeneratorV2.newId(), "GRID_POS_Z", PermissionLevel.READ);

    // Map dimension components (attached to the map entity itself)
    public static final BaseComponent MAP_WIDTH = new PermissionComponent(
            IdGeneratorV2.newId(), "MAP_WIDTH", PermissionLevel.READ);
    public static final BaseComponent MAP_HEIGHT = new PermissionComponent(
            IdGeneratorV2.newId(), "MAP_HEIGHT", PermissionLevel.READ);
    public static final BaseComponent MAP_DEPTH = new PermissionComponent(
            IdGeneratorV2.newId(), "MAP_DEPTH", PermissionLevel.READ);

    // Map entity marker
    public static final BaseComponent MAP_ENTITY = new PermissionComponent(
            IdGeneratorV2.newId(), "MAP_ENTITY", PermissionLevel.READ);

    public static final BaseComponent FLAG = new PermissionComponent(
            IdGeneratorV2.newId(), "gridmap", PermissionLevel.READ);

    public static final BaseComponent POSITION_X = new PermissionComponent(
            IdGeneratorV2.newId(), "POSITION_X", PermissionLevel.WRITE);
    public static final BaseComponent POSITION_Y = new PermissionComponent(
            IdGeneratorV2.newId(), "POSITION_Y", PermissionLevel.WRITE);
    public static final BaseComponent POSITION_Z = new PermissionComponent(
            IdGeneratorV2.newId(), "POSITION_Z", PermissionLevel.WRITE);

    /**
     * Grid position components for entities (discrete/tile-based).
     */
    public static final List<BaseComponent> GRID_POSITION_COMPONENTS =
            List.of(GRID_POS_X, GRID_POS_Y, GRID_POS_Z);

    /**
     * Continuous position components (for physics/rendering).
     */
    public static final List<BaseComponent> POSITION_COMPONENTS =
            List.of(POSITION_X, POSITION_Y, POSITION_Z);

    /**
     * Map configuration components.
     */
    public static final List<BaseComponent> MAP_COMPONENTS =
            List.of(MAP_WIDTH, MAP_HEIGHT, MAP_DEPTH, MAP_ENTITY);

    /**
     * All components provided by this module.
     */
    public static final List<BaseComponent> ALL_COMPONENTS =
            List.of(GRID_POS_X, GRID_POS_Y, GRID_POS_Z,
                    POSITION_X, POSITION_Y, POSITION_Z,
                    MAP_WIDTH, MAP_HEIGHT, MAP_DEPTH, MAP_ENTITY);
}
