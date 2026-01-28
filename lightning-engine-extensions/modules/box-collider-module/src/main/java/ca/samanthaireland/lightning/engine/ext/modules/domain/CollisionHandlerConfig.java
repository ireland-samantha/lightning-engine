package ca.samanthaireland.lightning.engine.ext.modules.domain;

/**
 * Domain entity representing collision handler configuration for an entity.
 */
public record CollisionHandlerConfig(
        long entityId,
        int handlerType,
        float param1,
        float param2
) {

    /**
     * Handler type indicating no handler is configured.
     */
    public static final int HANDLER_NONE = 0;

    /**
     * Creates a new collision handler configuration.
     */
    public static CollisionHandlerConfig create(int handlerType, float param1, float param2) {
        return new CollisionHandlerConfig(0, handlerType, param1, param2);
    }

    /**
     * Creates a configuration with no handler.
     */
    public static CollisionHandlerConfig none() {
        return new CollisionHandlerConfig(0, HANDLER_NONE, 0, 0);
    }

    /**
     * Returns a new configuration with the specified entity ID.
     */
    public CollisionHandlerConfig withEntityId(long newEntityId) {
        return new CollisionHandlerConfig(newEntityId, handlerType, param1, param2);
    }

    /**
     * Returns true if a handler is configured.
     */
    public boolean hasHandler() {
        return handlerType != HANDLER_NONE;
    }
}
