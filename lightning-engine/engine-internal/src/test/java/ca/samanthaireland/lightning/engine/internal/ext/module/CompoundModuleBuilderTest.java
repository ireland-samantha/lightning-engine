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

import ca.samanthaireland.lightning.engine.ext.module.CompoundModule;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleIdentifier;
import ca.samanthaireland.lightning.engine.ext.module.ModuleResolver;
import ca.samanthaireland.lightning.engine.ext.module.ModuleVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompoundModuleBuilderTest {

    private ModuleResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = mock(ModuleResolver.class);
    }

    @Test
    void create_withValidName_returnsBuilder() {
        CompoundModuleBuilder builder = CompoundModuleBuilder.create("TestModule");

        assertThat(builder).isNotNull();
    }

    @Test
    void create_withNullName_throwsException() {
        assertThatThrownBy(() -> CompoundModuleBuilder.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or blank");
    }

    @Test
    void create_withBlankName_throwsException() {
        assertThatThrownBy(() -> CompoundModuleBuilder.create("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or blank");
    }

    @Test
    void build_setsModuleNameCorrectly() {
        EngineModule componentA = mockModule("ComponentA", 1, 0);
        when(resolver.resolveModule("ComponentA")).thenReturn(componentA);

        CompoundModule module = CompoundModuleBuilder.create("MyCompound")
                .version(1, 0)
                .requireModule("ComponentA", 1, 0)
                .build(resolver);

        assertThat(module.getName()).isEqualTo("MyCompound");
    }

    @Test
    void version_withTwoArgs_setsVersionCorrectly() {
        EngineModule componentA = mockModule("ComponentA", 1, 0);
        when(resolver.resolveModule("ComponentA")).thenReturn(componentA);

        CompoundModule module = CompoundModuleBuilder.create("MyCompound")
                .version(2, 5)
                .requireModule("ComponentA", 1, 0)
                .build(resolver);

        assertThat(module.getVersion()).isEqualTo(ModuleVersion.of(2, 5));
    }

    @Test
    void version_withThreeArgs_setsVersionCorrectly() {
        EngineModule componentA = mockModule("ComponentA", 1, 0);
        when(resolver.resolveModule("ComponentA")).thenReturn(componentA);

        CompoundModule module = CompoundModuleBuilder.create("MyCompound")
                .version(2, 5, 3)
                .requireModule("ComponentA", 1, 0)
                .build(resolver);

        assertThat(module.getVersion()).isEqualTo(ModuleVersion.of(2, 5, 3));
    }

    @Test
    void version_withModuleVersion_setsVersionCorrectly() {
        EngineModule componentA = mockModule("ComponentA", 1, 0);
        when(resolver.resolveModule("ComponentA")).thenReturn(componentA);

        CompoundModule module = CompoundModuleBuilder.create("MyCompound")
                .version(ModuleVersion.of(3, 1, 4))
                .requireModule("ComponentA", 1, 0)
                .build(resolver);

        assertThat(module.getVersion()).isEqualTo(ModuleVersion.of(3, 1, 4));
    }

    @Test
    void version_withNull_throwsException() {
        assertThatThrownBy(() -> CompoundModuleBuilder.create("Test")
                .version(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Version cannot be null");
    }

    @Test
    void requireModule_addsComponentIdentifier() {
        EngineModule componentA = mockModule("ComponentA", 1, 0);
        EngineModule componentB = mockModule("ComponentB", 2, 0);
        when(resolver.resolveModule("ComponentA")).thenReturn(componentA);
        when(resolver.resolveModule("ComponentB")).thenReturn(componentB);

        CompoundModule module = CompoundModuleBuilder.create("MyCompound")
                .requireModule("ComponentA", 1, 0)
                .requireModule("ComponentB", 2, 0)
                .build(resolver);

        assertThat(module.getComponentModules()).containsExactly(
                ModuleIdentifier.of("ComponentA", 1, 0),
                ModuleIdentifier.of("ComponentB", 2, 0)
        );
    }

    @Test
    void requireModule_withIdentifier_addsComponent() {
        EngineModule componentA = mockModule("ComponentA", 1, 5);
        when(resolver.resolveModule("ComponentA")).thenReturn(componentA);

        CompoundModule module = CompoundModuleBuilder.create("MyCompound")
                .requireModule(ModuleIdentifier.of("ComponentA", 1, 0))
                .build(resolver);

        assertThat(module.getComponentModules()).containsExactly(
                ModuleIdentifier.of("ComponentA", 1, 0)
        );
    }

    @Test
    void requireModule_withSpec_parsesAndAddsComponent() {
        EngineModule componentA = mockModule("ComponentA", 1, 2);
        when(resolver.resolveModule("ComponentA")).thenReturn(componentA);

        CompoundModule module = CompoundModuleBuilder.create("MyCompound")
                .requireModule("ComponentA:1.2")
                .build(resolver);

        assertThat(module.getComponentModules()).containsExactly(
                ModuleIdentifier.of("ComponentA", 1, 2)
        );
    }

    @Test
    void requireModule_withNullIdentifier_throwsException() {
        assertThatThrownBy(() -> CompoundModuleBuilder.create("Test")
                .requireModule((ModuleIdentifier) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identifier cannot be null");
    }

    @Test
    void build_resolvesComponents() {
        EngineModule componentA = mockModule("ComponentA", 1, 0);
        EngineModule componentB = mockModule("ComponentB", 2, 0);
        when(resolver.resolveModule("ComponentA")).thenReturn(componentA);
        when(resolver.resolveModule("ComponentB")).thenReturn(componentB);

        CompoundModule module = CompoundModuleBuilder.create("MyCompound")
                .requireModule("ComponentA", 1, 0)
                .requireModule("ComponentB", 2, 0)
                .build(resolver);

        assertThat(module.getResolvedComponents()).containsExactly(componentA, componentB);
    }

    @Test
    void build_withUnresolvedModule_throwsException() {
        when(resolver.resolveModule("NonExistent")).thenReturn(null);

        assertThatThrownBy(() -> CompoundModuleBuilder.create("MyCompound")
                .requireModule("NonExistent", 1, 0)
                .build(resolver))
                .isInstanceOf(CompoundModuleBuilder.ModuleResolutionException.class)
                .hasMessageContaining("Failed to resolve component module: NonExistent");
    }

    @Test
    void build_withIncompatibleVersion_throwsException() {
        EngineModule componentA = mockModule("ComponentA", 2, 0); // Has version 2.0
        when(resolver.resolveModule("ComponentA")).thenReturn(componentA);

        // Requires version 1.x but module has 2.x
        assertThatThrownBy(() -> CompoundModuleBuilder.create("MyCompound")
                .requireModule("ComponentA", 1, 0)
                .build(resolver))
                .isInstanceOf(CompoundModuleBuilder.ModuleResolutionException.class)
                .hasMessageContaining("Version mismatch")
                .hasMessageContaining("ComponentA");
    }

    @Test
    void build_withCompatibleHigherMinorVersion_succeeds() {
        EngineModule componentA = mockModule("ComponentA", 1, 5); // Has version 1.5
        when(resolver.resolveModule("ComponentA")).thenReturn(componentA);

        // Requires 1.0 but module has 1.5 (compatible)
        CompoundModule module = CompoundModuleBuilder.create("MyCompound")
                .requireModule("ComponentA", 1, 0)
                .build(resolver);

        assertThat(module.getResolvedComponents()).containsExactly(componentA);
    }

    @Test
    void build_withNullResolver_throwsException() {
        assertThatThrownBy(() -> CompoundModuleBuilder.create("MyCompound")
                .requireModule("ComponentA", 1, 0)
                .build(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resolver cannot be null");
    }

    @Test
    void build_withNoComponents_throwsException() {
        assertThatThrownBy(() -> CompoundModuleBuilder.create("MyCompound")
                .build(resolver))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must have at least one component");
    }

    @Test
    void buildWithComponents_createsModuleWithPreResolvedComponents() {
        EngineModule componentA = mockModule("ComponentA", 1, 0);
        EngineModule componentB = mockModule("ComponentB", 2, 0);

        CompoundModule module = CompoundModuleBuilder.create("MyCompound")
                .version(1, 0)
                .requireModule("ComponentA", 1, 0)
                .requireModule("ComponentB", 2, 0)
                .buildWithComponents(List.of(componentA, componentB));

        assertThat(module.getName()).isEqualTo("MyCompound");
        assertThat(module.getResolvedComponents()).containsExactly(componentA, componentB);
    }

    @Test
    void buildWithComponents_withNullComponents_throwsException() {
        assertThatThrownBy(() -> CompoundModuleBuilder.create("MyCompound")
                .requireModule("ComponentA", 1, 0)
                .buildWithComponents(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("components cannot be null");
    }

    @Test
    void buildWithComponents_withMismatchedCount_throwsException() {
        EngineModule componentA = mockModule("ComponentA", 1, 0);

        assertThatThrownBy(() -> CompoundModuleBuilder.create("MyCompound")
                .requireModule("ComponentA", 1, 0)
                .requireModule("ComponentB", 2, 0)
                .buildWithComponents(List.of(componentA)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must match");
    }

    @Test
    void buildWithComponents_withNoRequirements_throwsException() {
        assertThatThrownBy(() -> CompoundModuleBuilder.create("MyCompound")
                .buildWithComponents(List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must have at least one component requirement");
    }

    @Test
    void defaultVersion_isOneZero() {
        EngineModule componentA = mockModule("ComponentA", 1, 0);
        when(resolver.resolveModule("ComponentA")).thenReturn(componentA);

        CompoundModule module = CompoundModuleBuilder.create("MyCompound")
                .requireModule("ComponentA", 1, 0)
                .build(resolver);

        assertThat(module.getVersion()).isEqualTo(ModuleVersion.of(1, 0));
    }

    private EngineModule mockModule(String name, int major, int minor) {
        EngineModule mock = mock(EngineModule.class);
        when(mock.getName()).thenReturn(name);
        when(mock.getVersion()).thenReturn(ModuleVersion.of(major, minor));
        when(mock.createSystems()).thenReturn(List.of());
        when(mock.createCommands()).thenReturn(List.of());
        when(mock.createComponents()).thenReturn(List.of());
        when(mock.getExports()).thenReturn(List.of());
        return mock;
    }
}
