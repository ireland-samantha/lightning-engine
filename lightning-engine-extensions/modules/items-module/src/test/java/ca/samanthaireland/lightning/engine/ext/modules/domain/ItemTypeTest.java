package ca.samanthaireland.lightning.engine.ext.modules.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ItemType}.
 */
@DisplayName("ItemType")
class ItemTypeTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should create item type with valid properties")
        void shouldCreateItemTypeWithValidProperties() {
            ItemType itemType = new ItemType(1L, "Health Potion", 10, 1, 50f, 0.5f, 100f, 0f, 0f);

            assertThat(itemType.id()).isEqualTo(1L);
            assertThat(itemType.name()).isEqualTo("Health Potion");
            assertThat(itemType.maxStack()).isEqualTo(10);
            assertThat(itemType.rarity()).isEqualTo(1);
            assertThat(itemType.value()).isEqualTo(50f);
            assertThat(itemType.weight()).isEqualTo(0.5f);
            assertThat(itemType.healAmount()).isEqualTo(100f);
            assertThat(itemType.damageBonus()).isEqualTo(0f);
            assertThat(itemType.armorValue()).isEqualTo(0f);
        }

        @Test
        @DisplayName("should throw for null name")
        void shouldThrowForNullName() {
            assertThatThrownBy(() -> new ItemType(1L, null, 10, 1, 50f, 0.5f, 100f, 0f, 0f))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should throw for empty name")
        void shouldThrowForEmptyName() {
            assertThatThrownBy(() -> new ItemType(1L, "", 10, 1, 50f, 0.5f, 100f, 0f, 0f))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should throw for blank name")
        void shouldThrowForBlankName() {
            assertThatThrownBy(() -> new ItemType(1L, "   ", 10, 1, 50f, 0.5f, 100f, 0f, 0f))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should throw for zero maxStack")
        void shouldThrowForZeroMaxStack() {
            assertThatThrownBy(() -> new ItemType(1L, "Sword", 0, 1, 50f, 0.5f, 0f, 10f, 0f))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("stack");
        }

        @Test
        @DisplayName("should throw for negative maxStack")
        void shouldThrowForNegativeMaxStack() {
            assertThatThrownBy(() -> new ItemType(1L, "Sword", -5, 1, 50f, 0.5f, 0f, 10f, 0f))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("stack");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create item type with id 0")
        void shouldCreateItemTypeWithIdZero() {
            ItemType itemType = ItemType.create("Sword", 1, 2, 100f, 5f, 0f, 25f, 0f);

            assertThat(itemType.id()).isEqualTo(0L);
            assertThat(itemType.name()).isEqualTo("Sword");
            assertThat(itemType.maxStack()).isEqualTo(1);
            assertThat(itemType.rarity()).isEqualTo(2);
            assertThat(itemType.damageBonus()).isEqualTo(25f);
        }
    }
}
