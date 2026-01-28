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

package ca.samanthaireland.lightning.engine.quarkus.api.persistence;

import ca.samanthaireland.lightning.engine.core.container.ContainerMatchOperations;
import ca.samanthaireland.lightning.engine.core.container.ContainerSnapshotOperations;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.core.match.Match;
import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ContainerSnapshotPersistenceListener")
class ContainerSnapshotPersistenceListenerTest {

    @Mock
    private ExecutionContainer container;

    @Mock
    private ContainerMatchOperations matchOperations;

    @Mock
    private ContainerSnapshotOperations snapshotOperations;

    @Mock
    private MongoClient mongoClient;

    @Mock
    private MongoDatabase database;

    @Mock
    private MongoCollection<Document> collection;

    private ContainerSnapshotPersistenceListener listener;

    private static final long CONTAINER_ID = 1L;
    private static final String DATABASE_NAME = "testdb";
    private static final String COLLECTION_NAME = "snapshots";

    @BeforeEach
    void setUp() {
        when(mongoClient.getDatabase(DATABASE_NAME)).thenReturn(database);
        when(database.getCollection(COLLECTION_NAME)).thenReturn(collection);
        when(container.matches()).thenReturn(matchOperations);
        when(container.snapshots()).thenReturn(snapshotOperations);
    }

    @Nested
    @DisplayName("onTickComplete")
    class OnTickComplete {

        @Test
        @DisplayName("should persist snapshots for all matches")
        void persistsSnapshotsForAllMatches() {
            listener = new ContainerSnapshotPersistenceListener(
                    CONTAINER_ID, container, mongoClient, DATABASE_NAME, COLLECTION_NAME, 1);

            Match match1 = mock(Match.class);
            Match match2 = mock(Match.class);
            when(match1.id()).thenReturn(1L);
            when(match2.id()).thenReturn(2L);
            when(matchOperations.all()).thenReturn(List.of(match1, match2));

            Snapshot snapshot1 = Snapshot.fromLegacyFormat(Map.of(
                    "EntityModule", Map.of("ENTITY_ID", List.of(1.0f, 2.0f))
            ));
            Snapshot snapshot2 = Snapshot.fromLegacyFormat(Map.of(
                    "EntityModule", Map.of("ENTITY_ID", List.of(3.0f))
            ));
            when(snapshotOperations.forMatch(1L)).thenReturn(snapshot1);
            when(snapshotOperations.forMatch(2L)).thenReturn(snapshot2);

            listener.onTickComplete(1L);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
            verify(collection).insertMany(captor.capture());

            List<Document> documents = captor.getValue();
            assertThat(documents).hasSize(2);

            Document doc1 = documents.get(0);
            assertThat(doc1.getLong("containerId")).isEqualTo(CONTAINER_ID);
            assertThat(doc1.getLong("matchId")).isEqualTo(1L);
            assertThat(doc1.getLong("tick")).isEqualTo(1L);
            assertThat(doc1.get("data")).isNotNull();

            Document doc2 = documents.get(1);
            assertThat(doc2.getLong("matchId")).isEqualTo(2L);
        }

        @Test
        @DisplayName("should skip ticks based on interval")
        void skipsTicksBasedOnInterval() {
            listener = new ContainerSnapshotPersistenceListener(
                    CONTAINER_ID, container, mongoClient, DATABASE_NAME, COLLECTION_NAME, 5);

            when(matchOperations.all()).thenReturn(List.of());

            // Ticks 1-4 should be skipped
            listener.onTickComplete(1L);
            listener.onTickComplete(2L);
            listener.onTickComplete(3L);
            listener.onTickComplete(4L);

            verify(matchOperations, never()).all();

            // Tick 5 should trigger persistence (5 % 5 == 0)
            listener.onTickComplete(5L);
            verify(matchOperations).all();
        }

        @Test
        @DisplayName("should persist on every tick when interval is 1")
        void persistsEveryTickWhenIntervalIsOne() {
            listener = new ContainerSnapshotPersistenceListener(
                    CONTAINER_ID, container, mongoClient, DATABASE_NAME, COLLECTION_NAME, 1);

            when(matchOperations.all()).thenReturn(List.of());

            listener.onTickComplete(1L);
            listener.onTickComplete(2L);
            listener.onTickComplete(3L);

            verify(matchOperations, times(3)).all();
        }

        @Test
        @DisplayName("should handle empty matches list gracefully")
        void handlesEmptyMatchesList() {
            listener = new ContainerSnapshotPersistenceListener(
                    CONTAINER_ID, container, mongoClient, DATABASE_NAME, COLLECTION_NAME, 1);

            when(matchOperations.all()).thenReturn(List.of());

            listener.onTickComplete(1L);

            verify(collection, never()).insertMany(anyList());
        }

        @Test
        @DisplayName("should skip empty snapshots")
        void skipsEmptySnapshots() {
            listener = new ContainerSnapshotPersistenceListener(
                    CONTAINER_ID, container, mongoClient, DATABASE_NAME, COLLECTION_NAME, 1);

            Match match = mock(Match.class);
            when(match.id()).thenReturn(1L);
            when(matchOperations.all()).thenReturn(List.of(match));

            // Empty snapshot
            Snapshot emptySnapshot = Snapshot.fromLegacyFormat(Map.of());
            when(snapshotOperations.forMatch(1L)).thenReturn(emptySnapshot);

            listener.onTickComplete(1L);

            verify(collection, never()).insertMany(anyList());
        }

        @Test
        @DisplayName("should handle null snapshots gracefully")
        void handlesNullSnapshots() {
            listener = new ContainerSnapshotPersistenceListener(
                    CONTAINER_ID, container, mongoClient, DATABASE_NAME, COLLECTION_NAME, 1);

            Match match = mock(Match.class);
            when(match.id()).thenReturn(1L);
            when(matchOperations.all()).thenReturn(List.of(match));
            when(snapshotOperations.forMatch(1L)).thenReturn(null);

            listener.onTickComplete(1L);

            verify(collection, never()).insertMany(anyList());
        }

        @Test
        @DisplayName("should continue processing other matches when one fails")
        void continuesWhenMatchFails() {
            listener = new ContainerSnapshotPersistenceListener(
                    CONTAINER_ID, container, mongoClient, DATABASE_NAME, COLLECTION_NAME, 1);

            Match match1 = mock(Match.class);
            Match match2 = mock(Match.class);
            when(match1.id()).thenReturn(1L);
            when(match2.id()).thenReturn(2L);
            when(matchOperations.all()).thenReturn(List.of(match1, match2));

            // First match throws exception
            when(snapshotOperations.forMatch(1L)).thenThrow(new RuntimeException("Test error"));
            // Second match succeeds
            Snapshot snapshot = Snapshot.fromLegacyFormat(Map.of(
                    "EntityModule", Map.of("ENTITY_ID", List.of(1.0f))
            ));
            when(snapshotOperations.forMatch(2L)).thenReturn(snapshot);

            listener.onTickComplete(1L);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
            verify(collection).insertMany(captor.capture());

            List<Document> documents = captor.getValue();
            assertThat(documents).hasSize(1);
            assertThat(documents.get(0).getLong("matchId")).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("document structure")
    class DocumentStructure {

        @Test
        @DisplayName("should include all required fields in document")
        void includesAllRequiredFields() {
            listener = new ContainerSnapshotPersistenceListener(
                    CONTAINER_ID, container, mongoClient, DATABASE_NAME, COLLECTION_NAME, 1);

            Match match = mock(Match.class);
            when(match.id()).thenReturn(42L);
            when(matchOperations.all()).thenReturn(List.of(match));

            Snapshot snapshot = Snapshot.fromLegacyFormat(Map.of(
                    "TestModule", Map.of(
                            "COMPONENT_A", List.of(1.0f, 2.0f),
                            "COMPONENT_B", List.of(3.0f)
                    )
            ));
            when(snapshotOperations.forMatch(42L)).thenReturn(snapshot);

            listener.onTickComplete(100L);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
            verify(collection).insertMany(captor.capture());

            Document doc = captor.getValue().get(0);

            assertThat(doc.getLong("containerId")).isEqualTo(CONTAINER_ID);
            assertThat(doc.getLong("matchId")).isEqualTo(42L);
            assertThat(doc.getLong("tick")).isEqualTo(100L);
            assertThat(doc.get("timestamp")).isNotNull();

            Document data = doc.get("data", Document.class);
            assertThat(data).isNotNull();

            Document moduleData = data.get("TestModule", Document.class);
            assertThat(moduleData).isNotNull();

            @SuppressWarnings("unchecked")
            List<?> componentA = moduleData.get("COMPONENT_A", List.class);
            assertThat(componentA).hasSize(2);
        }
    }
}
