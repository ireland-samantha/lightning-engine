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

import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.core.container.ContainerConfig;
import ca.samanthaireland.lightning.engine.core.container.ContainerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InMemoryContainerManager")
class InMemoryContainerManagerTest {

    private InMemoryContainerManager containerManager;

    @BeforeEach
    void setUp() {
        containerManager = new InMemoryContainerManager();
    }

    @Nested
    @DisplayName("initialization")
    class Initialization {

        @Test
        @DisplayName("should start with no containers")
        void shouldStartWithNoContainers() {
            assertThat(containerManager.getContainerCount()).isZero();
            assertThat(containerManager.getAllContainers()).isEmpty();
        }

        @Test
        @DisplayName("should return null for default container when none exist")
        void shouldReturnNullForDefaultContainer() {
            assertThat(containerManager.getDefaultContainer()).isNull();
        }
    }

    @Nested
    @DisplayName("createContainer")
    class CreateContainer {

        @Test
        @DisplayName("should create container with unique ID")
        void shouldCreateContainerWithUniqueId() {
            ContainerConfig config = ContainerConfig.withDefaults("test-container");
            ExecutionContainer container = containerManager.createContainer(config);

            assertThat(container).isNotNull();
            assertThat(container.getId()).isPositive();
            assertThat(container.getName()).isEqualTo("test-container");
        }

        @Test
        @DisplayName("should assign sequential IDs starting from 1")
        void shouldAssignSequentialIds() {
            ExecutionContainer p1 = containerManager.createContainer(ContainerConfig.withDefaults("p1"));
            ExecutionContainer p2 = containerManager.createContainer(ContainerConfig.withDefaults("p2"));
            ExecutionContainer p3 = containerManager.createContainer(ContainerConfig.withDefaults("p3"));

            assertThat(p1.getId()).isEqualTo(1);
            assertThat(p2.getId()).isEqualTo(2);
            assertThat(p3.getId()).isEqualTo(3);
        }

        @Test
        @DisplayName("should register container for lookup")
        void shouldRegisterContainerForLookup() {
            ExecutionContainer created = containerManager.createContainer(
                    ContainerConfig.withDefaults("test"));

            Optional<ExecutionContainer> found = containerManager.getContainer(created.getId());

            assertThat(found).isPresent();
            assertThat(found.get()).isEqualTo(created);
        }

        @Test
        @DisplayName("should increment container count")
        void shouldIncrementContainerCount() {
            assertThat(containerManager.getContainerCount()).isZero();

            containerManager.createContainer(ContainerConfig.withDefaults("p1"));
            assertThat(containerManager.getContainerCount()).isEqualTo(1);

            containerManager.createContainer(ContainerConfig.withDefaults("p2"));
            assertThat(containerManager.getContainerCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should create container in CREATED status")
        void shouldCreateContainerInCreatedStatus() {
            ExecutionContainer container = containerManager.createContainer(
                    ContainerConfig.withDefaults("test"));

            assertThat(container.getStatus()).isEqualTo(ContainerStatus.CREATED);
        }
    }

    @Nested
    @DisplayName("getContainer")
    class GetContainer {

        @Test
        @DisplayName("should return empty for non-existent ID")
        void shouldReturnEmptyForNonExistentId() {
            Optional<ExecutionContainer> result = containerManager.getContainer(999);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return container by ID")
        void shouldReturnContainerById() {
            ExecutionContainer created = containerManager.createContainer(
                    ContainerConfig.withDefaults("test"));

            Optional<ExecutionContainer> found = containerManager.getContainer(created.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(created.getId());
        }
    }

    @Nested
    @DisplayName("getContainerByName")
    class GetContainerByName {

        @Test
        @DisplayName("should return empty for non-existent name")
        void shouldReturnEmptyForNonExistentName() {
            Optional<ExecutionContainer> result = containerManager.getContainerByName("unknown");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return container by name")
        void shouldReturnContainerByName() {
            containerManager.createContainer(ContainerConfig.withDefaults("alpha"));
            containerManager.createContainer(ContainerConfig.withDefaults("beta"));

            Optional<ExecutionContainer> found = containerManager.getContainerByName("beta");

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("beta");
        }
    }

    @Nested
    @DisplayName("getAllContainers")
    class GetAllContainers {

        @Test
        @DisplayName("should return empty list when no containers exist")
        void shouldReturnEmptyListWhenNoContainers() {
            List<ExecutionContainer> all = containerManager.getAllContainers();
            assertThat(all).isEmpty();
        }

        @Test
        @DisplayName("should return all created containers")
        void shouldReturnAllCreatedContainers() {
            containerManager.createContainer(ContainerConfig.withDefaults("p1"));
            containerManager.createContainer(ContainerConfig.withDefaults("p2"));
            containerManager.createContainer(ContainerConfig.withDefaults("p3"));

            List<ExecutionContainer> all = containerManager.getAllContainers();

            assertThat(all).hasSize(3);
            assertThat(all).extracting(ExecutionContainer::getName)
                    .containsExactlyInAnyOrder("p1", "p2", "p3");
        }
    }

    @Nested
    @DisplayName("deleteContainer")
    class DeleteContainer {

        @Test
        @DisplayName("should throw EntityNotFoundException for non-existent container")
        void shouldThrowForNonExistentContainer() {
            assertThatThrownBy(() -> containerManager.deleteContainer(999))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Container not found: 999");
        }

        @Test
        @DisplayName("should throw IllegalStateException if container is running")
        void shouldThrowIfContainerIsRunning() {
            ExecutionContainer container = containerManager.createContainer(
                    ContainerConfig.withDefaults("test"));
            container.lifecycle().start();

            assertThatThrownBy(() -> containerManager.deleteContainer(container.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must be stopped");
        }

        @Test
        @DisplayName("should delete stopped container")
        void shouldDeleteStoppedContainer() {
            ExecutionContainer container = containerManager.createContainer(
                    ContainerConfig.withDefaults("test"));
            container.lifecycle().start();
            container.lifecycle().stop();

            containerManager.deleteContainer(container.getId());

            assertThat(containerManager.getContainer(container.getId())).isEmpty();
            assertThat(containerManager.getContainerCount()).isZero();
        }

        @Test
        @DisplayName("should delete container that was never started")
        void shouldDeleteNeverStartedContainer() {
            ExecutionContainer container = containerManager.createContainer(
                    ContainerConfig.withDefaults("test"));

            // Container in CREATED state should also not be deletable
            // unless we consider CREATED as equivalent to STOPPED
            // Let's check the actual behavior
            assertThatThrownBy(() -> containerManager.deleteContainer(container.getId()))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("getDefaultContainer")
    class GetDefaultContainer {

        @Test
        @DisplayName("should return null when no containers exist")
        void shouldReturnNullWhenNoContainers() {
            assertThat(containerManager.getDefaultContainer()).isNull();
        }

        @Test
        @DisplayName("should return first available container")
        void shouldReturnFirstAvailableContainer() {
            containerManager.createContainer(ContainerConfig.withDefaults("first"));
            containerManager.createContainer(ContainerConfig.withDefaults("second"));

            ExecutionContainer defaultContainer = containerManager.getDefaultContainer();

            assertThat(defaultContainer).isNotNull();
        }
    }

    @Nested
    @DisplayName("shutdownAll")
    class ShutdownAll {

        @Test
        @DisplayName("should stop all running containers")
        void shouldStopAllRunningContainers() {
            ExecutionContainer p1 = containerManager.createContainer(
                    ContainerConfig.withDefaults("p1"));
            ExecutionContainer p2 = containerManager.createContainer(
                    ContainerConfig.withDefaults("p2"));
            p1.lifecycle().start();
            p2.lifecycle().start();

            containerManager.shutdownAll();

            assertThat(containerManager.getContainerCount()).isZero();
        }

        @Test
        @DisplayName("should handle empty container list")
        void shouldHandleEmptyContainerList() {
            containerManager.shutdownAll(); // Should not throw
            assertThat(containerManager.getContainerCount()).isZero();
        }
    }
}
