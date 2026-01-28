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
 * Unit tests for {@link HealthComponents}.
 */
class HealthComponentsTest {

    // ========== Component Existence Tests ==========

    @Test
    @DisplayName("CURRENT_HP component should exist")
    void currentHpComponentShouldExist() {
        assertThat(HealthComponents.CURRENT_HP).isNotNull();
        assertThat(HealthComponents.CURRENT_HP.getName()).isEqualTo("CURRENT_HP");
    }

    @Test
    @DisplayName("MAX_HP component should exist")
    void maxHpComponentShouldExist() {
        assertThat(HealthComponents.MAX_HP).isNotNull();
        assertThat(HealthComponents.MAX_HP.getName()).isEqualTo("MAX_HP");
    }

    @Test
    @DisplayName("DAMAGE_TAKEN component should exist")
    void damageTakenComponentShouldExist() {
        assertThat(HealthComponents.DAMAGE_TAKEN).isNotNull();
        assertThat(HealthComponents.DAMAGE_TAKEN.getName()).isEqualTo("DAMAGE_TAKEN");
    }

    @Test
    @DisplayName("IS_DEAD component should exist")
    void isDeadComponentShouldExist() {
        assertThat(HealthComponents.IS_DEAD).isNotNull();
        assertThat(HealthComponents.IS_DEAD.getName()).isEqualTo("IS_DEAD");
    }

    @Test
    @DisplayName("INVULNERABLE component should exist")
    void invulnerableComponentShouldExist() {
        assertThat(HealthComponents.INVULNERABLE).isNotNull();
        assertThat(HealthComponents.INVULNERABLE.getName()).isEqualTo("INVULNERABLE");
    }

    @Test
    @DisplayName("FLAG component should exist")
    void flagComponentShouldExist() {
        assertThat(HealthComponents.FLAG).isNotNull();
        assertThat(HealthComponents.FLAG.getName()).isEqualTo("health");
    }

    // ========== Unique ID Tests ==========

    @Test
    @DisplayName("All components should have unique IDs")
    void allComponentsShouldHaveUniqueIds() {
        Set<Long> ids = new HashSet<>();
        for (BaseComponent component : HealthComponents.CORE_COMPONENTS) {
            ids.add(component.getId());
        }
        ids.add(HealthComponents.FLAG.getId());

        // 5 core components + FLAG = 6
        assertThat(ids).hasSize(6);
    }

    @Test
    @DisplayName("All components should have positive IDs")
    void componentsShouldHavePositiveIds() {
        assertThat(HealthComponents.CURRENT_HP.getId()).isPositive();
        assertThat(HealthComponents.MAX_HP.getId()).isPositive();
        assertThat(HealthComponents.DAMAGE_TAKEN.getId()).isPositive();
        assertThat(HealthComponents.IS_DEAD.getId()).isPositive();
        assertThat(HealthComponents.INVULNERABLE.getId()).isPositive();
        assertThat(HealthComponents.FLAG.getId()).isPositive();
    }

    // ========== Component List Tests ==========

    @Test
    @DisplayName("CORE_COMPONENTS should contain 5 components")
    void coreComponentsShouldContainExpectedComponents() {
        assertThat(HealthComponents.CORE_COMPONENTS)
                .hasSize(5)
                .containsExactly(
                        HealthComponents.CURRENT_HP,
                        HealthComponents.MAX_HP,
                        HealthComponents.DAMAGE_TAKEN,
                        HealthComponents.IS_DEAD,
                        HealthComponents.INVULNERABLE
                );
    }

    @Test
    @DisplayName("ALL_COMPONENTS should equal CORE_COMPONENTS")
    void allComponentsShouldEqualCoreComponents() {
        assertThat(HealthComponents.ALL_COMPONENTS)
                .isEqualTo(HealthComponents.CORE_COMPONENTS);
    }

    @Test
    @DisplayName("Component lists should be immutable")
    void componentListsShouldBeImmutable() {
        assertThat(HealthComponents.CORE_COMPONENTS.getClass().getName())
                .contains("ImmutableCollections");
    }

    // ========== Component Identity Tests ==========

    @Test
    @DisplayName("Same component reference should be returned on multiple accesses")
    void sameComponentReferenceShouldBeReturned() {
        BaseComponent first = HealthComponents.CURRENT_HP;
        BaseComponent second = HealthComponents.CURRENT_HP;

        assertThat(first).isSameAs(second);
    }
}
