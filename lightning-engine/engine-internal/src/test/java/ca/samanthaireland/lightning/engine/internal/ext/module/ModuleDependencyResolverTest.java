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
import ca.samanthaireland.lightning.engine.ext.module.ModuleVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModuleDependencyResolverTest {

    private ModuleDependencyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ModuleDependencyResolver();
    }

    @Test
    void addModule_simpleModule_addsToGraph() {
        EngineModule moduleA = mockModule("ModuleA");

        resolver.addModule(moduleA);

        assertThat(resolver.getDependencies("ModuleA")).isEmpty();
    }

    @Test
    void addModule_compoundModule_addsDependencies() {
        EngineModule moduleA = mockModule("ModuleA");
        EngineModule moduleB = mockModule("ModuleB");
        CompoundModule compound = mockCompoundModule("Compound",
                List.of(ModuleIdentifier.of("ModuleA", 1, 0), ModuleIdentifier.of("ModuleB", 1, 0)));

        resolver.addModule(moduleA);
        resolver.addModule(moduleB);
        resolver.addModule(compound);

        assertThat(resolver.getDependencies("Compound"))
                .containsExactlyInAnyOrder("ModuleA", "ModuleB");
    }

    @Test
    void addModule_nullModule_throwsException() {
        assertThatThrownBy(() -> resolver.addModule(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Module cannot be null");
    }

    @Test
    void addModules_addsAllModules() {
        EngineModule moduleA = mockModule("ModuleA");
        EngineModule moduleB = mockModule("ModuleB");

        resolver.addModules(List.of(moduleA, moduleB));

        assertThat(resolver.getDependencies("ModuleA")).isEmpty();
        assertThat(resolver.getDependencies("ModuleB")).isEmpty();
    }

    @Test
    void addModules_nullCollection_throwsException() {
        assertThatThrownBy(() -> resolver.addModules(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Modules cannot be null");
    }

    @Test
    void detectCircularDependencies_noCycle_returnsEmpty() {
        EngineModule moduleA = mockModule("ModuleA");
        EngineModule moduleB = mockModule("ModuleB");
        CompoundModule compound = mockCompoundModule("Compound",
                List.of(ModuleIdentifier.of("ModuleA", 1, 0)));

        resolver.addModule(moduleA);
        resolver.addModule(moduleB);
        resolver.addModule(compound);

        assertThat(resolver.detectCircularDependencies()).isEmpty();
    }

    @Test
    void detectCircularDependencies_directCycle_returnsCycle() {
        // A depends on B, B depends on A
        CompoundModule moduleA = mockCompoundModule("ModuleA",
                List.of(ModuleIdentifier.of("ModuleB", 1, 0)));
        CompoundModule moduleB = mockCompoundModule("ModuleB",
                List.of(ModuleIdentifier.of("ModuleA", 1, 0)));

        resolver.addModule(moduleA);
        resolver.addModule(moduleB);

        List<String> cycle = resolver.detectCircularDependencies();

        assertThat(cycle).isNotEmpty();
        assertThat(cycle).containsAll(List.of("ModuleA", "ModuleB"));
    }

    @Test
    void detectCircularDependencies_indirectCycle_returnsCycle() {
        // A -> B -> C -> A
        CompoundModule moduleA = mockCompoundModule("ModuleA",
                List.of(ModuleIdentifier.of("ModuleB", 1, 0)));
        CompoundModule moduleB = mockCompoundModule("ModuleB",
                List.of(ModuleIdentifier.of("ModuleC", 1, 0)));
        CompoundModule moduleC = mockCompoundModule("ModuleC",
                List.of(ModuleIdentifier.of("ModuleA", 1, 0)));

        resolver.addModule(moduleA);
        resolver.addModule(moduleB);
        resolver.addModule(moduleC);

        List<String> cycle = resolver.detectCircularDependencies();

        assertThat(cycle).isNotEmpty();
    }

    @Test
    void getTopologicalOrder_simpleChain_returnsCorrectOrder() {
        // C depends on B, B depends on A
        EngineModule moduleA = mockModule("ModuleA");
        CompoundModule moduleB = mockCompoundModule("ModuleB",
                List.of(ModuleIdentifier.of("ModuleA", 1, 0)));
        CompoundModule moduleC = mockCompoundModule("ModuleC",
                List.of(ModuleIdentifier.of("ModuleB", 1, 0)));

        resolver.addModule(moduleA);
        resolver.addModule(moduleB);
        resolver.addModule(moduleC);

        List<EngineModule> order = resolver.getTopologicalOrder();

        // A should come before B, B should come before C
        assertThat(order).hasSize(3);
        assertThat(order.indexOf(moduleA)).isLessThan(order.indexOf(moduleB));
        assertThat(order.indexOf(moduleB)).isLessThan(order.indexOf(moduleC));
    }

    @Test
    void getTopologicalOrder_independentModules_returnsAll() {
        EngineModule moduleA = mockModule("ModuleA");
        EngineModule moduleB = mockModule("ModuleB");
        EngineModule moduleC = mockModule("ModuleC");

        resolver.addModule(moduleA);
        resolver.addModule(moduleB);
        resolver.addModule(moduleC);

        List<EngineModule> order = resolver.getTopologicalOrder();

        assertThat(order).containsExactlyInAnyOrder(moduleA, moduleB, moduleC);
    }

    @Test
    void getTopologicalOrder_circularDependency_throwsException() {
        CompoundModule moduleA = mockCompoundModule("ModuleA",
                List.of(ModuleIdentifier.of("ModuleB", 1, 0)));
        CompoundModule moduleB = mockCompoundModule("ModuleB",
                List.of(ModuleIdentifier.of("ModuleA", 1, 0)));

        resolver.addModule(moduleA);
        resolver.addModule(moduleB);

        assertThatThrownBy(() -> resolver.getTopologicalOrder())
                .isInstanceOf(ModuleDependencyResolver.CircularDependencyException.class)
                .hasMessageContaining("Circular dependency detected");
    }

    @Test
    void getTransitiveDependencies_chainedDependencies_returnsAll() {
        // D -> C -> B -> A
        EngineModule moduleA = mockModule("ModuleA");
        CompoundModule moduleB = mockCompoundModule("ModuleB",
                List.of(ModuleIdentifier.of("ModuleA", 1, 0)));
        CompoundModule moduleC = mockCompoundModule("ModuleC",
                List.of(ModuleIdentifier.of("ModuleB", 1, 0)));
        CompoundModule moduleD = mockCompoundModule("ModuleD",
                List.of(ModuleIdentifier.of("ModuleC", 1, 0)));

        resolver.addModule(moduleA);
        resolver.addModule(moduleB);
        resolver.addModule(moduleC);
        resolver.addModule(moduleD);

        Set<String> deps = resolver.getTransitiveDependencies("ModuleD");

        assertThat(deps).containsExactlyInAnyOrder("ModuleA", "ModuleB", "ModuleC");
    }

    @Test
    void getTransitiveDependencies_noDependencies_returnsEmpty() {
        EngineModule moduleA = mockModule("ModuleA");
        resolver.addModule(moduleA);

        Set<String> deps = resolver.getTransitiveDependencies("ModuleA");

        assertThat(deps).isEmpty();
    }

    @Test
    void getTransitiveDependencies_diamondDependency_returnsAll() {
        // D depends on B and C, both B and C depend on A
        EngineModule moduleA = mockModule("ModuleA");
        CompoundModule moduleB = mockCompoundModule("ModuleB",
                List.of(ModuleIdentifier.of("ModuleA", 1, 0)));
        CompoundModule moduleC = mockCompoundModule("ModuleC",
                List.of(ModuleIdentifier.of("ModuleA", 1, 0)));
        CompoundModule moduleD = mockCompoundModule("ModuleD",
                List.of(ModuleIdentifier.of("ModuleB", 1, 0), ModuleIdentifier.of("ModuleC", 1, 0)));

        resolver.addModule(moduleA);
        resolver.addModule(moduleB);
        resolver.addModule(moduleC);
        resolver.addModule(moduleD);

        Set<String> deps = resolver.getTransitiveDependencies("ModuleD");

        assertThat(deps).containsExactlyInAnyOrder("ModuleA", "ModuleB", "ModuleC");
    }

    @Test
    void clear_removesAllData() {
        EngineModule moduleA = mockModule("ModuleA");
        resolver.addModule(moduleA);

        resolver.clear();

        assertThat(resolver.getDependencies("ModuleA")).isEmpty();
        assertThat(resolver.getTopologicalOrder()).isEmpty();
    }

    private EngineModule mockModule(String name) {
        EngineModule mock = mock(EngineModule.class);
        when(mock.getName()).thenReturn(name);
        when(mock.getVersion()).thenReturn(ModuleVersion.of(1, 0));
        return mock;
    }

    private CompoundModule mockCompoundModule(String name, List<ModuleIdentifier> components) {
        CompoundModule mock = mock(CompoundModule.class);
        when(mock.getName()).thenReturn(name);
        when(mock.getVersion()).thenReturn(ModuleVersion.of(1, 0));
        when(mock.getComponentModules()).thenReturn(components);
        return mock;
    }
}
