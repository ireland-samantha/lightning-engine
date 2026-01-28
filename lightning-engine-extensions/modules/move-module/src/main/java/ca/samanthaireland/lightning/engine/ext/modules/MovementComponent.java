package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;

/**
 * Base component for movement-related data.
 * @deprecated use RigidBodyModule
 */
@Deprecated
public class MovementComponent extends BaseComponent {
    public MovementComponent(long id, String name) {
        super(id, name);
    }
}
