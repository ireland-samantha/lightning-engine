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

import ca.samanthaireland.lightning.controlplane.client.LightningNodeClient;
import ca.samanthaireland.lightning.controlplane.config.ModuleStorageConfiguration;
import ca.samanthaireland.lightning.controlplane.module.exception.ModuleDistributionException;
import ca.samanthaireland.lightning.controlplane.module.exception.ModuleNotFoundException;
import ca.samanthaireland.lightning.controlplane.module.model.ModuleMetadata;
import ca.samanthaireland.lightning.controlplane.module.repository.ModuleRepository;
import ca.samanthaireland.lightning.controlplane.node.exception.NodeNotFoundException;
import ca.samanthaireland.lightning.controlplane.node.model.Node;
import ca.samanthaireland.lightning.controlplane.node.model.NodeStatus;
import ca.samanthaireland.lightning.controlplane.node.service.NodeRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of ModuleRegistryService.
 *
 * <p>This is a pure domain implementation with no framework dependencies.
 * Dependencies are provided via constructor injection.
 */
public class ModuleRegistryServiceImpl implements ModuleRegistryService {
    private static final Logger log = LoggerFactory.getLogger(ModuleRegistryServiceImpl.class);
    private static final Duration DISTRIBUTION_TIMEOUT = Duration.ofSeconds(30);

    private final ModuleRepository moduleRepository;
    private final NodeRegistryService nodeRegistryService;
    private final ModuleStorageConfiguration config;
    private final HttpClient httpClient;

    /**
     * Creates a new ModuleRegistryServiceImpl.
     *
     * @param moduleRepository    the module repository
     * @param nodeRegistryService the node registry service
     * @param config              the module storage configuration
     */
    public ModuleRegistryServiceImpl(
            ModuleRepository moduleRepository,
            NodeRegistryService nodeRegistryService,
            ModuleStorageConfiguration config
    ) {
        this.moduleRepository = moduleRepository;
        this.nodeRegistryService = nodeRegistryService;
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(DISTRIBUTION_TIMEOUT)
                .build();
    }

    /**
     * Creates a new ModuleRegistryServiceImpl with a custom HttpClient.
     * This constructor is useful for testing.
     *
     * @param moduleRepository    the module repository
     * @param nodeRegistryService the node registry service
     * @param config              the module storage configuration
     * @param httpClient          the HTTP client to use
     */
    public ModuleRegistryServiceImpl(
            ModuleRepository moduleRepository,
            NodeRegistryService nodeRegistryService,
            ModuleStorageConfiguration config,
            HttpClient httpClient
    ) {
        this.moduleRepository = moduleRepository;
        this.nodeRegistryService = nodeRegistryService;
        this.config = config;
        this.httpClient = httpClient;
    }

    @Override
    public ModuleMetadata uploadModule(
            String name,
            String version,
            String description,
            String fileName,
            byte[] jarData,
            String uploadedBy
    ) {
        // Validate file size
        if (jarData.length > config.maxFileSize()) {
            throw new IllegalArgumentException(
                    "File size " + jarData.length + " exceeds maximum " + config.maxFileSize()
            );
        }

        // Calculate checksum
        String checksum = calculateChecksum(jarData);

        // Create metadata
        ModuleMetadata metadata = ModuleMetadata.create(
                name, version, description, fileName, jarData.length, checksum, uploadedBy
        );

        // Save to repository
        ModuleMetadata saved = moduleRepository.save(metadata, jarData);

        log.info("Uploaded module {}:{} ({} bytes, checksum: {})",
                name, version, jarData.length, checksum.substring(0, 16) + "...");

        return saved;
    }

    @Override
    public Optional<ModuleMetadata> getModule(String name, String version) {
        return moduleRepository.findByNameAndVersion(name, version);
    }

    @Override
    public List<ModuleMetadata> getModuleVersions(String name) {
        return moduleRepository.findByName(name);
    }

    @Override
    public List<ModuleMetadata> listAllModules() {
        return moduleRepository.findAll();
    }

    @Override
    public InputStream downloadModule(String name, String version) {
        return moduleRepository.getJarFile(name, version)
                .orElseThrow(() -> new ModuleNotFoundException(name, version));
    }

    @Override
    public void deleteModule(String name, String version) {
        if (!moduleRepository.exists(name, version)) {
            throw new ModuleNotFoundException(name, version);
        }

        moduleRepository.delete(name, version);
        log.info("Deleted module {}:{}", name, version);
    }

    @Override
    public void distributeToNode(String name, String version, String nodeId) {
        // Get module metadata and JAR
        ModuleMetadata metadata = moduleRepository.findByNameAndVersion(name, version)
                .orElseThrow(() -> new ModuleNotFoundException(name, version));

        byte[] jarData;
        try (InputStream is = moduleRepository.getJarFile(name, version)
                .orElseThrow(() -> new ModuleNotFoundException(name, version))) {
            jarData = is.readAllBytes();
        } catch (IOException e) {
            throw new ModuleDistributionException(name, nodeId, e);
        }

        // Get node
        Node node = nodeRegistryService.findById(nodeId)
                .orElseThrow(() -> new NodeNotFoundException(nodeId));

        // Send to node
        sendModuleToNode(node, metadata, jarData);
    }

    @Override
    public int distributeToAllNodes(String name, String version) {
        // Get module metadata and JAR
        ModuleMetadata metadata = moduleRepository.findByNameAndVersion(name, version)
                .orElseThrow(() -> new ModuleNotFoundException(name, version));

        byte[] jarData;
        try (InputStream is = moduleRepository.getJarFile(name, version)
                .orElseThrow(() -> new ModuleNotFoundException(name, version))) {
            jarData = is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read module JAR", e);
        }

        // Get all healthy nodes
        List<Node> healthyNodes = nodeRegistryService.findAll().stream()
                .filter(n -> n.status() == NodeStatus.HEALTHY)
                .toList();

        int successCount = 0;
        for (Node node : healthyNodes) {
            try {
                sendModuleToNode(node, metadata, jarData);
                successCount++;
            } catch (Exception e) {
                log.warn("Failed to distribute {}:{} to node {}: {}",
                        name, version, node.nodeId(), e.getMessage());
            }
        }

        log.info("Distributed {}:{} to {}/{} nodes", name, version, successCount, healthyNodes.size());
        return successCount;
    }

    private void sendModuleToNode(Node node, ModuleMetadata metadata, byte[] jarData) {
        String url = node.advertiseAddress() + "/api/modules";

        try {
            // Create multipart form data
            String boundary = "----ModuleBoundary" + System.currentTimeMillis();

            ByteArrayInputStream bodyStream = createMultipartBody(boundary, metadata, jarData);
            byte[] bodyBytes = bodyStream.readAllBytes();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .timeout(DISTRIBUTION_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                throw new ModuleDistributionException(
                        metadata.name(),
                        node.nodeId(),
                        "HTTP " + response.statusCode() + ": " + response.body()
                );
            }

            log.debug("Distributed {}:{} to node {}", metadata.name(), metadata.version(), node.nodeId());

        } catch (IOException | InterruptedException e) {
            throw new ModuleDistributionException(metadata.name(), node.nodeId(), e);
        }
    }

    private ByteArrayInputStream createMultipartBody(String boundary, ModuleMetadata metadata, byte[] jarData)
            throws IOException {
        var baos = new java.io.ByteArrayOutputStream();
        var writer = new java.io.PrintWriter(baos);

        // Module name
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"name\"\r\n\r\n");
        writer.append(metadata.name()).append("\r\n");

        // Module version
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"version\"\r\n\r\n");
        writer.append(metadata.version()).append("\r\n");

        // JAR file
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                .append(metadata.fileName()).append("\"\r\n");
        writer.append("Content-Type: application/java-archive\r\n\r\n");
        writer.flush();
        baos.write(jarData);
        writer.append("\r\n");

        // End boundary
        writer.append("--").append(boundary).append("--\r\n");
        writer.flush();

        return new ByteArrayInputStream(baos.toByteArray());
    }

    private String calculateChecksum(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
