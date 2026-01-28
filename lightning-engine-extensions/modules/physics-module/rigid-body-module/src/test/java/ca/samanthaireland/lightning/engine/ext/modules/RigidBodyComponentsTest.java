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
 * Unit tests for {@link RigidBodyComponents}.
 */
class RigidBodyComponentsTest {

    // ========== Component Existence Tests ==========

    @Test
    @DisplayName("Velocity components should exist with correct names")
    void velocityComponentsShouldExist() {
        assertThat(RigidBodyComponents.VELOCITY_X).isNotNull();
        assertThat(RigidBodyComponents.VELOCITY_X.getName()).isEqualTo("VELOCITY_X");

        assertThat(RigidBodyComponents.VELOCITY_Y).isNotNull();
        assertThat(RigidBodyComponents.VELOCITY_Y.getName()).isEqualTo("VELOCITY_Y");

        assertThat(RigidBodyComponents.VELOCITY_Z).isNotNull();
        assertThat(RigidBodyComponents.VELOCITY_Z.getName()).isEqualTo("VELOCITY_Z");
    }

    @Test
    @DisplayName("Acceleration components should exist with correct names")
    void accelerationComponentsShouldExist() {
        assertThat(RigidBodyComponents.ACCELERATION_X).isNotNull();
        assertThat(RigidBodyComponents.ACCELERATION_X.getName()).isEqualTo("ACCELERATION_X");

        assertThat(RigidBodyComponents.ACCELERATION_Y).isNotNull();
        assertThat(RigidBodyComponents.ACCELERATION_Y.getName()).isEqualTo("ACCELERATION_Y");

        assertThat(RigidBodyComponents.ACCELERATION_Z).isNotNull();
        assertThat(RigidBodyComponents.ACCELERATION_Z.getName()).isEqualTo("ACCELERATION_Z");
    }

    @Test
    @DisplayName("Force components should exist with correct names")
    void forceComponentsShouldExist() {
        assertThat(RigidBodyComponents.FORCE_X).isNotNull();
        assertThat(RigidBodyComponents.FORCE_X.getName()).isEqualTo("FORCE_X");

        assertThat(RigidBodyComponents.FORCE_Y).isNotNull();
        assertThat(RigidBodyComponents.FORCE_Y.getName()).isEqualTo("FORCE_Y");

        assertThat(RigidBodyComponents.FORCE_Z).isNotNull();
        assertThat(RigidBodyComponents.FORCE_Z.getName()).isEqualTo("FORCE_Z");
    }

    @Test
    @DisplayName("Mass component should exist")
    void massComponentShouldExist() {
        assertThat(RigidBodyComponents.MASS).isNotNull();
        assertThat(RigidBodyComponents.MASS.getName()).isEqualTo("MASS");
    }

    @Test
    @DisplayName("Angular components should exist with correct names")
    void angularComponentsShouldExist() {
        assertThat(RigidBodyComponents.ANGULAR_VELOCITY).isNotNull();
        assertThat(RigidBodyComponents.ANGULAR_VELOCITY.getName()).isEqualTo("ANGULAR_VELOCITY");

        assertThat(RigidBodyComponents.ROTATION).isNotNull();
        assertThat(RigidBodyComponents.ROTATION.getName()).isEqualTo("ROTATION");

        assertThat(RigidBodyComponents.TORQUE).isNotNull();
        assertThat(RigidBodyComponents.TORQUE.getName()).isEqualTo("TORQUE");

        assertThat(RigidBodyComponents.INERTIA).isNotNull();
        assertThat(RigidBodyComponents.INERTIA.getName()).isEqualTo("INERTIA");
    }

    @Test
    @DisplayName("Drag components should exist with correct names")
    void dragComponentsShouldExist() {
        assertThat(RigidBodyComponents.LINEAR_DRAG).isNotNull();
        assertThat(RigidBodyComponents.LINEAR_DRAG.getName()).isEqualTo("LINEAR_DRAG");

        assertThat(RigidBodyComponents.ANGULAR_DRAG).isNotNull();
        assertThat(RigidBodyComponents.ANGULAR_DRAG.getName()).isEqualTo("ANGULAR_DRAG");
    }

    @Test
    @DisplayName("FLAG component should exist")
    void flagComponentShouldExist() {
        assertThat(RigidBodyComponents.FLAG).isNotNull();
        assertThat(RigidBodyComponents.FLAG.getName()).isEqualTo("rigidBody");
    }

    // ========== Unique ID Tests ==========

    @Test
    @DisplayName("All components should have unique IDs")
    void allComponentsShouldHaveUniqueIds() {
        List<BaseComponent> allComponents = List.of(
                RigidBodyComponents.VELOCITY_X, RigidBodyComponents.VELOCITY_Y, RigidBodyComponents.VELOCITY_Z,
                RigidBodyComponents.ACCELERATION_X, RigidBodyComponents.ACCELERATION_Y, RigidBodyComponents.ACCELERATION_Z,
                RigidBodyComponents.FORCE_X, RigidBodyComponents.FORCE_Y, RigidBodyComponents.FORCE_Z,
                RigidBodyComponents.MASS,
                RigidBodyComponents.ANGULAR_VELOCITY, RigidBodyComponents.ROTATION, RigidBodyComponents.TORQUE, RigidBodyComponents.INERTIA,
                RigidBodyComponents.LINEAR_DRAG, RigidBodyComponents.ANGULAR_DRAG,
                RigidBodyComponents.FLAG
        );

        Set<Long> ids = new HashSet<>();
        for (BaseComponent component : allComponents) {
            ids.add(component.getId());
        }

        assertThat(ids).hasSize(allComponents.size());
    }

    @Test
    @DisplayName("All components should have positive IDs")
    void componentsShouldHavePositiveIds() {
        assertThat(RigidBodyComponents.VELOCITY_X.getId()).isPositive();
        assertThat(RigidBodyComponents.VELOCITY_Y.getId()).isPositive();
        assertThat(RigidBodyComponents.VELOCITY_Z.getId()).isPositive();
        assertThat(RigidBodyComponents.MASS.getId()).isPositive();
        assertThat(RigidBodyComponents.FLAG.getId()).isPositive();
    }

    // ========== Component List Tests ==========

    @Test
    @DisplayName("VELOCITY_COMPONENTS should contain all velocity components")
    void velocityComponentsListShouldContainAllVelocityComponents() {
        assertThat(RigidBodyComponents.VELOCITY_COMPONENTS)
                .hasSize(3)
                .containsExactly(
                        RigidBodyComponents.VELOCITY_X,
                        RigidBodyComponents.VELOCITY_Y,
                        RigidBodyComponents.VELOCITY_Z
                );
    }

    @Test
    @DisplayName("ACCELERATION_COMPONENTS should contain all acceleration components")
    void accelerationComponentsListShouldContainAllAccelerationComponents() {
        assertThat(RigidBodyComponents.ACCELERATION_COMPONENTS)
                .hasSize(3)
                .containsExactly(
                        RigidBodyComponents.ACCELERATION_X,
                        RigidBodyComponents.ACCELERATION_Y,
                        RigidBodyComponents.ACCELERATION_Z
                );
    }

    @Test
    @DisplayName("FORCE_COMPONENTS should contain all force components")
    void forceComponentsListShouldContainAllForceComponents() {
        assertThat(RigidBodyComponents.FORCE_COMPONENTS)
                .hasSize(3)
                .containsExactly(
                        RigidBodyComponents.FORCE_X,
                        RigidBodyComponents.FORCE_Y,
                        RigidBodyComponents.FORCE_Z
                );
    }

    @Test
    @DisplayName("CORE_COMPONENTS should contain 16 physics components")
    void coreComponentsShouldContainExpectedCount() {
        assertThat(RigidBodyComponents.CORE_COMPONENTS).hasSize(16);
    }

    @Test
    @DisplayName("CORE_COMPONENTS should not include FLAG")
    void coreComponentsShouldNotIncludeFlag() {
        assertThat(RigidBodyComponents.CORE_COMPONENTS)
                .doesNotContain(RigidBodyComponents.FLAG);
    }

    @Test
    @DisplayName("ALL_COMPONENTS should contain 17 components including FLAG")
    void allComponentsShouldContainExpectedCount() {
        assertThat(RigidBodyComponents.ALL_COMPONENTS).hasSize(17);
        assertThat(RigidBodyComponents.ALL_COMPONENTS)
                .contains(RigidBodyComponents.FLAG);
    }

    @Test
    @DisplayName("Component lists should be immutable")
    void componentListsShouldBeImmutable() {
        assertThat(RigidBodyComponents.VELOCITY_COMPONENTS.getClass().getName())
                .contains("ImmutableCollections");
        assertThat(RigidBodyComponents.CORE_COMPONENTS.getClass().getName())
                .contains("ImmutableCollections");
        assertThat(RigidBodyComponents.ALL_COMPONENTS.getClass().getName())
                .contains("ImmutableCollections");
    }

    // ========== Component Identity Tests ==========

    @Test
    @DisplayName("Same component reference should be returned on multiple accesses")
    void sameComponentReferenceShouldBeReturned() {
        BaseComponent first = RigidBodyComponents.VELOCITY_X;
        BaseComponent second = RigidBodyComponents.VELOCITY_X;

        assertThat(first).isSameAs(second);
    }
}
