/*
 * Copyright (c) 2026 Samantha Ireland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ItemComponents}.
 */
class ItemComponentsTest {

    // ========== Component Existence Tests ==========

    @Test
    @DisplayName("Item instance components should exist with correct names")
    void itemInstanceComponentsShouldExist() {
        assertThat(ItemComponents.ITEM_TYPE_ID).isNotNull();
        assertThat(ItemComponents.ITEM_TYPE_ID.getName()).isEqualTo("ITEM_TYPE_ID");

        assertThat(ItemComponents.STACK_SIZE).isNotNull();
        assertThat(ItemComponents.STACK_SIZE.getName()).isEqualTo("STACK_SIZE");

        assertThat(ItemComponents.MAX_STACK).isNotNull();
        assertThat(ItemComponents.MAX_STACK.getName()).isEqualTo("MAX_STACK");

        assertThat(ItemComponents.OWNER_ENTITY_ID).isNotNull();
        assertThat(ItemComponents.OWNER_ENTITY_ID.getName()).isEqualTo("OWNER_ENTITY_ID");

        assertThat(ItemComponents.SLOT_INDEX).isNotNull();
        assertThat(ItemComponents.SLOT_INDEX.getName()).isEqualTo("SLOT_INDEX");
    }

    @Test
    @DisplayName("Item type property components should exist with correct names")
    void itemTypePropertyComponentsShouldExist() {
        assertThat(ItemComponents.ITEM_NAME_HASH).isNotNull();
        assertThat(ItemComponents.ITEM_NAME_HASH.getName()).isEqualTo("ITEM_NAME_HASH");

        assertThat(ItemComponents.ITEM_RARITY).isNotNull();
        assertThat(ItemComponents.ITEM_RARITY.getName()).isEqualTo("ITEM_RARITY");

        assertThat(ItemComponents.ITEM_VALUE).isNotNull();
        assertThat(ItemComponents.ITEM_VALUE.getName()).isEqualTo("ITEM_VALUE");

        assertThat(ItemComponents.ITEM_WEIGHT).isNotNull();
        assertThat(ItemComponents.ITEM_WEIGHT.getName()).isEqualTo("ITEM_WEIGHT");
    }

    @Test
    @DisplayName("Item effect components should exist with correct names")
    void itemEffectComponentsShouldExist() {
        assertThat(ItemComponents.HEAL_AMOUNT).isNotNull();
        assertThat(ItemComponents.HEAL_AMOUNT.getName()).isEqualTo("HEAL_AMOUNT");

        assertThat(ItemComponents.DAMAGE_BONUS).isNotNull();
        assertThat(ItemComponents.DAMAGE_BONUS.getName()).isEqualTo("DAMAGE_BONUS");

        assertThat(ItemComponents.ARMOR_VALUE).isNotNull();
        assertThat(ItemComponents.ARMOR_VALUE.getName()).isEqualTo("ARMOR_VALUE");
    }

    @Test
    @DisplayName("FLAG component should exist")
    void flagComponentShouldExist() {
        assertThat(ItemComponents.FLAG).isNotNull();
        assertThat(ItemComponents.FLAG.getName()).isEqualTo("item");
    }

    // ========== Unique ID Tests ==========

    @Test
    @DisplayName("All components should have unique IDs")
    void allComponentsShouldHaveUniqueIds() {
        Set<Long> ids = new HashSet<>();
        for (BaseComponent component : ItemComponents.ALL_COMPONENTS) {
            ids.add(component.getId());
        }
        ids.add(ItemComponents.FLAG.getId());

        // 12 components + FLAG = 13
        assertThat(ids).hasSize(13);
    }

    @Test
    @DisplayName("All components should have positive IDs")
    void componentsShouldHavePositiveIds() {
        for (BaseComponent component : ItemComponents.ALL_COMPONENTS) {
            assertThat(component.getId()).isPositive();
        }
        assertThat(ItemComponents.FLAG.getId()).isPositive();
    }

    // ========== Component List Tests ==========

    @Test
    @DisplayName("CORE_COMPONENTS should contain 5 item instance components")
    void coreComponentsShouldContainExpectedComponents() {
        assertThat(ItemComponents.CORE_COMPONENTS)
                .hasSize(5)
                .containsExactly(
                        ItemComponents.ITEM_TYPE_ID,
                        ItemComponents.STACK_SIZE,
                        ItemComponents.MAX_STACK,
                        ItemComponents.OWNER_ENTITY_ID,
                        ItemComponents.SLOT_INDEX
                );
    }

    @Test
    @DisplayName("ALL_COMPONENTS should contain 12 components")
    void allComponentsShouldContainExpectedCount() {
        assertThat(ItemComponents.ALL_COMPONENTS).hasSize(12);
    }

    @Test
    @DisplayName("ALL_COMPONENTS should contain all core and extended components")
    void allComponentsShouldContainAllComponents() {
        assertThat(ItemComponents.ALL_COMPONENTS)
                .contains(
                        ItemComponents.ITEM_TYPE_ID,
                        ItemComponents.STACK_SIZE,
                        ItemComponents.ITEM_NAME_HASH,
                        ItemComponents.HEAL_AMOUNT,
                        ItemComponents.DAMAGE_BONUS,
                        ItemComponents.ARMOR_VALUE
                );
    }

    @Test
    @DisplayName("Component lists should be immutable")
    void componentListsShouldBeImmutable() {
        assertThat(ItemComponents.CORE_COMPONENTS.getClass().getName())
                .contains("ImmutableCollections");
        assertThat(ItemComponents.ALL_COMPONENTS.getClass().getName())
                .contains("ImmutableCollections");
    }

    // ========== Component Identity Tests ==========

    @Test
    @DisplayName("Same component reference should be returned on multiple accesses")
    void sameComponentReferenceShouldBeReturned() {
        BaseComponent first = ItemComponents.ITEM_TYPE_ID;
        BaseComponent second = ItemComponents.ITEM_TYPE_ID;

        assertThat(first).isSameAs(second);
    }
}
