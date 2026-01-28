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

package ca.samanthaireland.lightning.engine.api.resource.adapter;

import ca.samanthaireland.lightning.engine.api.resource.adapter.dto.SnapshotDataDto;
import ca.samanthaireland.lightning.engine.api.resource.adapter.dto.SnapshotDataDto.ModuleDataDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for ECS snapshot data using Jackson for JSON processing.
 *
 * <p>This class is responsible for converting JSON snapshot data into
 * a structured map of module -> component -> values. The returned data
 * can be wrapped in domain objects by the caller.
 *
 * <p>This class follows the Single Responsibility Principle - it only
 * handles JSON parsing, not domain logic.
 */
public class SnapshotParser {

    private static final TypeReference<Map<String, Map<String, List<Number>>>> RAW_SNAPSHOT_TYPE =
            new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public SnapshotParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse a snapshot JSON string into a structured map.
     *
     * @param jsonData the JSON string containing snapshot data
     * @return a map of module name -> component name -> values
     * @throws SnapshotParseException if JSON parsing fails
     */
    public Map<String, Map<String, List<Float>>> parse(String jsonData) {
        if (jsonData == null || jsonData.isBlank()) {
            return Map.of();
        }

        try {
            Map<String, Map<String, List<Number>>> rawData = objectMapper.readValue(jsonData, RAW_SNAPSHOT_TYPE);
            SnapshotDataDto dto = convertToDto(rawData);
            return convertToFloatMap(dto);
        } catch (JsonProcessingException e) {
            throw new SnapshotParseException("Failed to parse snapshot JSON", e);
        }
    }

    /**
     * Parse snapshot data from a pre-parsed Map.
     * Useful when working with history snapshots that return parsed data.
     *
     * @param dataMap the map containing snapshot data (module -> component -> values)
     * @return a map of module name -> component name -> float values
     */
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, List<Float>>> parseFromMap(Map<String, Object> dataMap) {
        if (dataMap == null || dataMap.isEmpty()) {
            return Map.of();
        }

        Map<String, Map<String, List<Float>>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> moduleEntry : dataMap.entrySet()) {
            if (moduleEntry.getValue() instanceof Map<?, ?> moduleMap) {
                Map<String, List<Float>> components = convertModuleComponents((Map<String, Object>) moduleMap);
                result.put(moduleEntry.getKey(), components);
            }
        }

        return result;
    }

    private SnapshotDataDto convertToDto(Map<String, Map<String, List<Number>>> rawData) {
        Map<String, ModuleDataDto> modules = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, List<Number>>> entry : rawData.entrySet()) {
            modules.put(entry.getKey(), new ModuleDataDto(entry.getValue()));
        }
        return new SnapshotDataDto(modules);
    }

    private Map<String, Map<String, List<Float>>> convertToFloatMap(SnapshotDataDto dto) {
        Map<String, Map<String, List<Float>>> data = new LinkedHashMap<>();

        for (Map.Entry<String, ModuleDataDto> moduleEntry : dto.modules().entrySet()) {
            Map<String, List<Float>> components = new LinkedHashMap<>();

            for (Map.Entry<String, List<Number>> compEntry : moduleEntry.getValue().components().entrySet()) {
                List<Float> floatValues = compEntry.getValue().stream()
                        .map(Number::floatValue)
                        .toList();
                components.put(compEntry.getKey(), floatValues);
            }
            data.put(moduleEntry.getKey(), components);
        }

        return data;
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<Float>> convertModuleComponents(Map<String, Object> moduleData) {
        Map<String, List<Float>> components = new LinkedHashMap<>();

        for (Map.Entry<String, Object> compEntry : moduleData.entrySet()) {
            if (compEntry.getValue() instanceof List<?> valueList) {
                List<Float> floatValues = valueList.stream()
                        .filter(Number.class::isInstance)
                        .map(val -> ((Number) val).floatValue())
                        .toList();
                components.put(compEntry.getKey(), floatValues);
            }
        }

        return components;
    }

    /**
     * Exception thrown when snapshot parsing fails.
     */
    public static class SnapshotParseException extends RuntimeException {
        public SnapshotParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
