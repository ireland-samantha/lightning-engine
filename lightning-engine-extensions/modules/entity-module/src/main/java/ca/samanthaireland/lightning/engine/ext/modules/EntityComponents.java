package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionLevel;
import ca.samanthaireland.lightning.engine.util.IdGeneratorV2;

import java.util.List;

/**
 * Component constants for the Entity module.
 *
 * <p>Provides core entity metadata components:
 * <ul>
 *   <li>ENTITY_TYPE - The type/class of entity</li>
 *   <li>OWNER_ID - Owning player/session ID</li>
 *   <li>PLAYER_ID - Player ID for player-controlled entities</li>
 *   <li>FLAG - Module presence marker</li>
 * </ul>
 */
public final class EntityComponents {

    private EntityComponents() {
        // Constants class
    }

    // Entity metadata components
    public static final BaseComponent ENTITY_TYPE = new PermissionComponent(
            IdGeneratorV2.newId(), "ENTITY_TYPE", PermissionLevel.READ);
    public static final BaseComponent OWNER_ID = new PermissionComponent(
            IdGeneratorV2.newId(), "OWNER_ID", PermissionLevel.READ);
    public static final BaseComponent PLAYER_ID = new PermissionComponent(
            IdGeneratorV2.newId(), "PLAYER_ID", PermissionLevel.READ);
    public static final BaseComponent FLAG = new PermissionComponent(
            IdGeneratorV2.newId(), "entity", PermissionLevel.READ);

    /**
     * Core entity components (type, owner, player).
     */
    public static final List<BaseComponent> CORE_COMPONENTS =
            List.of(ENTITY_TYPE, OWNER_ID, PLAYER_ID);

    /**
     * Components included in snapshots.
     * Note: Position components are provided by GridMapModule.
     */
    public static final List<BaseComponent> ALL_COMPONENTS =
            List.of(ENTITY_TYPE, OWNER_ID, PLAYER_ID);
}
