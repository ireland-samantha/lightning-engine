package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionLevel;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;
import ca.samanthaireland.lightning.engine.util.IdGeneratorV2;

import java.util.List;

/**
 * Module factory for the Movement module.
 *
 * @deprecated use RigidBodyModuleFactory
 */
@Deprecated
public class MoveModuleFactory implements ModuleFactory {

    public static final BaseComponent VELOCITY_X = new MovementComponent(
            IdGeneratorV2.newId(), "VELOCITY_X");
    public static final BaseComponent VELOCITY_Y = new MovementComponent(
            IdGeneratorV2.newId(), "VELOCITY_Y");
    public static final BaseComponent VELOCITY_Z = new MovementComponent(
            IdGeneratorV2.newId(), "VELOCITY_Z");

    public static final BaseComponent POSITION_X = new MovementComponent(
            IdGeneratorV2.newId(), "POSITION_X");
    public static final BaseComponent POSITION_Y = new MovementComponent(
            IdGeneratorV2.newId(), "POSITION_Y");
    public static final BaseComponent POSITION_Z = new MovementComponent(
            IdGeneratorV2.newId(), "POSITION_Z");

    public static final BaseComponent MODULE = new PermissionComponent(
            IdGeneratorV2.newId(), "move", PermissionLevel.WRITE);

    public static final List<BaseComponent> ALL_COMPONENTS =
            List.of(VELOCITY_X, VELOCITY_Y, VELOCITY_Z, POSITION_X, POSITION_Y, POSITION_Z, MODULE);

    public static final List<BaseComponent> MOVE_COMPONENTS =
            List.of(POSITION_X, POSITION_Y, POSITION_Z, VELOCITY_X, VELOCITY_Y, VELOCITY_Z);

    @Override
    public EngineModule create(ModuleContext context) {
        return new MovementModule(context);
    }
}
