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
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.core.store.PermissionComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionLevel;
import ca.samanthaireland.lightning.engine.core.store.PermissionRegistry;
import ca.samanthaireland.lightning.engine.core.system.EngineSystem;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.module.ModuleExports;
import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;
import ca.samanthaireland.lightning.engine.internal.core.store.ArrayEntityComponentStore;
import ca.samanthaireland.lightning.engine.internal.core.store.EcsProperties;
import ca.samanthaireland.lightning.engine.internal.core.store.SimplePermissionRegistry;
import ca.samanthaireland.lightning.engine.internal.ext.jar.FactoryClassloader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for module exports functionality.
 *
 * <p>Tests verify that:
 * <ul>
 *   <li>Module exports are registered during module initialization</li>
 *   <li>Module exports can be retrieved by other modules</li>
 *   <li>Module exports can call methods across modules</li>
 *   <li>Multiple modules can export different types</li>
 * </ul>
 */
@DisplayName("Module Exports Integration")
class ModuleExportsIntegrationTest {

    @TempDir
    Path tempDir;

    private OnDiskModuleManager moduleManager;
    private DefaultInjector sharedContext;
    private ArrayEntityComponentStore baseStore;
    private PermissionRegistry permissionRegistry;

    @BeforeEach
    void setUp() {
        // Create base infrastructure
        baseStore = new ArrayEntityComponentStore(new EcsProperties(1000, 100));
        sharedContext = new DefaultInjector();
        sharedContext.addClass(EntityComponentStore.class, baseStore);

        // Create permission registry
        permissionRegistry = new SimplePermissionRegistry();

        // Create a no-op factory classloader (we'll install modules by class)
        FactoryClassloader<ModuleFactory> noOpClassloader = jarFile -> Collections.emptyList();

        moduleManager = new OnDiskModuleManager(tempDir, noOpClassloader, sharedContext, permissionRegistry, baseStore);
    }

    @Test
    @DisplayName("module exports should be registered during module initialization")
    void moduleExports_shouldBeRegisteredDuringInit() {
        // Install a module with exports
        moduleManager.installModule(CalculatorModuleFactory.class);

        // The export should be accessible from the shared context
        CalculatorExports exports = sharedContext.getModuleExports(CalculatorExports.class);

        assertThat(exports).isNotNull();
        assertThat(exports.add(2, 3)).isEqualTo(5);
    }

    @Test
    @DisplayName("module should be able to retrieve exports from another module")
    void module_shouldRetrieveExportsFromAnotherModule() {
        // Install the calculator module first
        moduleManager.installModule(CalculatorModuleFactory.class);

        // Install a consumer module that uses the calculator
        moduleManager.installModule(ConsumerModuleFactory.class);

        // Verify the consumer module was able to use the calculator exports
        ConsumerModule consumer = (ConsumerModule) moduleManager.resolveModule("ConsumerModule");
        assertThat(consumer.getComputedValue()).isEqualTo(10); // 4 + 6 = 10
    }

    @Test
    @DisplayName("multiple modules can export different types")
    void multipleModules_canExportDifferentTypes() {
        // Install both modules
        moduleManager.installModule(CalculatorModuleFactory.class);
        moduleManager.installModule(GreeterModuleFactory.class);

        // Both exports should be available
        CalculatorExports calc = sharedContext.getModuleExports(CalculatorExports.class);
        GreeterExports greeter = sharedContext.getModuleExports(GreeterExports.class);

        assertThat(calc).isNotNull();
        assertThat(greeter).isNotNull();
        assertThat(calc.multiply(3, 4)).isEqualTo(12);
        assertThat(greeter.greet("World")).isEqualTo("Hello, World!");
    }

    @Test
    @DisplayName("getModuleExports returns null for unregistered export type")
    void getModuleExports_returnsNullForUnregisteredType() {
        // Don't install any modules

        UnregisteredExports result = sharedContext.getModuleExports(UnregisteredExports.class);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("exports should be stateful - maintaining internal state across calls")
    void exports_shouldBeStateful() {
        moduleManager.installModule(CounterModuleFactory.class);

        CounterExports counter = sharedContext.getModuleExports(CounterExports.class);

        assertThat(counter.getCount()).isEqualTo(0);
        counter.increment();
        counter.increment();
        assertThat(counter.getCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("module without exports should work correctly")
    void module_withoutExports_shouldWorkCorrectly() {
        moduleManager.installModule(NoExportsModuleFactory.class);

        EngineModule module = moduleManager.resolveModule("NoExportsModule");

        assertThat(module).isNotNull();
        assertThat(module.getName()).isEqualTo("NoExportsModule");
    }

    @Test
    @DisplayName("exports from reloaded module should replace old exports")
    void exportsFromReloadedModule_shouldReplaceOldExports() {
        // Install module
        moduleManager.installModule(CounterModuleFactory.class);

        CounterExports counter1 = sharedContext.getModuleExports(CounterExports.class);
        counter1.increment();
        counter1.increment();
        assertThat(counter1.getCount()).isEqualTo(2);

        // Reset and reinstall
        moduleManager.reset();
        moduleManager.installModule(CounterModuleFactory.class);

        // New export should have fresh state
        CounterExports counter2 = sharedContext.getModuleExports(CounterExports.class);
        assertThat(counter2.getCount()).isEqualTo(0);
    }

    // ==================== Test Export Classes ====================

    /**
     * Calculator export interface.
     */
    public static class CalculatorExports implements ModuleExports {
        public int add(int a, int b) {
            return a + b;
        }

        public int multiply(int a, int b) {
            return a * b;
        }
    }

    /**
     * Greeter export interface.
     */
    public static class GreeterExports implements ModuleExports {
        public String greet(String name) {
            return "Hello, " + name + "!";
        }
    }

    /**
     * Counter export with mutable state.
     */
    public static class CounterExports implements ModuleExports {
        private final AtomicInteger count = new AtomicInteger(0);

        public void increment() {
            count.incrementAndGet();
        }

        public int getCount() {
            return count.get();
        }
    }

    /**
     * Unregistered export type for testing null case.
     */
    public static class UnregisteredExports implements ModuleExports {
    }

    // ==================== Test Module Factories ====================

    /**
     * Factory for calculator module.
     */
    public static class CalculatorModuleFactory implements ModuleFactory {
        @Override
        public EngineModule create(ModuleContext context) {
            return new SimpleTestModule("CalculatorModule", new CalculatorExports());
        }
    }

    /**
     * Factory for greeter module.
     */
    public static class GreeterModuleFactory implements ModuleFactory {
        @Override
        public EngineModule create(ModuleContext context) {
            return new SimpleTestModule("GreeterModule", new GreeterExports());
        }
    }

    /**
     * Factory for counter module.
     */
    public static class CounterModuleFactory implements ModuleFactory {
        @Override
        public EngineModule create(ModuleContext context) {
            return new SimpleTestModule("CounterModule", new CounterExports());
        }
    }

    /**
     * Factory for module without exports.
     */
    public static class NoExportsModuleFactory implements ModuleFactory {
        @Override
        public EngineModule create(ModuleContext context) {
            return new SimpleTestModule("NoExportsModule", null);
        }
    }

    /**
     * Factory for consumer module that uses calculator exports.
     */
    public static class ConsumerModuleFactory implements ModuleFactory {
        @Override
        public EngineModule create(ModuleContext context) {
            return new ConsumerModule(context);
        }
    }

    // ==================== Test Module Implementations ====================

    /**
     * Simple test module with optional export.
     */
    private static class SimpleTestModule implements EngineModule {
        private final String name;
        private final ModuleExports export;
        private final PermissionComponent flagComponent;

        SimpleTestModule(String name, ModuleExports export) {
            this.name = name;
            this.export = export;
            this.flagComponent = PermissionComponent.create(name + "_FLAG", PermissionLevel.PRIVATE);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<EngineSystem> createSystems() {
            return Collections.emptyList();
        }

        @Override
        public List<EngineCommand> createCommands() {
            return Collections.emptyList();
        }

        @Override
        public List<BaseComponent> createComponents() {
            return Collections.emptyList();
        }

        @Override
        public BaseComponent createFlagComponent() {
            return flagComponent;
        }

        @Override
        public List<ModuleExports> getExports() {
            return export != null ? List.of(export) : List.of();
        }
    }

    /**
     * Consumer module that uses exports from another module.
     */
    private static class ConsumerModule implements EngineModule {
        private final ModuleContext context;
        private final PermissionComponent flagComponent;
        private int computedValue;

        ConsumerModule(ModuleContext context) {
            this.context = context;
            this.flagComponent = PermissionComponent.create("ConsumerModule_FLAG", PermissionLevel.PRIVATE);

            // Use the calculator exports during initialization
            CalculatorExports calc = context.getModuleExports(CalculatorExports.class);
            if (calc != null) {
                this.computedValue = calc.add(4, 6);
            }
        }

        @Override
        public String getName() {
            return "ConsumerModule";
        }

        @Override
        public List<EngineSystem> createSystems() {
            return Collections.emptyList();
        }

        @Override
        public List<EngineCommand> createCommands() {
            return Collections.emptyList();
        }

        @Override
        public List<BaseComponent> createComponents() {
            return Collections.emptyList();
        }

        @Override
        public BaseComponent createFlagComponent() {
            return flagComponent;
        }

        public int getComputedValue() {
            return computedValue;
        }
    }
}
