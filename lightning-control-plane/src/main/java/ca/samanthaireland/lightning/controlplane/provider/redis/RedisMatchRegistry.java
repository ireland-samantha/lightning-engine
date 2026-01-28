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

import ca.samanthaireland.lightning.controlplane.match.model.MatchRegistryEntry;
import ca.samanthaireland.lightning.controlplane.match.model.MatchStatus;
import ca.samanthaireland.lightning.controlplane.match.repository.MatchRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
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
 * Redis-backed implementation of MatchRegistry.
 *
 * <p>Key structure:
 * <ul>
 *   <li>{@code matches:{matchId}} - JSON match entry</li>
 *   <li>{@code matches:node:{nodeId}:{matchId}} - Index for finding matches by node</li>
 * </ul>
 */
@ApplicationScoped
public class RedisMatchRegistry implements MatchRegistry {
    private static final Logger log = LoggerFactory.getLogger(RedisMatchRegistry.class);
    private static final String MATCH_KEY_PREFIX = "matches:";
    private static final String NODE_INDEX_PREFIX = "matches:node:";

    private final ValueCommands<String, String> valueCommands;
    private final KeyCommands<String> keyCommands;
    private final ObjectMapper objectMapper;

    @Inject
    public RedisMatchRegistry(RedisDataSource redisDataSource) {
        this.valueCommands = redisDataSource.value(String.class);
        this.keyCommands = redisDataSource.key();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public MatchRegistryEntry save(MatchRegistryEntry entry) {
        String key = matchKey(entry.matchId());
        String nodeIndexKey = nodeIndexKey(entry.nodeId(), entry.matchId());

        try {
            String json = objectMapper.writeValueAsString(toMap(entry));
            valueCommands.set(key, json);
            // Also store a reference in the node index
            valueCommands.set(nodeIndexKey, entry.matchId());
            log.debug("Saved match {} on node {}", entry.matchId(), entry.nodeId());
            return entry;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize match: " + entry.matchId(), e);
        }
    }

    @Override
    public Optional<MatchRegistryEntry> findById(String matchId) {
        String key = matchKey(matchId);
        String json = valueCommands.get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(fromJson(json));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize match {}: {}", matchId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<MatchRegistryEntry> findAll() {
        List<String> keys = keyCommands.keys(MATCH_KEY_PREFIX + "*");
        List<MatchRegistryEntry> entries = new ArrayList<>();

        for (String key : keys) {
            // Skip node index keys
            if (key.contains(":node:")) {
                continue;
            }
            String json = valueCommands.get(key);
            if (json != null) {
                try {
                    entries.add(fromJson(json));
                } catch (JsonProcessingException e) {
                    log.error("Failed to deserialize match from key {}: {}", key, e.getMessage());
                }
            }
        }

        return entries;
    }

    @Override
    public List<MatchRegistryEntry> findByNodeId(String nodeId) {
        List<String> nodeIndexKeys = keyCommands.keys(NODE_INDEX_PREFIX + nodeId + ":*");
        List<MatchRegistryEntry> entries = new ArrayList<>();

        for (String indexKey : nodeIndexKeys) {
            String matchId = valueCommands.get(indexKey);
            if (matchId != null) {
                findById(matchId).ifPresent(entries::add);
            }
        }

        return entries;
    }

    @Override
    public List<MatchRegistryEntry> findByStatus(MatchStatus status) {
        return findAll().stream()
                .filter(entry -> entry.status() == status)
                .toList();
    }

    @Override
    public void deleteById(String matchId) {
        Optional<MatchRegistryEntry> entry = findById(matchId);
        String key = matchKey(matchId);
        keyCommands.del(key);

        // Also remove from node index
        entry.ifPresent(e -> {
            String nodeIndexKey = nodeIndexKey(e.nodeId(), matchId);
            keyCommands.del(nodeIndexKey);
        });

        log.debug("Deleted match {}", matchId);
    }

    @Override
    public void deleteByNodeId(String nodeId) {
        List<MatchRegistryEntry> matches = findByNodeId(nodeId);
        for (MatchRegistryEntry match : matches) {
            deleteById(match.matchId());
        }
        log.info("Deleted {} matches for node {}", matches.size(), nodeId);
    }

    @Override
    public boolean existsById(String matchId) {
        String key = matchKey(matchId);
        return keyCommands.exists(key);
    }

    @Override
    public long countActive() {
        return findAll().stream()
                .filter(e -> e.status() == MatchStatus.CREATING || e.status() == MatchStatus.RUNNING)
                .count();
    }

    @Override
    public long countActiveByNodeId(String nodeId) {
        return findByNodeId(nodeId).stream()
                .filter(e -> e.status() == MatchStatus.CREATING || e.status() == MatchStatus.RUNNING)
                .count();
    }

    private String matchKey(String matchId) {
        return MATCH_KEY_PREFIX + matchId;
    }

    private String nodeIndexKey(String nodeId, String matchId) {
        return NODE_INDEX_PREFIX + nodeId + ":" + matchId;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(MatchRegistryEntry entry) {
        return Map.of(
                "matchId", entry.matchId(),
                "nodeId", entry.nodeId(),
                "containerId", entry.containerId(),
                "status", entry.status().name(),
                "createdAt", entry.createdAt().toString(),
                "moduleNames", entry.moduleNames(),
                "advertiseAddress", entry.advertiseAddress(),
                "websocketUrl", entry.websocketUrl(),
                "playerCount", entry.playerCount()
        );
    }

    @SuppressWarnings("unchecked")
    private MatchRegistryEntry fromJson(String json) throws JsonProcessingException {
        Map<String, Object> map = objectMapper.readValue(json, Map.class);

        List<String> moduleNames = (List<String>) map.get("moduleNames");

        return new MatchRegistryEntry(
                (String) map.get("matchId"),
                (String) map.get("nodeId"),
                ((Number) map.get("containerId")).longValue(),
                MatchStatus.valueOf((String) map.get("status")),
                Instant.parse((String) map.get("createdAt")),
                moduleNames,
                (String) map.get("advertiseAddress"),
                (String) map.get("websocketUrl"),
                ((Number) map.get("playerCount")).intValue()
        );
    }
}
