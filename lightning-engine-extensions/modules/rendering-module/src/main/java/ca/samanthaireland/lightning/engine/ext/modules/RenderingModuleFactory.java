package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;

import java.util.List;

/**
 * Module factory for rendering-related functionality.
 *
 * <p>Provides the ability to attach resource IDs to entities for rendering.
 * The RESOURCE_ID component links an entity to a binary resource (texture, sprite, etc.)
 * stored in the ResourceManager.
 *
 * <p>Sprite components include:
 * <ul>
 *   <li>RESOURCE_ID - Links to texture/sprite binary resource</li>
 *   <li>SPRITE_WIDTH, SPRITE_HEIGHT - Sprite dimensions</li>
 *   <li>SPRITE_ROTATION - Rotation in degrees</li>
 *   <li>SPRITE_Z_INDEX - Render order (higher values render on top)</li>
 *   <li>SPRITE_VISIBLE - Visibility flag (0 = hidden, non-zero = visible)</li>
 * </ul>
 *
 * @see RenderingComponents for component constants
 */
public class RenderingModuleFactory implements ModuleFactory {

    // Delegated constants for backwards compatibility
    public static final BaseComponent RESOURCE_ID = RenderingComponents.RESOURCE_ID;
    public static final BaseComponent SPRITE_WIDTH = RenderingComponents.SPRITE_WIDTH;
    public static final BaseComponent SPRITE_HEIGHT = RenderingComponents.SPRITE_HEIGHT;
    public static final BaseComponent SPRITE_ROTATION = RenderingComponents.SPRITE_ROTATION;
    public static final BaseComponent SPRITE_Z_INDEX = RenderingComponents.SPRITE_Z_INDEX;
    public static final BaseComponent SPRITE_VISIBLE = RenderingComponents.SPRITE_VISIBLE;
    public static final BaseComponent FLAG = RenderingComponents.FLAG;
    public static final List<BaseComponent> ALL_SPRITE_COMPONENTS = RenderingComponents.ALL_SPRITE_COMPONENTS;

    @Override
    public EngineModule create(ModuleContext context) {
        return new RenderModule(context);
    }
}
