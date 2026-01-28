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


package ca.samanthaireland.lightning.engine.internal.core.snapshot;

import ca.samanthaireland.lightning.engine.core.snapshot.DeltaSnapshot;
import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeltaCompressionServiceImplTest {

    private DeltaCompressionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeltaCompressionServiceImpl();
    }

    @Test
    void computeDelta_withIdenticalSnapshots_returnsEmptyDelta() {
        // Given
        Map<String, Map<String, List<Float>>> data = Map.of(
                "TestModule", Map.of(
                        "ENTITY_ID", List.of(1.0f, 2.0f),
                        "POSITION_X", List.of(100.0f, 200.0f)
                )
        );
        Snapshot from = Snapshot.fromLegacyFormat(data);
        Snapshot to = Snapshot.fromLegacyFormat(data);

        // When
        DeltaSnapshot delta = service.computeDelta(1L, 0L, from, 1L, to);

        // Then
        assertThat(delta.matchId()).isEqualTo(1L);
        assertThat(delta.fromTick()).isEqualTo(0L);
        assertThat(delta.toTick()).isEqualTo(1L);
        assertThat(delta.isEmpty()).isTrue();
        assertThat(delta.changeCount()).isZero();
        assertThat(delta.addedEntities()).isEmpty();
        assertThat(delta.removedEntities()).isEmpty();
    }

    @Test
    void computeDelta_withChangedValues_detectsChanges() {
        // Given
        Snapshot from = Snapshot.fromLegacyFormat(Map.of(
                "TestModule", Map.of(
                        "ENTITY_ID", List.of(1.0f, 2.0f),
                        "POSITION_X", List.of(100.0f, 200.0f),
                        "POSITION_Y", List.of(50.0f, 60.0f)
                )
        ));
        Snapshot to = Snapshot.fromLegacyFormat(Map.of(
                "TestModule", Map.of(
                        "ENTITY_ID", List.of(1.0f, 2.0f),
                        "POSITION_X", List.of(150.0f, 200.0f), // Entity 1 changed
                        "POSITION_Y", List.of(50.0f, 75.0f)   // Entity 2 changed
                )
        ));

        // When
        DeltaSnapshot delta = service.computeDelta(1L, 0L, from, 1L, to);

        // Then
        assertThat(delta.isEmpty()).isFalse();
        assertThat(delta.changeCount()).isEqualTo(2);
        assertThat(delta.changedComponents()).containsKey("TestModule");

        Map<String, Map<Long, Float>> moduleChanges = delta.changedComponents().get("TestModule");
        assertThat(moduleChanges.get("POSITION_X")).containsEntry(1L, 150.0f);
        assertThat(moduleChanges.get("POSITION_Y")).containsEntry(2L, 75.0f);
    }

    @Test
    void computeDelta_withAddedEntity_detectsAddition() {
        // Given
        Snapshot from = Snapshot.fromLegacyFormat(Map.of(
                "TestModule", Map.of(
                        "ENTITY_ID", List.of(1.0f),
                        "POSITION_X", List.of(100.0f)
                )
        ));
        Snapshot to = Snapshot.fromLegacyFormat(Map.of(
                "TestModule", Map.of(
                        "ENTITY_ID", List.of(1.0f, 2.0f),
                        "POSITION_X", List.of(100.0f, 200.0f)
                )
        ));

        // When
        DeltaSnapshot delta = service.computeDelta(1L, 0L, from, 1L, to);

        // Then
        assertThat(delta.addedEntities()).contains(2L);
        assertThat(delta.removedEntities()).isEmpty();
        // New entity's values should be in changed components
        assertThat(delta.changedComponents().get("TestModule").get("POSITION_X")).containsEntry(2L, 200.0f);
    }

    @Test
    void computeDelta_withRemovedEntity_detectsRemoval() {
        // Given
        Snapshot from = Snapshot.fromLegacyFormat(Map.of(
                "TestModule", Map.of(
                        "ENTITY_ID", List.of(1.0f, 2.0f),
                        "POSITION_X", List.of(100.0f, 200.0f)
                )
        ));
        Snapshot to = Snapshot.fromLegacyFormat(Map.of(
                "TestModule", Map.of(
                        "ENTITY_ID", List.of(1.0f),
                        "POSITION_X", List.of(100.0f)
                )
        ));

        // When
        DeltaSnapshot delta = service.computeDelta(1L, 0L, from, 1L, to);

        // Then
        assertThat(delta.removedEntities()).contains(2L);
        assertThat(delta.addedEntities()).isEmpty();
    }

    @Test
    void computeDelta_withEmptyFrom_detectsAllAsAdded() {
        // Given
        Snapshot from = Snapshot.fromLegacyFormat(Map.of());
        Snapshot to = Snapshot.fromLegacyFormat(Map.of(
                "TestModule", Map.of(
                        "ENTITY_ID", List.of(1.0f, 2.0f),
                        "POSITION_X", List.of(100.0f, 200.0f)
                )
        ));

        // When
        DeltaSnapshot delta = service.computeDelta(1L, 0L, from, 1L, to);

        // Then
        assertThat(delta.addedEntities()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(delta.removedEntities()).isEmpty();
    }

    @Test
    void computeDelta_withEmptyTo_detectsAllAsRemoved() {
        // Given
        Snapshot from = Snapshot.fromLegacyFormat(Map.of(
                "TestModule", Map.of(
                        "ENTITY_ID", List.of(1.0f, 2.0f),
                        "POSITION_X", List.of(100.0f, 200.0f)
                )
        ));
        Snapshot to = Snapshot.fromLegacyFormat(Map.of());

        // When
        DeltaSnapshot delta = service.computeDelta(1L, 0L, from, 1L, to);

        // Then
        assertThat(delta.removedEntities()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(delta.addedEntities()).isEmpty();
    }

    @Test
    void computeDelta_withMultipleModules_handlesAllModules() {
        // Given
        Snapshot from = Snapshot.fromLegacyFormat(Map.of(
                "ModuleA", Map.of(
                        "ENTITY_ID", List.of(1.0f),
                        "VALUE_A", List.of(10.0f)
                ),
                "ModuleB", Map.of(
                        "ENTITY_ID", List.of(1.0f),
                        "VALUE_B", List.of(20.0f)
                )
        ));
        Snapshot to = Snapshot.fromLegacyFormat(Map.of(
                "ModuleA", Map.of(
                        "ENTITY_ID", List.of(1.0f),
                        "VALUE_A", List.of(15.0f)  // Changed
                ),
                "ModuleB", Map.of(
                        "ENTITY_ID", List.of(1.0f),
                        "VALUE_B", List.of(20.0f)  // Unchanged
                )
        ));

        // When
        DeltaSnapshot delta = service.computeDelta(1L, 0L, from, 1L, to);

        // Then
        assertThat(delta.changedComponents()).containsKey("ModuleA");
        assertThat(delta.changedComponents().get("ModuleA").get("VALUE_A")).containsEntry(1L, 15.0f);
        // ModuleB should not be in changes since nothing changed
        assertThat(delta.changedComponents().containsKey("ModuleB")).isFalse();
    }

    @Test
    void computeDelta_withNullSnapshots_throwsException() {
        assertThatThrownBy(() -> service.computeDelta(1L, 0L, null, 1L, Snapshot.fromLegacyFormat(Map.of())))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.computeDelta(1L, 0L, Snapshot.fromLegacyFormat(Map.of()), 1L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applyDelta_withChangedValues_appliesCorrectly() {
        // Given
        Snapshot base = Snapshot.fromLegacyFormat(Map.of(
                "TestModule", Map.of(
                        "ENTITY_ID", List.of(1.0f, 2.0f),
                        "POSITION_X", List.of(100.0f, 200.0f)
                )
        ));

        DeltaSnapshot delta = new DeltaSnapshot(
                1L, 0L, 1L,
                Map.of("TestModule", Map.of("POSITION_X", Map.of(1L, 150.0f))),
                java.util.Set.of(),
                java.util.Set.of()
        );

        // When
        Snapshot result = service.applyDelta(base, delta);

        // Then
        assertThat(result.toLegacyFormat().get("TestModule").get("POSITION_X"))
                .containsExactly(150.0f, 200.0f);
    }

    @Test
    void applyDelta_roundTrip_producesOriginal() {
        // Given
        Snapshot from = Snapshot.fromLegacyFormat(Map.of(
                "TestModule", Map.of(
                        "ENTITY_ID", List.of(1.0f, 2.0f),
                        "POSITION_X", List.of(100.0f, 200.0f),
                        "POSITION_Y", List.of(50.0f, 60.0f)
                )
        ));
        Snapshot to = Snapshot.fromLegacyFormat(Map.of(
                "TestModule", Map.of(
                        "ENTITY_ID", List.of(1.0f, 2.0f),
                        "POSITION_X", List.of(150.0f, 250.0f),
                        "POSITION_Y", List.of(55.0f, 65.0f)
                )
        ));

        // When
        DeltaSnapshot delta = service.computeDelta(1L, 0L, from, 1L, to);
        Snapshot result = service.applyDelta(from, delta);

        // Then
        assertThat(result.toLegacyFormat().get("TestModule").get("POSITION_X"))
                .containsExactly(150.0f, 250.0f);
        assertThat(result.toLegacyFormat().get("TestModule").get("POSITION_Y"))
                .containsExactly(55.0f, 65.0f);
    }
}
