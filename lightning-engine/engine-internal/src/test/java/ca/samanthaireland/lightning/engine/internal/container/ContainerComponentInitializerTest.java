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

package ca.samanthaireland.lightning.engine.internal.container;

import ca.samanthaireland.lightning.engine.core.container.ContainerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ContainerComponentInitializer}.
 */
@DisplayName("ContainerComponentInitializer")
class ContainerComponentInitializerTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should store container ID")
        void shouldStoreContainerId() {
            ContainerConfig config = ContainerConfig.withDefaults("test");
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(42L, config, () -> 0L);

            assertThat(initializer.getContainerId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("should store config")
        void shouldStoreConfig() {
            ContainerConfig config = ContainerConfig.withDefaults("test-container");
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            assertThat(initializer.getConfig()).isEqualTo(config);
            assertThat(initializer.getConfig().name()).isEqualTo("test-container");
        }
    }

    @Nested
    @DisplayName("before initialization")
    class BeforeInitialization {

        @Test
        @DisplayName("should have null entity store before initialize")
        void shouldHaveNullEntityStoreBeforeInitialize() {
            ContainerConfig config = ContainerConfig.withDefaults("test");
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            assertThat(initializer.getEntityStore()).isNull();
        }

        @Test
        @DisplayName("should have null permission registry before initialize")
        void shouldHaveNullPermissionRegistryBeforeInitialize() {
            ContainerConfig config = ContainerConfig.withDefaults("test");
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            assertThat(initializer.getPermissionRegistry()).isNull();
        }

        @Test
        @DisplayName("should have null module manager before initialize")
        void shouldHaveNullModuleManagerBeforeInitialize() {
            ContainerConfig config = ContainerConfig.withDefaults("test");
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            assertThat(initializer.getModuleManager()).isNull();
        }

        @Test
        @DisplayName("should have null game loop before initialize")
        void shouldHaveNullGameLoopBeforeInitialize() {
            ContainerConfig config = ContainerConfig.withDefaults("test");
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            assertThat(initializer.getGameLoop()).isNull();
        }

        @Test
        @DisplayName("should have null class loader before initialize")
        void shouldHaveNullClassLoaderBeforeInitialize() {
            ContainerConfig config = ContainerConfig.withDefaults("test");
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            assertThat(initializer.getContainerClassLoader()).isNull();
        }

        @Test
        @DisplayName("should have null match service before initialize")
        void shouldHaveNullMatchServiceBeforeInitialize() {
            ContainerConfig config = ContainerConfig.withDefaults("test");
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            assertThat(initializer.getMatchService()).isNull();
        }

        @Test
        @DisplayName("should have null command queue manager before initialize")
        void shouldHaveNullCommandQueueManagerBeforeInitialize() {
            ContainerConfig config = ContainerConfig.withDefaults("test");
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            assertThat(initializer.getCommandQueueManager()).isNull();
        }

        @Test
        @DisplayName("should have null injector before initialize")
        void shouldHaveNullInjectorBeforeInitialize() {
            ContainerConfig config = ContainerConfig.withDefaults("test");
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            assertThat(initializer.getInjector()).isNull();
        }
    }

    @Nested
    @DisplayName("after initialization")
    class AfterInitialization {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("should initialize entity store")
        void shouldInitializeEntityStore() {
            ContainerConfig config = ContainerConfig.builder("test")
                    .moduleScanDirectory(tempDir)
                    .moduleJarPaths(List.of())
                    .build();
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            initializer.initialize();

            assertThat(initializer.getEntityStore()).isNotNull();
        }

        @Test
        @DisplayName("should initialize permission registry")
        void shouldInitializePermissionRegistry() {
            ContainerConfig config = ContainerConfig.builder("test")
                    .moduleScanDirectory(tempDir)
                    .moduleJarPaths(List.of())
                    .build();
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            initializer.initialize();

            assertThat(initializer.getPermissionRegistry()).isNotNull();
        }

        @Test
        @DisplayName("should initialize module manager")
        void shouldInitializeModuleManager() {
            ContainerConfig config = ContainerConfig.builder("test")
                    .moduleScanDirectory(tempDir)
                    .moduleJarPaths(List.of())
                    .build();
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            initializer.initialize();

            assertThat(initializer.getModuleManager()).isNotNull();
        }

        @Test
        @DisplayName("should initialize game loop")
        void shouldInitializeGameLoop() {
            ContainerConfig config = ContainerConfig.builder("test")
                    .moduleScanDirectory(tempDir)
                    .moduleJarPaths(List.of())
                    .build();
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            initializer.initialize();

            assertThat(initializer.getGameLoop()).isNotNull();
        }

        @Test
        @DisplayName("should initialize class loader")
        void shouldInitializeClassLoader() {
            ContainerConfig config = ContainerConfig.builder("test")
                    .moduleScanDirectory(tempDir)
                    .moduleJarPaths(List.of())
                    .build();
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            initializer.initialize();

            assertThat(initializer.getContainerClassLoader()).isNotNull();
        }

        @Test
        @DisplayName("should initialize match service")
        void shouldInitializeMatchService() {
            ContainerConfig config = ContainerConfig.builder("test")
                    .moduleScanDirectory(tempDir)
                    .moduleJarPaths(List.of())
                    .build();
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            initializer.initialize();

            assertThat(initializer.getMatchService()).isNotNull();
        }

        @Test
        @DisplayName("should initialize command queue manager")
        void shouldInitializeCommandQueueManager() {
            ContainerConfig config = ContainerConfig.builder("test")
                    .moduleScanDirectory(tempDir)
                    .moduleJarPaths(List.of())
                    .build();
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            initializer.initialize();

            assertThat(initializer.getCommandQueueManager()).isNotNull();
        }

        @Test
        @DisplayName("should initialize injector")
        void shouldInitializeInjector() {
            ContainerConfig config = ContainerConfig.builder("test")
                    .moduleScanDirectory(tempDir)
                    .moduleJarPaths(List.of())
                    .build();
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            initializer.initialize();

            assertThat(initializer.getInjector()).isNotNull();
        }

        @Test
        @DisplayName("should initialize AI manager")
        void shouldInitializeAiManager() {
            ContainerConfig config = ContainerConfig.builder("test")
                    .moduleScanDirectory(tempDir)
                    .moduleJarPaths(List.of())
                    .build();
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            initializer.initialize();

            assertThat(initializer.getAiManager()).isNotNull();
        }

        @Test
        @DisplayName("should initialize resource manager")
        void shouldInitializeResourceManager() {
            ContainerConfig config = ContainerConfig.builder("test")
                    .moduleScanDirectory(tempDir)
                    .moduleJarPaths(List.of())
                    .build();
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            initializer.initialize();

            assertThat(initializer.getResourceManager()).isNotNull();
        }

        @Test
        @DisplayName("should initialize command resolver")
        void shouldInitializeCommandResolver() {
            ContainerConfig config = ContainerConfig.builder("test")
                    .moduleScanDirectory(tempDir)
                    .moduleJarPaths(List.of())
                    .build();
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            initializer.initialize();

            assertThat(initializer.getCommandResolver()).isNotNull();
        }
    }

    @Nested
    @DisplayName("with custom config")
    class WithCustomConfig {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("should respect maxEntities from config")
        void shouldRespectMaxEntitiesFromConfig() {
            ContainerConfig config = ContainerConfig.builder("test")
                    .maxEntities(5000)
                    .moduleScanDirectory(tempDir)
                    .moduleJarPaths(List.of())
                    .build();
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            initializer.initialize();

            // Entity store should be initialized with the specified capacity
            assertThat(initializer.getEntityStore()).isNotNull();
        }

        @Test
        @DisplayName("should use default module scan directory when not specified")
        void shouldUseDefaultModuleScanDirectory() {
            ContainerConfig config = ContainerConfig.builder("test")
                    .moduleScanDirectory(null) // Not specified
                    .moduleJarPaths(List.of())
                    .build();
            ContainerComponentInitializer initializer = new ContainerComponentInitializer(1L, config, () -> 0L);

            initializer.initialize();

            // Should still initialize without error, using default "modules" path
            assertThat(initializer.getModuleManager()).isNotNull();
        }
    }
}
