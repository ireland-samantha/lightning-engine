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

class SnapshotTest {

    @Test
    void empty_returnsEmptySnapshot() {
        Snapshot snapshot = Snapshot.empty();

        assertThat(snapshot.isEmpty()).isTrue();
        assertThat(snapshot.moduleCount()).isEqualTo(0);
        assertThat(snapshot.modules()).isEmpty();
    }

    @Test
    void empty_returnsSameInstance() {
        assertThat(Snapshot.empty()).isSameAs(Snapshot.empty());
    }

    @Test
    void builder_createsSnapshot() {
        Snapshot snapshot = Snapshot.builder()
                .module("EntityModule", "1.0",
                        ComponentData.of("ENTITY_TYPE", 1.0f, 2.0f),
                        ComponentData.of("OWNER_ID", 100.0f, 200.0f))
                .module("MovementModule", "0.2",
                        ComponentData.of("POSITION_X", 10.0f, 20.0f))
                .build();

        assertThat(snapshot.moduleCount()).isEqualTo(2);
        assertThat(snapshot.moduleNames()).containsExactly("EntityModule", "MovementModule");
    }

    @Test
    void builder_withModuleVersion_createsSnapshot() {
        Snapshot snapshot = Snapshot.builder()
                .module("Test", ModuleVersion.of(2, 1, 3), List.of(
                        ComponentData.of("X", 1.0f)))
                .build();

        assertThat(snapshot.module("Test"))
                .isPresent()
                .hasValueSatisfying(m -> assertThat(m.version()).isEqualTo(ModuleVersion.of(2, 1, 3)));
    }

    @Test
    void builder_withModuleData_createsSnapshot() {
        ModuleData moduleData = ModuleData.of("Custom", "1.5", List.of(
                ComponentData.of("VALUE", 42.0f)));

        Snapshot snapshot = Snapshot.builder()
                .module(moduleData)
                .build();

        assertThat(snapshot.module("Custom")).isPresent();
    }

    @Test
    void constructor_withNullModules_throwsException() {
        assertThatThrownBy(() -> new Snapshot(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Modules cannot be null");
    }

    @Test
    void module_whenExists_returnsModule() {
        Snapshot snapshot = Snapshot.builder()
                .module("TestModule", "1.0", ComponentData.of("X", 1.0f))
                .build();

        assertThat(snapshot.module("TestModule"))
                .isPresent()
                .hasValueSatisfying(m -> assertThat(m.name()).isEqualTo("TestModule"));
    }

    @Test
    void module_whenNotExists_returnsEmpty() {
        Snapshot snapshot = Snapshot.builder()
                .module("OtherModule", "1.0", ComponentData.of("X", 1.0f))
                .build();

        assertThat(snapshot.module("NonExistent")).isEmpty();
    }

    @Test
    void hasModule_whenExists_returnsTrue() {
        Snapshot snapshot = Snapshot.builder()
                .module("EntityModule", "1.0", ComponentData.of("X", 1.0f))
                .build();

        assertThat(snapshot.hasModule("EntityModule")).isTrue();
    }

    @Test
    void hasModule_whenNotExists_returnsFalse() {
        Snapshot snapshot = Snapshot.builder()
                .module("EntityModule", "1.0", ComponentData.of("X", 1.0f))
                .build();

        assertThat(snapshot.hasModule("Other")).isFalse();
    }

    @Test
    void toLegacyFormat_convertsCorrectly() {
        Snapshot snapshot = Snapshot.builder()
                .module("EntityModule", "1.0",
                        ComponentData.of("ENTITY_TYPE", 1.0f, 2.0f),
                        ComponentData.of("OWNER_ID", 100.0f, 200.0f))
                .build();

        Map<String, Map<String, List<Float>>> legacy = snapshot.toLegacyFormat();

        assertThat(legacy).hasSize(1);
        assertThat(legacy.get("EntityModule")).containsOnlyKeys("ENTITY_TYPE", "OWNER_ID");
        assertThat(legacy.get("EntityModule").get("ENTITY_TYPE")).containsExactly(1.0f, 2.0f);
    }

    @Test
    void fromLegacyFormat_convertsCorrectly() {
        Map<String, Map<String, List<Float>>> legacy = Map.of(
                "TestModule", Map.of(
                        "POSITION_X", List.of(1.0f, 2.0f),
                        "POSITION_Y", List.of(10.0f, 20.0f)
                )
        );

        Snapshot snapshot = Snapshot.fromLegacyFormat(legacy);

        assertThat(snapshot.moduleCount()).isEqualTo(1);
        assertThat(snapshot.module("TestModule"))
                .isPresent()
                .hasValueSatisfying(m -> {
                    assertThat(m.version()).isEqualTo(ModuleVersion.of(1, 0)); // Default version
                    assertThat(m.hasComponent("POSITION_X")).isTrue();
                    assertThat(m.hasComponent("POSITION_Y")).isTrue();
                });
    }

    @Test
    void fromLegacyFormat_withNull_returnsEmpty() {
        Snapshot snapshot = Snapshot.fromLegacyFormat(null);

        assertThat(snapshot.isEmpty()).isTrue();
        assertThat(snapshot).isSameAs(Snapshot.empty());
    }

    @Test
    void fromLegacyFormat_withEmptyMap_returnsEmpty() {
        Snapshot snapshot = Snapshot.fromLegacyFormat(Map.of());

        assertThat(snapshot.isEmpty()).isTrue();
        assertThat(snapshot).isSameAs(Snapshot.empty());
    }

    @Test
    void roundTrip_legacyFormat_preservesData() {
        Snapshot original = Snapshot.builder()
                .module("Module1", "1.0",
                        ComponentData.of("A", 1.0f, 2.0f),
                        ComponentData.of("B", 10.0f, 20.0f))
                .module("Module2", "2.0",
                        ComponentData.of("C", 100.0f))
                .build();

        // Convert to legacy and back
        Map<String, Map<String, List<Float>>> legacy = original.toLegacyFormat();
        Snapshot restored = Snapshot.fromLegacyFormat(legacy);

        // Data should be preserved (version info lost)
        assertThat(restored.moduleCount()).isEqualTo(original.moduleCount());
        assertThat(restored.module("Module1").get().toComponentMap())
                .isEqualTo(original.module("Module1").get().toComponentMap());
    }

    @Test
    void fluent_accessPattern_works() {
        Snapshot snapshot = Snapshot.builder()
                .module("EntityModule", "1.0",
                        ComponentData.of("ENTITY_TYPE", 1.0f, 2.0f, 3.0f))
                .build();

        // Fluent access pattern
        Float value = snapshot.module("EntityModule")
                .flatMap(m -> m.component("ENTITY_TYPE"))
                .map(c -> c.valueAt(1))
                .orElse(-1.0f);

        assertThat(value).isEqualTo(2.0f);
    }

    @Test
    void modules_areImmutable() {
        List<ModuleData> mutableList = new java.util.ArrayList<>(List.of(
                ModuleData.of("Test", List.of(ComponentData.of("X", 1.0f)))));
        Snapshot snapshot = new Snapshot(mutableList);

        // Modify original list
        mutableList.add(ModuleData.of("Test2", List.of()));

        // Snapshot should be unaffected
        assertThat(snapshot.modules()).hasSize(1);
    }
}
