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

package ca.samanthaireland.lightning.controlplane.module.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class ModuleMetadataTest {

    @Test
    void create_setsAllFields() {
        // Act
        ModuleMetadata metadata = ModuleMetadata.create(
                "entity-module",
                "1.0.0",
                "Entity management module",
                "entity-module-1.0.0.jar",
                1024,
                "abc123",
                "admin"
        );

        // Assert
        assertThat(metadata.name()).isEqualTo("entity-module");
        assertThat(metadata.version()).isEqualTo("1.0.0");
        assertThat(metadata.description()).isEqualTo("Entity management module");
        assertThat(metadata.fileName()).isEqualTo("entity-module-1.0.0.jar");
        assertThat(metadata.fileSize()).isEqualTo(1024);
        assertThat(metadata.checksum()).isEqualTo("abc123");
        assertThat(metadata.uploadedBy()).isEqualTo("admin");
        assertThat(metadata.uploadedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void create_withNullDescription_usesEmptyString() {
        // Act
        ModuleMetadata metadata = ModuleMetadata.create(
                "test", "1.0", null, "test.jar", 100, "abc", "user"
        );

        // Assert
        assertThat(metadata.description()).isEqualTo("");
    }

    @Test
    void create_withNullUploadedBy_usesSystem() {
        // Act
        ModuleMetadata metadata = ModuleMetadata.create(
                "test", "1.0", "desc", "test.jar", 100, "abc", null
        );

        // Assert
        assertThat(metadata.uploadedBy()).isEqualTo("system");
    }

    @Test
    void id_returnsNameColonVersion() {
        // Arrange
        ModuleMetadata metadata = ModuleMetadata.create(
                "entity-module", "1.0.0", "", "test.jar", 100, "abc", "user"
        );

        // Act & Assert
        assertThat(metadata.id()).isEqualTo("entity-module:1.0.0");
    }

    @Test
    void constructor_nullName_throwsException() {
        assertThatThrownBy(() -> new ModuleMetadata(
                null, "1.0", "desc", "test.jar", 100, "abc", Instant.now(), "user"
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_blankName_throwsException() {
        assertThatThrownBy(() -> new ModuleMetadata(
                "  ", "1.0", "desc", "test.jar", 100, "abc", Instant.now(), "user"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be blank");
    }

    @Test
    void constructor_blankVersion_throwsException() {
        assertThatThrownBy(() -> new ModuleMetadata(
                "test", "  ", "desc", "test.jar", 100, "abc", Instant.now(), "user"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version cannot be blank");
    }

    @Test
    void constructor_negativeFileSize_throwsException() {
        assertThatThrownBy(() -> new ModuleMetadata(
                "test", "1.0", "desc", "test.jar", -1, "abc", Instant.now(), "user"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fileSize cannot be negative");
    }
}
