package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Sprite;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.SpriteRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ca.samanthaireland.lightning.engine.ext.modules.RenderingModuleFactory.*;

/**
 * ECS-backed implementation of SpriteRepository.
 *
 * <p>Stores sprite data as entity components in the EntityComponentStore.
 */
public class EcsSpriteRepository implements SpriteRepository {

    private final EntityComponentStore store;

    public EcsSpriteRepository(EntityComponentStore store) {
        this.store = store;
    }

    @Override
    public void save(Sprite sprite) {
        store.attachComponent(sprite.entityId(), RESOURCE_ID, (float) sprite.resourceId());
        store.attachComponent(sprite.entityId(), SPRITE_WIDTH, sprite.width());
        store.attachComponent(sprite.entityId(), SPRITE_HEIGHT, sprite.height());
        store.attachComponent(sprite.entityId(), SPRITE_ROTATION, sprite.rotation());
        store.attachComponent(sprite.entityId(), SPRITE_Z_INDEX, sprite.zIndex());
        store.attachComponent(sprite.entityId(), SPRITE_VISIBLE, sprite.visible() ? 1.0f : 0.0f);
    }

    @Override
    public Optional<Sprite> findByEntityId(long entityId) {
        Set<Long> entitiesWithSprite = store.getEntitiesWithComponents(List.of(RESOURCE_ID));

        if (!entitiesWithSprite.contains(entityId)) {
            return Optional.empty();
        }

        long resourceId = (long) store.getComponent(entityId, RESOURCE_ID);
        float width = store.getComponent(entityId, SPRITE_WIDTH);
        float height = store.getComponent(entityId, SPRITE_HEIGHT);
        float rotation = store.getComponent(entityId, SPRITE_ROTATION);
        float zIndex = store.getComponent(entityId, SPRITE_Z_INDEX);
        float visibleValue = store.getComponent(entityId, SPRITE_VISIBLE);
        boolean visible = visibleValue != 0.0f;

        return Optional.of(new Sprite(entityId, resourceId, width, height, rotation, zIndex, visible));
    }

    @Override
    public boolean exists(long entityId) {
        Set<Long> entitiesWithSprite = store.getEntitiesWithComponents(List.of(RESOURCE_ID));
        return entitiesWithSprite.contains(entityId);
    }
}
