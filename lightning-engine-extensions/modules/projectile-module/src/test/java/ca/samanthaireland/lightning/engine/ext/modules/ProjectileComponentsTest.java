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
 * Unit tests for {@link ProjectileComponents}.
 */
class ProjectileComponentsTest {

    // ========== Component Existence Tests ==========

    @Test
    @DisplayName("Projectile metadata components should exist with correct names")
    void projectileMetadataComponentsShouldExist() {
        assertThat(ProjectileComponents.OWNER_ENTITY_ID).isNotNull();
        assertThat(ProjectileComponents.OWNER_ENTITY_ID.getName()).isEqualTo("OWNER_ENTITY_ID");

        assertThat(ProjectileComponents.DAMAGE).isNotNull();
        assertThat(ProjectileComponents.DAMAGE.getName()).isEqualTo("DAMAGE");
    }

    @Test
    @DisplayName("Movement components should exist with correct names")
    void movementComponentsShouldExist() {
        assertThat(ProjectileComponents.SPEED).isNotNull();
        assertThat(ProjectileComponents.SPEED.getName()).isEqualTo("SPEED");

        assertThat(ProjectileComponents.DIRECTION_X).isNotNull();
        assertThat(ProjectileComponents.DIRECTION_X.getName()).isEqualTo("DIRECTION_X");

        assertThat(ProjectileComponents.DIRECTION_Y).isNotNull();
        assertThat(ProjectileComponents.DIRECTION_Y.getName()).isEqualTo("DIRECTION_Y");
    }

    @Test
    @DisplayName("Lifetime components should exist with correct names")
    void lifetimeComponentsShouldExist() {
        assertThat(ProjectileComponents.LIFETIME).isNotNull();
        assertThat(ProjectileComponents.LIFETIME.getName()).isEqualTo("LIFETIME");

        assertThat(ProjectileComponents.TICKS_ALIVE).isNotNull();
        assertThat(ProjectileComponents.TICKS_ALIVE.getName()).isEqualTo("TICKS_ALIVE");
    }

    @Test
    @DisplayName("Piercing components should exist with correct names")
    void piercingComponentsShouldExist() {
        assertThat(ProjectileComponents.PIERCE_COUNT).isNotNull();
        assertThat(ProjectileComponents.PIERCE_COUNT.getName()).isEqualTo("PIERCE_COUNT");

        assertThat(ProjectileComponents.HITS_REMAINING).isNotNull();
        assertThat(ProjectileComponents.HITS_REMAINING.getName()).isEqualTo("HITS_REMAINING");
    }

    @Test
    @DisplayName("Projectile type component should exist")
    void projectileTypeComponentShouldExist() {
        assertThat(ProjectileComponents.PROJECTILE_TYPE).isNotNull();
        assertThat(ProjectileComponents.PROJECTILE_TYPE.getName()).isEqualTo("PROJECTILE_TYPE");
    }

    @Test
    @DisplayName("PENDING_DESTROY component should exist")
    void pendingDestroyComponentShouldExist() {
        assertThat(ProjectileComponents.PENDING_DESTROY).isNotNull();
        assertThat(ProjectileComponents.PENDING_DESTROY.getName()).isEqualTo("PENDING_DESTROY");
    }

    @Test
    @DisplayName("FLAG component should exist")
    void flagComponentShouldExist() {
        assertThat(ProjectileComponents.FLAG).isNotNull();
        assertThat(ProjectileComponents.FLAG.getName()).isEqualTo("projectile");
    }

    // ========== Unique ID Tests ==========

    @Test
    @DisplayName("All components should have unique IDs")
    void allComponentsShouldHaveUniqueIds() {
        Set<Long> ids = new HashSet<>();
        for (BaseComponent component : ProjectileComponents.CORE_COMPONENTS) {
            ids.add(component.getId());
        }
        ids.add(ProjectileComponents.FLAG.getId());

        // 11 core + FLAG = 12
        assertThat(ids).hasSize(12);
    }

    @Test
    @DisplayName("All components should have positive IDs")
    void componentsShouldHavePositiveIds() {
        for (BaseComponent component : ProjectileComponents.CORE_COMPONENTS) {
            assertThat(component.getId()).isPositive();
        }
        assertThat(ProjectileComponents.FLAG.getId()).isPositive();
    }

    // ========== Component List Tests ==========

    @Test
    @DisplayName("CORE_COMPONENTS should contain 11 components")
    void coreComponentsShouldContainExpectedCount() {
        assertThat(ProjectileComponents.CORE_COMPONENTS).hasSize(11);
    }

    @Test
    @DisplayName("CORE_COMPONENTS should contain all projectile components")
    void coreComponentsShouldContainAllComponents() {
        assertThat(ProjectileComponents.CORE_COMPONENTS)
                .containsExactly(
                        ProjectileComponents.OWNER_ENTITY_ID,
                        ProjectileComponents.DAMAGE,
                        ProjectileComponents.SPEED,
                        ProjectileComponents.DIRECTION_X,
                        ProjectileComponents.DIRECTION_Y,
                        ProjectileComponents.LIFETIME,
                        ProjectileComponents.TICKS_ALIVE,
                        ProjectileComponents.PIERCE_COUNT,
                        ProjectileComponents.HITS_REMAINING,
                        ProjectileComponents.PROJECTILE_TYPE,
                        ProjectileComponents.PENDING_DESTROY
                );
    }

    @Test
    @DisplayName("ALL_COMPONENTS should equal CORE_COMPONENTS")
    void allComponentsShouldEqualCoreComponents() {
        assertThat(ProjectileComponents.ALL_COMPONENTS)
                .isEqualTo(ProjectileComponents.CORE_COMPONENTS);
    }

    @Test
    @DisplayName("Component lists should be immutable")
    void componentListsShouldBeImmutable() {
        assertThat(ProjectileComponents.CORE_COMPONENTS.getClass().getName())
                .contains("ImmutableCollections");
    }

    // ========== Component Identity Tests ==========

    @Test
    @DisplayName("Same component reference should be returned on multiple accesses")
    void sameComponentReferenceShouldBeReturned() {
        BaseComponent first = ProjectileComponents.DAMAGE;
        BaseComponent second = ProjectileComponents.DAMAGE;

        assertThat(first).isSameAs(second);
    }
}
