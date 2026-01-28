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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchRoutingServiceImplTest {

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private LightningNodeClient nodeClient;

    @Mock
    private MatchRegistry matchRegistry;

    private MatchRoutingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MatchRoutingServiceImpl(schedulerService, nodeClient, matchRegistry);
    }

    @Test
    void createMatch_success_createsMatchOnNodeAndStoresInRegistry() {
        // Arrange
        List<String> moduleNames = List.of("entity-module", "grid-map-module");
        String preferredNode = null;

        Node selectedNode = Node.register("node-1", "http://localhost:8080", new NodeCapacity(100));
        long containerId = 42L;
        long nodeMatchId = 7L;

        when(schedulerService.selectNodeForMatch(moduleNames, preferredNode)).thenReturn(selectedNode);
        when(nodeClient.createContainer(selectedNode, moduleNames)).thenReturn(containerId);
        when(nodeClient.createMatch(selectedNode, containerId, moduleNames)).thenReturn(nodeMatchId);

        // Act
        MatchRegistryEntry result = service.createMatch(moduleNames, preferredNode);

        // Assert
        assertThat(result.matchId()).isEqualTo("node-1-42-7");
        assertThat(result.nodeId()).isEqualTo("node-1");
        assertThat(result.containerId()).isEqualTo(containerId);
        assertThat(result.status()).isEqualTo(MatchStatus.RUNNING);
        assertThat(result.moduleNames()).containsExactlyElementsOf(moduleNames);
        assertThat(result.websocketUrl()).isEqualTo("ws://localhost:8080/ws/containers/42/matches/node-1-42-7/snapshots");

        verify(matchRegistry).save(any(MatchRegistryEntry.class));
    }

    @Test
    void createMatch_withPreferredNode_passesToScheduler() {
        // Arrange
        List<String> moduleNames = List.of("entity-module");
        String preferredNode = "preferred-node";

        Node selectedNode = Node.register("preferred-node", "http://localhost:8080", new NodeCapacity(100));

        when(schedulerService.selectNodeForMatch(moduleNames, preferredNode)).thenReturn(selectedNode);
        when(nodeClient.createContainer(any(), any())).thenReturn(1L);
        when(nodeClient.createMatch(any(), anyLong(), any())).thenReturn(1L);

        // Act
        service.createMatch(moduleNames, preferredNode);

        // Assert
        verify(schedulerService).selectNodeForMatch(moduleNames, preferredNode);
    }

    @Test
    void findById_existingMatch_returnsMatch() {
        // Arrange
        String matchId = "node-1-42-7";
        MatchRegistryEntry entry = MatchRegistryEntry.creating(
                matchId, "node-1", 42L, List.of("entity-module"), "http://localhost:8080"
        ).running();

        when(matchRegistry.findById(matchId)).thenReturn(Optional.of(entry));

        // Act
        Optional<MatchRegistryEntry> result = service.findById(matchId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().matchId()).isEqualTo(matchId);
    }

    @Test
    void findById_nonExistentMatch_returnsEmpty() {
        // Arrange
        when(matchRegistry.findById("unknown")).thenReturn(Optional.empty());

        // Act
        Optional<MatchRegistryEntry> result = service.findById("unknown");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findAll_returnsAllMatches() {
        // Arrange
        List<MatchRegistryEntry> entries = List.of(
                MatchRegistryEntry.creating("match-1", "node-1", 1L, List.of("module"), "http://localhost:8080"),
                MatchRegistryEntry.creating("match-2", "node-2", 2L, List.of("module"), "http://localhost:8081")
        );
        when(matchRegistry.findAll()).thenReturn(entries);

        // Act
        List<MatchRegistryEntry> result = service.findAll();

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    void findByStatus_returnsFilteredMatches() {
        // Arrange
        MatchRegistryEntry running = MatchRegistryEntry.creating("match-1", "node-1", 1L, List.of("module"), "http://localhost:8080").running();
        when(matchRegistry.findByStatus(MatchStatus.RUNNING)).thenReturn(List.of(running));

        // Act
        List<MatchRegistryEntry> result = service.findByStatus(MatchStatus.RUNNING);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().status()).isEqualTo(MatchStatus.RUNNING);
    }

    @Test
    void deleteMatch_existingMatch_deletesFromNodeAndRegistry() {
        // Arrange
        String matchId = "node-1-42-7";
        MatchRegistryEntry entry = MatchRegistryEntry.creating(
                matchId, "node-1", 42L, List.of("entity-module"), "http://localhost:8080"
        ).running();

        when(matchRegistry.findById(matchId)).thenReturn(Optional.of(entry));

        // Act
        service.deleteMatch(matchId);

        // Assert
        verify(nodeClient).deleteMatch(any(Node.class), eq(42L), eq(7L));
        verify(matchRegistry).deleteById(matchId);
    }

    @Test
    void deleteMatch_nonExistentMatch_throwsException() {
        // Arrange
        when(matchRegistry.findById("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.deleteMatch("unknown"))
                .isInstanceOf(MatchNotFoundException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void deleteMatch_nodeClientFails_stillDeletesFromRegistry() {
        // Arrange
        String matchId = "node-1-42-7";
        MatchRegistryEntry entry = MatchRegistryEntry.creating(
                matchId, "node-1", 42L, List.of("entity-module"), "http://localhost:8080"
        ).running();

        when(matchRegistry.findById(matchId)).thenReturn(Optional.of(entry));
        doThrow(new RuntimeException("Node unreachable")).when(nodeClient).deleteMatch(any(), anyLong(), anyLong());

        // Act
        service.deleteMatch(matchId);

        // Assert - should still delete from registry
        verify(matchRegistry).deleteById(matchId);
    }

    @Test
    void updatePlayerCount_existingMatch_updatesCount() {
        // Arrange
        String matchId = "node-1-42-7";
        MatchRegistryEntry entry = MatchRegistryEntry.creating(
                matchId, "node-1", 42L, List.of("entity-module"), "http://localhost:8080"
        ).running();

        when(matchRegistry.findById(matchId)).thenReturn(Optional.of(entry));

        // Act
        service.updatePlayerCount(matchId, 10);

        // Assert
        ArgumentCaptor<MatchRegistryEntry> captor = ArgumentCaptor.forClass(MatchRegistryEntry.class);
        verify(matchRegistry).save(captor.capture());
        assertThat(captor.getValue().playerCount()).isEqualTo(10);
    }

    @Test
    void updatePlayerCount_nonExistentMatch_throwsException() {
        // Arrange
        when(matchRegistry.findById("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.updatePlayerCount("unknown", 10))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    void finishMatch_existingMatch_setsStatusToFinished() {
        // Arrange
        String matchId = "node-1-42-7";
        MatchRegistryEntry entry = MatchRegistryEntry.creating(
                matchId, "node-1", 42L, List.of("entity-module"), "http://localhost:8080"
        ).running();

        when(matchRegistry.findById(matchId)).thenReturn(Optional.of(entry));

        // Act
        service.finishMatch(matchId);

        // Assert
        ArgumentCaptor<MatchRegistryEntry> captor = ArgumentCaptor.forClass(MatchRegistryEntry.class);
        verify(matchRegistry).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(MatchStatus.FINISHED);
    }

    @Test
    void finishMatch_nonExistentMatch_throwsException() {
        // Arrange
        when(matchRegistry.findById("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.finishMatch("unknown"))
                .isInstanceOf(MatchNotFoundException.class);
    }
}
