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

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionLevel;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModulePermissionClaimBuilderTest {

    @Test
    void forModule_withOwnComponents_addsOwnerClaims() {
        // Given
        PermissionComponent component1 = createMockPermissionComponent(1L, "COMP_A", PermissionLevel.READ);
        PermissionComponent component2 = createMockPermissionComponent(2L, "COMP_B", PermissionLevel.PRIVATE);
        List<BaseComponent> ownComponents = List.of(component1, component2);

        // When
        Map<String, ModuleAuthToken.ComponentPermission> claims =
                ModulePermissionClaimBuilder.forModule("TestModule")
                        .withOwnComponents(ownComponents)
                        .build();

        // Then
        assertThat(claims).hasSize(2);
        assertThat(claims.get("TestModule.COMP_A")).isEqualTo(ModuleAuthToken.ComponentPermission.OWNER);
        assertThat(claims.get("TestModule.COMP_B")).isEqualTo(ModuleAuthToken.ComponentPermission.OWNER);
    }

    @Test
    void forModule_withAccessibleComponents_addsReadClaims() {
        // Given
        EngineModule existingModule = createMockModule("ExistingModule",
                List.of(createMockPermissionComponent(10L, "POSITION_X", PermissionLevel.READ)));

        // When
        Map<String, ModuleAuthToken.ComponentPermission> claims =
                ModulePermissionClaimBuilder.forModule("NewModule")
                        .withAccessibleComponentsFrom(List.of(existingModule))
                        .build();

        // Then
        assertThat(claims).hasSize(1);
        assertThat(claims.get("ExistingModule.POSITION_X")).isEqualTo(ModuleAuthToken.ComponentPermission.READ);
    }

    @Test
    void forModule_withWriteComponents_addsWriteClaims() {
        // Given
        EngineModule existingModule = createMockModule("ExistingModule",
                List.of(createMockPermissionComponent(10L, "VELOCITY_X", PermissionLevel.WRITE)));

        // When
        Map<String, ModuleAuthToken.ComponentPermission> claims =
                ModulePermissionClaimBuilder.forModule("NewModule")
                        .withAccessibleComponentsFrom(List.of(existingModule))
                        .build();

        // Then
        assertThat(claims).hasSize(1);
        assertThat(claims.get("ExistingModule.VELOCITY_X")).isEqualTo(ModuleAuthToken.ComponentPermission.WRITE);
    }

    @Test
    void forModule_withPrivateComponents_doesNotAddClaims() {
        // Given
        EngineModule existingModule = createMockModule("ExistingModule",
                List.of(createMockPermissionComponent(10L, "INTERNAL_STATE", PermissionLevel.PRIVATE)));

        // When
        Map<String, ModuleAuthToken.ComponentPermission> claims =
                ModulePermissionClaimBuilder.forModule("NewModule")
                        .withAccessibleComponentsFrom(List.of(existingModule))
                        .build();

        // Then
        assertThat(claims).isEmpty();
    }

    @Test
    void forModule_skipsOwnModuleInExistingModules() {
        // Given
        PermissionComponent ownComponent = createMockPermissionComponent(1L, "OWN_COMP", PermissionLevel.READ);

        // Include the same module in existing modules (simulating already cached)
        EngineModule sameModule = createMockModule("TestModule",
                List.of(createMockPermissionComponent(1L, "OWN_COMP", PermissionLevel.READ)));

        // When
        Map<String, ModuleAuthToken.ComponentPermission> claims =
                ModulePermissionClaimBuilder.forModule("TestModule")
                        .withOwnComponents(List.of(ownComponent))
                        .withAccessibleComponentsFrom(List.of(sameModule))
                        .build();

        // Then - should only have OWNER, not READ from the "existing" module
        assertThat(claims).hasSize(1);
        assertThat(claims.get("TestModule.OWN_COMP")).isEqualTo(ModuleAuthToken.ComponentPermission.OWNER);
    }

    @Test
    void forModule_withMultipleModules_addsMixedClaims() {
        // Given
        PermissionComponent ownComponent = createMockPermissionComponent(1L, "MY_COMP", PermissionLevel.PRIVATE);

        EngineModule module1 = createMockModule("EntityModule",
                List.of(createMockPermissionComponent(10L, "ENTITY_TYPE", PermissionLevel.READ)));

        EngineModule module2 = createMockModule("RigidBodyModule",
                List.of(
                        createMockPermissionComponent(20L, "VELOCITY_X", PermissionLevel.WRITE),
                        createMockPermissionComponent(21L, "INTERNAL_FLAG", PermissionLevel.PRIVATE)
                ));

        // When
        Map<String, ModuleAuthToken.ComponentPermission> claims =
                ModulePermissionClaimBuilder.forModule("NewModule")
                        .withOwnComponents(List.of(ownComponent))
                        .withAccessibleComponentsFrom(List.of(module1, module2))
                        .build();

        // Then
        assertThat(claims).hasSize(3);
        assertThat(claims.get("NewModule.MY_COMP")).isEqualTo(ModuleAuthToken.ComponentPermission.OWNER);
        assertThat(claims.get("EntityModule.ENTITY_TYPE")).isEqualTo(ModuleAuthToken.ComponentPermission.READ);
        assertThat(claims.get("RigidBodyModule.VELOCITY_X")).isEqualTo(ModuleAuthToken.ComponentPermission.WRITE);
        assertThat(claims.get("RigidBodyModule.INTERNAL_FLAG")).isNull(); // PRIVATE - not accessible
    }

    @Test
    void forModule_withEmptyInputs_returnsEmptyClaims() {
        // When
        Map<String, ModuleAuthToken.ComponentPermission> claims =
                ModulePermissionClaimBuilder.forModule("EmptyModule")
                        .withOwnComponents(List.of())
                        .withAccessibleComponentsFrom(List.of())
                        .build();

        // Then
        assertThat(claims).isEmpty();
    }

    @Test
    void forModule_withFlagComponent_addsOwnerClaim() {
        // Given
        PermissionComponent flagComponent = createMockPermissionComponent(100L, "FLAG", PermissionLevel.READ);

        // When
        Map<String, ModuleAuthToken.ComponentPermission> claims =
                ModulePermissionClaimBuilder.forModule("TestModule")
                        .withOwnComponents(List.of(flagComponent))
                        .build();

        // Then
        assertThat(claims).hasSize(1);
        assertThat(claims.get("TestModule.FLAG")).isEqualTo(ModuleAuthToken.ComponentPermission.OWNER);
    }

    @Test
    void build_returnsImmutableMap() {
        // Given
        PermissionComponent component = createMockPermissionComponent(1L, "COMP", PermissionLevel.READ);

        // When
        Map<String, ModuleAuthToken.ComponentPermission> claims =
                ModulePermissionClaimBuilder.forModule("TestModule")
                        .withOwnComponents(List.of(component))
                        .build();

        // Then - should throw UnsupportedOperationException when trying to modify
        assertThat(claims).isUnmodifiable();
    }

    @Test
    void builderPattern_allowsChaining() {
        // Given
        PermissionComponent ownComponent = createMockPermissionComponent(1L, "OWN", PermissionLevel.READ);
        EngineModule otherModule = createMockModule("Other",
                List.of(createMockPermissionComponent(2L, "FOREIGN", PermissionLevel.WRITE)));

        // When
        Map<String, ModuleAuthToken.ComponentPermission> claims =
                ModulePermissionClaimBuilder.forModule("Test")
                        .withOwnComponents(List.of(ownComponent))
                        .withAccessibleComponentsFrom(List.of(otherModule))
                        .build();

        // Then
        assertThat(claims).hasSize(2);
        assertThat(claims.get("Test.OWN")).isEqualTo(ModuleAuthToken.ComponentPermission.OWNER);
        assertThat(claims.get("Other.FOREIGN")).isEqualTo(ModuleAuthToken.ComponentPermission.WRITE);
    }

    private PermissionComponent createMockPermissionComponent(long id, String name, PermissionLevel level) {
        PermissionComponent component = mock(PermissionComponent.class);
        when(component.getId()).thenReturn(id);
        when(component.getName()).thenReturn(name);
        when(component.getPermissionLevel()).thenReturn(level);
        return component;
    }

    private EngineModule createMockModule(String name, List<PermissionComponent> components) {
        EngineModule module = mock(EngineModule.class);
        when(module.getName()).thenReturn(name);
        when(module.createFlagComponent()).thenReturn(null);
        when(module.createComponents()).thenReturn(new ArrayList<>(components));
        return module;
    }
}
