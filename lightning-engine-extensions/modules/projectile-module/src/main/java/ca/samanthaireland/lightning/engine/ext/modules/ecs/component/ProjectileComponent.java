package ca.samanthaireland.lightning.engine.ext.modules.ecs.component;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;

/**
 * Base component for projectile-related data.
 */
public class ProjectileComponent extends BaseComponent {
    public ProjectileComponent(long id, String name) {
        super(id, name);
    }
}
