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

package ca.samanthaireland.lightning.controlplane.provider.client;

import ca.samanthaireland.lightning.controlplane.client.LightningNodeClient;
import ca.samanthaireland.lightning.controlplane.node.model.Node;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * HTTP client implementation for communicating with Lightning Engine nodes.
 */
@ApplicationScoped
public class LightningNodeClientImpl implements LightningNodeClient {
    private static final Logger log = LoggerFactory.getLogger(LightningNodeClientImpl.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LightningNodeClientImpl() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public long createContainer(Node node, List<String> moduleNames) {
        String url = node.advertiseAddress() + "/api/containers";

        try {
            Map<String, Object> requestBody = Map.of(
                    "name", "cluster-container-" + System.currentTimeMillis(),
                    "moduleNames", moduleNames,
                    "aiNames", List.of()
            );

            String body = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(REQUEST_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201 && response.statusCode() != 200) {
                throw new RuntimeException("Failed to create container: " + response.statusCode() + " - " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            long containerId = json.get("id").asLong();

            log.info("Created container {} on node {}", containerId, node.nodeId());
            return containerId;

        } catch (Exception e) {
            log.error("Failed to create container on node {}: {}", node.nodeId(), e.getMessage());
            throw new RuntimeException("Failed to create container on node " + node.nodeId(), e);
        }
    }

    @Override
    public long createMatch(Node node, long containerId, List<String> moduleNames) {
        String url = node.advertiseAddress() + "/api/containers/" + containerId + "/matches";

        try {
            Map<String, Object> requestBody = Map.of(
                    "enabledModuleNames", moduleNames
            );

            String body = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(REQUEST_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201 && response.statusCode() != 200) {
                throw new RuntimeException("Failed to create match: " + response.statusCode() + " - " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            long matchId = json.get("id").asLong();

            log.info("Created match {} in container {} on node {}", matchId, containerId, node.nodeId());
            return matchId;

        } catch (Exception e) {
            log.error("Failed to create match on node {}: {}", node.nodeId(), e.getMessage());
            throw new RuntimeException("Failed to create match on node " + node.nodeId(), e);
        }
    }

    @Override
    public void deleteMatch(Node node, long containerId, long matchId) {
        String url = node.advertiseAddress() + "/api/containers/" + containerId + "/matches/" + matchId;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 204 && response.statusCode() != 200) {
                throw new RuntimeException("Failed to delete match: " + response.statusCode() + " - " + response.body());
            }

            log.info("Deleted match {} from container {} on node {}", matchId, containerId, node.nodeId());

        } catch (Exception e) {
            log.error("Failed to delete match {} from node {}: {}", matchId, node.nodeId(), e.getMessage());
            throw new RuntimeException("Failed to delete match from node " + node.nodeId(), e);
        }
    }

    @Override
    public void deleteContainer(Node node, long containerId) {
        String url = node.advertiseAddress() + "/api/containers/" + containerId;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 204 && response.statusCode() != 200) {
                throw new RuntimeException("Failed to delete container: " + response.statusCode() + " - " + response.body());
            }

            log.info("Deleted container {} from node {}", containerId, node.nodeId());

        } catch (Exception e) {
            log.error("Failed to delete container {} from node {}: {}", containerId, node.nodeId(), e.getMessage());
            throw new RuntimeException("Failed to delete container from node " + node.nodeId(), e);
        }
    }

    @Override
    public boolean isReachable(Node node) {
        String url = node.advertiseAddress() + "/api/health";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;

        } catch (Exception e) {
            log.debug("Node {} not reachable: {}", node.nodeId(), e.getMessage());
            return false;
        }
    }
}
