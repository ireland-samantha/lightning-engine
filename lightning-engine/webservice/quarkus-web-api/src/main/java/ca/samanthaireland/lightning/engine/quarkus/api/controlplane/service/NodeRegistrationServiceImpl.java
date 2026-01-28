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

package ca.samanthaireland.lightning.engine.quarkus.api.controlplane.service;

import ca.samanthaireland.lightning.engine.core.container.ContainerManager;
import ca.samanthaireland.lightning.engine.quarkus.api.controlplane.config.ControlPlaneClientConfig;
import ca.samanthaireland.lightning.engine.quarkus.api.controlplane.dto.HeartbeatRequest;
import ca.samanthaireland.lightning.engine.quarkus.api.controlplane.dto.NodeCapacityDto;
import ca.samanthaireland.lightning.engine.quarkus.api.controlplane.dto.NodeMetricsDto;
import ca.samanthaireland.lightning.engine.quarkus.api.controlplane.dto.NodeRegistrationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of NodeRegistrationService using HTTP client to communicate with control plane.
 */
@ApplicationScoped
public class NodeRegistrationServiceImpl implements NodeRegistrationService {
    private static final Logger log = LoggerFactory.getLogger(NodeRegistrationServiceImpl.class);
    private static final String AUTH_HEADER = "X-Control-Plane-Token";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final ControlPlaneClientConfig config;
    private final ContainerManager containerManager;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String nodeId;
    private final AtomicBoolean registered = new AtomicBoolean(false);

    @Inject
    public NodeRegistrationServiceImpl(
            ControlPlaneClientConfig config,
            ContainerManager containerManager
    ) {
        this.config = config;
        this.containerManager = containerManager;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
        this.nodeId = config.nodeId().orElseGet(() -> UUID.randomUUID().toString());
    }

    @Override
    public void register() {
        if (!config.isEnabled()) {
            log.debug("Control plane integration disabled");
            return;
        }

        String advertiseAddress = config.advertiseAddress()
                .orElseThrow(() -> new IllegalStateException(
                        "control-plane.advertise-address must be set when control-plane.url is configured"));

        NodeRegistrationRequest request = new NodeRegistrationRequest(
                nodeId,
                advertiseAddress,
                new NodeCapacityDto(config.maxContainers())
        );

        try {
            String url = config.url().get() + "/api/nodes/register";
            String body = objectMapper.writeValueAsString(request);

            HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(REQUEST_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(body));

            config.token().ifPresent(token -> httpRequestBuilder.header(AUTH_HEADER, token));

            HttpResponse<String> response = httpClient.send(
                    httpRequestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 201 || response.statusCode() == 200) {
                registered.set(true);
                log.info("Registered with control plane: nodeId={}, address={}", nodeId, advertiseAddress);
            } else {
                log.error("Failed to register with control plane: status={}, body={}",
                        response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to register with control plane: {}", e.getMessage());
        }
    }

    @Override
    public void heartbeat() {
        if (!config.isEnabled() || !registered.get()) {
            return;
        }

        HeartbeatRequest request = new HeartbeatRequest(getCurrentMetrics());

        try {
            String url = config.url().get() + "/api/nodes/" + nodeId + "/heartbeat";
            String body = objectMapper.writeValueAsString(request);

            HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(REQUEST_TIMEOUT)
                    .PUT(HttpRequest.BodyPublishers.ofString(body));

            config.token().ifPresent(token -> httpRequestBuilder.header(AUTH_HEADER, token));

            HttpResponse<String> response = httpClient.send(
                    httpRequestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {
                log.debug("Heartbeat sent to control plane: nodeId={}", nodeId);
            } else if (response.statusCode() == 404) {
                // Node was removed (TTL expired), re-register
                log.warn("Node not found in control plane, re-registering");
                registered.set(false);
                register();
            } else {
                log.warn("Heartbeat failed: status={}, body={}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("Failed to send heartbeat: {}", e.getMessage());
        }
    }

    @Override
    public void deregister() {
        if (!config.isEnabled() || !registered.get()) {
            return;
        }

        try {
            String url = config.url().get() + "/api/nodes/" + nodeId;

            HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .DELETE();

            config.token().ifPresent(token -> httpRequestBuilder.header(AUTH_HEADER, token));

            HttpResponse<String> response = httpClient.send(
                    httpRequestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 204 || response.statusCode() == 200) {
                registered.set(false);
                log.info("Deregistered from control plane: nodeId={}", nodeId);
            } else {
                log.warn("Deregister returned unexpected status: {}", response.statusCode());
            }
        } catch (Exception e) {
            log.warn("Failed to deregister from control plane: {}", e.getMessage());
        }
    }

    @Override
    public NodeMetricsDto getCurrentMetrics() {
        int containerCount = containerManager.getAllContainers().size();
        int matchCount = containerManager.getAllContainers().stream()
                .mapToInt(c -> c.matches().all().size())
                .sum();

        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);

        double cpuUsage = getCpuUsage();

        return new NodeMetricsDto(
                containerCount,
                matchCount,
                cpuUsage,
                usedMemory,
                maxMemory
        );
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public boolean isRegistered() {
        return registered.get();
    }

    private double getCpuUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double load = osBean.getSystemLoadAverage();
            int processors = osBean.getAvailableProcessors();
            if (load >= 0 && processors > 0) {
                return Math.min(1.0, load / processors);
            }
        } catch (Exception e) {
            log.debug("Failed to get CPU usage: {}", e.getMessage());
        }
        return 0.0;
    }
}
