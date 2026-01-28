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

package ca.samanthaireland.lightning.engine.core.snapshot;

import ca.samanthaireland.lightning.engine.ext.module.ModuleVersion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ModuleDataTest {

    @Test
    void of_withVersionString_createsModule() {
        ComponentData posX = ComponentData.of("POSITION_X", 1.0f, 2.0f);
        ComponentData posY = ComponentData.of("POSITION_Y", 10.0f, 20.0f);

        ModuleData module = ModuleData.of("TestModule", "1.2", List.of(posX, posY));

        assertThat(module.name()).isEqualTo("TestModule");
        assertThat(module.version()).isEqualTo(ModuleVersion.of(1, 2));
        assertThat(module.components()).hasSize(2);
    }

    @Test
    void of_withModuleVersion_createsModule() {
        ComponentData comp = ComponentData.of("TEST", 1.0f);

        ModuleData module = ModuleData.of("MyModule", ModuleVersion.of(2, 1, 3), List.of(comp));

        assertThat(module.name()).isEqualTo("MyModule");
        assertThat(module.version()).isEqualTo(ModuleVersion.of(2, 1, 3));
    }

    @Test
    void of_withoutVersion_usesDefaultVersion() {
        ComponentData comp = ComponentData.of("TEST", 1.0f);

        ModuleData module = ModuleData.of("DefaultModule", List.of(comp));

        assertThat(module.version()).isEqualTo(ModuleVersion.of(1, 0));
    }

    @Test
    void constructor_withNullName_throwsException() {
        assertThatThrownBy(() -> new ModuleData(null, ModuleVersion.of(1, 0), List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Module name cannot be null");
    }

    @Test
    void constructor_withNullVersion_throwsException() {
        assertThatThrownBy(() -> new ModuleData("Test", null, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Module version cannot be null");
    }

    @Test
    void constructor_withNullComponents_throwsException() {
        assertThatThrownBy(() -> new ModuleData("Test", ModuleVersion.of(1, 0), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Components cannot be null");
    }

    @Test
    void componentCount_returnsCorrectSize() {
        ComponentData c1 = ComponentData.of("A", 1.0f);
        ComponentData c2 = ComponentData.of("B", 2.0f);
        ComponentData c3 = ComponentData.of("C", 3.0f);

        ModuleData module = ModuleData.of("Test", List.of(c1, c2, c3));

        assertThat(module.componentCount()).isEqualTo(3);
    }

    @Test
    void component_whenExists_returnsComponent() {
        ComponentData posX = ComponentData.of("POSITION_X", 100.0f);
        ComponentData posY = ComponentData.of("POSITION_Y", 200.0f);
        ModuleData module = ModuleData.of("Movement", List.of(posX, posY));

        assertThat(module.component("POSITION_X"))
                .isPresent()
                .hasValueSatisfying(c -> assertThat(c.values()).containsExactly(100.0f));
    }

    @Test
    void component_whenNotExists_returnsEmpty() {
        ModuleData module = ModuleData.of("Movement", List.of(ComponentData.of("X", 1.0f)));

        assertThat(module.component("NONEXISTENT")).isEmpty();
    }

    @Test
    void hasComponent_whenExists_returnsTrue() {
        ModuleData module = ModuleData.of("Test", List.of(ComponentData.of("VELOCITY", 1.0f)));

        assertThat(module.hasComponent("VELOCITY")).isTrue();
    }

    @Test
    void hasComponent_whenNotExists_returnsFalse() {
        ModuleData module = ModuleData.of("Test", List.of(ComponentData.of("VELOCITY", 1.0f)));

        assertThat(module.hasComponent("OTHER")).isFalse();
    }

    @Test
    void versionString_returnsFormattedVersion() {
        ModuleData module = ModuleData.of("Test", "2.3.4", List.of());

        assertThat(module.versionString()).isEqualTo("2.3.4");
    }

    @Test
    void isEmpty_withNoComponents_returnsTrue() {
        ModuleData module = ModuleData.of("Empty", List.of());

        assertThat(module.isEmpty()).isTrue();
    }

    @Test
    void isEmpty_withComponents_returnsFalse() {
        ModuleData module = ModuleData.of("Test", List.of(ComponentData.of("X", 1.0f)));

        assertThat(module.isEmpty()).isFalse();
    }

    @Test
    void toComponentMap_convertsCorrectly() {
        ComponentData posX = ComponentData.of("POSITION_X", 1.0f, 2.0f);
        ComponentData posY = ComponentData.of("POSITION_Y", 10.0f, 20.0f);
        ModuleData module = ModuleData.of("Movement", List.of(posX, posY));

        Map<String, List<Float>> map = module.toComponentMap();

        assertThat(map).hasSize(2);
        assertThat(map.get("POSITION_X")).containsExactly(1.0f, 2.0f);
        assertThat(map.get("POSITION_Y")).containsExactly(10.0f, 20.0f);
    }

    @Test
    void components_areImmutable() {
        List<ComponentData> mutableList = new java.util.ArrayList<>(
                List.of(ComponentData.of("X", 1.0f)));
        ModuleData module = ModuleData.of("Test", mutableList);

        // Modify original list
        mutableList.add(ComponentData.of("Y", 2.0f));

        // Module should be unaffected
        assertThat(module.components()).hasSize(1);
    }
}
