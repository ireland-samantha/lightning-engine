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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JsonMapper}.
 */
@DisplayName("JsonMapper")
class JsonMapperTest {

    @Nested
    @DisplayName("toJson")
    class ToJson {

        @Test
        @DisplayName("should serialize simple object to JSON")
        void shouldSerializeSimpleObjectToJson() {
            TestDto dto = new TestDto("test", 42);

            String json = JsonMapper.toJson(dto);

            assertThat(json).contains("\"name\":\"test\"");
            assertThat(json).contains("\"value\":42");
        }

        @Test
        @DisplayName("should serialize null as null string")
        void shouldSerializeNullAsNullString() {
            String json = JsonMapper.toJson(null);

            assertThat(json).isEqualTo("null");
        }

        @Test
        @DisplayName("should handle nested objects")
        void shouldHandleNestedObjects() {
            NestedDto nested = new NestedDto("outer", new TestDto("inner", 100));

            String json = JsonMapper.toJson(nested);

            assertThat(json).contains("\"outerName\":\"outer\"");
            assertThat(json).contains("\"inner\"");
            assertThat(json).contains("\"name\":\"inner\"");
        }

        @Test
        @DisplayName("should serialize lists")
        void shouldSerializeLists() {
            List<String> list = List.of("a", "b", "c");

            String json = JsonMapper.toJson(list);

            assertThat(json).isEqualTo("[\"a\",\"b\",\"c\"]");
        }

        @Test
        @DisplayName("should serialize maps")
        void shouldSerializeMaps() {
            Map<String, Integer> map = Map.of("key1", 1, "key2", 2);

            String json = JsonMapper.toJson(map);

            assertThat(json).contains("\"key1\":1");
            assertThat(json).contains("\"key2\":2");
        }
    }

    @Nested
    @DisplayName("fromJson with Class")
    class FromJsonClass {

        @Test
        @DisplayName("should deserialize JSON to object")
        void shouldDeserializeJsonToObject() {
            String json = "{\"name\":\"test\",\"value\":42}";

            TestDto result = JsonMapper.fromJson(json, TestDto.class);

            assertThat(result.name()).isEqualTo("test");
            assertThat(result.value()).isEqualTo(42);
        }

        @Test
        @DisplayName("should ignore unknown properties")
        void shouldIgnoreUnknownProperties() {
            String json = "{\"name\":\"test\",\"value\":42,\"unknownField\":\"ignored\"}";

            TestDto result = JsonMapper.fromJson(json, TestDto.class);

            assertThat(result.name()).isEqualTo("test");
            assertThat(result.value()).isEqualTo(42);
        }

        @Test
        @DisplayName("should throw JsonSerializationException on parse error")
        void shouldThrowOnParseError() {
            String invalidJson = "{invalid json}";

            assertThatThrownBy(() -> JsonMapper.fromJson(invalidJson, TestDto.class))
                    .isInstanceOf(JsonMapper.JsonSerializationException.class)
                    .hasMessageContaining("Failed to deserialize");
        }

        @Test
        @DisplayName("should handle null values in JSON")
        void shouldHandleNullValuesInJson() {
            String json = "{\"name\":null,\"value\":0}";

            TestDto result = JsonMapper.fromJson(json, TestDto.class);

            assertThat(result.name()).isNull();
            assertThat(result.value()).isZero();
        }
    }

    @Nested
    @DisplayName("fromJson with TypeReference")
    class FromJsonTypeReference {

        @Test
        @DisplayName("should deserialize generic types")
        void shouldDeserializeGenericTypes() {
            String json = "[{\"name\":\"first\",\"value\":1},{\"name\":\"second\",\"value\":2}]";

            List<TestDto> result = JsonMapper.fromJson(json, new TypeReference<>() {});

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("first");
            assertThat(result.get(1).name()).isEqualTo("second");
        }

        @Test
        @DisplayName("should work with map type reference")
        void shouldWorkWithMapTypeReference() {
            String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";

            Map<String, String> result = JsonMapper.fromJson(json, new TypeReference<>() {});

            assertThat(result).containsEntry("key1", "value1").containsEntry("key2", "value2");
        }
    }

    @Nested
    @DisplayName("fromJsonList")
    class FromJsonList {

        @Test
        @DisplayName("should deserialize JSON array to typed list")
        void shouldDeserializeJsonArrayToTypedList() {
            String json = "[{\"name\":\"a\",\"value\":1},{\"name\":\"b\",\"value\":2}]";

            List<TestDto> result = JsonMapper.fromJsonList(json, TestDto.class);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("a");
            assertThat(result.get(1).name()).isEqualTo("b");
        }

        @Test
        @DisplayName("should return empty list for empty array")
        void shouldReturnEmptyListForEmptyArray() {
            String json = "[]";

            List<TestDto> result = JsonMapper.fromJsonList(json, TestDto.class);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw JsonSerializationException on invalid JSON")
        void shouldThrowOnInvalidJson() {
            String invalidJson = "not an array";

            assertThatThrownBy(() -> JsonMapper.fromJsonList(invalidJson, TestDto.class))
                    .isInstanceOf(JsonMapper.JsonSerializationException.class);
        }
    }

    @Nested
    @DisplayName("fromJsonMap")
    class FromJsonMap {

        @Test
        @DisplayName("should deserialize JSON object to Map")
        void shouldDeserializeJsonObjectToMap() {
            String json = "{\"key1\":\"value1\",\"key2\":123}";

            Map<String, Object> result = JsonMapper.fromJsonMap(json);

            assertThat(result).containsEntry("key1", "value1").containsEntry("key2", 123);
        }

        @Test
        @DisplayName("should handle nested maps")
        void shouldHandleNestedMaps() {
            String json = "{\"outer\":{\"inner\":\"value\"}}";

            Map<String, Object> result = JsonMapper.fromJsonMap(json);

            assertThat(result).containsKey("outer");
            @SuppressWarnings("unchecked")
            Map<String, Object> outer = (Map<String, Object>) result.get("outer");
            assertThat(outer).containsEntry("inner", "value");
        }

        @Test
        @DisplayName("should throw JsonSerializationException on invalid JSON")
        void shouldThrowOnInvalidJson() {
            String invalidJson = "{broken";

            assertThatThrownBy(() -> JsonMapper.fromJsonMap(invalidJson))
                    .isInstanceOf(JsonMapper.JsonSerializationException.class)
                    .hasMessageContaining("Failed to deserialize JSON to Map");
        }

        @Test
        @DisplayName("should return empty map for empty object")
        void shouldReturnEmptyMapForEmptyObject() {
            String json = "{}";

            Map<String, Object> result = JsonMapper.fromJsonMap(json);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("extractString")
    class ExtractString {

        @Test
        @DisplayName("should extract string field value")
        void shouldExtractStringFieldValue() {
            String json = "{\"name\":\"test\",\"value\":42}";

            String result = JsonMapper.extractString(json, "name");

            assertThat(result).isEqualTo("test");
        }

        @Test
        @DisplayName("should return null when field not found")
        void shouldReturnNullWhenFieldNotFound() {
            String json = "{\"name\":\"test\"}";

            String result = JsonMapper.extractString(json, "missing");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should convert non-string values to string")
        void shouldConvertNonStringValuesToString() {
            String json = "{\"number\":42,\"bool\":true}";

            assertThat(JsonMapper.extractString(json, "number")).isEqualTo("42");
            assertThat(JsonMapper.extractString(json, "bool")).isEqualTo("true");
        }
    }

    @Nested
    @DisplayName("extractLong")
    class ExtractLong {

        @Test
        @DisplayName("should extract long field value")
        void shouldExtractLongFieldValue() {
            String json = "{\"id\":12345678901234}";

            long result = JsonMapper.extractLong(json, "id");

            assertThat(result).isEqualTo(12345678901234L);
        }

        @Test
        @DisplayName("should return 0 when field not found")
        void shouldReturnZeroWhenFieldNotFound() {
            String json = "{\"name\":\"test\"}";

            long result = JsonMapper.extractLong(json, "missing");

            assertThat(result).isZero();
        }

        @Test
        @DisplayName("should return 0 for non-numeric values")
        void shouldReturnZeroForNonNumericValues() {
            String json = "{\"name\":\"test\"}";

            long result = JsonMapper.extractLong(json, "name");

            assertThat(result).isZero();
        }

        @Test
        @DisplayName("should handle integer values")
        void shouldHandleIntegerValues() {
            String json = "{\"count\":42}";

            long result = JsonMapper.extractLong(json, "count");

            assertThat(result).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("extractBoolean")
    class ExtractBoolean {

        @Test
        @DisplayName("should extract boolean field value")
        void shouldExtractBooleanFieldValue() {
            String json = "{\"active\":true,\"deleted\":false}";

            assertThat(JsonMapper.extractBoolean(json, "active")).isTrue();
            assertThat(JsonMapper.extractBoolean(json, "deleted")).isFalse();
        }

        @Test
        @DisplayName("should return false when field not found")
        void shouldReturnFalseWhenFieldNotFound() {
            String json = "{\"name\":\"test\"}";

            boolean result = JsonMapper.extractBoolean(json, "missing");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for non-boolean values")
        void shouldReturnFalseForNonBooleanValues() {
            String json = "{\"name\":\"test\",\"count\":1}";

            assertThat(JsonMapper.extractBoolean(json, "name")).isFalse();
            assertThat(JsonMapper.extractBoolean(json, "count")).isFalse();
        }
    }

    @Nested
    @DisplayName("extractStringSet")
    class ExtractStringSet {

        @Test
        @DisplayName("should extract list as Set of strings")
        void shouldExtractListAsSetOfStrings() {
            String json = "{\"tags\":[\"a\",\"b\",\"c\"]}";

            Set<String> result = JsonMapper.extractStringSet(json, "tags");

            assertThat(result).containsExactlyInAnyOrder("a", "b", "c");
        }

        @Test
        @DisplayName("should return empty set when field not found")
        void shouldReturnEmptySetWhenFieldNotFound() {
            String json = "{\"name\":\"test\"}";

            Set<String> result = JsonMapper.extractStringSet(json, "missing");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty set for non-list values")
        void shouldReturnEmptySetForNonListValues() {
            String json = "{\"name\":\"test\"}";

            Set<String> result = JsonMapper.extractStringSet(json, "name");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle empty list")
        void shouldHandleEmptyList() {
            String json = "{\"tags\":[]}";

            Set<String> result = JsonMapper.extractStringSet(json, "tags");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getMapper")
    class GetMapper {

        @Test
        @DisplayName("should return ObjectMapper instance")
        void shouldReturnObjectMapperInstance() {
            ObjectMapper mapper = JsonMapper.getMapper();

            assertThat(mapper).isNotNull();
        }

        @Test
        @DisplayName("should return same instance on multiple calls")
        void shouldReturnSameInstanceOnMultipleCalls() {
            ObjectMapper first = JsonMapper.getMapper();
            ObjectMapper second = JsonMapper.getMapper();

            assertThat(first).isSameAs(second);
        }
    }

    // Test DTOs
    record TestDto(String name, int value) {}
    record NestedDto(String outerName, TestDto inner) {}
}
