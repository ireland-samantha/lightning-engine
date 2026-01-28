package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;

import java.util.List;

/**
 * Module factory for the Entity module.
 *
 * <p>Provides core entity functionality:
 * <ul>
 *   <li>Entity creation (spawn command)</li>
 *   <li>Entity metadata (type, owner, player)</li>
 * </ul>
 *
 * <p>Note: Position components are provided by GridMapModule.
 * Use EntityModuleExports or GridMapExports for position operations.
 *
 * @see EntityComponents for component constants
 */
public class EntityModuleFactory implements ModuleFactory {

    // Delegated constants for backwards compatibility
    public static final BaseComponent ENTITY_TYPE = EntityComponents.ENTITY_TYPE;
    public static final BaseComponent OWNER_ID = EntityComponents.OWNER_ID;
    public static final BaseComponent PLAYER_ID = EntityComponents.PLAYER_ID;
    public static final BaseComponent FLAG = EntityComponents.FLAG;
    public static final List<BaseComponent> CORE_COMPONENTS = EntityComponents.CORE_COMPONENTS;
    public static final List<BaseComponent> ALL_COMPONENTS = EntityComponents.ALL_COMPONENTS;

    @Override
    public EngineModule create(ModuleContext context) {
        return new EntityModule(context);
    }
}
