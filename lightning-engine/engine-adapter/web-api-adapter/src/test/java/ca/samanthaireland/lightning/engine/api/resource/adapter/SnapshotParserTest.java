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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SnapshotParser")
class SnapshotParserTest {

    private SnapshotParser parser;

    @BeforeEach
    void setUp() {
        parser = new SnapshotParser(new ObjectMapper());
    }

    @Nested
    @DisplayName("parse(String)")
    class ParseJsonString {

        @Test
        @DisplayName("returns empty map for null input")
        void returnsEmptyMapForNull() {
            var result = parser.parse(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty map for blank input")
        void returnsEmptyMapForBlank() {
            var result = parser.parse("   ");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("parses single module with single component")
        void parsesSingleModule() {
            String json = """
                {
                    "TestModule": {
                        "POSITION_X": [1.0, 2.0, 3.0]
                    }
                }
                """;

            var result = parser.parse(json);

            assertThat(result).containsKey("TestModule");
            assertThat(result.get("TestModule")).containsKey("POSITION_X");
            assertThat(result.get("TestModule").get("POSITION_X"))
                    .containsExactly(1.0f, 2.0f, 3.0f);
        }

        @Test
        @DisplayName("parses multiple modules with multiple components")
        void parsesMultipleModules() {
            String json = """
                {
                    "EntityModule": {
                        "ENTITY_ID": [1, 2, 3],
                        "PLAYER_ID": [100, 100, 200]
                    },
                    "GridMapModule": {
                        "POSITION_X": [10.5, 20.5, 30.5],
                        "POSITION_Y": [5.0, 15.0, 25.0]
                    }
                }
                """;

            var result = parser.parse(json);

            assertThat(result).hasSize(2);
            assertThat(result).containsKeys("EntityModule", "GridMapModule");

            var entityModule = result.get("EntityModule");
            assertThat(entityModule.get("ENTITY_ID")).containsExactly(1.0f, 2.0f, 3.0f);
            assertThat(entityModule.get("PLAYER_ID")).containsExactly(100.0f, 100.0f, 200.0f);

            var gridMapModule = result.get("GridMapModule");
            assertThat(gridMapModule.get("POSITION_X")).containsExactly(10.5f, 20.5f, 30.5f);
            assertThat(gridMapModule.get("POSITION_Y")).containsExactly(5.0f, 15.0f, 25.0f);
        }

        @Test
        @DisplayName("handles empty arrays")
        void handlesEmptyArrays() {
            String json = """
                {
                    "TestModule": {
                        "EMPTY_COMPONENT": []
                    }
                }
                """;

            var result = parser.parse(json);

            assertThat(result.get("TestModule").get("EMPTY_COMPONENT")).isEmpty();
        }

        @Test
        @DisplayName("converts integers to floats")
        void convertsIntegersToFloats() {
            String json = """
                {
                    "TestModule": {
                        "VALUES": [1, 2, 3]
                    }
                }
                """;

            var result = parser.parse(json);

            assertThat(result.get("TestModule").get("VALUES"))
                    .containsExactly(1.0f, 2.0f, 3.0f);
        }

        @Test
        @DisplayName("throws SnapshotParseException for invalid JSON")
        void throwsExceptionForInvalidJson() {
            String invalidJson = "{ invalid json }";

            assertThatThrownBy(() -> parser.parse(invalidJson))
                    .isInstanceOf(SnapshotParser.SnapshotParseException.class)
                    .hasMessageContaining("Failed to parse snapshot JSON");
        }
    }

    @Nested
    @DisplayName("parseFromMap(Map)")
    class ParseFromMap {

        @Test
        @DisplayName("returns empty map for null input")
        void returnsEmptyMapForNull() {
            var result = parser.parseFromMap(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty map for empty input")
        void returnsEmptyMapForEmpty() {
            var result = parser.parseFromMap(Map.of());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("parses nested map structure")
        void parsesNestedMapStructure() {
            Map<String, Object> dataMap = Map.of(
                    "TestModule", Map.of(
                            "POSITION_X", List.of(1.0, 2.0, 3.0),
                            "POSITION_Y", List.of(4.0, 5.0, 6.0)
                    )
            );

            var result = parser.parseFromMap(dataMap);

            assertThat(result).containsKey("TestModule");
            assertThat(result.get("TestModule").get("POSITION_X"))
                    .containsExactly(1.0f, 2.0f, 3.0f);
            assertThat(result.get("TestModule").get("POSITION_Y"))
                    .containsExactly(4.0f, 5.0f, 6.0f);
        }

        @Test
        @DisplayName("converts Number types to Float")
        void convertsNumberTypesToFloat() {
            Map<String, Object> dataMap = Map.of(
                    "TestModule", Map.of(
                            "MIXED_NUMBERS", List.of(1, 2.5, 3L, 4.0f)
                    )
            );

            var result = parser.parseFromMap(dataMap);

            assertThat(result.get("TestModule").get("MIXED_NUMBERS"))
                    .containsExactly(1.0f, 2.5f, 3.0f, 4.0f);
        }

        @Test
        @DisplayName("ignores non-Number values in lists")
        void ignoresNonNumberValues() {
            Map<String, Object> dataMap = Map.of(
                    "TestModule", Map.of(
                            "VALUES", List.of(1.0, "not a number", 3.0)
                    )
            );

            var result = parser.parseFromMap(dataMap);

            assertThat(result.get("TestModule").get("VALUES"))
                    .containsExactly(1.0f, 3.0f);
        }

        @Test
        @DisplayName("ignores non-Map module values")
        void ignoresNonMapModuleValues() {
            Map<String, Object> dataMap = Map.of(
                    "ValidModule", Map.of("COMPONENT", List.of(1.0)),
                    "InvalidModule", "not a map"
            );

            var result = parser.parseFromMap(dataMap);

            assertThat(result).containsKey("ValidModule");
            assertThat(result).doesNotContainKey("InvalidModule");
        }
    }

    @Nested
    @DisplayName("Factory integration")
    class FactoryIntegration {

        @Test
        @DisplayName("SnapshotParserFactory.create() returns working parser")
        void factoryCreatesWorkingParser() {
            var factoryParser = SnapshotParserFactory.create();

            String json = """
                {
                    "TestModule": {
                        "VALUE": [42.0]
                    }
                }
                """;

            var result = factoryParser.parse(json);

            assertThat(result.get("TestModule").get("VALUE")).containsExactly(42.0f);
        }

        @Test
        @DisplayName("SnapshotParserFactory.create(ObjectMapper) uses provided mapper")
        void factoryUsesProvidedMapper() {
            var customMapper = new ObjectMapper();
            var factoryParser = SnapshotParserFactory.create(customMapper);

            String json = """
                {
                    "TestModule": {
                        "VALUE": [42.0]
                    }
                }
                """;

            var result = factoryParser.parse(json);

            assertThat(result.get("TestModule").get("VALUE")).containsExactly(42.0f);
        }
    }
}
