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

package ca.samanthaireland.lightning.engine.ext.module;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class ModuleIdentifierTest {

    @Test
    void parse_validSpec_returnsIdentifier() {
        ModuleIdentifier id = ModuleIdentifier.parse("physics:0.2");

        assertThat(id.name()).isEqualTo("physics");
        assertThat(id.version()).isEqualTo(ModuleVersion.of(0, 2));
    }

    @Test
    void parse_specWithPatchVersion_returnsIdentifier() {
        ModuleIdentifier id = ModuleIdentifier.parse("gridmap:1.2.3");

        assertThat(id.name()).isEqualTo("gridmap");
        assertThat(id.version()).isEqualTo(ModuleVersion.of(1, 2, 3));
    }

    @Test
    void parse_specWithLongName_returnsIdentifier() {
        ModuleIdentifier id = ModuleIdentifier.parse("my-complex-module-name:2.0");

        assertThat(id.name()).isEqualTo("my-complex-module-name");
        assertThat(id.version()).isEqualTo(ModuleVersion.of(2, 0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "noversion", ":1.0", "name:", "a:b:1.0"})
    void parse_invalidSpec_throwsException(String invalid) {
        assertThatThrownBy(() -> ModuleIdentifier.parse(invalid))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_nullSpec_throwsException() {
        assertThatThrownBy(() -> ModuleIdentifier.parse(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or blank");
    }

    @Test
    void of_withNameAndVersion_createsIdentifier() {
        ModuleVersion version = ModuleVersion.of(1, 5);
        ModuleIdentifier id = ModuleIdentifier.of("test", version);

        assertThat(id.name()).isEqualTo("test");
        assertThat(id.version()).isEqualTo(version);
    }

    @Test
    void of_withNameAndVersionComponents_createsIdentifier() {
        ModuleIdentifier id = ModuleIdentifier.of("test", 2, 3);

        assertThat(id.name()).isEqualTo("test");
        assertThat(id.version()).isEqualTo(ModuleVersion.of(2, 3));
    }

    @Test
    void constructor_nullName_throwsException() {
        ModuleVersion version = ModuleVersion.of(1, 0);

        assertThatThrownBy(() -> new ModuleIdentifier(null, version))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or blank");
    }

    @Test
    void constructor_blankName_throwsException() {
        ModuleVersion version = ModuleVersion.of(1, 0);

        assertThatThrownBy(() -> new ModuleIdentifier("  ", version))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or blank");
    }

    @Test
    void constructor_nameWithColon_throwsException() {
        ModuleVersion version = ModuleVersion.of(1, 0);

        assertThatThrownBy(() -> new ModuleIdentifier("bad:name", version))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot contain ':'");
    }

    @Test
    void constructor_nullVersion_throwsException() {
        assertThatThrownBy(() -> new ModuleIdentifier("test", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version cannot be null");
    }

    @Test
    void toSpec_returnsCorrectFormat() {
        ModuleIdentifier id = ModuleIdentifier.of("physics", 0, 2);

        assertThat(id.toSpec()).isEqualTo("physics:0.2");
    }

    @Test
    void toSpec_withPatchVersion_includesPatch() {
        ModuleIdentifier id = ModuleIdentifier.of("physics", ModuleVersion.of(1, 2, 3));

        assertThat(id.toSpec()).isEqualTo("physics:1.2.3");
    }

    @Test
    void toString_returnsSpec() {
        ModuleIdentifier id = ModuleIdentifier.of("physics", 0, 2);

        assertThat(id.toString()).isEqualTo("physics:0.2");
    }

    @Test
    void equals_sameIdentifiers_returnsTrue() {
        ModuleIdentifier id1 = ModuleIdentifier.of("test", 1, 2);
        ModuleIdentifier id2 = ModuleIdentifier.of("test", 1, 2);

        assertThat(id1).isEqualTo(id2);
    }

    @Test
    void equals_differentNames_returnsFalse() {
        ModuleIdentifier id1 = ModuleIdentifier.of("test1", 1, 2);
        ModuleIdentifier id2 = ModuleIdentifier.of("test2", 1, 2);

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void equals_differentVersions_returnsFalse() {
        ModuleIdentifier id1 = ModuleIdentifier.of("test", 1, 2);
        ModuleIdentifier id2 = ModuleIdentifier.of("test", 1, 3);

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void parse_roundTrip_preservesIdentifier() {
        ModuleIdentifier original = ModuleIdentifier.of("my-module", 3, 14);
        String spec = original.toSpec();
        ModuleIdentifier parsed = ModuleIdentifier.parse(spec);

        assertThat(parsed).isEqualTo(original);
    }
}
