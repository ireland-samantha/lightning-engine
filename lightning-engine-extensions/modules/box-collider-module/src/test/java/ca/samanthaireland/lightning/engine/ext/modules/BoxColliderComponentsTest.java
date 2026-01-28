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
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BoxColliderComponents}.
 */
class BoxColliderComponentsTest {

    // ========== Component Existence Tests ==========

    @Test
    @DisplayName("Box dimension components should exist with correct names")
    void boxDimensionComponentsShouldExist() {
        assertThat(BoxColliderComponents.BOX_WIDTH).isNotNull();
        assertThat(BoxColliderComponents.BOX_WIDTH.getName()).isEqualTo("BOX_WIDTH");

        assertThat(BoxColliderComponents.BOX_HEIGHT).isNotNull();
        assertThat(BoxColliderComponents.BOX_HEIGHT.getName()).isEqualTo("BOX_HEIGHT");

        assertThat(BoxColliderComponents.BOX_DEPTH).isNotNull();
        assertThat(BoxColliderComponents.BOX_DEPTH.getName()).isEqualTo("BOX_DEPTH");
    }

    @Test
    @DisplayName("Offset components should exist with correct names")
    void offsetComponentsShouldExist() {
        assertThat(BoxColliderComponents.OFFSET_X).isNotNull();
        assertThat(BoxColliderComponents.OFFSET_X.getName()).isEqualTo("OFFSET_X");

        assertThat(BoxColliderComponents.OFFSET_Y).isNotNull();
        assertThat(BoxColliderComponents.OFFSET_Y.getName()).isEqualTo("OFFSET_Y");

        assertThat(BoxColliderComponents.OFFSET_Z).isNotNull();
        assertThat(BoxColliderComponents.OFFSET_Z.getName()).isEqualTo("OFFSET_Z");
    }

    @Test
    @DisplayName("Collision filtering components should exist with correct names")
    void collisionFilteringComponentsShouldExist() {
        assertThat(BoxColliderComponents.COLLISION_LAYER).isNotNull();
        assertThat(BoxColliderComponents.COLLISION_LAYER.getName()).isEqualTo("COLLISION_LAYER");

        assertThat(BoxColliderComponents.COLLISION_MASK).isNotNull();
        assertThat(BoxColliderComponents.COLLISION_MASK.getName()).isEqualTo("COLLISION_MASK");

        assertThat(BoxColliderComponents.IS_TRIGGER).isNotNull();
        assertThat(BoxColliderComponents.IS_TRIGGER.getName()).isEqualTo("IS_TRIGGER");
    }

    @Test
    @DisplayName("Collision state components should exist with correct names")
    void collisionStateComponentsShouldExist() {
        assertThat(BoxColliderComponents.IS_COLLIDING).isNotNull();
        assertThat(BoxColliderComponents.IS_COLLIDING.getName()).isEqualTo("IS_COLLIDING");

        assertThat(BoxColliderComponents.COLLISION_COUNT).isNotNull();
        assertThat(BoxColliderComponents.COLLISION_COUNT.getName()).isEqualTo("COLLISION_COUNT");

        assertThat(BoxColliderComponents.LAST_COLLISION_ENTITY).isNotNull();
        assertThat(BoxColliderComponents.LAST_COLLISION_ENTITY.getName()).isEqualTo("LAST_COLLISION_ENTITY");
    }

    @Test
    @DisplayName("Collision normal components should exist with correct names")
    void collisionNormalComponentsShouldExist() {
        assertThat(BoxColliderComponents.COLLISION_NORMAL_X).isNotNull();
        assertThat(BoxColliderComponents.COLLISION_NORMAL_X.getName()).isEqualTo("COLLISION_NORMAL_X");

        assertThat(BoxColliderComponents.COLLISION_NORMAL_Y).isNotNull();
        assertThat(BoxColliderComponents.COLLISION_NORMAL_Y.getName()).isEqualTo("COLLISION_NORMAL_Y");

        assertThat(BoxColliderComponents.PENETRATION_DEPTH).isNotNull();
        assertThat(BoxColliderComponents.PENETRATION_DEPTH.getName()).isEqualTo("PENETRATION_DEPTH");
    }

    @Test
    @DisplayName("Collision handler components should exist with correct names")
    void collisionHandlerComponentsShouldExist() {
        assertThat(BoxColliderComponents.COLLISION_HANDLER_TYPE).isNotNull();
        assertThat(BoxColliderComponents.COLLISION_HANDLER_TYPE.getName()).isEqualTo("COLLISION_HANDLER_TYPE");

        assertThat(BoxColliderComponents.COLLISION_HANDLER_PARAM1).isNotNull();
        assertThat(BoxColliderComponents.COLLISION_HANDLER_PARAM1.getName()).isEqualTo("COLLISION_HANDLER_PARAM1");

        assertThat(BoxColliderComponents.COLLISION_HANDLER_PARAM2).isNotNull();
        assertThat(BoxColliderComponents.COLLISION_HANDLER_PARAM2.getName()).isEqualTo("COLLISION_HANDLER_PARAM2");

        assertThat(BoxColliderComponents.COLLISION_HANDLED_TICK).isNotNull();
        assertThat(BoxColliderComponents.COLLISION_HANDLED_TICK.getName()).isEqualTo("COLLISION_HANDLED_TICK");
    }

    @Test
    @DisplayName("FLAG component should exist")
    void flagComponentShouldExist() {
        assertThat(BoxColliderComponents.FLAG).isNotNull();
        assertThat(BoxColliderComponents.FLAG.getName()).isEqualTo("boxCollider");
    }

    @Test
    @DisplayName("HANDLER_NONE constant should be zero")
    void handlerNoneConstantShouldBeZero() {
        assertThat(BoxColliderComponents.HANDLER_NONE).isEqualTo(0);
    }

    // ========== Unique ID Tests ==========

    @Test
    @DisplayName("All components should have unique IDs")
    void allComponentsShouldHaveUniqueIds() {
        Set<Long> ids = new HashSet<>();
        for (BaseComponent component : BoxColliderComponents.ALL_COMPONENTS) {
            ids.add(component.getId());
        }

        assertThat(ids).hasSize(BoxColliderComponents.ALL_COMPONENTS.size());
    }

    @Test
    @DisplayName("All components should have positive IDs")
    void componentsShouldHavePositiveIds() {
        for (BaseComponent component : BoxColliderComponents.ALL_COMPONENTS) {
            assertThat(component.getId()).isPositive();
        }
    }

    // ========== Component List Tests ==========

    @Test
    @DisplayName("DIMENSION_COMPONENTS should contain 3 components")
    void dimensionComponentsShouldContainExpectedComponents() {
        assertThat(BoxColliderComponents.DIMENSION_COMPONENTS)
                .hasSize(3)
                .containsExactly(
                        BoxColliderComponents.BOX_WIDTH,
                        BoxColliderComponents.BOX_HEIGHT,
                        BoxColliderComponents.BOX_DEPTH
                );
    }

    @Test
    @DisplayName("OFFSET_COMPONENTS should contain 3 components")
    void offsetComponentsShouldContainExpectedComponents() {
        assertThat(BoxColliderComponents.OFFSET_COMPONENTS)
                .hasSize(3)
                .containsExactly(
                        BoxColliderComponents.OFFSET_X,
                        BoxColliderComponents.OFFSET_Y,
                        BoxColliderComponents.OFFSET_Z
                );
    }

    @Test
    @DisplayName("COLLISION_STATE_COMPONENTS should contain 6 components")
    void collisionStateComponentsShouldContainExpectedComponents() {
        assertThat(BoxColliderComponents.COLLISION_STATE_COMPONENTS)
                .hasSize(6)
                .containsExactly(
                        BoxColliderComponents.IS_COLLIDING,
                        BoxColliderComponents.COLLISION_COUNT,
                        BoxColliderComponents.LAST_COLLISION_ENTITY,
                        BoxColliderComponents.COLLISION_NORMAL_X,
                        BoxColliderComponents.COLLISION_NORMAL_Y,
                        BoxColliderComponents.PENETRATION_DEPTH
                );
    }

    @Test
    @DisplayName("HANDLER_COMPONENTS should contain 4 components")
    void handlerComponentsShouldContainExpectedComponents() {
        assertThat(BoxColliderComponents.HANDLER_COMPONENTS)
                .hasSize(4)
                .containsExactly(
                        BoxColliderComponents.COLLISION_HANDLER_TYPE,
                        BoxColliderComponents.COLLISION_HANDLER_PARAM1,
                        BoxColliderComponents.COLLISION_HANDLER_PARAM2,
                        BoxColliderComponents.COLLISION_HANDLED_TICK
                );
    }

    @Test
    @DisplayName("ALL_COMPONENTS should contain 20 components")
    void allComponentsShouldContainExpectedCount() {
        assertThat(BoxColliderComponents.ALL_COMPONENTS).hasSize(20);
    }

    @Test
    @DisplayName("ALL_COMPONENTS should include FLAG")
    void allComponentsShouldIncludeFlag() {
        assertThat(BoxColliderComponents.ALL_COMPONENTS)
                .contains(BoxColliderComponents.FLAG);
    }

    @Test
    @DisplayName("Component lists should be immutable")
    void componentListsShouldBeImmutable() {
        assertThat(BoxColliderComponents.DIMENSION_COMPONENTS.getClass().getName())
                .contains("ImmutableCollections");
        assertThat(BoxColliderComponents.OFFSET_COMPONENTS.getClass().getName())
                .contains("ImmutableCollections");
        assertThat(BoxColliderComponents.COLLISION_STATE_COMPONENTS.getClass().getName())
                .contains("ImmutableCollections");
        assertThat(BoxColliderComponents.ALL_COMPONENTS.getClass().getName())
                .contains("ImmutableCollections");
    }

    // ========== Component Identity Tests ==========

    @Test
    @DisplayName("Same component reference should be returned on multiple accesses")
    void sameComponentReferenceShouldBeReturned() {
        BaseComponent first = BoxColliderComponents.BOX_WIDTH;
        BaseComponent second = BoxColliderComponents.BOX_WIDTH;

        assertThat(first).isSameAs(second);
    }
}
