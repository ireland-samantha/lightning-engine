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


package ca.samanthaireland.lightning.engine.api.resource.adapter.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Thread-safe JSON mapper utility wrapping Jackson's ObjectMapper.
 *
 * <p>Provides convenient methods for JSON serialization/deserialization
 * with sensible defaults for API communication.
 */
public final class JsonMapper {

    private static final ObjectMapper MAPPER = createMapper();

    private JsonMapper() {
        // Utility class
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Ignore unknown properties for forward compatibility
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Don't fail on empty beans
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return mapper;
    }

    /**
     * Serializes an object to JSON string.
     *
     * @param value the object to serialize
     * @return JSON string representation
     * @throws JsonSerializationException if serialization fails
     */
    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new JsonSerializationException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * Deserializes a JSON string to an object of the specified type.
     *
     * @param json the JSON string
     * @param type the target class
     * @param <T> the target type
     * @return the deserialized object
     * @throws JsonSerializationException if deserialization fails
     */
    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new JsonSerializationException("Failed to deserialize JSON to " + type.getSimpleName(), e);
        }
    }

    /**
     * Deserializes a JSON string to a generic type using TypeReference.
     *
     * @param json the JSON string
     * @param typeReference the type reference for generic types
     * @param <T> the target type
     * @return the deserialized object
     * @throws JsonSerializationException if deserialization fails
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new JsonSerializationException("Failed to deserialize JSON", e);
        }
    }

    /**
     * Deserializes a JSON string to a list of the specified element type.
     *
     * @param json the JSON string
     * @param elementType the element class
     * @param <T> the element type
     * @return the deserialized list
     * @throws JsonSerializationException if deserialization fails
     */
    public static <T> List<T> fromJsonList(String json, Class<T> elementType) {
        try {
            return MAPPER.readValue(json,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (JsonProcessingException e) {
            throw new JsonSerializationException("Failed to deserialize JSON array", e);
        }
    }

    /**
     * Deserializes a JSON string to a Map.
     *
     * @param json the JSON string
     * @return the deserialized map
     * @throws JsonSerializationException if deserialization fails
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> fromJsonMap(String json) {
        try {
            return MAPPER.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new JsonSerializationException("Failed to deserialize JSON to Map", e);
        }
    }

    /**
     * Gets the underlying ObjectMapper for advanced usage.
     *
     * @return the ObjectMapper instance
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    /**
     * Extract a string value from JSON by field name.
     *
     * @param json the JSON string
     * @param fieldName the field name to extract
     * @return the string value, or null if not found
     */
    public static String extractString(String json, String fieldName) {
        Map<String, Object> map = fromJsonMap(json);
        Object value = map.get(fieldName);
        return value != null ? value.toString() : null;
    }

    /**
     * Extract a long value from JSON by field name.
     *
     * @param json the JSON string
     * @param fieldName the field name to extract
     * @return the long value, or 0 if not found
     */
    public static long extractLong(String json, String fieldName) {
        Map<String, Object> map = fromJsonMap(json);
        Object value = map.get(fieldName);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0;
    }

    /**
     * Extract a boolean value from JSON by field name.
     *
     * @param json the JSON string
     * @param fieldName the field name to extract
     * @return the boolean value, or false if not found
     */
    public static boolean extractBoolean(String json, String fieldName) {
        Map<String, Object> map = fromJsonMap(json);
        Object value = map.get(fieldName);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }

    /**
     * Extract a string set from JSON by field name.
     *
     * @param json the JSON string
     * @param fieldName the field name to extract
     * @return the string set, or empty set if not found
     */
    @SuppressWarnings("unchecked")
    public static Set<String> extractStringSet(String json, String fieldName) {
        Map<String, Object> map = fromJsonMap(json);
        Object value = map.get(fieldName);
        if (value instanceof List) {
            return new HashSet<>((List<String>) value);
        }
        return Set.of();
    }

    /**
     * Exception thrown when JSON serialization or deserialization fails.
     */
    public static class JsonSerializationException extends RuntimeException {
        public JsonSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
