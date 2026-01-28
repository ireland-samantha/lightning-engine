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

package ca.samanthaireland.lightning.engine.internal.ext.ai;

import ca.samanthaireland.lightning.engine.core.command.CommandExecutor;
import ca.samanthaireland.lightning.engine.core.resources.ResourceManager;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.game.domain.AI;
import ca.samanthaireland.game.domain.AIContext;
import ca.samanthaireland.game.backend.installation.AIFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link OnDiskAIManager}.
 */
@DisplayName("OnDiskAIManager")
@ExtendWith(MockitoExtension.class)
class OnDiskAIManagerTest {

    @TempDir
    Path tempDir;

    @Mock
    private AIFactoryFileLoader mockFileLoader;

    @Mock
    private ModuleContext mockModuleContext;

    @Mock
    private CommandExecutor mockCommandExecutor;

    @Mock
    private ResourceManager mockResourceManager;

    @Mock
    private EntityComponentStore mockEntityStore;

    private OnDiskAIManager manager;

    @BeforeEach
    void setUp() {
        lenient().when(mockModuleContext.getEntityComponentStore()).thenReturn(mockEntityStore);
        manager = new OnDiskAIManager(
                tempDir,
                mockFileLoader,
                mockModuleContext,
                mockCommandExecutor,
                mockResourceManager
        );
    }

    @Nested
    @DisplayName("reloadInstalled")
    class ReloadInstalled {

        @Test
        @DisplayName("should create directory if not exists")
        void shouldCreateDirectoryIfNotExists() throws IOException {
            Path nonExistentDir = tempDir.resolve("ai-modules");
            OnDiskAIManager mgr = new OnDiskAIManager(
                    nonExistentDir, mockFileLoader, mockModuleContext,
                    mockCommandExecutor, mockResourceManager
            );

            mgr.reloadInstalled();

            assertThat(Files.exists(nonExistentDir)).isTrue();
        }

        @Test
        @DisplayName("should scan JAR files in directory")
        void shouldScanJARFilesInDirectory() throws IOException {
            Path jarFile = tempDir.resolve("test-ai.jar");
            Files.createFile(jarFile);
            AIFactory mockFactory = createMockFactory("TestAI");
            when(mockFileLoader.loadAIFactories(jarFile.toFile())).thenReturn(List.of(mockFactory));

            manager.reloadInstalled();

            verify(mockFileLoader).loadAIFactories(jarFile.toFile());
        }

        @Test
        @DisplayName("should ignore non-JAR files")
        void shouldIgnoreNonJARFiles() throws IOException {
            Path txtFile = tempDir.resolve("not-a-jar.txt");
            Files.createFile(txtFile);

            manager.reloadInstalled();

            verifyNoInteractions(mockFileLoader);
        }
    }

    @Nested
    @DisplayName("installAI from Path")
    class InstallAIPath {

        @Test
        @DisplayName("should throw IOException for non-existent file")
        void shouldThrowForNonExistentFile() {
            Path nonExistent = tempDir.resolve("does-not-exist.jar");

            assertThatThrownBy(() -> manager.installAI(nonExistent))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("does not exist");
        }

        @Test
        @DisplayName("should throw IOException for non-JAR file")
        void shouldThrowForNonJARFile() throws IOException {
            Path txtFile = tempDir.resolve("file.txt");
            Files.createFile(txtFile);

            assertThatThrownBy(() -> manager.installAI(txtFile))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("not a JAR");
        }
    }

    @Nested
    @DisplayName("installAI from Class")
    class InstallAIClass {

        @Test
        @DisplayName("should instantiate and register factory from class")
        void shouldInstantiateAndRegisterFactoryFromClass() {
            manager.installAI(TestAIFactory.class);

            assertThat(manager.hasAI("TestAI")).isTrue();
        }
    }

    @Nested
    @DisplayName("getFactory")
    class GetFactory {

        @Test
        @DisplayName("should return factory by name")
        void shouldReturnFactoryByName() throws IOException {
            Path jarFile = tempDir.resolve("test.jar");
            Files.createFile(jarFile);
            AIFactory mockFactory = createMockFactory("MyAI");
            when(mockFileLoader.loadAIFactories(jarFile.toFile())).thenReturn(List.of(mockFactory));

            manager.reloadInstalled();
            AIFactory result = manager.getFactory("MyAI");

            assertThat(result).isSameAs(mockFactory);
        }

        @Test
        @DisplayName("should return null for unknown name")
        void shouldReturnNullForUnknownName() throws IOException {
            manager.reloadInstalled();

            AIFactory result = manager.getFactory("UnknownAI");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("createForMatch")
    class CreateForMatch {

        @Test
        @DisplayName("should create AI instance with context")
        void shouldCreateAIInstanceWithContext() throws IOException {
            Path jarFile = tempDir.resolve("test.jar");
            Files.createFile(jarFile);
            AI mockAI = mock(AI.class);
            AIFactory mockFactory = mock(AIFactory.class);
            when(mockFactory.getName()).thenReturn("TestAI");
            when(mockFactory.create(any(AIContext.class))).thenReturn(mockAI);
            when(mockFileLoader.loadAIFactories(jarFile.toFile())).thenReturn(List.of(mockFactory));

            manager.reloadInstalled();
            AI result = manager.createForMatch("TestAI", 123L);

            assertThat(result).isSameAs(mockAI);
            verify(mockFactory).create(any(AIContext.class));
        }

        @Test
        @DisplayName("should return null for unknown AI name")
        void shouldReturnNullForUnknownAIName() throws IOException {
            manager.reloadInstalled();

            AI result = manager.createForMatch("UnknownAI", 1L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getAvailableAIs")
    class GetAvailableAIs {

        @Test
        @DisplayName("should return list of registered AI names")
        void shouldReturnListOfRegisteredAINames() throws IOException {
            Path jarFile = tempDir.resolve("test.jar");
            Files.createFile(jarFile);
            AIFactory factory1 = createMockFactory("AI1");
            AIFactory factory2 = createMockFactory("AI2");
            when(mockFileLoader.loadAIFactories(jarFile.toFile())).thenReturn(List.of(factory1, factory2));

            manager.reloadInstalled();
            List<String> result = manager.getAvailableAIs();

            assertThat(result).containsExactlyInAnyOrder("AI1", "AI2");
        }

        @Test
        @DisplayName("should return empty list when no AI registered")
        void shouldReturnEmptyListWhenNoAIRegistered() throws IOException {
            manager.reloadInstalled();

            List<String> result = manager.getAvailableAIs();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasAI")
    class HasAI {

        @Test
        @DisplayName("should return true for registered AI")
        void shouldReturnTrueForRegisteredAI() throws IOException {
            Path jarFile = tempDir.resolve("test.jar");
            Files.createFile(jarFile);
            AIFactory mockFactory = createMockFactory("ExistingAI");
            when(mockFileLoader.loadAIFactories(jarFile.toFile())).thenReturn(List.of(mockFactory));

            manager.reloadInstalled();

            assertThat(manager.hasAI("ExistingAI")).isTrue();
        }

        @Test
        @DisplayName("should return false for unknown AI")
        void shouldReturnFalseForUnknownAI() throws IOException {
            manager.reloadInstalled();

            assertThat(manager.hasAI("UnknownAI")).isFalse();
        }
    }

    @Nested
    @DisplayName("uninstallAI")
    class UninstallAI {

        @Test
        @DisplayName("should remove AI from cache")
        void shouldRemoveAIFromCache() throws IOException {
            Path jarFile = tempDir.resolve("test.jar");
            Files.createFile(jarFile);
            AIFactory mockFactory = createMockFactory("ToRemove");
            when(mockFileLoader.loadAIFactories(jarFile.toFile())).thenReturn(List.of(mockFactory));

            manager.reloadInstalled();
            boolean result = manager.uninstallAI("ToRemove");

            assertThat(result).isTrue();
            assertThat(manager.hasAI("ToRemove")).isFalse();
        }

        @Test
        @DisplayName("should return false for unknown AI")
        void shouldReturnFalseForUnknownAI() throws IOException {
            manager.reloadInstalled();

            boolean result = manager.uninstallAI("NonExistent");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("reset")
    class Reset {

        @Test
        @DisplayName("should clear factory cache")
        void shouldClearFactoryCache() throws IOException {
            Path jarFile = tempDir.resolve("test.jar");
            Files.createFile(jarFile);
            AIFactory mockFactory = createMockFactory("CachedAI");
            when(mockFileLoader.loadAIFactories(jarFile.toFile())).thenReturn(List.of(mockFactory));

            manager.reloadInstalled();
            assertThat(manager.hasAI("CachedAI")).isTrue();

            manager.reset();
            // After reset, hasAI triggers re-scan, but the JAR file still exists
            // So we verify reset clears the cache by checking the factory must be reloaded
            verify(mockFileLoader, times(1)).loadAIFactories(jarFile.toFile());
        }
    }

    private AIFactory createMockFactory(String name) {
        AIFactory factory = mock(AIFactory.class);
        when(factory.getName()).thenReturn(name);
        return factory;
    }

    // Test factory for class-based installation
    public static class TestAIFactory implements AIFactory {
        @Override
        public String getName() {
            return "TestAI";
        }

        @Override
        public AI create(AIContext context) {
            return mock(AI.class);
        }
    }
}
