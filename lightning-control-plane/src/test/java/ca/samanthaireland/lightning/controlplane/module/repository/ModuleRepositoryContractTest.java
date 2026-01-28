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

package ca.samanthaireland.lightning.controlplane.module.repository;

import ca.samanthaireland.lightning.controlplane.module.model.ModuleMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for {@link ModuleRepository} interface.
 *
 * <p>These tests verify the expected behavior of any ModuleRepository implementation.
 * They use an in-memory implementation to test the interface contract without
 * requiring external dependencies like file system storage.
 */
@DisplayName("ModuleRepository Contract Tests")
class ModuleRepositoryContractTest {

    private ModuleRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryModuleRepository();
    }

    @Test
    @DisplayName("save() should persist module metadata and JAR data")
    void save_shouldPersistMetadataAndJar() {
        ModuleMetadata metadata = createTestMetadata("TestModule", "1.0.0");
        byte[] jarData = "test jar content".getBytes(StandardCharsets.UTF_8);

        repository.save(metadata, jarData);

        Optional<ModuleMetadata> found = repository.findByNameAndVersion("TestModule", "1.0.0");
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("TestModule");
    }

    @Test
    @DisplayName("findByName() should return all versions of a module")
    void findByName_shouldReturnAllVersions() {
        byte[] jarData = "test".getBytes(StandardCharsets.UTF_8);
        repository.save(createTestMetadata("TestModule", "1.0.0"), jarData);
        repository.save(createTestMetadata("TestModule", "1.1.0"), jarData);
        repository.save(createTestMetadata("TestModule", "2.0.0"), jarData);
        repository.save(createTestMetadata("OtherModule", "1.0.0"), jarData);

        List<ModuleMetadata> versions = repository.findByName("TestModule");

        assertThat(versions).hasSize(3);
        assertThat(versions).allMatch(m -> m.name().equals("TestModule"));
    }

    @Test
    @DisplayName("findByNameAndVersion() should return specific version")
    void findByNameAndVersion_shouldReturnSpecificVersion() {
        byte[] jarData = "test".getBytes(StandardCharsets.UTF_8);
        repository.save(createTestMetadata("TestModule", "1.0.0"), jarData);
        repository.save(createTestMetadata("TestModule", "2.0.0"), jarData);

        Optional<ModuleMetadata> found = repository.findByNameAndVersion("TestModule", "1.0.0");

        assertThat(found).isPresent();
        assertThat(found.get().version()).isEqualTo("1.0.0");
    }

    @Test
    @DisplayName("findByNameAndVersion() should return empty for non-existent")
    void findByNameAndVersion_shouldReturnEmptyForNonExistent() {
        Optional<ModuleMetadata> found = repository.findByNameAndVersion("NonExistent", "1.0.0");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findAll() should return all modules")
    void findAll_shouldReturnAllModules() {
        byte[] jarData = "test".getBytes(StandardCharsets.UTF_8);
        repository.save(createTestMetadata("Module1", "1.0.0"), jarData);
        repository.save(createTestMetadata("Module2", "1.0.0"), jarData);
        repository.save(createTestMetadata("Module3", "1.0.0"), jarData);

        List<ModuleMetadata> all = repository.findAll();

        assertThat(all).hasSize(3);
    }

    @Test
    @DisplayName("delete() should remove module and return true")
    void delete_shouldRemoveModule() {
        byte[] jarData = "test".getBytes(StandardCharsets.UTF_8);
        repository.save(createTestMetadata("TestModule", "1.0.0"), jarData);

        boolean deleted = repository.delete("TestModule", "1.0.0");

        assertThat(deleted).isTrue();
        assertThat(repository.findByNameAndVersion("TestModule", "1.0.0")).isEmpty();
    }

    @Test
    @DisplayName("delete() should return false for non-existent module")
    void delete_shouldReturnFalseForNonExistent() {
        boolean deleted = repository.delete("NonExistent", "1.0.0");

        assertThat(deleted).isFalse();
    }

    @Test
    @DisplayName("exists() should return true for existing module")
    void exists_shouldReturnTrueForExisting() {
        byte[] jarData = "test".getBytes(StandardCharsets.UTF_8);
        repository.save(createTestMetadata("TestModule", "1.0.0"), jarData);

        assertThat(repository.exists("TestModule", "1.0.0")).isTrue();
    }

    @Test
    @DisplayName("exists() should return false for non-existent module")
    void exists_shouldReturnFalseForNonExistent() {
        assertThat(repository.exists("NonExistent", "1.0.0")).isFalse();
    }

    @Test
    @DisplayName("getJarFile() should return stored JAR data")
    void getJarFile_shouldReturnStoredJar() throws Exception {
        ModuleMetadata metadata = createTestMetadata("TestModule", "1.0.0");
        byte[] jarData = "test module content".getBytes(StandardCharsets.UTF_8);

        repository.save(metadata, jarData);

        Optional<InputStream> retrieved = repository.getJarFile("TestModule", "1.0.0");

        assertThat(retrieved).isPresent();
        byte[] retrievedData = retrieved.get().readAllBytes();
        assertThat(retrievedData).isEqualTo(jarData);
    }

    @Test
    @DisplayName("getJarFile() should return empty for non-existent module")
    void getJarFile_shouldReturnEmptyForNonExistent() {
        Optional<InputStream> retrieved = repository.getJarFile("NonExistent", "1.0.0");

        assertThat(retrieved).isEmpty();
    }

    private ModuleMetadata createTestMetadata(String name, String version) {
        return new ModuleMetadata(
                name,
                version,
                "Test module description",
                name.toLowerCase() + "-" + version + ".jar",
                1024L,
                "abc123checksum",
                Instant.now(),
                "test-user"
        );
    }

    /**
     * In-memory implementation for contract testing.
     */
    private static class InMemoryModuleRepository implements ModuleRepository {
        private final Map<String, ModuleMetadata> modules = new HashMap<>();
        private final Map<String, byte[]> jarFiles = new HashMap<>();

        private String key(String name, String version) {
            return name + ":" + version;
        }

        @Override
        public ModuleMetadata save(ModuleMetadata metadata, byte[] jarData) {
            String key = key(metadata.name(), metadata.version());
            modules.put(key, metadata);
            jarFiles.put(key, jarData);
            return metadata;
        }

        @Override
        public Optional<ModuleMetadata> findByNameAndVersion(String name, String version) {
            return Optional.ofNullable(modules.get(key(name, version)));
        }

        @Override
        public List<ModuleMetadata> findByName(String name) {
            return modules.values().stream()
                    .filter(m -> m.name().equals(name))
                    .toList();
        }

        @Override
        public List<ModuleMetadata> findAll() {
            return new ArrayList<>(modules.values());
        }

        @Override
        public Optional<InputStream> getJarFile(String name, String version) {
            byte[] data = jarFiles.get(key(name, version));
            return data != null ? Optional.of(new ByteArrayInputStream(data)) : Optional.empty();
        }

        @Override
        public boolean delete(String name, String version) {
            String key = key(name, version);
            if (modules.containsKey(key)) {
                modules.remove(key);
                jarFiles.remove(key);
                return true;
            }
            return false;
        }

        @Override
        public boolean exists(String name, String version) {
            return modules.containsKey(key(name, version));
        }
    }
}
