package ca.samanthaireland.lightning.engine.ext.modules.domain;

import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;

/**
 * Collision handler interface for custom collision responses.
 */
@FunctionalInterface
public interface CollisionHandler {
    /**
     * Handle a collision event.
     *
     * @param context the module context
     * @param event the collision event containing all relevant data
     */
    void handle(ModuleContext context, CollisionEvent event);
}
