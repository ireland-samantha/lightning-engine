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


package ca.samanthaireland.lightning.engine.core.store;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PermissionComponent")
class PermissionComponentTest {

    @Test
    @DisplayName("create() generates unique ID")
    void createGeneratesUniqueId() {
        PermissionComponent c1 = PermissionComponent.create("COMP1", PermissionLevel.READ);
        PermissionComponent c2 = PermissionComponent.create("COMP2", PermissionLevel.WRITE);

        assertThat(c1.getId()).isNotEqualTo(c2.getId());
    }

    @Test
    @DisplayName("create() sets name and permission level")
    void createSetsNameAndPermission() {
        PermissionComponent component = PermissionComponent.create("POSITION", PermissionLevel.READ);

        assertThat(component.getName()).isEqualTo("POSITION");
        assertThat(component.getPermissionLevel()).isEqualTo(PermissionLevel.READ);
    }

    @Test
    @DisplayName("withId() uses provided ID")
    void withIdUsesProvidedId() {
        PermissionComponent component = PermissionComponent.withId(42L, "VELOCITY", PermissionLevel.WRITE);

        assertThat(component.getId()).isEqualTo(42L);
        assertThat(component.getName()).isEqualTo("VELOCITY");
        assertThat(component.getPermissionLevel()).isEqualTo(PermissionLevel.WRITE);
    }

    @Test
    @DisplayName("constructor with ID validates positive ID")
    void constructorValidatesPositiveId() {
        assertThatThrownBy(() -> new PermissionComponent(0, "NAME", PermissionLevel.PRIVATE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");

        assertThatThrownBy(() -> new PermissionComponent(-1, "NAME", PermissionLevel.PRIVATE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("constructor validates non-null name")
    void constructorValidatesNonNullName() {
        assertThatThrownBy(() -> new PermissionComponent(1, null, PermissionLevel.PRIVATE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("constructor validates non-blank name")
    void constructorValidatesNonBlankName() {
        assertThatThrownBy(() -> new PermissionComponent(1, "  ", PermissionLevel.PRIVATE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("constructor validates non-null permission level")
    void constructorValidatesNonNullPermission() {
        assertThatThrownBy(() -> new PermissionComponent(1, "NAME", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    @DisplayName("single-arg constructor validates non-null permission level")
    void singleArgConstructorValidatesNonNullPermission() {
        assertThatThrownBy(() -> new PermissionComponent("NAME", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    @DisplayName("all permission levels can be set")
    void allPermissionLevelsCanBeSet() {
        PermissionComponent privateComp = PermissionComponent.create("P", PermissionLevel.PRIVATE);
        PermissionComponent readComp = PermissionComponent.create("R", PermissionLevel.READ);
        PermissionComponent writeComp = PermissionComponent.create("W", PermissionLevel.WRITE);

        assertThat(privateComp.getPermissionLevel()).isEqualTo(PermissionLevel.PRIVATE);
        assertThat(readComp.getPermissionLevel()).isEqualTo(PermissionLevel.READ);
        assertThat(writeComp.getPermissionLevel()).isEqualTo(PermissionLevel.WRITE);
    }

    @Test
    @DisplayName("equals based on ID only")
    void equalsBasedOnId() {
        PermissionComponent c1 = PermissionComponent.withId(100, "NAME1", PermissionLevel.READ);
        PermissionComponent c2 = PermissionComponent.withId(100, "NAME2", PermissionLevel.WRITE);
        PermissionComponent c3 = PermissionComponent.withId(200, "NAME1", PermissionLevel.READ);

        assertThat(c1).isEqualTo(c2);  // Same ID
        assertThat(c1).isNotEqualTo(c3);  // Different ID
    }

    @Test
    @DisplayName("hashCode based on ID")
    void hashCodeBasedOnId() {
        PermissionComponent c1 = PermissionComponent.withId(100, "NAME1", PermissionLevel.READ);
        PermissionComponent c2 = PermissionComponent.withId(100, "NAME2", PermissionLevel.WRITE);

        assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
    }

    @Test
    @DisplayName("toString includes permission level")
    void toStringIncludesPermission() {
        PermissionComponent component = PermissionComponent.withId(42, "TEST", PermissionLevel.READ);
        String str = component.toString();

        assertThat(str).contains("42");
        assertThat(str).contains("TEST");
        assertThat(str).contains("READ");
    }
}
