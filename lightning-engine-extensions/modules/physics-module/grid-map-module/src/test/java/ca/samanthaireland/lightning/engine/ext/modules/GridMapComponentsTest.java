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
 * Unit tests for {@link GridMapComponents}.
 */
class GridMapComponentsTest {

    // ========== Component Existence Tests ==========

    @Test
    @DisplayName("Grid position components should exist with correct names")
    void gridPositionComponentsShouldExist() {
        assertThat(GridMapComponents.GRID_POS_X).isNotNull();
        assertThat(GridMapComponents.GRID_POS_X.getName()).isEqualTo("GRID_POS_X");

        assertThat(GridMapComponents.GRID_POS_Y).isNotNull();
        assertThat(GridMapComponents.GRID_POS_Y.getName()).isEqualTo("GRID_POS_Y");

        assertThat(GridMapComponents.GRID_POS_Z).isNotNull();
        assertThat(GridMapComponents.GRID_POS_Z.getName()).isEqualTo("GRID_POS_Z");
    }

    @Test
    @DisplayName("Map dimension components should exist with correct names")
    void mapDimensionComponentsShouldExist() {
        assertThat(GridMapComponents.MAP_WIDTH).isNotNull();
        assertThat(GridMapComponents.MAP_WIDTH.getName()).isEqualTo("MAP_WIDTH");

        assertThat(GridMapComponents.MAP_HEIGHT).isNotNull();
        assertThat(GridMapComponents.MAP_HEIGHT.getName()).isEqualTo("MAP_HEIGHT");

        assertThat(GridMapComponents.MAP_DEPTH).isNotNull();
        assertThat(GridMapComponents.MAP_DEPTH.getName()).isEqualTo("MAP_DEPTH");
    }

    @Test
    @DisplayName("Position components should exist with correct names")
    void positionComponentsShouldExist() {
        assertThat(GridMapComponents.POSITION_X).isNotNull();
        assertThat(GridMapComponents.POSITION_X.getName()).isEqualTo("POSITION_X");

        assertThat(GridMapComponents.POSITION_Y).isNotNull();
        assertThat(GridMapComponents.POSITION_Y.getName()).isEqualTo("POSITION_Y");

        assertThat(GridMapComponents.POSITION_Z).isNotNull();
        assertThat(GridMapComponents.POSITION_Z.getName()).isEqualTo("POSITION_Z");
    }

    @Test
    @DisplayName("MAP_ENTITY component should exist")
    void mapEntityComponentShouldExist() {
        assertThat(GridMapComponents.MAP_ENTITY).isNotNull();
        assertThat(GridMapComponents.MAP_ENTITY.getName()).isEqualTo("MAP_ENTITY");
    }

    @Test
    @DisplayName("FLAG component should exist")
    void flagComponentShouldExist() {
        assertThat(GridMapComponents.FLAG).isNotNull();
        assertThat(GridMapComponents.FLAG.getName()).isEqualTo("gridmap");
    }

    // ========== Unique ID Tests ==========

    @Test
    @DisplayName("All components should have unique IDs")
    void allComponentsShouldHaveUniqueIds() {
        Set<Long> ids = new HashSet<>();
        for (BaseComponent component : GridMapComponents.ALL_COMPONENTS) {
            ids.add(component.getId());
        }
        ids.add(GridMapComponents.FLAG.getId());

        // ALL_COMPONENTS has 10 + FLAG = 11 unique components
        assertThat(ids).hasSize(11);
    }

    @Test
    @DisplayName("All components should have positive IDs")
    void componentsShouldHavePositiveIds() {
        for (BaseComponent component : GridMapComponents.ALL_COMPONENTS) {
            assertThat(component.getId()).isPositive();
        }
        assertThat(GridMapComponents.FLAG.getId()).isPositive();
    }

    // ========== Component List Tests ==========

    @Test
    @DisplayName("GRID_POSITION_COMPONENTS should contain 3 components")
    void gridPositionComponentsShouldContainExpectedComponents() {
        assertThat(GridMapComponents.GRID_POSITION_COMPONENTS)
                .hasSize(3)
                .containsExactly(
                        GridMapComponents.GRID_POS_X,
                        GridMapComponents.GRID_POS_Y,
                        GridMapComponents.GRID_POS_Z
                );
    }

    @Test
    @DisplayName("POSITION_COMPONENTS should contain 3 components")
    void positionComponentsShouldContainExpectedComponents() {
        assertThat(GridMapComponents.POSITION_COMPONENTS)
                .hasSize(3)
                .containsExactly(
                        GridMapComponents.POSITION_X,
                        GridMapComponents.POSITION_Y,
                        GridMapComponents.POSITION_Z
                );
    }

    @Test
    @DisplayName("MAP_COMPONENTS should contain 4 components")
    void mapComponentsShouldContainExpectedComponents() {
        assertThat(GridMapComponents.MAP_COMPONENTS)
                .hasSize(4)
                .containsExactly(
                        GridMapComponents.MAP_WIDTH,
                        GridMapComponents.MAP_HEIGHT,
                        GridMapComponents.MAP_DEPTH,
                        GridMapComponents.MAP_ENTITY
                );
    }

    @Test
    @DisplayName("ALL_COMPONENTS should contain 10 components")
    void allComponentsShouldContainExpectedCount() {
        assertThat(GridMapComponents.ALL_COMPONENTS).hasSize(10);
    }

    @Test
    @DisplayName("Component lists should be immutable")
    void componentListsShouldBeImmutable() {
        assertThat(GridMapComponents.GRID_POSITION_COMPONENTS.getClass().getName())
                .contains("ImmutableCollections");
        assertThat(GridMapComponents.POSITION_COMPONENTS.getClass().getName())
                .contains("ImmutableCollections");
        assertThat(GridMapComponents.MAP_COMPONENTS.getClass().getName())
                .contains("ImmutableCollections");
        assertThat(GridMapComponents.ALL_COMPONENTS.getClass().getName())
                .contains("ImmutableCollections");
    }

    // ========== Component Identity Tests ==========

    @Test
    @DisplayName("Same component reference should be returned on multiple accesses")
    void sameComponentReferenceShouldBeReturned() {
        BaseComponent first = GridMapComponents.GRID_POS_X;
        BaseComponent second = GridMapComponents.GRID_POS_X;

        assertThat(first).isSameAs(second);
    }
}
