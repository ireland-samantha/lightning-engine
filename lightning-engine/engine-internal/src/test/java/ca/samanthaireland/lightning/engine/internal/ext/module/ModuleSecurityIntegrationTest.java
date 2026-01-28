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
import ca.samanthaireland.lightning.engine.core.exception.EcsAccessForbiddenException;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.core.store.PermissionComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionLevel;
import ca.samanthaireland.lightning.engine.core.store.PermissionRegistry;
import ca.samanthaireland.lightning.engine.core.system.EngineSystem;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;
import ca.samanthaireland.lightning.engine.internal.core.store.ArrayEntityComponentStore;
import ca.samanthaireland.lightning.engine.internal.core.store.EcsProperties;
import ca.samanthaireland.lightning.engine.internal.core.store.ModuleScopedStore;
import ca.samanthaireland.lightning.engine.internal.core.store.SimplePermissionRegistry;
import ca.samanthaireland.lightning.engine.internal.ext.jar.FactoryClassloader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration test for module-level security enforcement.
 *
 * <p>This test verifies that:
 * <ul>
 *   <li>Modules can always access their own components</li>
 *   <li>PRIVATE components cannot be accessed by other modules</li>
 *   <li>READ components can only be read (not written) by other modules</li>
 *   <li>WRITE components can be read and written by other modules</li>
 * </ul>
 */
class ModuleSecurityIntegrationTest {

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

    /**
     * Helper to get TestModule by name and cast it.
     */
    private TestModule getTestModule(String name) {
        return (TestModule) moduleManager.resolveModule(name);
    }

    @Test
    void moduleCanAccessItsOwnPrivateComponents() {
        // Install Module A
        moduleManager.installModule(ModuleAFactory.class);
        TestModule moduleA = getTestModule("ModuleA");

        // Get Module A's scoped store
        ModuleScopedContext contextA = moduleManager.getModuleContext("ModuleA");
        EntityComponentStore storeA = contextA.getEntityComponentStore();

        // Create an entity
        long entityId = 1L;
        baseStore.createEntity(entityId);

        // Module A should be able to read and write its own PRIVATE component
        assertThatCode(() -> storeA.attachComponent(entityId, moduleA.getPrivateComponent(), 42.0f))
                .doesNotThrowAnyException();

        assertThatCode(() -> {
            float value = storeA.getComponent(entityId, moduleA.getPrivateComponent());
            assertThat(value).isEqualTo(42.0f);
        }).doesNotThrowAnyException();
    }

    @Test
    void moduleCannotAccessOtherModulePrivateComponents() {
        // Install both modules
        moduleManager.installModule(ModuleAFactory.class);
        moduleManager.installModule(ModuleBFactory.class);
        TestModule moduleB = getTestModule("ModuleB");

        // Get Module A's scoped store
        ModuleScopedContext contextA = moduleManager.getModuleContext("ModuleA");
        EntityComponentStore storeA = contextA.getEntityComponentStore();

        // Create an entity
        long entityId = 1L;
        baseStore.createEntity(entityId);

        // Module B attaches its private component using direct store access
        ModuleScopedContext contextB = moduleManager.getModuleContext("ModuleB");
        EntityComponentStore storeB = contextB.getEntityComponentStore();
        storeB.attachComponent(entityId, moduleB.getPrivateComponent(), 100.0f);

        // Module A should NOT be able to read Module B's PRIVATE component
        assertThatThrownBy(() -> storeA.getComponent(entityId, moduleB.getPrivateComponent()))
                .isInstanceOf(EcsAccessForbiddenException.class)
                .hasMessageContaining("PRIVATE");

        // Module A should NOT be able to write Module B's PRIVATE component
        assertThatThrownBy(() -> storeA.attachComponent(entityId, moduleB.getPrivateComponent(), 200.0f))
                .isInstanceOf(EcsAccessForbiddenException.class)
                .hasMessageContaining("PRIVATE");
    }

    @Test
    void moduleCanReadButNotWriteOtherModuleReadComponents() {
        // Install both modules
        moduleManager.installModule(ModuleAFactory.class);
        moduleManager.installModule(ModuleBFactory.class);
        TestModule moduleB = getTestModule("ModuleB");

        // Get both modules' scoped stores
        ModuleScopedContext contextA = moduleManager.getModuleContext("ModuleA");
        EntityComponentStore storeA = contextA.getEntityComponentStore();
        ModuleScopedContext contextB = moduleManager.getModuleContext("ModuleB");
        EntityComponentStore storeB = contextB.getEntityComponentStore();

        // Create an entity
        long entityId = 1L;
        baseStore.createEntity(entityId);

        // Module B attaches its READ component
        storeB.attachComponent(entityId, moduleB.getReadComponent(), 100.0f);

        // Module A SHOULD be able to read Module B's READ component
        assertThatCode(() -> {
            float value = storeA.getComponent(entityId, moduleB.getReadComponent());
            assertThat(value).isEqualTo(100.0f);
        }).doesNotThrowAnyException();

        // Module A should NOT be able to write Module B's READ component
        assertThatThrownBy(() -> storeA.attachComponent(entityId, moduleB.getReadComponent(), 200.0f))
                .isInstanceOf(EcsAccessForbiddenException.class)
                .hasMessageContaining("READ");
    }

    @Test
    void moduleCanReadAndWriteOtherModuleWriteComponents() {
        // Install both modules
        moduleManager.installModule(ModuleAFactory.class);
        moduleManager.installModule(ModuleBFactory.class);
        TestModule moduleB = getTestModule("ModuleB");

        // Get both modules' scoped stores
        ModuleScopedContext contextA = moduleManager.getModuleContext("ModuleA");
        EntityComponentStore storeA = contextA.getEntityComponentStore();
        ModuleScopedContext contextB = moduleManager.getModuleContext("ModuleB");
        EntityComponentStore storeB = contextB.getEntityComponentStore();

        // Create an entity
        long entityId = 1L;
        baseStore.createEntity(entityId);

        // Module B attaches its WRITE component
        storeB.attachComponent(entityId, moduleB.getWriteComponent(), 100.0f);

        // Module A SHOULD be able to read Module B's WRITE component
        assertThatCode(() -> {
            float value = storeA.getComponent(entityId, moduleB.getWriteComponent());
            assertThat(value).isEqualTo(100.0f);
        }).doesNotThrowAnyException();

        // Module A SHOULD be able to write Module B's WRITE component
        assertThatCode(() -> storeA.attachComponent(entityId, moduleB.getWriteComponent(), 200.0f))
                .doesNotThrowAnyException();

        // Verify the value was updated
        assertThat(storeA.getComponent(entityId, moduleB.getWriteComponent())).isEqualTo(200.0f);
    }

    @Test
    void moduleCanAlwaysAccessItsOwnComponentsRegardlessOfPermissionLevel() {
        // Install Module A
        moduleManager.installModule(ModuleAFactory.class);
        TestModule moduleA = getTestModule("ModuleA");

        // Get Module A's scoped store
        ModuleScopedContext contextA = moduleManager.getModuleContext("ModuleA");
        EntityComponentStore storeA = contextA.getEntityComponentStore();

        // Create an entity
        long entityId = 1L;
        baseStore.createEntity(entityId);

        // Module A should be able to access ALL its own components
        assertThatCode(() -> {
            // Write and read PRIVATE
            storeA.attachComponent(entityId, moduleA.getPrivateComponent(), 1.0f);
            assertThat(storeA.getComponent(entityId, moduleA.getPrivateComponent())).isEqualTo(1.0f);

            // Write and read READ
            storeA.attachComponent(entityId, moduleA.getReadComponent(), 2.0f);
            assertThat(storeA.getComponent(entityId, moduleA.getReadComponent())).isEqualTo(2.0f);

            // Write and read WRITE
            storeA.attachComponent(entityId, moduleA.getWriteComponent(), 3.0f);
            assertThat(storeA.getComponent(entityId, moduleA.getWriteComponent())).isEqualTo(3.0f);
        }).doesNotThrowAnyException();
    }

    @Test
    void getEntitiesWithComponentsRespectsPermissions() {
        // Install both modules
        moduleManager.installModule(ModuleAFactory.class);
        moduleManager.installModule(ModuleBFactory.class);
        TestModule moduleB = getTestModule("ModuleB");

        // Get both modules' scoped stores
        ModuleScopedContext contextA = moduleManager.getModuleContext("ModuleA");
        EntityComponentStore storeA = contextA.getEntityComponentStore();
        ModuleScopedContext contextB = moduleManager.getModuleContext("ModuleB");
        EntityComponentStore storeB = contextB.getEntityComponentStore();

        // Create entities
        long entityId = 1L;
        baseStore.createEntity(entityId);

        // Module B attaches components
        storeB.attachComponent(entityId, moduleB.getPrivateComponent(), 1.0f);
        storeB.attachComponent(entityId, moduleB.getReadComponent(), 2.0f);
        storeB.attachComponent(entityId, moduleB.getWriteComponent(), 3.0f);

        // Module A can query entities with READ or WRITE components
        assertThat(storeA.getEntitiesWithComponents(moduleB.getReadComponent())).contains(entityId);
        assertThat(storeA.getEntitiesWithComponents(moduleB.getWriteComponent())).contains(entityId);

        // Module A cannot query entities with PRIVATE components
        assertThatThrownBy(() -> storeA.getEntitiesWithComponents(moduleB.getPrivateComponent()))
                .isInstanceOf(EcsAccessForbiddenException.class);
    }

    @Test
    void hasComponentRespectsPermissions() {
        // Install both modules
        moduleManager.installModule(ModuleAFactory.class);
        moduleManager.installModule(ModuleBFactory.class);
        TestModule moduleB = getTestModule("ModuleB");

        // Get both modules' scoped stores
        ModuleScopedContext contextA = moduleManager.getModuleContext("ModuleA");
        EntityComponentStore storeA = contextA.getEntityComponentStore();
        ModuleScopedContext contextB = moduleManager.getModuleContext("ModuleB");
        EntityComponentStore storeB = contextB.getEntityComponentStore();

        // Create entities
        long entityId = 1L;
        baseStore.createEntity(entityId);

        // Module B attaches components
        storeB.attachComponent(entityId, moduleB.getPrivateComponent(), 1.0f);
        storeB.attachComponent(entityId, moduleB.getReadComponent(), 2.0f);

        // Module A can check READ components
        assertThat(storeA.hasComponent(entityId, moduleB.getReadComponent())).isTrue();

        // Module A cannot check PRIVATE components
        assertThatThrownBy(() -> storeA.hasComponent(entityId, moduleB.getPrivateComponent()))
                .isInstanceOf(EcsAccessForbiddenException.class);
    }

    @Test
    void removeComponentRespectsPermissions() {
        // Install both modules
        moduleManager.installModule(ModuleAFactory.class);
        moduleManager.installModule(ModuleBFactory.class);
        TestModule moduleB = getTestModule("ModuleB");

        // Get both modules' scoped stores
        ModuleScopedContext contextA = moduleManager.getModuleContext("ModuleA");
        EntityComponentStore storeA = contextA.getEntityComponentStore();
        ModuleScopedContext contextB = moduleManager.getModuleContext("ModuleB");
        EntityComponentStore storeB = contextB.getEntityComponentStore();

        // Create entities
        long entityId = 1L;
        baseStore.createEntity(entityId);

        // Module B attaches components
        storeB.attachComponent(entityId, moduleB.getPrivateComponent(), 1.0f);
        storeB.attachComponent(entityId, moduleB.getReadComponent(), 2.0f);
        storeB.attachComponent(entityId, moduleB.getWriteComponent(), 3.0f);

        // Module A cannot remove PRIVATE components
        assertThatThrownBy(() -> storeA.removeComponent(entityId, moduleB.getPrivateComponent()))
                .isInstanceOf(EcsAccessForbiddenException.class);

        // Module A cannot remove READ components
        assertThatThrownBy(() -> storeA.removeComponent(entityId, moduleB.getReadComponent()))
                .isInstanceOf(EcsAccessForbiddenException.class);

        // Module A CAN remove WRITE components
        assertThatCode(() -> storeA.removeComponent(entityId, moduleB.getWriteComponent()))
                .doesNotThrowAnyException();
    }

    // ==================== Test Module Factories ====================

    /**
     * Factory for Module A with PRIVATE, READ, and WRITE components.
     */
    public static class ModuleAFactory implements ModuleFactory {
        @Override
        public EngineModule create(ModuleContext context) {
            return new TestModule("ModuleA",
                    PermissionComponent.create("MODULE_A_PRIVATE", PermissionLevel.PRIVATE),
                    PermissionComponent.create("MODULE_A_READ", PermissionLevel.READ),
                    PermissionComponent.create("MODULE_A_WRITE", PermissionLevel.WRITE));
        }
    }

    /**
     * Factory for Module B with PRIVATE, READ, and WRITE components.
     */
    public static class ModuleBFactory implements ModuleFactory {
        @Override
        public EngineModule create(ModuleContext context) {
            return new TestModule("ModuleB",
                    PermissionComponent.create("MODULE_B_PRIVATE", PermissionLevel.PRIVATE),
                    PermissionComponent.create("MODULE_B_READ", PermissionLevel.READ),
                    PermissionComponent.create("MODULE_B_WRITE", PermissionLevel.WRITE));
        }
    }

    /**
     * Simple test module implementation.
     */
    private static class TestModule implements EngineModule {
        private final String name;
        private final PermissionComponent privateComp;
        private final PermissionComponent readComp;
        private final PermissionComponent writeComp;
        private final PermissionComponent flagComponent;

        TestModule(String name, PermissionComponent privateComp,
                   PermissionComponent readComp, PermissionComponent writeComp) {
            this.name = name;
            this.privateComp = privateComp;
            this.readComp = readComp;
            this.writeComp = writeComp;
            // Use PRIVATE permission for the flag component - only this module can access it
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
            return List.of(privateComp, readComp, writeComp);
        }

        @Override
        public BaseComponent createFlagComponent() {
            return flagComponent;
        }

        public PermissionComponent getPrivateComponent() {
            return privateComp;
        }

        public PermissionComponent getReadComponent() {
            return readComp;
        }

        public PermissionComponent getWriteComponent() {
            return writeComp;
        }
    }
}
