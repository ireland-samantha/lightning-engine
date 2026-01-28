package ca.samanthaireland.lightning.engine.ext.modules.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link BoxCollider}.
 */
@DisplayName("BoxCollider")
class BoxColliderTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should create box collider with valid dimensions")
        void shouldCreateBoxColliderWithValidDimensions() {
            BoxCollider collider = new BoxCollider(1L, 10f, 20f, 5f, 0, 0, 0, 1, -1, false);

            assertThat(collider.entityId()).isEqualTo(1L);
            assertThat(collider.width()).isEqualTo(10f);
            assertThat(collider.height()).isEqualTo(20f);
            assertThat(collider.depth()).isEqualTo(5f);
        }

        @Test
        @DisplayName("should throw for zero width")
        void shouldThrowForZeroWidth() {
            assertThatThrownBy(() -> new BoxCollider(1L, 0, 10f, 5f, 0, 0, 0, 1, -1, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("width must be positive");
        }

        @Test
        @DisplayName("should throw for negative height")
        void shouldThrowForNegativeHeight() {
            assertThatThrownBy(() -> new BoxCollider(1L, 10f, -5f, 5f, 0, 0, 0, 1, -1, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("height must be positive");
        }

        @Test
        @DisplayName("should throw for zero depth")
        void shouldThrowForZeroDepth() {
            assertThatThrownBy(() -> new BoxCollider(1L, 10f, 10f, 0, 0, 0, 0, 1, -1, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("depth must be positive");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create box collider without entity ID")
        void shouldCreateBoxColliderWithoutEntityId() {
            BoxCollider collider = BoxCollider.create(10f, 20f, 5f, 1f, 2f, 3f, 2, 7, true);

            assertThat(collider.entityId()).isEqualTo(0);
            assertThat(collider.width()).isEqualTo(10f);
            assertThat(collider.height()).isEqualTo(20f);
            assertThat(collider.depth()).isEqualTo(5f);
            assertThat(collider.offsetX()).isEqualTo(1f);
            assertThat(collider.offsetY()).isEqualTo(2f);
            assertThat(collider.offsetZ()).isEqualTo(3f);
            assertThat(collider.layer()).isEqualTo(2);
            assertThat(collider.mask()).isEqualTo(7);
            assertThat(collider.isTrigger()).isTrue();
        }
    }

    @Nested
    @DisplayName("createSimple")
    class CreateSimple {

        @Test
        @DisplayName("should create simple box collider with defaults")
        void shouldCreateSimpleBoxColliderWithDefaults() {
            BoxCollider collider = BoxCollider.createSimple(10f, 20f, 5f);

            assertThat(collider.entityId()).isEqualTo(0);
            assertThat(collider.width()).isEqualTo(10f);
            assertThat(collider.height()).isEqualTo(20f);
            assertThat(collider.depth()).isEqualTo(5f);
            assertThat(collider.offsetX()).isEqualTo(0f);
            assertThat(collider.offsetY()).isEqualTo(0f);
            assertThat(collider.offsetZ()).isEqualTo(0f);
            assertThat(collider.layer()).isEqualTo(BoxCollider.DEFAULT_LAYER);
            assertThat(collider.mask()).isEqualTo(BoxCollider.DEFAULT_MASK);
            assertThat(collider.isTrigger()).isFalse();
        }
    }

    @Nested
    @DisplayName("withSize")
    class WithSize {

        @Test
        @DisplayName("should return new collider with updated size")
        void shouldReturnNewColliderWithUpdatedSize() {
            BoxCollider original = BoxCollider.createSimple(10f, 20f, 5f);

            BoxCollider updated = original.withSize(15f, 25f);

            assertThat(updated.width()).isEqualTo(15f);
            assertThat(updated.height()).isEqualTo(25f);
            assertThat(updated.depth()).isEqualTo(5f);
            assertThat(original.width()).isEqualTo(10f);
        }
    }

    @Nested
    @DisplayName("withLayerMask")
    class WithLayerMask {

        @Test
        @DisplayName("should return new collider with updated layer and mask")
        void shouldReturnNewColliderWithUpdatedLayerMask() {
            BoxCollider original = BoxCollider.createSimple(10f, 20f, 5f);

            BoxCollider updated = original.withLayerMask(4, 12);

            assertThat(updated.layer()).isEqualTo(4);
            assertThat(updated.mask()).isEqualTo(12);
            assertThat(original.layer()).isEqualTo(BoxCollider.DEFAULT_LAYER);
        }
    }

    @Nested
    @DisplayName("withEntityId")
    class WithEntityId {

        @Test
        @DisplayName("should return new collider with entity ID")
        void shouldReturnNewColliderWithEntityId() {
            BoxCollider original = BoxCollider.createSimple(10f, 20f, 5f);

            BoxCollider updated = original.withEntityId(42L);

            assertThat(updated.entityId()).isEqualTo(42L);
            assertThat(original.entityId()).isEqualTo(0);
        }
    }
}
