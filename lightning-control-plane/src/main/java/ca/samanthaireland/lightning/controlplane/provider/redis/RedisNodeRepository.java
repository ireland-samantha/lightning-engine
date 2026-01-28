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

package ca.samanthaireland.lightning.controlplane.provider.redis;

import ca.samanthaireland.lightning.controlplane.node.model.Node;
import ca.samanthaireland.lightning.controlplane.node.model.NodeCapacity;
import ca.samanthaireland.lightning.controlplane.node.model.NodeMetrics;
import ca.samanthaireland.lightning.controlplane.node.model.NodeStatus;
import ca.samanthaireland.lightning.controlplane.node.repository.NodeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Redis-backed implementation of NodeRepository.
 *
 * <p>Uses Redis keys with TTL for automatic node expiration when heartbeats stop.
 * Key structure:
 * <ul>
 *   <li>{@code nodes:{nodeId}} - JSON node object with TTL</li>
 * </ul>
 */
@ApplicationScoped
public class RedisNodeRepository implements NodeRepository {
    private static final Logger log = LoggerFactory.getLogger(RedisNodeRepository.class);
    private static final String NODE_KEY_PREFIX = "nodes:";

    private final ValueCommands<String, String> valueCommands;
    private final KeyCommands<String> keyCommands;
    private final ObjectMapper objectMapper;

    @Inject
    public RedisNodeRepository(RedisDataSource redisDataSource) {
        this.valueCommands = redisDataSource.value(String.class);
        this.keyCommands = redisDataSource.key();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Node save(Node node, int ttlSeconds) {
        String key = nodeKey(node.nodeId());
        try {
            String json = objectMapper.writeValueAsString(toMap(node));
            valueCommands.set(key, json, new SetArgs().ex(ttlSeconds));
            log.debug("Saved node {} with TTL {}s", node.nodeId(), ttlSeconds);
            return node;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize node: " + node.nodeId(), e);
        }
    }

    @Override
    public Optional<Node> findById(String nodeId) {
        String key = nodeKey(nodeId);
        String json = valueCommands.get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(fromJson(json));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize node {}: {}", nodeId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<Node> findAll() {
        List<String> keys = keyCommands.keys(NODE_KEY_PREFIX + "*");
        List<Node> nodes = new ArrayList<>();

        for (String key : keys) {
            String json = valueCommands.get(key);
            if (json != null) {
                try {
                    nodes.add(fromJson(json));
                } catch (JsonProcessingException e) {
                    log.error("Failed to deserialize node from key {}: {}", key, e.getMessage());
                }
            }
        }

        return nodes;
    }

    @Override
    public void deleteById(String nodeId) {
        String key = nodeKey(nodeId);
        keyCommands.del(key);
        log.debug("Deleted node {}", nodeId);
    }

    @Override
    public boolean existsById(String nodeId) {
        String key = nodeKey(nodeId);
        return keyCommands.exists(key);
    }

    private String nodeKey(String nodeId) {
        return NODE_KEY_PREFIX + nodeId;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Node node) {
        return Map.of(
                "nodeId", node.nodeId(),
                "advertiseAddress", node.advertiseAddress(),
                "status", node.status().name(),
                "capacity", Map.of(
                        "maxContainers", node.capacity().maxContainers()
                ),
                "metrics", Map.of(
                        "containerCount", node.metrics().containerCount(),
                        "matchCount", node.metrics().matchCount(),
                        "cpuUsage", node.metrics().cpuUsage(),
                        "memoryUsedMb", node.metrics().memoryUsedMb(),
                        "memoryMaxMb", node.metrics().memoryMaxMb()
                ),
                "registeredAt", node.registeredAt().toString(),
                "lastHeartbeat", node.lastHeartbeat().toString()
        );
    }

    @SuppressWarnings("unchecked")
    private Node fromJson(String json) throws JsonProcessingException {
        Map<String, Object> map = objectMapper.readValue(json, Map.class);

        Map<String, Object> capacityMap = (Map<String, Object>) map.get("capacity");
        NodeCapacity capacity = new NodeCapacity(((Number) capacityMap.get("maxContainers")).intValue());

        Map<String, Object> metricsMap = (Map<String, Object>) map.get("metrics");
        NodeMetrics metrics = new NodeMetrics(
                ((Number) metricsMap.get("containerCount")).intValue(),
                ((Number) metricsMap.get("matchCount")).intValue(),
                ((Number) metricsMap.get("cpuUsage")).doubleValue(),
                ((Number) metricsMap.get("memoryUsedMb")).longValue(),
                ((Number) metricsMap.get("memoryMaxMb")).longValue()
        );

        return new Node(
                (String) map.get("nodeId"),
                (String) map.get("advertiseAddress"),
                NodeStatus.valueOf((String) map.get("status")),
                capacity,
                metrics,
                Instant.parse((String) map.get("registeredAt")),
                Instant.parse((String) map.get("lastHeartbeat"))
        );
    }
}
