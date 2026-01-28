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

package ca.samanthaireland.lightning.controlplane.module.service;

import ca.samanthaireland.lightning.controlplane.config.ModuleStorageConfiguration;
import ca.samanthaireland.lightning.controlplane.module.exception.ModuleNotFoundException;
import ca.samanthaireland.lightning.controlplane.module.model.ModuleMetadata;
import ca.samanthaireland.lightning.controlplane.module.repository.ModuleRepository;
import ca.samanthaireland.lightning.controlplane.node.model.Node;
import ca.samanthaireland.lightning.controlplane.node.model.NodeCapacity;
import ca.samanthaireland.lightning.controlplane.node.service.NodeRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModuleRegistryServiceImplTest {

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private NodeRegistryService nodeRegistryService;

    @Mock
    private ModuleStorageConfiguration config;

    private ModuleRegistryServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(config.maxFileSize()).thenReturn(100_000_000L);
        service = new ModuleRegistryServiceImpl(moduleRepository, nodeRegistryService, config);
    }

    @Test
    void uploadModule_success_savesToRepository() {
        // Arrange
        byte[] jarData = "fake jar content".getBytes();
        when(moduleRepository.save(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ModuleMetadata result = service.uploadModule(
                "test-module", "1.0.0", "Test module", "test.jar", jarData, "admin"
        );

        // Assert
        assertThat(result.name()).isEqualTo("test-module");
        assertThat(result.version()).isEqualTo("1.0.0");
        assertThat(result.fileSize()).isEqualTo(jarData.length);
        assertThat(result.checksum()).isNotBlank();

        verify(moduleRepository).save(any(ModuleMetadata.class), eq(jarData));
    }

    @Test
    void uploadModule_exceedsMaxSize_throwsException() {
        // Arrange
        when(config.maxFileSize()).thenReturn(10L);
        byte[] largeData = new byte[100];

        // Act & Assert
        assertThatThrownBy(() -> service.uploadModule(
                "test", "1.0", "desc", "test.jar", largeData, "admin"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum");
    }

    @Test
    void getModule_found_returnsMetadata() {
        // Arrange
        ModuleMetadata metadata = ModuleMetadata.create(
                "test", "1.0", "desc", "test.jar", 100, "abc", "admin"
        );
        when(moduleRepository.findByNameAndVersion("test", "1.0")).thenReturn(Optional.of(metadata));

        // Act
        Optional<ModuleMetadata> result = service.getModule("test", "1.0");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("test");
    }

    @Test
    void getModule_notFound_returnsEmpty() {
        // Arrange
        when(moduleRepository.findByNameAndVersion("test", "1.0")).thenReturn(Optional.empty());

        // Act
        Optional<ModuleMetadata> result = service.getModule("test", "1.0");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void listAllModules_returnsAllFromRepository() {
        // Arrange
        List<ModuleMetadata> modules = List.of(
                ModuleMetadata.create("mod1", "1.0", "", "mod1.jar", 100, "abc", "admin"),
                ModuleMetadata.create("mod2", "1.0", "", "mod2.jar", 200, "def", "admin")
        );
        when(moduleRepository.findAll()).thenReturn(modules);

        // Act
        List<ModuleMetadata> result = service.listAllModules();

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    void downloadModule_found_returnsStream() {
        // Arrange
        InputStream jarStream = new ByteArrayInputStream("jar content".getBytes());
        when(moduleRepository.getJarFile("test", "1.0")).thenReturn(Optional.of(jarStream));

        // Act
        InputStream result = service.downloadModule("test", "1.0");

        // Assert
        assertThat(result).isNotNull();
    }

    @Test
    void downloadModule_notFound_throwsException() {
        // Arrange
        when(moduleRepository.getJarFile("test", "1.0")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.downloadModule("test", "1.0"))
                .isInstanceOf(ModuleNotFoundException.class);
    }

    @Test
    void deleteModule_exists_deletesFromRepository() {
        // Arrange
        when(moduleRepository.exists("test", "1.0")).thenReturn(true);
        when(moduleRepository.delete("test", "1.0")).thenReturn(true);

        // Act
        service.deleteModule("test", "1.0");

        // Assert
        verify(moduleRepository).delete("test", "1.0");
    }

    @Test
    void deleteModule_notExists_throwsException() {
        // Arrange
        when(moduleRepository.exists("test", "1.0")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> service.deleteModule("test", "1.0"))
                .isInstanceOf(ModuleNotFoundException.class);
    }

    @Test
    void distributeToAllNodes_returnsSuccessCount() {
        // Arrange
        ModuleMetadata metadata = ModuleMetadata.create(
                "test", "1.0", "", "test.jar", 100, "abc", "admin"
        );
        when(moduleRepository.findByNameAndVersion("test", "1.0")).thenReturn(Optional.of(metadata));
        when(moduleRepository.getJarFile("test", "1.0"))
                .thenReturn(Optional.of(new ByteArrayInputStream("jar".getBytes())));

        Node node1 = Node.register("node-1", "http://localhost:8080", new NodeCapacity(100));
        Node node2 = Node.register("node-2", "http://localhost:8081", new NodeCapacity(100));
        when(nodeRegistryService.findAll()).thenReturn(List.of(node1, node2));

        // Act - will fail HTTP calls but should still count attempts
        int result = service.distributeToAllNodes("test", "1.0");

        // Assert - both nodes attempted, both failed (no real server)
        assertThat(result).isEqualTo(0);
    }

    @Test
    void getModuleVersions_returnsAllVersions() {
        // Arrange
        List<ModuleMetadata> versions = List.of(
                ModuleMetadata.create("test", "1.0", "", "test.jar", 100, "abc", "admin"),
                ModuleMetadata.create("test", "2.0", "", "test.jar", 100, "def", "admin")
        );
        when(moduleRepository.findByName("test")).thenReturn(versions);

        // Act
        List<ModuleMetadata> result = service.getModuleVersions("test");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ModuleMetadata::version).containsExactly("1.0", "2.0");
    }
}
