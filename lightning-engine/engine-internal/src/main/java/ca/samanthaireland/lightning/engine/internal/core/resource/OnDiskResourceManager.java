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


package ca.samanthaireland.lightning.engine.internal.core.resource;

import ca.samanthaireland.lightning.engine.core.resources.Resource;
import ca.samanthaireland.lightning.engine.core.resources.ResourceManager;
import ca.samanthaireland.lightning.engine.core.resources.ResourceType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * File-system based resource manager that stores resources on disk.
 * Supports saving and retrieving resources with metadata tracking.
 */
@Slf4j
public class OnDiskResourceManager implements ResourceManager {

    private final Path resourceDirectory;
    private final ConcurrentHashMap<Long, ResourceMetadata> resourceIndex;
    private final AtomicLong nextResourceId;

    public OnDiskResourceManager(Path resourceDirectory) {
        this.resourceDirectory = resourceDirectory;
        this.resourceIndex = new ConcurrentHashMap<>();
        this.nextResourceId = new AtomicLong(1);
        initializeDirectory();
        loadExistingResources();
    }

    private void initializeDirectory() {
        try {
            if (!Files.exists(resourceDirectory)) {
                Files.createDirectories(resourceDirectory);
                log.info("Created resource directory: {}", resourceDirectory);
            }
        } catch (IOException e) {
            log.error("Failed to create resource directory: {}", resourceDirectory, e);
            throw new RuntimeException("Cannot initialize resource directory", e);
        }
    }

    private void loadExistingResources() {
        try (var files = Files.list(resourceDirectory)) {
            files.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".meta"))
                 .forEach(this::loadMetadata);
        } catch (IOException e) {
            log.warn("Failed to load existing resources from {}", resourceDirectory, e);
        }
    }

    private void loadMetadata(Path metaPath) {
        try {
            var lines = Files.readAllLines(metaPath);
            if (lines.size() >= 3) {
                long id = Long.parseLong(lines.get(0));
                String name = lines.get(1);
                ResourceType type = ResourceType.valueOf(lines.get(2));
                String dataFileName = metaPath.getFileName().toString().replace(".meta", ".dat");
                Path dataPath = resourceDirectory.resolve(dataFileName);

                if (Files.exists(dataPath)) {
                    resourceIndex.put(id, new ResourceMetadata(id, name, dataPath.toString(), type));
                    nextResourceId.updateAndGet(current -> Math.max(current, id + 1));
                    log.debug("Loaded resource metadata: id={}, name={}", id, name);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load metadata from {}", metaPath, e);
        }
    }

    @Override
    public long saveResource(Resource resource) {
        long resourceId = resource.resourceId() > 0 ? resource.resourceId() : nextResourceId.getAndIncrement();
        String fileName = "resource_" + resourceId;
        Path dataPath = resourceDirectory.resolve(fileName + ".dat");
        Path metaPath = resourceDirectory.resolve(fileName + ".meta");

        try {
            // Write binary data
            Files.write(dataPath, resource.blob(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

            // Write metadata
            String metadata = String.join("\n",
                String.valueOf(resourceId),
                resource.resourceName(),
                resource.resourceType().name()
            );
            Files.writeString(metaPath, metadata,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

            resourceIndex.put(resourceId, new ResourceMetadata(
                resourceId,
                resource.resourceName(),
                dataPath.toString(),
                resource.resourceType()
            ));

            log.info("Saved resource: id={}, name={}, size={} bytes",
                resourceId, resource.resourceName(), resource.blob().length);

            return resourceId;
        } catch (IOException e) {
            log.error("Failed to save resource: id={}", resourceId, e);
            throw new RuntimeException("Failed to save resource", e);
        }
    }

    @Override
    public Stream<Resource> requestResource() {
        return resourceIndex.values().stream()
            .map(this::loadResource)
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    /**
     * Get a specific resource by ID.
     */
    public Optional<Resource> getResource(long resourceId) {
        ResourceMetadata metadata = resourceIndex.get(resourceId);
        if (metadata == null) {
            return Optional.empty();
        }
        return loadResource(metadata);
    }

    /**
     * Get resource data as a chunked stream for large files.
     */
    public Optional<ChunkedResource> getResourceChunked(long resourceId, int chunkSize) {
        ResourceMetadata metadata = resourceIndex.get(resourceId);
        if (metadata == null) {
            return Optional.empty();
        }

        Path dataPath = Path.of(metadata.path());
        try {
            long fileSize = Files.size(dataPath);
            return Optional.of(new ChunkedResource(
                metadata.id(),
                metadata.name(),
                metadata.type(),
                fileSize,
                chunkSize,
                dataPath
            ));
        } catch (IOException e) {
            log.error("Failed to get chunked resource: id={}", resourceId, e);
            return Optional.empty();
        }
    }

    /**
     * Delete a resource by ID.
     */
    public boolean deleteResource(long resourceId) {
        ResourceMetadata metadata = resourceIndex.remove(resourceId);
        if (metadata == null) {
            return false;
        }

        try {
            Path dataPath = Path.of(metadata.path());
            Path metaPath = Path.of(metadata.path().replace(".dat", ".meta"));
            Files.deleteIfExists(dataPath);
            Files.deleteIfExists(metaPath);
            log.info("Deleted resource: id={}", resourceId);
            return true;
        } catch (IOException e) {
            log.error("Failed to delete resource files: id={}", resourceId, e);
            return false;
        }
    }

    /**
     * Check if a resource exists.
     */
    public boolean hasResource(long resourceId) {
        return resourceIndex.containsKey(resourceId);
    }

    /**
     * Get total number of resources.
     */
    public int getResourceCount() {
        return resourceIndex.size();
    }

    private Optional<Resource> loadResource(ResourceMetadata metadata) {
        try {
            byte[] data = Files.readAllBytes(Path.of(metadata.path()));
            return Optional.of(new StoredResource(
                metadata.id(),
                metadata.name(),
                metadata.path(),
                data,
                metadata.type()
            ));
        } catch (IOException e) {
            log.error("Failed to load resource data: id={}", metadata.id(), e);
            return Optional.empty();
        }
    }

    /**
     * Internal metadata record for tracking resources.
     */
    private record ResourceMetadata(long id, String name, String path, ResourceType type) {}

    /**
     * Implementation of Resource interface for stored resources.
     */
    public record StoredResource(
        long resourceId,
        String resourceName,
        String resourcePath,
        byte[] blob,
        ResourceType resourceType
    ) implements Resource {}

    /**
     * Chunked resource for streaming large files.
     */
    public record ChunkedResource(
        long resourceId,
        String resourceName,
        ResourceType resourceType,
        long totalSize,
        int chunkSize,
        Path dataPath
    ) {
        public int getTotalChunks() {
            return (int) Math.ceil((double) totalSize / chunkSize);
        }

        public byte[] readChunk(int chunkIndex) throws IOException {
            long offset = (long) chunkIndex * chunkSize;
            int bytesToRead = (int) Math.min(chunkSize, totalSize - offset);

            if (offset >= totalSize || bytesToRead <= 0) {
                return new byte[0];
            }

            try (var channel = Files.newByteChannel(dataPath)) {
                channel.position(offset);
                byte[] buffer = new byte[bytesToRead];
                var byteBuffer = java.nio.ByteBuffer.wrap(buffer);
                channel.read(byteBuffer);
                return buffer;
            }
        }
    }
}
