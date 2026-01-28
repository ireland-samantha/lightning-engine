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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class ModuleVersionTest {

    @Test
    void parse_withMajorMinor_returnsVersionWithZeroPatch() {
        ModuleVersion version = ModuleVersion.parse("1.2");

        assertThat(version.major()).isEqualTo(1);
        assertThat(version.minor()).isEqualTo(2);
        assertThat(version.patch()).isZero();
    }

    @Test
    void parse_withMajorMinorPatch_returnsCompleteVersion() {
        ModuleVersion version = ModuleVersion.parse("1.2.3");

        assertThat(version.major()).isEqualTo(1);
        assertThat(version.minor()).isEqualTo(2);
        assertThat(version.patch()).isEqualTo(3);
    }

    @Test
    void parse_withZeroVersion_returnsZeroVersion() {
        ModuleVersion version = ModuleVersion.parse("0.0.0");

        assertThat(version.major()).isZero();
        assertThat(version.minor()).isZero();
        assertThat(version.patch()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "1", "1.2.3.4", "a.b", "1.b", "1.2.c"})
    void parse_withInvalidFormat_throwsException(String invalid) {
        assertThatThrownBy(() -> ModuleVersion.parse(invalid))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_withNull_throwsException() {
        assertThatThrownBy(() -> ModuleVersion.parse(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or blank");
    }

    @Test
    void of_withTwoArgs_createVersionWithZeroPatch() {
        ModuleVersion version = ModuleVersion.of(2, 5);

        assertThat(version.major()).isEqualTo(2);
        assertThat(version.minor()).isEqualTo(5);
        assertThat(version.patch()).isZero();
    }

    @Test
    void of_withThreeArgs_createsCompleteVersion() {
        ModuleVersion version = ModuleVersion.of(2, 5, 7);

        assertThat(version.major()).isEqualTo(2);
        assertThat(version.minor()).isEqualTo(5);
        assertThat(version.patch()).isEqualTo(7);
    }

    @Test
    void constructor_withNegativeMajor_throwsException() {
        assertThatThrownBy(() -> new ModuleVersion(-1, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Major version must be non-negative");
    }

    @Test
    void constructor_withNegativeMinor_throwsException() {
        assertThatThrownBy(() -> new ModuleVersion(0, -1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Minor version must be non-negative");
    }

    @Test
    void constructor_withNegativePatch_throwsException() {
        assertThatThrownBy(() -> new ModuleVersion(0, 0, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Patch version must be non-negative");
    }

    @ParameterizedTest
    @CsvSource({
            "1.0, 1.0, true",    // Same version
            "1.1, 1.0, true",    // Higher minor
            "1.5, 1.3, true",    // Higher minor
            "1.0.5, 1.0, true",  // Same major/minor, different patch
            "2.0, 1.0, false",   // Different major (higher)
            "0.9, 1.0, false",   // Different major (lower)
            "1.0, 1.1, false",   // Lower minor
    })
    void isCompatibleWith_variousVersions(String thisVersion, String requiredVersion, boolean expected) {
        ModuleVersion version = ModuleVersion.parse(thisVersion);
        ModuleVersion required = ModuleVersion.parse(requiredVersion);

        assertThat(version.isCompatibleWith(required)).isEqualTo(expected);
    }

    @Test
    void isCompatibleWith_nullRequired_returnsTrue() {
        ModuleVersion version = ModuleVersion.of(1, 0);

        assertThat(version.isCompatibleWith(null)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "1.0, 0.9, 1",   // Greater major
            "1.0, 1.0, 0",   // Equal
            "0.9, 1.0, -1",  // Lesser major
            "1.1, 1.0, 1",   // Greater minor
            "1.0, 1.1, -1",  // Lesser minor
            "1.0.1, 1.0.0, 1",  // Greater patch
            "1.0.0, 1.0.1, -1", // Lesser patch
    })
    void compareTo_variousVersions(String v1, String v2, int expectedSign) {
        ModuleVersion version1 = ModuleVersion.parse(v1);
        ModuleVersion version2 = ModuleVersion.parse(v2);

        int result = version1.compareTo(version2);

        if (expectedSign > 0) {
            assertThat(result).isPositive();
        } else if (expectedSign < 0) {
            assertThat(result).isNegative();
        } else {
            assertThat(result).isZero();
        }
    }

    @Test
    void compareTo_nullVersion_returnsPositive() {
        ModuleVersion version = ModuleVersion.of(1, 0);

        assertThat(version.compareTo(null)).isPositive();
    }

    @Test
    void toString_withZeroPatch_returnsMajorMinor() {
        ModuleVersion version = ModuleVersion.of(1, 2);

        assertThat(version.toString()).isEqualTo("1.2");
    }

    @Test
    void toString_withNonZeroPatch_returnsFullVersion() {
        ModuleVersion version = ModuleVersion.of(1, 2, 3);

        assertThat(version.toString()).isEqualTo("1.2.3");
    }

    @Test
    void equals_sameVersions_returnsTrue() {
        ModuleVersion v1 = ModuleVersion.of(1, 2, 3);
        ModuleVersion v2 = ModuleVersion.of(1, 2, 3);

        assertThat(v1).isEqualTo(v2);
    }

    @Test
    void equals_differentVersions_returnsFalse() {
        ModuleVersion v1 = ModuleVersion.of(1, 2, 3);
        ModuleVersion v2 = ModuleVersion.of(1, 2, 4);

        assertThat(v1).isNotEqualTo(v2);
    }
}
