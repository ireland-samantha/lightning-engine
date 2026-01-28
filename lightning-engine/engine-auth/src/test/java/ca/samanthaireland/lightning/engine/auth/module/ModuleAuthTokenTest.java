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

package ca.samanthaireland.lightning.engine.auth.module;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static ca.samanthaireland.lightning.engine.auth.module.ModuleAuthToken.ComponentPermission.*;
import static org.assertj.core.api.Assertions.assertThat;

class ModuleAuthTokenTest {

    @Test
    void permissionKey_formatsCorrectly() {
        // When
        String key = ModuleAuthToken.permissionKey("EntityModule", "ENTITY_TYPE");

        // Then
        assertThat(key).isEqualTo("EntityModule.ENTITY_TYPE");
    }

    @Test
    void ownsComponent_returnsTrueForOwnerPermission() {
        // Given
        ModuleAuthToken token = new ModuleAuthToken(
                "TestModule",
                Map.of("TestModule.COMP_A", OWNER),
                false,
                null
        );

        // Then
        assertThat(token.ownsComponent("TestModule", "COMP_A")).isTrue();
    }

    @Test
    void ownsComponent_returnsFalseForReadPermission() {
        // Given
        ModuleAuthToken token = new ModuleAuthToken(
                "TestModule",
                Map.of("OtherModule.COMP_A", READ),
                false,
                null
        );

        // Then
        assertThat(token.ownsComponent("OtherModule", "COMP_A")).isFalse();
    }

    @Test
    void ownsComponent_returnsFalseForWritePermission() {
        // Given
        ModuleAuthToken token = new ModuleAuthToken(
                "TestModule",
                Map.of("OtherModule.COMP_A", WRITE),
                false,
                null
        );

        // Then
        assertThat(token.ownsComponent("OtherModule", "COMP_A")).isFalse();
    }

    @Test
    void ownsComponent_returnsFalseForNoPermission() {
        // Given
        ModuleAuthToken token = new ModuleAuthToken(
                "TestModule",
                Map.of(),
                false,
                null
        );

        // Then
        assertThat(token.ownsComponent("OtherModule", "COMP_A")).isFalse();
    }

    @Test
    void canRead_returnsTrueForOwnerPermission() {
        // Given
        ModuleAuthToken token = new ModuleAuthToken(
                "TestModule",
                Map.of("TestModule.COMP_A", OWNER),
                false,
                null
        );

        // Then
        assertThat(token.canRead("TestModule", "COMP_A")).isTrue();
    }

    @Test
    void canRead_returnsTrueForReadPermission() {
        // Given
        ModuleAuthToken token = new ModuleAuthToken(
                "TestModule",
                Map.of("OtherModule.COMP_A", READ),
                false,
                null
        );

        // Then
        assertThat(token.canRead("OtherModule", "COMP_A")).isTrue();
    }

    @Test
    void canRead_returnsTrueForWritePermission() {
        // Given
        ModuleAuthToken token = new ModuleAuthToken(
                "TestModule",
                Map.of("OtherModule.COMP_A", WRITE),
                false,
                null
        );

        // Then
        assertThat(token.canRead("OtherModule", "COMP_A")).isTrue();
    }

    @Test
    void canRead_returnsFalseForNoPermission() {
        // Given
        ModuleAuthToken token = new ModuleAuthToken(
                "TestModule",
                Map.of(),
                false,
                null
        );

        // Then
        assertThat(token.canRead("OtherModule", "COMP_A")).isFalse();
    }

    @Test
    void canRead_returnsTrueForSuperuser() {
        // Given
        ModuleAuthToken token = new ModuleAuthToken(
                "EntityModule",
                Map.of(),
                true,  // superuser
                null
        );

        // Then - superuser can read anything
        assertThat(token.canRead("AnyModule", "ANY_COMPONENT")).isTrue();
    }

    @Test
    void canWrite_returnsTrueForOwnerPermission() {
        // Given
        ModuleAuthToken token = new ModuleAuthToken(
                "TestModule",
                Map.of("TestModule.COMP_A", OWNER),
                false,
                null
        );

        // Then
        assertThat(token.canWrite("TestModule", "COMP_A")).isTrue();
    }

    @Test
    void canWrite_returnsFalseForReadPermission() {
        // Given
        ModuleAuthToken token = new ModuleAuthToken(
                "TestModule",
                Map.of("OtherModule.COMP_A", READ),
                false,
                null
        );

        // Then
        assertThat(token.canWrite("OtherModule", "COMP_A")).isFalse();
    }

    @Test
    void canWrite_returnsTrueForWritePermission() {
        // Given
        ModuleAuthToken token = new ModuleAuthToken(
                "TestModule",
                Map.of("OtherModule.COMP_A", WRITE),
                false,
                null
        );

        // Then
        assertThat(token.canWrite("OtherModule", "COMP_A")).isTrue();
    }

    @Test
    void canWrite_returnsFalseForNoPermission() {
        // Given
        ModuleAuthToken token = new ModuleAuthToken(
                "TestModule",
                Map.of(),
                false,
                null
        );

        // Then
        assertThat(token.canWrite("OtherModule", "COMP_A")).isFalse();
    }

    @Test
    void canWrite_returnsTrueForSuperuser() {
        // Given
        ModuleAuthToken token = new ModuleAuthToken(
                "EntityModule",
                Map.of(),
                true,  // superuser
                null
        );

        // Then - superuser can write anything
        assertThat(token.canWrite("AnyModule", "ANY_COMPONENT")).isTrue();
    }

    @Test
    void formatPermissions_withNoPermissions_returnsNoPermissions() {
        // Given
        ModuleAuthToken token = new ModuleAuthToken(
                "TestModule",
                Map.of(),
                false,
                null
        );

        // Then
        assertThat(token.formatPermissions()).isEqualTo("(no permissions)");
    }

    @Test
    void formatPermissions_withPermissions_formatsCorrectly() {
        // Given
        ModuleAuthToken token = new ModuleAuthToken(
                "TestModule",
                Map.of(
                        "TestModule.COMP_A", OWNER,
                        "OtherModule.COMP_B", READ
                ),
                false,
                null
        );

        // When
        String formatted = token.formatPermissions();

        // Then
        assertThat(formatted).contains("TestModule.COMP_A.owner");
        assertThat(formatted).contains("OtherModule.COMP_B.read");
    }
}
