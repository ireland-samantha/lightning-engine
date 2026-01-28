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


package ca.samanthaireland.lightning.engine.core.match;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Match}.
 */
@DisplayName("Match")
class MatchTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should create match with id and enabled modules")
        void shouldCreateMatchWithIdAndModules() {
            List<String> modules = List.of("MoveModule", "SpawnModule");
            Match match = new Match(1L, modules, List.of());

            assertThat(match.id()).isEqualTo(1L);
            assertThat(match.enabledModules()).containsExactly("MoveModule", "SpawnModule");
        }

        @Test
        @DisplayName("should create match with empty modules list")
        void shouldCreateMatchWithEmptyModules() {
            Match match = new Match(1L, List.of(), List.of());

            assertThat(match.id()).isEqualTo(1L);
            assertThat(match.enabledModules()).isEmpty();
        }

        @Test
        @DisplayName("should allow null modules (no validation in record)")
        void shouldAllowNullModules() {
            Match match = new Match(1L, null, null);

            assertThat(match.id()).isEqualTo(1L);
            assertThat(match.enabledModules()).isNull();
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        @DisplayName("should be equal when id and modules are the same")
        void shouldBeEqualWhenIdAndModulesAreSame() {
            List<String> modules = List.of("MoveModule");
            Match match1 = new Match(1L, modules, List.of());
            Match match2 = new Match(1L, modules, List.of());

            assertThat(match1).isEqualTo(match2);
            assertThat(match1.hashCode()).isEqualTo(match2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when ids differ")
        void shouldNotBeEqualWhenIdsDiffer() {
            List<String> modules = List.of("MoveModule");
            Match match1 = new Match(1L, modules, List.of());
            Match match2 = new Match(2L, modules, List.of());

            assertThat(match1).isNotEqualTo(match2);
        }

        @Test
        @DisplayName("should not be equal when modules differ")
        void shouldNotBeEqualWhenModulesDiffer() {
            Match match1 = new Match(1L, List.of("MoveModule"), List.of());
            Match match2 = new Match(1L, List.of("SpawnModule"), List.of());

            assertThat(match1).isNotEqualTo(match2);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("should include id and modules in string representation")
        void shouldIncludeIdAndModulesInString() {
            Match match = new Match(42L, List.of("MoveModule", "SpawnModule"), List.of());

            String str = match.toString();

            assertThat(str).contains("42");
            assertThat(str).contains("MoveModule");
            assertThat(str).contains("SpawnModule");
        }
    }
}
