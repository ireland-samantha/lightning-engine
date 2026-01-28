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

package ca.samanthaireland.lightning.controlplane.provider.filesystem;

import ca.samanthaireland.lightning.controlplane.provider.config.QuarkusModuleStorageConfig;
import ca.samanthaireland.lightning.controlplane.module.model.ModuleMetadata;
import ca.samanthaireland.lightning.controlplane.module.repository.ModuleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Filesystem-based implementation of ModuleRepository.
 * Stores modules in a directory structure: {baseDir}/{moduleName}/{version}/
 */
@ApplicationScoped
@Startup
public class FileSystemModuleRepository implements ModuleRepository {
    private static final Logger log = LoggerFactory.getLogger(FileSystemModuleRepository.class);

    private static final String METADATA_FILE = "metadata.json";
    private static final String JAR_FILE = "module.jar";

    private final Path baseDirectory;
    private final ObjectMapper objectMapper;

    @Inject
    public FileSystemModuleRepository(QuarkusModuleStorageConfig config) {
        this.baseDirectory = Path.of(config.directory());
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        initializeStorage();
    }

    private void initializeStorage() {
        try {
            Files.createDirectories(baseDirectory);
            log.info("Module storage initialized at: {}", baseDirectory.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize module storage directory", e);
        }
    }

    @Override
    public ModuleMetadata save(ModuleMetadata metadata, byte[] jarData) {
        Path moduleDir = getModuleVersionDir(metadata.name(), metadata.version());

        try {
            Files.createDirectories(moduleDir);

            // Write metadata
            Path metadataPath = moduleDir.resolve(METADATA_FILE);
            objectMapper.writeValue(metadataPath.toFile(), metadata);

            // Write JAR file
            Path jarPath = moduleDir.resolve(JAR_FILE);
            Files.write(jarPath, jarData);

            log.info("Saved module {}:{} ({} bytes)", metadata.name(), metadata.version(), jarData.length);
            return metadata;

        } catch (IOException e) {
            log.error("Failed to save module {}:{}: {}", metadata.name(), metadata.version(), e.getMessage());
            throw new RuntimeException("Failed to save module", e);
        }
    }

    @Override
    public Optional<ModuleMetadata> findByNameAndVersion(String name, String version) {
        Path metadataPath = getModuleVersionDir(name, version).resolve(METADATA_FILE);

        if (!Files.exists(metadataPath)) {
            return Optional.empty();
        }

        try {
            ModuleMetadata metadata = objectMapper.readValue(metadataPath.toFile(), ModuleMetadata.class);
            return Optional.of(metadata);
        } catch (IOException e) {
            log.error("Failed to read metadata for {}:{}: {}", name, version, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<ModuleMetadata> findByName(String name) {
        Path moduleDir = baseDirectory.resolve(sanitizeName(name));

        if (!Files.exists(moduleDir)) {
            return List.of();
        }

        List<ModuleMetadata> versions = new ArrayList<>();
        try (Stream<Path> stream = Files.list(moduleDir)) {
            stream.filter(Files::isDirectory)
                    .forEach(versionDir -> {
                        Path metadataPath = versionDir.resolve(METADATA_FILE);
                        if (Files.exists(metadataPath)) {
                            try {
                                ModuleMetadata metadata = objectMapper.readValue(metadataPath.toFile(), ModuleMetadata.class);
                                versions.add(metadata);
                            } catch (IOException e) {
                                log.warn("Failed to read metadata from {}: {}", metadataPath, e.getMessage());
                            }
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to list versions for module {}: {}", name, e.getMessage());
        }

        return versions;
    }

    @Override
    public List<ModuleMetadata> findAll() {
        List<ModuleMetadata> all = new ArrayList<>();

        try (Stream<Path> stream = Files.list(baseDirectory)) {
            stream.filter(Files::isDirectory)
                    .forEach(moduleDir -> {
                        String moduleName = moduleDir.getFileName().toString();
                        all.addAll(findByName(moduleName));
                    });
        } catch (IOException e) {
            log.error("Failed to list all modules: {}", e.getMessage());
        }

        return all;
    }

    @Override
    public Optional<InputStream> getJarFile(String name, String version) {
        Path jarPath = getModuleVersionDir(name, version).resolve(JAR_FILE);

        if (!Files.exists(jarPath)) {
            return Optional.empty();
        }

        try {
            return Optional.of(Files.newInputStream(jarPath));
        } catch (IOException e) {
            log.error("Failed to read JAR for {}:{}: {}", name, version, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(String name, String version) {
        Path moduleVersionDir = getModuleVersionDir(name, version);

        if (!Files.exists(moduleVersionDir)) {
            return false;
        }

        try {
            // Delete all files in the version directory
            try (Stream<Path> stream = Files.walk(moduleVersionDir)) {
                stream.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("Failed to delete {}: {}", path, e.getMessage());
                            }
                        });
            }

            // Try to delete parent module directory if empty
            Path moduleDir = baseDirectory.resolve(sanitizeName(name));
            try (Stream<Path> stream = Files.list(moduleDir)) {
                if (stream.findAny().isEmpty()) {
                    Files.delete(moduleDir);
                }
            }

            log.info("Deleted module {}:{}", name, version);
            return true;

        } catch (IOException e) {
            log.error("Failed to delete module {}:{}: {}", name, version, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(String name, String version) {
        Path metadataPath = getModuleVersionDir(name, version).resolve(METADATA_FILE);
        return Files.exists(metadataPath);
    }

    private Path getModuleVersionDir(String name, String version) {
        return baseDirectory
                .resolve(sanitizeName(name))
                .resolve(sanitizeName(version));
    }

    /**
     * Sanitizes a name for use in filesystem paths.
     */
    private String sanitizeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
