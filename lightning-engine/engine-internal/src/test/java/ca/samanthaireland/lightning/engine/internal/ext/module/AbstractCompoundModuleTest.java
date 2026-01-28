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

package ca.samanthaireland.lightning.engine.internal.ext.module;

import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.system.EngineSystem;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleExports;
import ca.samanthaireland.lightning.engine.ext.module.ModuleIdentifier;
import ca.samanthaireland.lightning.engine.ext.module.ModuleVersion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbstractCompoundModuleTest {

    @Test
    void getName_returnsConfiguredName() {
        TestCompoundModule module = createModule("TestModule", ModuleVersion.of(1, 0));

        assertThat(module.getName()).isEqualTo("TestModule");
    }

    @Test
    void getVersion_returnsConfiguredVersion() {
        TestCompoundModule module = createModule("TestModule", ModuleVersion.of(2, 3));

        assertThat(module.getVersion()).isEqualTo(ModuleVersion.of(2, 3));
    }

    @Test
    void getIdentifier_combinesNameAndVersion() {
        TestCompoundModule module = createModule("TestModule", ModuleVersion.of(2, 3));

        assertThat(module.getIdentifier()).isEqualTo(ModuleIdentifier.of("TestModule", 2, 3));
    }

    @Test
    void getComponentModules_returnsConfiguredIdentifiers() {
        List<ModuleIdentifier> identifiers = List.of(
                ModuleIdentifier.of("ModuleA", 1, 0),
                ModuleIdentifier.of("ModuleB", 2, 0)
        );
        EngineModule mockA = mockModule("ModuleA");
        EngineModule mockB = mockModule("ModuleB");

        TestCompoundModule module = new TestCompoundModule(
                "Compound", ModuleVersion.of(1, 0), identifiers, List.of(mockA, mockB));

        assertThat(module.getComponentModules()).isEqualTo(identifiers);
    }

    @Test
    void containsModule_existingModule_returnsTrue() {
        List<ModuleIdentifier> identifiers = List.of(
                ModuleIdentifier.of("ModuleA", 1, 0),
                ModuleIdentifier.of("ModuleB", 2, 0)
        );
        EngineModule mockA = mockModule("ModuleA");
        EngineModule mockB = mockModule("ModuleB");

        TestCompoundModule module = new TestCompoundModule(
                "Compound", ModuleVersion.of(1, 0), identifiers, List.of(mockA, mockB));

        assertThat(module.containsModule("ModuleA")).isTrue();
        assertThat(module.containsModule("ModuleB")).isTrue();
    }

    @Test
    void containsModule_nonExistingModule_returnsFalse() {
        List<ModuleIdentifier> identifiers = List.of(
                ModuleIdentifier.of("ModuleA", 1, 0)
        );
        EngineModule mockA = mockModule("ModuleA");

        TestCompoundModule module = new TestCompoundModule(
                "Compound", ModuleVersion.of(1, 0), identifiers, List.of(mockA));

        assertThat(module.containsModule("NonExistent")).isFalse();
    }

    @Test
    void getResolvedComponents_returnsConfiguredComponents() {
        EngineModule mockA = mockModule("ModuleA");
        EngineModule mockB = mockModule("ModuleB");

        TestCompoundModule module = new TestCompoundModule(
                "Compound", ModuleVersion.of(1, 0),
                List.of(ModuleIdentifier.of("ModuleA", 1, 0), ModuleIdentifier.of("ModuleB", 1, 0)),
                List.of(mockA, mockB));

        assertThat(module.getResolvedComponents()).containsExactly(mockA, mockB);
    }

    @Test
    void createSystems_aggregatesFromAllComponents() {
        EngineModule mockA = mockModule("ModuleA");
        EngineModule mockB = mockModule("ModuleB");
        EngineSystem systemA = mock(EngineSystem.class);
        EngineSystem systemB = mock(EngineSystem.class);
        when(mockA.createSystems()).thenReturn(List.of(systemA));
        when(mockB.createSystems()).thenReturn(List.of(systemB));

        TestCompoundModule module = new TestCompoundModule(
                "Compound", ModuleVersion.of(1, 0),
                List.of(ModuleIdentifier.of("ModuleA", 1, 0), ModuleIdentifier.of("ModuleB", 1, 0)),
                List.of(mockA, mockB));

        List<EngineSystem> systems = module.createSystems();

        assertThat(systems).containsExactly(systemA, systemB);
    }

    @Test
    void createSystems_handlesNullFromComponent() {
        EngineModule mockA = mockModule("ModuleA");
        EngineModule mockB = mockModule("ModuleB");
        EngineSystem systemA = mock(EngineSystem.class);
        when(mockA.createSystems()).thenReturn(List.of(systemA));
        when(mockB.createSystems()).thenReturn(null);

        TestCompoundModule module = new TestCompoundModule(
                "Compound", ModuleVersion.of(1, 0),
                List.of(ModuleIdentifier.of("ModuleA", 1, 0), ModuleIdentifier.of("ModuleB", 1, 0)),
                List.of(mockA, mockB));

        List<EngineSystem> systems = module.createSystems();

        assertThat(systems).containsExactly(systemA);
    }

    @Test
    void createCommands_aggregatesFromAllComponents() {
        EngineModule mockA = mockModule("ModuleA");
        EngineModule mockB = mockModule("ModuleB");
        EngineCommand commandA = mock(EngineCommand.class);
        EngineCommand commandB = mock(EngineCommand.class);
        when(mockA.createCommands()).thenReturn(List.of(commandA));
        when(mockB.createCommands()).thenReturn(List.of(commandB));

        TestCompoundModule module = new TestCompoundModule(
                "Compound", ModuleVersion.of(1, 0),
                List.of(ModuleIdentifier.of("ModuleA", 1, 0), ModuleIdentifier.of("ModuleB", 1, 0)),
                List.of(mockA, mockB));

        List<EngineCommand> commands = module.createCommands();

        assertThat(commands).containsExactly(commandA, commandB);
    }

    @Test
    void createComponents_aggregatesFromAllComponents() {
        EngineModule mockA = mockModule("ModuleA");
        EngineModule mockB = mockModule("ModuleB");
        BaseComponent componentA = mock(BaseComponent.class);
        BaseComponent componentB = mock(BaseComponent.class);
        when(mockA.createComponents()).thenReturn(List.of(componentA));
        when(mockB.createComponents()).thenReturn(List.of(componentB));

        TestCompoundModule module = new TestCompoundModule(
                "Compound", ModuleVersion.of(1, 0),
                List.of(ModuleIdentifier.of("ModuleA", 1, 0), ModuleIdentifier.of("ModuleB", 1, 0)),
                List.of(mockA, mockB));

        List<BaseComponent> components = module.createComponents();

        assertThat(components).containsExactly(componentA, componentB);
    }

    @Test
    void createFlagComponent_returnsNull() {
        TestCompoundModule module = createModule("TestModule", ModuleVersion.of(1, 0));

        assertThat(module.createFlagComponent()).isNull();
    }

    @Test
    void getExports_aggregatesFromAllComponents() {
        EngineModule mockA = mockModule("ModuleA");
        EngineModule mockB = mockModule("ModuleB");
        ModuleExports exportA = mock(ModuleExports.class);
        ModuleExports exportB = mock(ModuleExports.class);
        when(mockA.getExports()).thenReturn(List.of(exportA));
        when(mockB.getExports()).thenReturn(List.of(exportB));

        TestCompoundModule module = new TestCompoundModule(
                "Compound", ModuleVersion.of(1, 0),
                List.of(ModuleIdentifier.of("ModuleA", 1, 0), ModuleIdentifier.of("ModuleB", 1, 0)),
                List.of(mockA, mockB));

        List<ModuleExports> exports = module.getExports();

        assertThat(exports).containsExactly(exportA, exportB);
    }

    @Test
    void constructor_nullName_throwsException() {
        assertThatThrownBy(() -> new TestCompoundModule(
                null, ModuleVersion.of(1, 0), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or blank");
    }

    @Test
    void constructor_blankName_throwsException() {
        assertThatThrownBy(() -> new TestCompoundModule(
                "  ", ModuleVersion.of(1, 0), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or blank");
    }

    @Test
    void constructor_nullVersion_throwsException() {
        assertThatThrownBy(() -> new TestCompoundModule(
                "Test", null, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version cannot be null");
    }

    @Test
    void constructor_nullIdentifiers_throwsException() {
        assertThatThrownBy(() -> new TestCompoundModule(
                "Test", ModuleVersion.of(1, 0), null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identifiers cannot be null");
    }

    @Test
    void constructor_nullComponents_throwsException() {
        assertThatThrownBy(() -> new TestCompoundModule(
                "Test", ModuleVersion.of(1, 0), List.of(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("components cannot be null");
    }

    @Test
    void constructor_mismatchedCounts_throwsException() {
        EngineModule mockA = mockModule("ModuleA");

        assertThatThrownBy(() -> new TestCompoundModule(
                "Test", ModuleVersion.of(1, 0),
                List.of(ModuleIdentifier.of("ModuleA", 1, 0), ModuleIdentifier.of("ModuleB", 1, 0)),
                List.of(mockA)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must match");
    }

    private TestCompoundModule createModule(String name, ModuleVersion version) {
        return new TestCompoundModule(name, version, List.of(), List.of());
    }

    private EngineModule mockModule(String name) {
        EngineModule mock = mock(EngineModule.class);
        when(mock.getName()).thenReturn(name);
        when(mock.getVersion()).thenReturn(ModuleVersion.of(1, 0));
        when(mock.createSystems()).thenReturn(List.of());
        when(mock.createCommands()).thenReturn(List.of());
        when(mock.createComponents()).thenReturn(List.of());
        when(mock.getExports()).thenReturn(List.of());
        return mock;
    }

    private static class TestCompoundModule extends AbstractCompoundModule {
        TestCompoundModule(String name, ModuleVersion version,
                          List<ModuleIdentifier> componentIdentifiers,
                          List<EngineModule> resolvedComponents) {
            super(name, version, componentIdentifiers, resolvedComponents);
        }
    }
}
