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
 * Unit tests for {@link EntityComponents}.
 */
class EntityComponentsTest {

    // ========== Component Existence Tests ==========

    @Test
    @DisplayName("ENTITY_TYPE component should exist")
    void entityTypeComponentShouldExist() {
        assertThat(EntityComponents.ENTITY_TYPE).isNotNull();
        assertThat(EntityComponents.ENTITY_TYPE.getName()).isEqualTo("ENTITY_TYPE");
    }

    @Test
    @DisplayName("OWNER_ID component should exist")
    void ownerIdComponentShouldExist() {
        assertThat(EntityComponents.OWNER_ID).isNotNull();
        assertThat(EntityComponents.OWNER_ID.getName()).isEqualTo("OWNER_ID");
    }

    @Test
    @DisplayName("PLAYER_ID component should exist")
    void playerIdComponentShouldExist() {
        assertThat(EntityComponents.PLAYER_ID).isNotNull();
        assertThat(EntityComponents.PLAYER_ID.getName()).isEqualTo("PLAYER_ID");
    }

    @Test
    @DisplayName("FLAG component should exist")
    void flagComponentShouldExist() {
        assertThat(EntityComponents.FLAG).isNotNull();
        assertThat(EntityComponents.FLAG.getName()).isEqualTo("entity");
    }

    // ========== Unique ID Tests ==========

    @Test
    @DisplayName("All components should have unique IDs")
    void allComponentsShouldHaveUniqueIds() {
        Set<Long> ids = new HashSet<>();

        ids.add(EntityComponents.ENTITY_TYPE.getId());
        ids.add(EntityComponents.OWNER_ID.getId());
        ids.add(EntityComponents.PLAYER_ID.getId());
        ids.add(EntityComponents.FLAG.getId());

        // If all IDs are unique, the set size should equal the number of components
        assertThat(ids).hasSize(4);
    }

    @Test
    @DisplayName("Components should have positive IDs")
    void componentsShouldHavePositiveIds() {
        assertThat(EntityComponents.ENTITY_TYPE.getId()).isPositive();
        assertThat(EntityComponents.OWNER_ID.getId()).isPositive();
        assertThat(EntityComponents.PLAYER_ID.getId()).isPositive();
        assertThat(EntityComponents.FLAG.getId()).isPositive();
    }

    // ========== Component Lists Tests ==========

    @Test
    @DisplayName("CORE_COMPONENTS should contain expected components")
    void coreComponentsShouldContainExpectedComponents() {
        assertThat(EntityComponents.CORE_COMPONENTS)
                .hasSize(3)
                .containsExactly(
                        EntityComponents.ENTITY_TYPE,
                        EntityComponents.OWNER_ID,
                        EntityComponents.PLAYER_ID
                );
    }

    @Test
    @DisplayName("ALL_COMPONENTS should contain expected components")
    void allComponentsShouldContainExpectedComponents() {
        assertThat(EntityComponents.ALL_COMPONENTS)
                .hasSize(3)
                .containsExactly(
                        EntityComponents.ENTITY_TYPE,
                        EntityComponents.OWNER_ID,
                        EntityComponents.PLAYER_ID
                );
    }

    @Test
    @DisplayName("Component lists should be immutable")
    void componentListsShouldBeImmutable() {
        // List.of() creates immutable lists, verify they can't be modified
        assertThat(EntityComponents.CORE_COMPONENTS.getClass().getName())
                .contains("ImmutableCollections");
        assertThat(EntityComponents.ALL_COMPONENTS.getClass().getName())
                .contains("ImmutableCollections");
    }

    // ========== Component Identity Tests ==========

    @Test
    @DisplayName("Same component reference should be returned on multiple accesses")
    void sameComponentReferenceShouldBeReturned() {
        BaseComponent first = EntityComponents.ENTITY_TYPE;
        BaseComponent second = EntityComponents.ENTITY_TYPE;

        assertThat(first).isSameAs(second);
    }
}
