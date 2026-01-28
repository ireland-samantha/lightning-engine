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

package ca.samanthaireland.lightning.controlplane.match.service;

import ca.samanthaireland.lightning.controlplane.client.LightningNodeClient;
import ca.samanthaireland.lightning.controlplane.match.exception.MatchNotFoundException;
import ca.samanthaireland.lightning.controlplane.match.model.MatchRegistryEntry;
import ca.samanthaireland.lightning.controlplane.match.model.MatchStatus;
import ca.samanthaireland.lightning.controlplane.match.repository.MatchRegistry;
import ca.samanthaireland.lightning.controlplane.node.model.Node;
import ca.samanthaireland.lightning.controlplane.node.model.NodeCapacity;
import ca.samanthaireland.lightning.controlplane.scheduler.service.SchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MatchRoutingServiceImpl}.
 *
 * <p>Tests verify correct coordination between scheduler, node client,
 * and match registry for match lifecycle operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MatchRoutingServiceImpl")
class MatchRoutingServiceImplTest {

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private LightningNodeClient nodeClient;

    @Mock
    private MatchRegistry matchRegistry;

    private MatchRoutingServiceImpl matchRoutingService;

    @BeforeEach
    void setUp() {
        matchRoutingService = new MatchRoutingServiceImpl(schedulerService, nodeClient, matchRegistry);
    }

    @Nested
    @DisplayName("createMatch")
    class CreateMatch {

        @Test
        @DisplayName("should create match with proper workflow")
        void shouldCreateMatchWithProperWorkflow() {
            // Arrange
            List<String> modules = List.of("EntityModule", "MovementModule");
            Node selectedNode = createNode("node-1", "http://node1:8080");

            when(schedulerService.selectNodeForMatch(modules, null)).thenReturn(selectedNode);
            when(nodeClient.createContainer(selectedNode, modules)).thenReturn(100L);
            when(nodeClient.createMatch(selectedNode, 100L, modules)).thenReturn(42L);
            when(matchRegistry.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            MatchRegistryEntry result = matchRoutingService.createMatch(modules, null);

            // Assert
            assertThat(result.nodeId()).isEqualTo("node-1");
            assertThat(result.containerId()).isEqualTo(100L);
            assertThat(result.status()).isEqualTo(MatchStatus.RUNNING);
            assertThat(result.moduleNames()).containsExactlyElementsOf(modules);
            assertThat(result.advertiseAddress()).isEqualTo("http://node1:8080");
            assertThat(result.matchId()).isEqualTo("node-1-100-42");

            verify(schedulerService).selectNodeForMatch(modules, null);
            verify(nodeClient).createContainer(selectedNode, modules);
            verify(nodeClient).createMatch(selectedNode, 100L, modules);
            verify(matchRegistry).save(any(MatchRegistryEntry.class));
        }

        @Test
        @DisplayName("should use preferred node when specified")
        void shouldUsePreferredNodeWhenSpecified() {
            // Arrange
            List<String> modules = List.of("EntityModule");
            String preferredNodeId = "preferred-node";
            Node selectedNode = createNode(preferredNodeId, "http://preferred:8080");

            when(schedulerService.selectNodeForMatch(modules, preferredNodeId)).thenReturn(selectedNode);
            when(nodeClient.createContainer(selectedNode, modules)).thenReturn(1L);
            when(nodeClient.createMatch(selectedNode, 1L, modules)).thenReturn(1L);
            when(matchRegistry.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            matchRoutingService.createMatch(modules, preferredNodeId);

            // Assert
            verify(schedulerService).selectNodeForMatch(modules, preferredNodeId);
        }

        @Test
        @DisplayName("should generate correct WebSocket URL")
        void shouldGenerateCorrectWebSocketUrl() {
            // Arrange
            List<String> modules = List.of("EntityModule");
            Node selectedNode = createNode("node-1", "http://node1:8080");

            when(schedulerService.selectNodeForMatch(modules, null)).thenReturn(selectedNode);
            when(nodeClient.createContainer(selectedNode, modules)).thenReturn(5L);
            when(nodeClient.createMatch(selectedNode, 5L, modules)).thenReturn(10L);
            when(matchRegistry.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            MatchRegistryEntry result = matchRoutingService.createMatch(modules, null);

            // Assert
            assertThat(result.websocketUrl())
                    .isEqualTo("ws://node1:8080/ws/containers/5/matches/node-1-5-10/snapshots");
        }

        @Test
        @DisplayName("should handle HTTPS to WSS conversion")
        void shouldHandleHttpsToWssConversion() {
            // Arrange
            List<String> modules = List.of("EntityModule");
            Node selectedNode = createNode("node-1", "https://secure-node:8443");

            when(schedulerService.selectNodeForMatch(modules, null)).thenReturn(selectedNode);
            when(nodeClient.createContainer(selectedNode, modules)).thenReturn(1L);
            when(nodeClient.createMatch(selectedNode, 1L, modules)).thenReturn(1L);
            when(matchRegistry.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            MatchRegistryEntry result = matchRoutingService.createMatch(modules, null);

            // Assert
            assertThat(result.websocketUrl()).startsWith("wss://");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should delegate to registry")
        void shouldDelegateToRegistry() {
            // Arrange
            MatchRegistryEntry entry = createEntry("node-1-100-42");
            when(matchRegistry.findById("node-1-100-42")).thenReturn(Optional.of(entry));

            // Act
            Optional<MatchRegistryEntry> result = matchRoutingService.findById("node-1-100-42");

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(entry);
            verify(matchRegistry).findById("node-1-100-42");
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            // Arrange
            when(matchRegistry.findById("nonexistent")).thenReturn(Optional.empty());

            // Act
            Optional<MatchRegistryEntry> result = matchRoutingService.findById("nonexistent");

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should delegate to registry")
        void shouldDelegateToRegistry() {
            // Arrange
            List<MatchRegistryEntry> entries = List.of(
                    createEntry("match-1"),
                    createEntry("match-2")
            );
            when(matchRegistry.findAll()).thenReturn(entries);

            // Act
            List<MatchRegistryEntry> result = matchRoutingService.findAll();

            // Assert
            assertThat(result).hasSize(2);
            verify(matchRegistry).findAll();
        }
    }

    @Nested
    @DisplayName("findByStatus")
    class FindByStatus {

        @Test
        @DisplayName("should delegate to registry with status filter")
        void shouldDelegateToRegistryWithStatusFilter() {
            // Arrange
            List<MatchRegistryEntry> runningMatches = List.of(createEntry("match-1"));
            when(matchRegistry.findByStatus(MatchStatus.RUNNING)).thenReturn(runningMatches);

            // Act
            List<MatchRegistryEntry> result = matchRoutingService.findByStatus(MatchStatus.RUNNING);

            // Assert
            assertThat(result).hasSize(1);
            verify(matchRegistry).findByStatus(MatchStatus.RUNNING);
        }
    }

    @Nested
    @DisplayName("deleteMatch")
    class DeleteMatch {

        @Test
        @DisplayName("should delete match from node and registry")
        void shouldDeleteMatchFromNodeAndRegistry() {
            // Arrange
            MatchRegistryEntry entry = createEntry("node-1-100-42");
            when(matchRegistry.findById("node-1-100-42")).thenReturn(Optional.of(entry));

            // Act
            matchRoutingService.deleteMatch("node-1-100-42");

            // Assert
            verify(nodeClient).deleteMatch(any(Node.class), eq(100L), eq(42L));
            verify(matchRegistry).deleteById("node-1-100-42");
        }

        @Test
        @DisplayName("should throw when match not found")
        void shouldThrowWhenMatchNotFound() {
            // Arrange
            when(matchRegistry.findById("nonexistent")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> matchRoutingService.deleteMatch("nonexistent"))
                    .isInstanceOf(MatchNotFoundException.class)
                    .hasMessageContaining("nonexistent");
        }

        @Test
        @DisplayName("should continue with registry deletion even if node deletion fails")
        void shouldContinueWithRegistryDeletionEvenIfNodeDeletionFails() {
            // Arrange
            MatchRegistryEntry entry = createEntry("node-1-100-42");
            when(matchRegistry.findById("node-1-100-42")).thenReturn(Optional.of(entry));
            doThrow(new RuntimeException("Node unreachable"))
                    .when(nodeClient).deleteMatch(any(), anyLong(), anyLong());

            // Act
            matchRoutingService.deleteMatch("node-1-100-42");

            // Assert - registry deletion should still happen
            verify(matchRegistry).deleteById("node-1-100-42");
        }
    }

    @Nested
    @DisplayName("updatePlayerCount")
    class UpdatePlayerCount {

        @Test
        @DisplayName("should update player count and save")
        void shouldUpdatePlayerCountAndSave() {
            // Arrange
            MatchRegistryEntry entry = createEntry("node-1-100-42");
            when(matchRegistry.findById("node-1-100-42")).thenReturn(Optional.of(entry));
            when(matchRegistry.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            matchRoutingService.updatePlayerCount("node-1-100-42", 5);

            // Assert
            ArgumentCaptor<MatchRegistryEntry> captor = ArgumentCaptor.forClass(MatchRegistryEntry.class);
            verify(matchRegistry).save(captor.capture());
            assertThat(captor.getValue().playerCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("should throw when match not found")
        void shouldThrowWhenMatchNotFound() {
            // Arrange
            when(matchRegistry.findById("nonexistent")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> matchRoutingService.updatePlayerCount("nonexistent", 5))
                    .isInstanceOf(MatchNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("finishMatch")
    class FinishMatch {

        @Test
        @DisplayName("should mark match as finished")
        void shouldMarkMatchAsFinished() {
            // Arrange
            MatchRegistryEntry entry = createEntry("node-1-100-42");
            when(matchRegistry.findById("node-1-100-42")).thenReturn(Optional.of(entry));
            when(matchRegistry.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            matchRoutingService.finishMatch("node-1-100-42");

            // Assert
            ArgumentCaptor<MatchRegistryEntry> captor = ArgumentCaptor.forClass(MatchRegistryEntry.class);
            verify(matchRegistry).save(captor.capture());
            assertThat(captor.getValue().status()).isEqualTo(MatchStatus.FINISHED);
        }

        @Test
        @DisplayName("should throw when match not found")
        void shouldThrowWhenMatchNotFound() {
            // Arrange
            when(matchRegistry.findById("nonexistent")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> matchRoutingService.finishMatch("nonexistent"))
                    .isInstanceOf(MatchNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("match ID parsing")
    class MatchIdParsing {

        @Test
        @DisplayName("should correctly parse node match ID from cluster match ID")
        void shouldCorrectlyParseNodeMatchId() {
            // Arrange
            MatchRegistryEntry entry = createEntry("node-1-100-42");
            when(matchRegistry.findById("node-1-100-42")).thenReturn(Optional.of(entry));

            // Act
            matchRoutingService.deleteMatch("node-1-100-42");

            // Assert - verify the correct match ID (42) was extracted
            verify(nodeClient).deleteMatch(any(Node.class), eq(100L), eq(42L));
        }

        @Test
        @DisplayName("should handle complex node IDs with hyphens")
        void shouldHandleComplexNodeIdsWithHyphens() {
            // Arrange
            // Match ID format: {nodeId}-{containerId}-{matchId}
            // Node ID can contain hyphens like "us-east-1-node-5"
            MatchRegistryEntry entry = MatchRegistryEntry.creating(
                    "us-east-1-node-5-200-99",
                    "us-east-1-node-5",
                    200L,
                    List.of("EntityModule"),
                    "http://node:8080"
            ).running();
            when(matchRegistry.findById("us-east-1-node-5-200-99")).thenReturn(Optional.of(entry));

            // Act
            matchRoutingService.deleteMatch("us-east-1-node-5-200-99");

            // Assert - the last part should be the match ID
            verify(nodeClient).deleteMatch(any(Node.class), eq(200L), eq(99L));
        }
    }

    // Helper methods

    private Node createNode(String nodeId, String advertiseAddress) {
        return Node.register(nodeId, advertiseAddress, NodeCapacity.defaultCapacity());
    }

    private MatchRegistryEntry createEntry(String matchId) {
        // Match ID format: {nodeId}-{containerId}-{matchId}
        // For test match ID "node-1-100-42": nodeId="node-1", containerId=100, matchId=42
        // Split from the end to handle node IDs with hyphens
        String[] parts = matchId.split("-");
        // Last two parts are containerId and nodeMatchId
        // Everything before that is the nodeId
        int len = parts.length;
        String nodeId;
        long containerId;

        if (len >= 3) {
            // Reconstruct nodeId from all parts except last two
            StringBuilder nodeIdBuilder = new StringBuilder();
            for (int i = 0; i < len - 2; i++) {
                if (i > 0) nodeIdBuilder.append("-");
                nodeIdBuilder.append(parts[i]);
            }
            nodeId = nodeIdBuilder.toString();
            containerId = Long.parseLong(parts[len - 2]);
        } else {
            nodeId = parts[0];
            containerId = len > 1 ? Long.parseLong(parts[1]) : 100L;
        }

        return MatchRegistryEntry.creating(
                matchId,
                nodeId,
                containerId,
                List.of("EntityModule"),
                "http://" + nodeId + ":8080"
        ).running();
    }
}
