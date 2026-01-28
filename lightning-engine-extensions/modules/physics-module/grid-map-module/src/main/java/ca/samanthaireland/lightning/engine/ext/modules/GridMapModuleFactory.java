package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;

import java.util.List;

/**
 * Module factory for grid-based map positioning.
 *
 * <p>Provides grid position components for tile-based games:
 * <ul>
 *   <li>GRID_POS_X, GRID_POS_Y, GRID_POS_Z - Entity grid coordinates</li>
 *   <li>MAP_WIDTH, MAP_HEIGHT, MAP_DEPTH - Grid map dimensions</li>
 * </ul>
 *
 * <p>This module enables creation of NxM (or NxMxD for 3D) grid maps
 * and allows entities to be positioned on discrete grid cells.
 *
 * @see GridMapComponents for component constants
 */
public class GridMapModuleFactory implements ModuleFactory {

    // Delegated constants for backwards compatibility
    public static final BaseComponent GRID_POS_X = GridMapComponents.GRID_POS_X;
    public static final BaseComponent GRID_POS_Y = GridMapComponents.GRID_POS_Y;
    public static final BaseComponent GRID_POS_Z = GridMapComponents.GRID_POS_Z;
    public static final BaseComponent MAP_WIDTH = GridMapComponents.MAP_WIDTH;
    public static final BaseComponent MAP_HEIGHT = GridMapComponents.MAP_HEIGHT;
    public static final BaseComponent MAP_DEPTH = GridMapComponents.MAP_DEPTH;
    public static final BaseComponent MAP_ENTITY = GridMapComponents.MAP_ENTITY;
    public static final BaseComponent FLAG = GridMapComponents.FLAG;
    public static final BaseComponent POSITION_X = GridMapComponents.POSITION_X;
    public static final BaseComponent POSITION_Y = GridMapComponents.POSITION_Y;
    public static final BaseComponent POSITION_Z = GridMapComponents.POSITION_Z;
    public static final List<BaseComponent> GRID_POSITION_COMPONENTS = GridMapComponents.GRID_POSITION_COMPONENTS;
    public static final List<BaseComponent> POSITION_COMPONENTS = GridMapComponents.POSITION_COMPONENTS;
    public static final List<BaseComponent> MAP_COMPONENTS = GridMapComponents.MAP_COMPONENTS;
    public static final List<BaseComponent> ALL_COMPONENTS = GridMapComponents.ALL_COMPONENTS;

    @Override
    public EngineModule create(ModuleContext context) {
        return new GridMapModule(context);
    }
}
