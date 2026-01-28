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


package ca.samanthaireland.lightning.engine.internal.ext;

import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.core.store.PermissionRegistry;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;
import ca.samanthaireland.lightning.engine.internal.ext.jar.FactoryClassloader;
import ca.samanthaireland.lightning.engine.internal.ext.module.OnDiskModuleManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OnDiskEngineModuleManagerTest {

    @TempDir
    Path tempDir;

    @Mock
    FactoryClassloader<ModuleFactory> factoryClassloader;

    @Mock
    ModuleFactory mockFactory;

    @Mock
    EngineModule mockModule;

    @Mock
    ModuleContext mockContext;

    @Mock
    PermissionRegistry permissionRegistry;

    @Mock
    EntityComponentStore entityComponentStore;

    private OnDiskModuleManager resolver;

    @BeforeEach
    void setUp() {
        resolver = new OnDiskModuleManager(tempDir, factoryClassloader, mockContext, permissionRegistry, entityComponentStore);
    }

    @Test
    void reloadInstalled_withEmptyDirectory_shouldCompleteSuccessfully() throws IOException {
        resolver.reloadInstalled();

        assertThat(resolver.getAvailableModules()).isEmpty();
    }

    @Test
    void reloadInstalled_withNonExistentDirectory_shouldCreateDirectoryAndComplete() throws IOException {
        Path nonExistent = tempDir.resolve("non-existent");
        OnDiskModuleManager resolverWithNonExistent = new OnDiskModuleManager(nonExistent, factoryClassloader, mockContext, permissionRegistry, entityComponentStore);

        resolverWithNonExistent.reloadInstalled();

        assertThat(Files.exists(nonExistent)).isTrue();
        assertThat(resolverWithNonExistent.getAvailableModules()).isEmpty();
    }

    @Test
    void reloadInstalled_withJarFiles_shouldLoadFactories() throws IOException {
        // Create a fake JAR file
        Path jarPath = tempDir.resolve("test-module.jar");
        Files.createFile(jarPath);

        when(factoryClassloader.loadFactoriesFromJar(any(File.class))).thenReturn(List.of(mockFactory));
        when(mockFactory.create(any(ModuleContext.class))).thenReturn(mockModule);
        when(mockModule.getName()).thenReturn("TestModule");

        resolver.reloadInstalled();

        assertThat(resolver.getAvailableModules()).containsExactly("TestModule");
    }

    @Test
    void reloadInstalled_withNonJarFiles_shouldIgnoreThem() throws IOException {
        // Create non-JAR files
        Files.createFile(tempDir.resolve("readme.txt"));
        Files.createFile(tempDir.resolve("config.xml"));

        resolver.reloadInstalled();

        verify(factoryClassloader, never()).loadFactoriesFromJar(any(File.class));
        assertThat(resolver.getAvailableModules()).isEmpty();
    }

    @Test
    void resolveModule_shouldReturnModuleInitializedDuringScanning() throws IOException {
        Path jarPath = tempDir.resolve("test-module.jar");
        Files.createFile(jarPath);

        when(factoryClassloader.loadFactoriesFromJar(any(File.class))).thenReturn(List.of(mockFactory));
        when(mockFactory.create(any(ModuleContext.class))).thenReturn(mockModule);
        when(mockModule.getName()).thenReturn("TestModule");

        EngineModule result = resolver.resolveModule("TestModule");

        assertThat(result).isEqualTo(mockModule);
        // Factory is called during scanning, not on resolve
        verify(mockFactory).create(any(ModuleContext.class));
    }

    @Test
    void resolveModule_shouldReturnCachedModule() throws IOException {
        Path jarPath = tempDir.resolve("test-module.jar");
        Files.createFile(jarPath);

        when(factoryClassloader.loadFactoriesFromJar(any(File.class))).thenReturn(List.of(mockFactory));
        when(mockFactory.create(any(ModuleContext.class))).thenReturn(mockModule);
        when(mockModule.getName()).thenReturn("TestModule");

        EngineModule first = resolver.resolveModule("TestModule");
        EngineModule second = resolver.resolveModule("TestModule");

        assertThat(first).isSameAs(second);
        // Factory is called once during scanning, cached for subsequent resolves
        verify(mockFactory, times(1)).create(any(ModuleContext.class));
    }

    @Test
    void resolveModule_withNonExistentModule_shouldReturnNull() throws IOException {
        resolver.reloadInstalled();

        EngineModule result = resolver.resolveModule("NonExistent");

        assertThat(result).isNull();
    }

    @Test
    void resolveModule_whenFactoryThrowsExceptionDuringScanning_shouldReturnNull() throws IOException {
        Path jarPath = tempDir.resolve("test-module.jar");
        Files.createFile(jarPath);

        when(factoryClassloader.loadFactoriesFromJar(any(File.class))).thenReturn(List.of(mockFactory));
        when(mockFactory.create(any(ModuleContext.class))).thenThrow(new RuntimeException("Factory failed"));

        // Module initialization fails during scanning, so it won't be in the cache
        EngineModule result = resolver.resolveModule("TestModule");

        assertThat(result).isNull();
    }

    @Test
    void resolveAllModules_shouldReturnAllModulesInitializedDuringScanning() throws IOException {
        Path jarPath = tempDir.resolve("test-module.jar");
        Files.createFile(jarPath);

        ModuleFactory factory1 = mock(ModuleFactory.class);
        ModuleFactory factory2 = mock(ModuleFactory.class);
        EngineModule module1 = mock(EngineModule.class);
        EngineModule module2 = mock(EngineModule.class);

        when(factoryClassloader.loadFactoriesFromJar(any(File.class))).thenReturn(List.of(factory1, factory2));
        when(factory1.create(any(ModuleContext.class))).thenReturn(module1);
        when(factory2.create(any(ModuleContext.class))).thenReturn(module2);
        when(module1.getName()).thenReturn("Module1");
        when(module2.getName()).thenReturn("Module2");

        List<EngineModule> result = resolver.resolveAllModules();

        assertThat(result).containsExactlyInAnyOrder(module1, module2);
    }

    @Test
    void getAvailableModules_shouldReturnAllModuleNames() throws IOException {
        Path jarPath = tempDir.resolve("test-module.jar");
        Files.createFile(jarPath);

        ModuleFactory factory1 = mock(ModuleFactory.class);
        ModuleFactory factory2 = mock(ModuleFactory.class);
        EngineModule module1 = mock(EngineModule.class);
        EngineModule module2 = mock(EngineModule.class);

        when(factoryClassloader.loadFactoriesFromJar(any(File.class))).thenReturn(List.of(factory1, factory2));
        when(factory1.create(any(ModuleContext.class))).thenReturn(module1);
        when(factory2.create(any(ModuleContext.class))).thenReturn(module2);
        when(module1.getName()).thenReturn("Alpha");
        when(module2.getName()).thenReturn("Beta");

        List<String> result = resolver.getAvailableModules();

        assertThat(result).containsExactlyInAnyOrder("Alpha", "Beta");
    }

    @Test
    void hasModule_shouldReturnTrueForExistingModule() throws IOException {
        Path jarPath = tempDir.resolve("test-module.jar");
        Files.createFile(jarPath);

        when(factoryClassloader.loadFactoriesFromJar(any(File.class))).thenReturn(List.of(mockFactory));
        when(mockFactory.create(any(ModuleContext.class))).thenReturn(mockModule);
        when(mockModule.getName()).thenReturn("TestModule");

        assertThat(resolver.hasModule("TestModule")).isTrue();
        assertThat(resolver.hasModule("NonExistent")).isFalse();
    }

    @Test
    void reset_shouldClearCachesAndRequireRescan() throws IOException {
        Path jarPath = tempDir.resolve("test-module.jar");
        Files.createFile(jarPath);

        when(factoryClassloader.loadFactoriesFromJar(any(File.class))).thenReturn(List.of(mockFactory));
        when(mockFactory.create(any(ModuleContext.class))).thenReturn(mockModule);
        when(mockModule.getName()).thenReturn("TestModule");

        // First access triggers scan
        resolver.resolveModule("TestModule");
        verify(factoryClassloader, times(1)).loadFactoriesFromJar(any(File.class));

        // Reset clears internal state
        resolver.reset();

        // Next access triggers re-scan
        resolver.resolveModule("TestModule");
        verify(factoryClassloader, times(2)).loadFactoriesFromJar(any(File.class));
    }

    @Test
    void reset_shouldClearModuleCache() throws IOException {
        Path jarPath = tempDir.resolve("test-module.jar");
        Files.createFile(jarPath);

        EngineModule module1 = mock(EngineModule.class);
        EngineModule module2 = mock(EngineModule.class);

        when(factoryClassloader.loadFactoriesFromJar(any(File.class))).thenReturn(List.of(mockFactory));
        when(mockFactory.create(any(ModuleContext.class))).thenReturn(module1).thenReturn(module2);
        when(module1.getName()).thenReturn("TestModule");
        when(module2.getName()).thenReturn("TestModule");

        // First access triggers scan which creates module1
        EngineModule first = resolver.resolveModule("TestModule");
        assertThat(first).isSameAs(module1);

        // Reset clears caches
        resolver.reset();

        // Next access triggers rescan which creates new module (module2)
        EngineModule second = resolver.resolveModule("TestModule");
        assertThat(second).isSameAs(module2);
        assertThat(second).isNotSameAs(first);
    }

    @Test
    void getJarDirectory_shouldReturnConfiguredPath() {
        assertThat(resolver.getScanDirectory()).isEqualTo(tempDir);
    }

    @Test
    void reloadInstalled_withDuplicateModuleNames_shouldKeepLastModule() throws IOException {
        Path jar1 = tempDir.resolve("module1.jar");
        Path jar2 = tempDir.resolve("module2.jar");
        Files.createFile(jar1);
        Files.createFile(jar2);

        ModuleFactory factory1 = mock(ModuleFactory.class);
        ModuleFactory factory2 = mock(ModuleFactory.class);
        EngineModule module1 = mock(EngineModule.class);
        EngineModule module2 = mock(EngineModule.class);

        when(factoryClassloader.loadFactoriesFromJar(jar1.toFile())).thenReturn(List.of(factory1));
        when(factoryClassloader.loadFactoriesFromJar(jar2.toFile())).thenReturn(List.of(factory2));
        // Use lenient stubbing since we don't know which factory will be used (depends on file iteration order)
        lenient().when(factory1.create(any(ModuleContext.class))).thenReturn(module1);
        lenient().when(factory2.create(any(ModuleContext.class))).thenReturn(module2);
        lenient().when(module1.getName()).thenReturn("DuplicateName");
        lenient().when(module2.getName()).thenReturn("DuplicateName");

        resolver.reloadInstalled();
        EngineModule result = resolver.resolveModule("DuplicateName");

        // Only one module should exist for the duplicate name
        assertThat(resolver.getAvailableModules()).containsExactly("DuplicateName");
        // Result should be one of the two modules
        assertThat(result).isIn(module1, module2);
    }

    @Test
    void reloadInstalled_whenJarLoaderThrowsIOException_shouldContinueWithOtherJars() throws IOException {
        Path jar1 = tempDir.resolve("bad.jar");
        Path jar2 = tempDir.resolve("good.jar");
        Files.createFile(jar1);
        Files.createFile(jar2);

        when(factoryClassloader.loadFactoriesFromJar(jar1.toFile())).thenThrow(new IOException("Bad JAR"));
        when(factoryClassloader.loadFactoriesFromJar(jar2.toFile())).thenReturn(List.of(mockFactory));
        when(mockFactory.create(any(ModuleContext.class))).thenReturn(mockModule);
        when(mockModule.getName()).thenReturn("GoodModule");

        resolver.reloadInstalled();

        assertThat(resolver.getAvailableModules()).containsExactly("GoodModule");
    }

    @Test
    void installModule_shouldCopyJarToReloadInstalledDirectoryAndRescan() throws IOException {
        // Create a source JAR file outside the scan directory
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        Path sourceJar = sourceDir.resolve("new-module.jar");
        Files.createFile(sourceJar);

        // Setup scan directory as a subdirectory
        Path scanDir = tempDir.resolve("modules");
        OnDiskModuleManager resolverWithScanDir = new OnDiskModuleManager(scanDir, factoryClassloader, mockContext, permissionRegistry, entityComponentStore);

        when(factoryClassloader.loadFactoriesFromJar(any(File.class))).thenReturn(List.of(mockFactory));
        when(mockFactory.create(any(ModuleContext.class))).thenReturn(mockModule);
        when(mockModule.getName()).thenReturn("NewModule");

        resolverWithScanDir.installModule(sourceJar);

        // Verify JAR was copied to scan directory
        Path copiedJar = scanDir.resolve("new-module.jar");
        assertThat(Files.exists(copiedJar)).isTrue();

        // Verify module is available
        assertThat(resolverWithScanDir.getAvailableModules()).containsExactly("NewModule");
    }

    @Test
    void installModule_shouldOverwriteExistingJar() throws IOException {
        // Create an existing JAR in scan directory
        Path existingJar = tempDir.resolve("existing.jar");
        Files.write(existingJar, "old content".getBytes());

        // Create a source JAR with new content
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        Path sourceJar = sourceDir.resolve("existing.jar");
        Files.write(sourceJar, "new content".getBytes());

        when(factoryClassloader.loadFactoriesFromJar(any(File.class))).thenReturn(List.of(mockFactory));
        when(mockFactory.create(any(ModuleContext.class))).thenReturn(mockModule);
        when(mockModule.getName()).thenReturn("ExistingModule");

        resolver.installModule(sourceJar);

        // Verify JAR was overwritten
        Path copiedJar = tempDir.resolve("existing.jar");
        assertThat(Files.readString(copiedJar)).isEqualTo("new content");
    }

    @Test
    void installModule_withNonExistentFile_shouldThrowIOException() {
        Path nonExistent = tempDir.resolve("non-existent.jar");

        assertThatThrownBy(() -> resolver.installModule(nonExistent))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void installModule_withNonJarFile_shouldThrowIOException() throws IOException {
        Path textFile = tempDir.resolve("readme.txt");
        Files.createFile(textFile);

        assertThatThrownBy(() -> resolver.installModule(textFile))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("not a JAR file");
    }

    @Test
    void installModule_shouldCreateReloadInstalledDirectoryIfNotExists() throws IOException {
        // Create source JAR
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        Path sourceJar = sourceDir.resolve("module.jar");
        Files.createFile(sourceJar);

        // Use non-existent scan directory
        Path nonExistentScanDir = tempDir.resolve("new-scan-dir");
        OnDiskModuleManager resolverWithNewDir = new OnDiskModuleManager(nonExistentScanDir, factoryClassloader, mockContext, permissionRegistry, entityComponentStore);

        when(factoryClassloader.loadFactoriesFromJar(any(File.class))).thenReturn(List.of());

        resolverWithNewDir.installModule(sourceJar);

        // Verify scan directory was created
        assertThat(Files.exists(nonExistentScanDir)).isTrue();
        assertThat(Files.isDirectory(nonExistentScanDir)).isTrue();

        // Verify JAR was copied
        assertThat(Files.exists(nonExistentScanDir.resolve("module.jar"))).isTrue();
    }

    @Test
    void installModule_shouldResetAndRescanAfterCopy() throws IOException {
        // First, add a module via direct scan
        Path existingJar = tempDir.resolve("existing.jar");
        Files.createFile(existingJar);

        ModuleFactory existingFactory = mock(ModuleFactory.class);
        EngineModule existingModule = mock(EngineModule.class);
        when(factoryClassloader.loadFactoriesFromJar(existingJar.toFile())).thenReturn(List.of(existingFactory));
        when(existingFactory.create(any(ModuleContext.class))).thenReturn(existingModule);
        when(existingModule.getName()).thenReturn("ExistingModule");

        resolver.reloadInstalled();
        assertThat(resolver.getAvailableModules()).containsExactly("ExistingModule");

        // Now install a new module
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        Path newJar = sourceDir.resolve("new.jar");
        Files.createFile(newJar);

        ModuleFactory newFactory = mock(ModuleFactory.class);
        EngineModule newModule = mock(EngineModule.class);
        // After install, the scan directory will have both jars
        Path copiedJar = tempDir.resolve("new.jar");
        lenient().when(factoryClassloader.loadFactoriesFromJar(existingJar.toFile())).thenReturn(List.of(existingFactory));
        lenient().when(factoryClassloader.loadFactoriesFromJar(copiedJar.toFile())).thenReturn(List.of(newFactory));
        lenient().when(existingFactory.create(any(ModuleContext.class))).thenReturn(existingModule);
        lenient().when(newFactory.create(any(ModuleContext.class))).thenReturn(newModule);
        lenient().when(existingModule.getName()).thenReturn("ExistingModule");
        lenient().when(newModule.getName()).thenReturn("NewModule");

        resolver.installModule(newJar);

        // Both modules should be available after rescan
        assertThat(resolver.getAvailableModules()).containsExactlyInAnyOrder("ExistingModule", "NewModule");
    }

    @Test
    void loadMultipleFactoriesFromSingleJar_shouldRegisterAll() throws IOException {
        // Create a single JAR that contains multiple ModuleFactories
        Path jarPath = tempDir.resolve("multi-module.jar");
        Files.createFile(jarPath);

        // Mock returning multiple factories from single JAR
        ModuleFactory spawnFactory = mock(ModuleFactory.class);
        ModuleFactory moveFactory = mock(ModuleFactory.class);
        ModuleFactory combatFactory = mock(ModuleFactory.class);
        EngineModule spawnModule = mock(EngineModule.class);
        EngineModule moveModule = mock(EngineModule.class);
        EngineModule combatModule = mock(EngineModule.class);

        when(factoryClassloader.loadFactoriesFromJar(jarPath.toFile()))
                .thenReturn(List.of(spawnFactory, moveFactory, combatFactory));
        when(spawnFactory.create(any(ModuleContext.class))).thenReturn(spawnModule);
        when(moveFactory.create(any(ModuleContext.class))).thenReturn(moveModule);
        when(combatFactory.create(any(ModuleContext.class))).thenReturn(combatModule);
        when(spawnModule.getName()).thenReturn("Spawn");
        when(moveModule.getName()).thenReturn("Move");
        when(combatModule.getName()).thenReturn("Combat");

        resolver.reloadInstalled();

        // All three modules should be available
        assertThat(resolver.getAvailableModules()).containsExactlyInAnyOrder("Spawn", "Move", "Combat");

        // All three modules should be resolvable
        List<EngineModule> allModules = resolver.resolveAllModules();
        assertThat(allModules).containsExactlyInAnyOrder(spawnModule, moveModule, combatModule);
    }

    @Test
    void installModuleByClass_shouldRegisterModule() {
        resolver.installModule(TestModuleFactory.class);

        // Module name comes from the module itself
        assertThat(resolver.hasModule("TestModule")).isTrue();
        assertThat(resolver.getAvailableModules()).contains("TestModule");
    }

    @Test
    void installModuleByClass_withDuplicateName_shouldOverwrite() {
        // First install via class
        resolver.installModule(TestModuleFactory.class);
        assertThat(resolver.hasModule("TestModule")).isTrue();

        // Install another with same name - should overwrite
        resolver.installModule(SameNameTestModuleFactory.class);
        assertThat(resolver.getAvailableModules().stream()
                .filter(n -> n.equals("TestModule")).count()).isEqualTo(1);
    }

    // Test helper classes for installModule by class
    public static class TestModuleFactory implements ModuleFactory {
        @Override
        public EngineModule create(ModuleContext context) {
            EngineModule module = mock(EngineModule.class);
            when(module.getName()).thenReturn("TestModule");
            return module;
        }
    }

    public static class SameNameTestModuleFactory implements ModuleFactory {
        @Override
        public EngineModule create(ModuleContext context) {
            EngineModule module = mock(EngineModule.class);
            when(module.getName()).thenReturn("TestModule"); // Same name as TestModuleFactory
            return module;
        }
    }
}
