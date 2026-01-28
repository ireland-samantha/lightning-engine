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
 * Unit tests for {@link RenderingComponents}.
 */
class RenderingComponentsTest {

    // ========== Component Existence Tests ==========

    @Test
    @DisplayName("RESOURCE_ID component should exist")
    void resourceIdComponentShouldExist() {
        assertThat(RenderingComponents.RESOURCE_ID).isNotNull();
        assertThat(RenderingComponents.RESOURCE_ID.getName()).isEqualTo("RESOURCE_ID");
    }

    @Test
    @DisplayName("Sprite dimension components should exist with correct names")
    void spriteDimensionComponentsShouldExist() {
        assertThat(RenderingComponents.SPRITE_WIDTH).isNotNull();
        assertThat(RenderingComponents.SPRITE_WIDTH.getName()).isEqualTo("SPRITE_WIDTH");

        assertThat(RenderingComponents.SPRITE_HEIGHT).isNotNull();
        assertThat(RenderingComponents.SPRITE_HEIGHT.getName()).isEqualTo("SPRITE_HEIGHT");
    }

    @Test
    @DisplayName("SPRITE_ROTATION component should exist")
    void spriteRotationComponentShouldExist() {
        assertThat(RenderingComponents.SPRITE_ROTATION).isNotNull();
        assertThat(RenderingComponents.SPRITE_ROTATION.getName()).isEqualTo("SPRITE_ROTATION");
    }

    @Test
    @DisplayName("SPRITE_Z_INDEX component should exist")
    void spriteZIndexComponentShouldExist() {
        assertThat(RenderingComponents.SPRITE_Z_INDEX).isNotNull();
        assertThat(RenderingComponents.SPRITE_Z_INDEX.getName()).isEqualTo("SPRITE_Z_INDEX");
    }

    @Test
    @DisplayName("SPRITE_VISIBLE component should exist")
    void spriteVisibleComponentShouldExist() {
        assertThat(RenderingComponents.SPRITE_VISIBLE).isNotNull();
        assertThat(RenderingComponents.SPRITE_VISIBLE.getName()).isEqualTo("SPRITE_VISIBLE");
    }

    @Test
    @DisplayName("FLAG component should exist")
    void flagComponentShouldExist() {
        assertThat(RenderingComponents.FLAG).isNotNull();
        assertThat(RenderingComponents.FLAG.getName()).isEqualTo("rendering");
    }

    // ========== Unique ID Tests ==========

    @Test
    @DisplayName("All components should have unique IDs")
    void allComponentsShouldHaveUniqueIds() {
        Set<Long> ids = new HashSet<>();
        for (BaseComponent component : RenderingComponents.ALL_SPRITE_COMPONENTS) {
            ids.add(component.getId());
        }
        ids.add(RenderingComponents.FLAG.getId());

        // 6 sprite components + FLAG = 7
        assertThat(ids).hasSize(7);
    }

    @Test
    @DisplayName("All components should have positive IDs")
    void componentsShouldHavePositiveIds() {
        assertThat(RenderingComponents.RESOURCE_ID.getId()).isPositive();
        assertThat(RenderingComponents.SPRITE_WIDTH.getId()).isPositive();
        assertThat(RenderingComponents.SPRITE_HEIGHT.getId()).isPositive();
        assertThat(RenderingComponents.SPRITE_ROTATION.getId()).isPositive();
        assertThat(RenderingComponents.SPRITE_Z_INDEX.getId()).isPositive();
        assertThat(RenderingComponents.SPRITE_VISIBLE.getId()).isPositive();
        assertThat(RenderingComponents.FLAG.getId()).isPositive();
    }

    // ========== Component List Tests ==========

    @Test
    @DisplayName("ALL_SPRITE_COMPONENTS should contain 6 components")
    void allSpriteComponentsShouldContainExpectedCount() {
        assertThat(RenderingComponents.ALL_SPRITE_COMPONENTS).hasSize(6);
    }

    @Test
    @DisplayName("ALL_SPRITE_COMPONENTS should contain all sprite components")
    void allSpriteComponentsShouldContainAllComponents() {
        assertThat(RenderingComponents.ALL_SPRITE_COMPONENTS)
                .containsExactly(
                        RenderingComponents.RESOURCE_ID,
                        RenderingComponents.SPRITE_WIDTH,
                        RenderingComponents.SPRITE_HEIGHT,
                        RenderingComponents.SPRITE_ROTATION,
                        RenderingComponents.SPRITE_Z_INDEX,
                        RenderingComponents.SPRITE_VISIBLE
                );
    }

    @Test
    @DisplayName("Component list should be immutable")
    void componentListShouldBeImmutable() {
        assertThat(RenderingComponents.ALL_SPRITE_COMPONENTS.getClass().getName())
                .contains("ImmutableCollections");
    }

    // ========== Component Identity Tests ==========

    @Test
    @DisplayName("Same component reference should be returned on multiple accesses")
    void sameComponentReferenceShouldBeReturned() {
        BaseComponent first = RenderingComponents.RESOURCE_ID;
        BaseComponent second = RenderingComponents.RESOURCE_ID;

        assertThat(first).isSameAs(second);
    }
}
