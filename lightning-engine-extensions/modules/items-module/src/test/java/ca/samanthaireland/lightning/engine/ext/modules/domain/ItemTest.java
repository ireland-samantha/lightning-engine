package ca.samanthaireland.lightning.engine.ext.modules.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Item}.
 */
@DisplayName("Item")
class ItemTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should create item with valid properties")
        void shouldCreateItemWithValidProperties() {
            Item item = new Item(1L, 10L, 5, 10, 0, -1, 100f, 200f, 12345, 1, 50f, 0.5f, 100f, 0f, 0f);

            assertThat(item.id()).isEqualTo(1L);
            assertThat(item.itemTypeId()).isEqualTo(10L);
            assertThat(item.stackSize()).isEqualTo(5);
            assertThat(item.maxStack()).isEqualTo(10);
            assertThat(item.ownerEntityId()).isEqualTo(0);
            assertThat(item.slotIndex()).isEqualTo(-1);
            assertThat(item.positionX()).isEqualTo(100f);
            assertThat(item.positionY()).isEqualTo(200f);
        }

        @Test
        @DisplayName("should throw for zero stackSize")
        void shouldThrowForZeroStackSize() {
            assertThatThrownBy(() -> new Item(1L, 10L, 0, 10, 0, -1, 0f, 0f, 0, 0, 0f, 0f, 0f, 0f, 0f))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Stack size");
        }

        @Test
        @DisplayName("should throw for negative stackSize")
        void shouldThrowForNegativeStackSize() {
            assertThatThrownBy(() -> new Item(1L, 10L, -1, 10, 0, -1, 0f, 0f, 0, 0, 0f, 0f, 0f, 0f, 0f))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Stack size");
        }

        @Test
        @DisplayName("should throw for stackSize exceeding maxStack")
        void shouldThrowForStackSizeExceedingMaxStack() {
            assertThatThrownBy(() -> new Item(1L, 10L, 15, 10, 0, -1, 0f, 0f, 0, 0, 0f, 0f, 0f, 0f, 0f))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceeds max stack");
        }
    }

    @Nested
    @DisplayName("isOnGround")
    class IsOnGround {

        @Test
        @DisplayName("should return true when ownerEntityId is 0")
        void shouldReturnTrueWhenOwnerEntityIdIsZero() {
            Item item = new Item(1L, 10L, 5, 10, 0, -1, 0f, 0f, 0, 0, 0f, 0f, 0f, 0f, 0f);
            assertThat(item.isOnGround()).isTrue();
        }

        @Test
        @DisplayName("should return false when ownerEntityId is positive")
        void shouldReturnFalseWhenOwnerEntityIdIsPositive() {
            Item item = new Item(1L, 10L, 5, 10, 42L, 0, 0f, 0f, 0, 0, 0f, 0f, 0f, 0f, 0f);
            assertThat(item.isOnGround()).isFalse();
        }
    }

    @Nested
    @DisplayName("isInInventory")
    class IsInInventory {

        @Test
        @DisplayName("should return true when ownerEntityId is positive")
        void shouldReturnTrueWhenOwnerEntityIdIsPositive() {
            Item item = new Item(1L, 10L, 5, 10, 42L, 0, 0f, 0f, 0, 0, 0f, 0f, 0f, 0f, 0f);
            assertThat(item.isInInventory()).isTrue();
        }

        @Test
        @DisplayName("should return false when ownerEntityId is 0")
        void shouldReturnFalseWhenOwnerEntityIdIsZero() {
            Item item = new Item(1L, 10L, 5, 10, 0, -1, 0f, 0f, 0, 0, 0f, 0f, 0f, 0f, 0f);
            assertThat(item.isInInventory()).isFalse();
        }
    }

    @Nested
    @DisplayName("withOwner")
    class WithOwner {

        @Test
        @DisplayName("should create copy with new owner and slot")
        void shouldCreateCopyWithNewOwnerAndSlot() {
            Item item = new Item(1L, 10L, 5, 10, 0, -1, 100f, 200f, 12345, 1, 50f, 0.5f, 100f, 0f, 0f);

            Item result = item.withOwner(42L, 3);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.ownerEntityId()).isEqualTo(42L);
            assertThat(result.slotIndex()).isEqualTo(3);
            assertThat(result.positionX()).isEqualTo(100f);
            assertThat(result.positionY()).isEqualTo(200f);
        }
    }

    @Nested
    @DisplayName("dropped")
    class Dropped {

        @Test
        @DisplayName("should create copy with no owner and new position")
        void shouldCreateCopyWithNoOwnerAndNewPosition() {
            Item item = new Item(1L, 10L, 5, 10, 42L, 3, 100f, 200f, 12345, 1, 50f, 0.5f, 100f, 0f, 0f);

            Item result = item.dropped(300f, 400f);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.ownerEntityId()).isEqualTo(0L);
            assertThat(result.slotIndex()).isEqualTo(-1);
            assertThat(result.positionX()).isEqualTo(300f);
            assertThat(result.positionY()).isEqualTo(400f);
        }
    }

    @Nested
    @DisplayName("withReducedStack")
    class WithReducedStack {

        @Test
        @DisplayName("should create copy with reduced stack")
        void shouldCreateCopyWithReducedStack() {
            Item item = new Item(1L, 10L, 5, 10, 0, -1, 0f, 0f, 0, 0, 0f, 0f, 0f, 0f, 0f);

            Item result = item.withReducedStack(2);

            assertThat(result).isNotNull();
            assertThat(result.stackSize()).isEqualTo(3);
        }

        @Test
        @DisplayName("should return null when stack is depleted")
        void shouldReturnNullWhenStackIsDepleted() {
            Item item = new Item(1L, 10L, 5, 10, 0, -1, 0f, 0f, 0, 0, 0f, 0f, 0f, 0f, 0f);

            Item result = item.withReducedStack(5);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null when stack goes negative")
        void shouldReturnNullWhenStackGoesNegative() {
            Item item = new Item(1L, 10L, 5, 10, 0, -1, 0f, 0f, 0, 0, 0f, 0f, 0f, 0f, 0f);

            Item result = item.withReducedStack(10);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("createFromType")
    class CreateFromType {

        @Test
        @DisplayName("should create item from type with capped stack size")
        void shouldCreateItemFromTypeWithCappedStackSize() {
            ItemType type = new ItemType(5L, "Potion", 10, 1, 50f, 0.5f, 100f, 0f, 0f);

            Item item = Item.createFromType(type, 15, 100f, 200f);

            assertThat(item.id()).isEqualTo(0L);
            assertThat(item.itemTypeId()).isEqualTo(5L);
            assertThat(item.stackSize()).isEqualTo(10); // capped to maxStack
            assertThat(item.maxStack()).isEqualTo(10);
            assertThat(item.positionX()).isEqualTo(100f);
            assertThat(item.positionY()).isEqualTo(200f);
            assertThat(item.healAmount()).isEqualTo(100f);
        }
    }

    @Nested
    @DisplayName("createDefault")
    class CreateDefault {

        @Test
        @DisplayName("should create default item with default properties")
        void shouldCreateDefaultItemWithDefaultProperties() {
            Item item = Item.createDefault(99L, 5, 100f, 200f);

            assertThat(item.id()).isEqualTo(0L);
            assertThat(item.itemTypeId()).isEqualTo(99L);
            assertThat(item.stackSize()).isEqualTo(5);
            assertThat(item.maxStack()).isEqualTo(100);
            assertThat(item.ownerEntityId()).isEqualTo(0L);
            assertThat(item.slotIndex()).isEqualTo(-1);
        }
    }
}
