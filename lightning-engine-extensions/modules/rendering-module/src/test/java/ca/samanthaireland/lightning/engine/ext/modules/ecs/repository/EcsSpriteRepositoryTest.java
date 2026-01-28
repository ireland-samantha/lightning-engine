package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Sprite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ca.samanthaireland.lightning.engine.ext.modules.RenderingModuleFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EcsSpriteRepository}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EcsSpriteRepository")
class EcsSpriteRepositoryTest {

    @Mock
    private EntityComponentStore store;

    private EcsSpriteRepository repository;

    @BeforeEach
    void setUp() {
        repository = new EcsSpriteRepository(store);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should attach sprite components to entity")
        void shouldAttachSpriteComponentsToEntity() {
            Sprite sprite = new Sprite(42L, 100L, 64.0f, 128.0f, 45.0f, 5.0f, true);

            repository.save(sprite);

            verify(store).attachComponent(42L, RESOURCE_ID, 100.0f);
            verify(store).attachComponent(42L, SPRITE_WIDTH, 64.0f);
            verify(store).attachComponent(42L, SPRITE_HEIGHT, 128.0f);
            verify(store).attachComponent(42L, SPRITE_ROTATION, 45.0f);
            verify(store).attachComponent(42L, SPRITE_Z_INDEX, 5.0f);
            verify(store).attachComponent(42L, SPRITE_VISIBLE, 1.0f);
        }

        @Test
        @DisplayName("should save hidden sprite with visible = 0")
        void shouldSaveHiddenSpriteWithVisibleZero() {
            Sprite sprite = new Sprite(42L, 100L, 32.0f, 32.0f, 0, 0, false);

            repository.save(sprite);

            verify(store).attachComponent(42L, SPRITE_VISIBLE, 0.0f);
        }

        @Test
        @DisplayName("should save sprite with default values")
        void shouldSaveSpriteWithDefaultValues() {
            Sprite sprite = Sprite.createDefault(42L, 100L);

            repository.save(sprite);

            verify(store).attachComponent(42L, RESOURCE_ID, 100.0f);
            verify(store).attachComponent(42L, SPRITE_WIDTH, Sprite.DEFAULT_WIDTH);
            verify(store).attachComponent(42L, SPRITE_HEIGHT, Sprite.DEFAULT_HEIGHT);
            verify(store).attachComponent(42L, SPRITE_ROTATION, 0.0f);
            verify(store).attachComponent(42L, SPRITE_Z_INDEX, 0.0f);
            verify(store).attachComponent(42L, SPRITE_VISIBLE, 1.0f);
        }
    }

    @Nested
    @DisplayName("findByEntityId")
    class FindByEntityId {

        @Test
        @DisplayName("should return sprite when entity has sprite components")
        void shouldReturnSpriteWhenEntityHasSpriteComponents() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(RESOURCE_ID)))
                    .thenReturn(Set.of(entityId));
            when(store.getComponent(entityId, RESOURCE_ID)).thenReturn(100.0f);
            when(store.getComponent(entityId, SPRITE_WIDTH)).thenReturn(64.0f);
            when(store.getComponent(entityId, SPRITE_HEIGHT)).thenReturn(128.0f);
            when(store.getComponent(entityId, SPRITE_ROTATION)).thenReturn(45.0f);
            when(store.getComponent(entityId, SPRITE_Z_INDEX)).thenReturn(5.0f);
            when(store.getComponent(entityId, SPRITE_VISIBLE)).thenReturn(1.0f);

            Optional<Sprite> result = repository.findByEntityId(entityId);

            assertThat(result).isPresent();
            Sprite sprite = result.get();
            assertThat(sprite.entityId()).isEqualTo(entityId);
            assertThat(sprite.resourceId()).isEqualTo(100L);
            assertThat(sprite.width()).isEqualTo(64.0f);
            assertThat(sprite.height()).isEqualTo(128.0f);
            assertThat(sprite.rotation()).isEqualTo(45.0f);
            assertThat(sprite.zIndex()).isEqualTo(5.0f);
            assertThat(sprite.visible()).isTrue();
        }

        @Test
        @DisplayName("should return empty when entity has no sprite components")
        void shouldReturnEmptyWhenEntityHasNoSpriteComponents() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(RESOURCE_ID)))
                    .thenReturn(Set.of(100L, 200L));

            Optional<Sprite> result = repository.findByEntityId(entityId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when no entities have sprite components")
        void shouldReturnEmptyWhenNoEntitiesHaveSpriteComponents() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(RESOURCE_ID)))
                    .thenReturn(Set.of());

            Optional<Sprite> result = repository.findByEntityId(entityId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return hidden sprite when visible is 0")
        void shouldReturnHiddenSpriteWhenVisibleIsZero() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(RESOURCE_ID)))
                    .thenReturn(Set.of(entityId));
            when(store.getComponent(entityId, RESOURCE_ID)).thenReturn(100.0f);
            when(store.getComponent(entityId, SPRITE_WIDTH)).thenReturn(32.0f);
            when(store.getComponent(entityId, SPRITE_HEIGHT)).thenReturn(32.0f);
            when(store.getComponent(entityId, SPRITE_ROTATION)).thenReturn(0.0f);
            when(store.getComponent(entityId, SPRITE_Z_INDEX)).thenReturn(0.0f);
            when(store.getComponent(entityId, SPRITE_VISIBLE)).thenReturn(0.0f);

            Optional<Sprite> result = repository.findByEntityId(entityId);

            assertThat(result).isPresent();
            assertThat(result.get().visible()).isFalse();
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("should return true when entity has sprite")
        void shouldReturnTrueWhenEntityHasSprite() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(RESOURCE_ID)))
                    .thenReturn(Set.of(entityId));

            boolean result = repository.exists(entityId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when entity has no sprite")
        void shouldReturnFalseWhenEntityHasNoSprite() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(RESOURCE_ID)))
                    .thenReturn(Set.of(100L, 200L));

            boolean result = repository.exists(entityId);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when no sprites exist")
        void shouldReturnFalseWhenNoSpritesExist() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(RESOURCE_ID)))
                    .thenReturn(Set.of());

            boolean result = repository.exists(entityId);

            assertThat(result).isFalse();
        }
    }
}
