package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.ext.modules.domain.Sprite;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.SpriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SpriteService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpriteService")
class SpriteServiceTest {

    @Mock
    private SpriteRepository spriteRepository;

    private SpriteService spriteService;

    @BeforeEach
    void setUp() {
        spriteService = new SpriteService(spriteRepository);
    }

    @Nested
    @DisplayName("attachSprite")
    class AttachSprite {

        private final long entityId = 42L;
        private final long resourceId = 100L;

        @Test
        @DisplayName("should attach sprite with specified parameters")
        void shouldAttachSpriteWithSpecifiedParameters() {
            float width = 64.0f;
            float height = 128.0f;
            float rotation = 45.0f;
            float zIndex = 5.0f;
            boolean visible = true;

            Sprite result = spriteService.attachSprite(
                    entityId, resourceId, width, height, rotation, zIndex, visible);

            assertThat(result.entityId()).isEqualTo(entityId);
            assertThat(result.resourceId()).isEqualTo(resourceId);
            assertThat(result.width()).isEqualTo(width);
            assertThat(result.height()).isEqualTo(height);
            assertThat(result.rotation()).isEqualTo(rotation);
            assertThat(result.zIndex()).isEqualTo(zIndex);
            assertThat(result.visible()).isEqualTo(visible);

            ArgumentCaptor<Sprite> spriteCaptor = ArgumentCaptor.forClass(Sprite.class);
            verify(spriteRepository).save(spriteCaptor.capture());
            assertThat(spriteCaptor.getValue()).isEqualTo(result);
        }

        @Test
        @DisplayName("should throw for invalid width")
        void shouldThrowForInvalidWidth() {
            assertThatThrownBy(() -> spriteService.attachSprite(
                    entityId, resourceId, 0, 32.0f, 0, 0, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("width");

            verify(spriteRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw for invalid height")
        void shouldThrowForInvalidHeight() {
            assertThatThrownBy(() -> spriteService.attachSprite(
                    entityId, resourceId, 32.0f, -5.0f, 0, 0, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("height");

            verify(spriteRepository, never()).save(any());
        }

        @Test
        @DisplayName("should allow hidden sprite")
        void shouldAllowHiddenSprite() {
            Sprite result = spriteService.attachSprite(
                    entityId, resourceId, 32.0f, 32.0f, 0, 0, false);

            assertThat(result.visible()).isFalse();
            verify(spriteRepository).save(any());
        }

        @Test
        @DisplayName("should allow negative rotation")
        void shouldAllowNegativeRotation() {
            Sprite result = spriteService.attachSprite(
                    entityId, resourceId, 32.0f, 32.0f, -90.0f, 0, true);

            assertThat(result.rotation()).isEqualTo(-90.0f);
            verify(spriteRepository).save(any());
        }

        @Test
        @DisplayName("should allow negative z-index")
        void shouldAllowNegativeZIndex() {
            Sprite result = spriteService.attachSprite(
                    entityId, resourceId, 32.0f, 32.0f, 0, -10.0f, true);

            assertThat(result.zIndex()).isEqualTo(-10.0f);
            verify(spriteRepository).save(any());
        }
    }

    @Nested
    @DisplayName("attachSpriteDefault")
    class AttachSpriteDefault {

        @Test
        @DisplayName("should create sprite with default values")
        void shouldCreateSpriteWithDefaultValues() {
            long entityId = 42L;
            long resourceId = 100L;

            Sprite result = spriteService.attachSpriteDefault(entityId, resourceId);

            assertThat(result.entityId()).isEqualTo(entityId);
            assertThat(result.resourceId()).isEqualTo(resourceId);
            assertThat(result.width()).isEqualTo(Sprite.DEFAULT_WIDTH);
            assertThat(result.height()).isEqualTo(Sprite.DEFAULT_HEIGHT);
            assertThat(result.rotation()).isEqualTo(0.0f);
            assertThat(result.zIndex()).isEqualTo(0.0f);
            assertThat(result.visible()).isTrue();

            verify(spriteRepository).save(any());
        }
    }

    @Nested
    @DisplayName("findByEntityId")
    class FindByEntityId {

        @Test
        @DisplayName("should return sprite when found")
        void shouldReturnSpriteWhenFound() {
            long entityId = 42L;
            Sprite sprite = new Sprite(entityId, 100L, 32.0f, 32.0f, 0, 0, true);
            when(spriteRepository.findByEntityId(entityId)).thenReturn(Optional.of(sprite));

            Optional<Sprite> result = spriteService.findByEntityId(entityId);

            assertThat(result).contains(sprite);
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            long entityId = 42L;
            when(spriteRepository.findByEntityId(entityId)).thenReturn(Optional.empty());

            Optional<Sprite> result = spriteService.findByEntityId(entityId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasSprite")
    class HasSprite {

        @Test
        @DisplayName("should return true when entity has sprite")
        void shouldReturnTrueWhenEntityHasSprite() {
            long entityId = 42L;
            when(spriteRepository.exists(entityId)).thenReturn(true);

            boolean result = spriteService.hasSprite(entityId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when entity has no sprite")
        void shouldReturnFalseWhenEntityHasNoSprite() {
            long entityId = 42L;
            when(spriteRepository.exists(entityId)).thenReturn(false);

            boolean result = spriteService.hasSprite(entityId);

            assertThat(result).isFalse();
        }
    }
}
