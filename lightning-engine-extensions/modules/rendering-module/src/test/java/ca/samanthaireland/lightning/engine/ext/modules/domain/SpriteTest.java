package ca.samanthaireland.lightning.engine.ext.modules.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Sprite}.
 */
@DisplayName("Sprite")
class SpriteTest {

    @Nested
    @DisplayName("constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("should create sprite with valid parameters")
        void shouldCreateSpriteWithValidParameters() {
            Sprite sprite = new Sprite(1L, 100L, 32.0f, 64.0f, 45.0f, 5.0f, true);

            assertThat(sprite.entityId()).isEqualTo(1L);
            assertThat(sprite.resourceId()).isEqualTo(100L);
            assertThat(sprite.width()).isEqualTo(32.0f);
            assertThat(sprite.height()).isEqualTo(64.0f);
            assertThat(sprite.rotation()).isEqualTo(45.0f);
            assertThat(sprite.zIndex()).isEqualTo(5.0f);
            assertThat(sprite.visible()).isTrue();
        }

        @Test
        @DisplayName("should throw for zero width")
        void shouldThrowForZeroWidth() {
            assertThatThrownBy(() -> new Sprite(1L, 100L, 0, 32.0f, 0, 0, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("width");
        }

        @Test
        @DisplayName("should throw for negative width")
        void shouldThrowForNegativeWidth() {
            assertThatThrownBy(() -> new Sprite(1L, 100L, -10.0f, 32.0f, 0, 0, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("width");
        }

        @Test
        @DisplayName("should throw for zero height")
        void shouldThrowForZeroHeight() {
            assertThatThrownBy(() -> new Sprite(1L, 100L, 32.0f, 0, 0, 0, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("height");
        }

        @Test
        @DisplayName("should throw for negative height")
        void shouldThrowForNegativeHeight() {
            assertThatThrownBy(() -> new Sprite(1L, 100L, 32.0f, -10.0f, 0, 0, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("height");
        }
    }

    @Nested
    @DisplayName("createDefault")
    class CreateDefault {

        @Test
        @DisplayName("should create sprite with default values")
        void shouldCreateSpriteWithDefaultValues() {
            Sprite sprite = Sprite.createDefault(1L, 100L);

            assertThat(sprite.entityId()).isEqualTo(1L);
            assertThat(sprite.resourceId()).isEqualTo(100L);
            assertThat(sprite.width()).isEqualTo(Sprite.DEFAULT_WIDTH);
            assertThat(sprite.height()).isEqualTo(Sprite.DEFAULT_HEIGHT);
            assertThat(sprite.rotation()).isEqualTo(0.0f);
            assertThat(sprite.zIndex()).isEqualTo(0.0f);
            assertThat(sprite.visible()).isTrue();
        }
    }

    @Nested
    @DisplayName("builder")
    class Builder {

        @Test
        @DisplayName("should build sprite with default values")
        void shouldBuildSpriteWithDefaultValues() {
            Sprite sprite = Sprite.builder(1L, 100L).build();

            assertThat(sprite.entityId()).isEqualTo(1L);
            assertThat(sprite.resourceId()).isEqualTo(100L);
            assertThat(sprite.width()).isEqualTo(Sprite.DEFAULT_WIDTH);
            assertThat(sprite.height()).isEqualTo(Sprite.DEFAULT_HEIGHT);
            assertThat(sprite.rotation()).isEqualTo(0.0f);
            assertThat(sprite.zIndex()).isEqualTo(0.0f);
            assertThat(sprite.visible()).isTrue();
        }

        @Test
        @DisplayName("should build sprite with custom values")
        void shouldBuildSpriteWithCustomValues() {
            Sprite sprite = Sprite.builder(1L, 100L)
                    .width(64.0f)
                    .height(128.0f)
                    .rotation(90.0f)
                    .zIndex(10.0f)
                    .visible(false)
                    .build();

            assertThat(sprite.width()).isEqualTo(64.0f);
            assertThat(sprite.height()).isEqualTo(128.0f);
            assertThat(sprite.rotation()).isEqualTo(90.0f);
            assertThat(sprite.zIndex()).isEqualTo(10.0f);
            assertThat(sprite.visible()).isFalse();
        }

        @Test
        @DisplayName("should throw for invalid width in builder")
        void shouldThrowForInvalidWidthInBuilder() {
            assertThatThrownBy(() -> Sprite.builder(1L, 100L).width(0).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw for invalid height in builder")
        void shouldThrowForInvalidHeightInBuilder() {
            assertThatThrownBy(() -> Sprite.builder(1L, 100L).height(-1.0f).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
