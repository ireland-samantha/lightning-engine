package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionLevel;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.component.RenderingComponent;
import ca.samanthaireland.lightning.engine.util.IdGeneratorV2;

import java.util.List;

/**
 * Component constants for the Rendering module.
 *
 * <p>Provides sprite and rendering components:
 * <ul>
 *   <li>RESOURCE_ID - Links to texture/sprite binary resource</li>
 *   <li>SPRITE_WIDTH, SPRITE_HEIGHT - Sprite dimensions</li>
 *   <li>SPRITE_ROTATION - Rotation in degrees</li>
 *   <li>SPRITE_Z_INDEX - Render order (higher values render on top)</li>
 *   <li>SPRITE_VISIBLE - Visibility flag (0 = hidden, non-zero = visible)</li>
 * </ul>
 */
public final class RenderingComponents {

    private RenderingComponents() {
        // Constants class
    }

    // Resource ID - links to texture/sprite binary resource
    public static final BaseComponent RESOURCE_ID =
            new RenderingComponent(IdGeneratorV2.newId(), "RESOURCE_ID");

    // Sprite dimensions
    public static final BaseComponent SPRITE_WIDTH =
            new RenderingComponent(IdGeneratorV2.newId(), "SPRITE_WIDTH");
    public static final BaseComponent SPRITE_HEIGHT =
            new RenderingComponent(IdGeneratorV2.newId(), "SPRITE_HEIGHT");

    // Sprite rotation in degrees
    public static final BaseComponent SPRITE_ROTATION =
            new RenderingComponent(IdGeneratorV2.newId(), "SPRITE_ROTATION");

    // Render order (higher values render on top)
    public static final BaseComponent SPRITE_Z_INDEX =
            new RenderingComponent(IdGeneratorV2.newId(), "SPRITE_Z_INDEX");

    // Visibility flag (0 = hidden, non-zero = visible)
    public static final BaseComponent SPRITE_VISIBLE =
            new RenderingComponent(IdGeneratorV2.newId(), "SPRITE_VISIBLE");

    // Module flag (PRIVATE - only EntityModule with superuser can attach during spawn)
    public static final BaseComponent FLAG = new PermissionComponent(
            IdGeneratorV2.newId(), "rendering", PermissionLevel.PRIVATE);

    /**
     * All sprite components for easy iteration.
     */
    public static final List<BaseComponent> ALL_SPRITE_COMPONENTS = List.of(
            RESOURCE_ID, SPRITE_WIDTH, SPRITE_HEIGHT,
            SPRITE_ROTATION, SPRITE_Z_INDEX, SPRITE_VISIBLE
    );
}
