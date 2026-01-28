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

package ca.samanthaireland.lightning.engine.internal.ext.ai;

import ca.samanthaireland.lightning.engine.core.match.Match;
import ca.samanthaireland.lightning.engine.core.match.MatchService;
import ca.samanthaireland.game.domain.AI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AITickService}.
 */
@DisplayName("AITickService")
@ExtendWith(MockitoExtension.class)
class AITickServiceTest {

    @Mock
    private AIManager mockAIManager;

    @Mock
    private MatchService mockMatchService;

    @Mock
    private AI mockAI;

    private AITickService service;

    @BeforeEach
    void setUp() {
        service = new AITickService(mockAIManager, mockMatchService);
    }

    @Nested
    @DisplayName("onTick")
    class OnTick {

        @Test
        @DisplayName("should skip matches with null enabledAIs")
        void shouldSkipMatchesWithNullEnabledAIs() {
            Match match = createMatch(1L, (List<String>) null);
            when(mockMatchService.getAllMatches()).thenReturn(List.of(match));

            service.onTick(1L);

            verifyNoInteractions(mockAIManager);
        }

        @Test
        @DisplayName("should skip matches with empty enabledAIs")
        void shouldSkipMatchesWithEmptyEnabledAIs() {
            Match match = createMatch(1L, List.of());
            when(mockMatchService.getAllMatches()).thenReturn(List.of(match));

            service.onTick(1L);

            verifyNoInteractions(mockAIManager);
        }

        @Test
        @DisplayName("should create AI instances for matches")
        void shouldCreateAIInstancesForMatches() {
            Match match = createMatch(1L, List.of("TestAI"));
            when(mockMatchService.getAllMatches()).thenReturn(List.of(match));
            when(mockAIManager.createForMatch("TestAI", 1L)).thenReturn(mockAI);

            service.onTick(1L);

            verify(mockAIManager).createForMatch("TestAI", 1L);
        }

        @Test
        @DisplayName("should call onTick for each AI")
        void shouldCallOnTickForEachAI() {
            Match match = createMatch(1L, List.of("TestAI"));
            when(mockMatchService.getAllMatches()).thenReturn(List.of(match));
            when(mockAIManager.createForMatch("TestAI", 1L)).thenReturn(mockAI);

            service.onTick(1L);

            verify(mockAI).onTick();
        }

        @Test
        @DisplayName("should cache AI instances for subsequent ticks")
        void shouldCacheAIInstancesForSubsequentTicks() {
            Match match = createMatch(1L, List.of("TestAI"));
            when(mockMatchService.getAllMatches()).thenReturn(List.of(match));
            when(mockAIManager.createForMatch("TestAI", 1L)).thenReturn(mockAI);

            service.onTick(1L);
            service.onTick(2L);

            // AI should only be created once
            verify(mockAIManager, times(1)).createForMatch("TestAI", 1L);
            // But onTick should be called twice
            verify(mockAI, times(2)).onTick();
        }

        @Test
        @DisplayName("should catch and log AI onTick exceptions")
        void shouldCatchAndLogAIOnTickExceptions() {
            Match match = createMatch(1L, List.of("TestAI"));
            when(mockMatchService.getAllMatches()).thenReturn(List.of(match));
            when(mockAIManager.createForMatch("TestAI", 1L)).thenReturn(mockAI);
            doThrow(new RuntimeException("AI error")).when(mockAI).onTick();

            assertThatCode(() -> service.onTick(1L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should handle multiple matches")
        void shouldHandleMultipleMatches() {
            Match match1 = createMatch(1L, List.of("AI1"));
            Match match2 = createMatch(2L, List.of("AI2"));
            AI ai1 = mock(AI.class);
            AI ai2 = mock(AI.class);
            when(mockMatchService.getAllMatches()).thenReturn(List.of(match1, match2));
            when(mockAIManager.createForMatch("AI1", 1L)).thenReturn(ai1);
            when(mockAIManager.createForMatch("AI2", 2L)).thenReturn(ai2);

            service.onTick(1L);

            verify(ai1).onTick();
            verify(ai2).onTick();
        }

        @Test
        @DisplayName("should handle null AI from manager")
        void shouldHandleNullAIFromManager() {
            Match match = createMatch(1L, List.of("UnknownAI"));
            when(mockMatchService.getAllMatches()).thenReturn(List.of(match));
            when(mockAIManager.createForMatch("UnknownAI", 1L)).thenReturn(null);

            assertThatCode(() -> service.onTick(1L))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invalidateMatch")
    class InvalidateMatch {

        @Test
        @DisplayName("should remove cached AI for match")
        void shouldRemoveCachedAIForMatch() {
            Match match = createMatch(1L, List.of("TestAI"));
            when(mockMatchService.getAllMatches()).thenReturn(List.of(match));
            when(mockAIManager.createForMatch("TestAI", 1L)).thenReturn(mockAI);

            service.onTick(1L);
            service.invalidateMatch(1L);
            service.onTick(2L);

            // AI should be created twice (after invalidation)
            verify(mockAIManager, times(2)).createForMatch("TestAI", 1L);
        }
    }

    @Nested
    @DisplayName("invalidateAll")
    class InvalidateAll {

        @Test
        @DisplayName("should clear all cached AI instances")
        void shouldClearAllCachedAIInstances() {
            Match match1 = createMatch(1L, List.of("AI1"));
            Match match2 = createMatch(2L, List.of("AI2"));
            AI ai1 = mock(AI.class);
            AI ai2 = mock(AI.class);
            when(mockMatchService.getAllMatches()).thenReturn(List.of(match1, match2));
            when(mockAIManager.createForMatch("AI1", 1L)).thenReturn(ai1);
            when(mockAIManager.createForMatch("AI2", 2L)).thenReturn(ai2);

            service.onTick(1L);
            service.invalidateAll();
            service.onTick(2L);

            // Each AI should be created twice (after invalidateAll)
            verify(mockAIManager, times(2)).createForMatch("AI1", 1L);
            verify(mockAIManager, times(2)).createForMatch("AI2", 2L);
        }
    }

    private Match createMatch(long id, List<String> enabledAIs) {
        return new Match(id, List.of(), enabledAIs);
    }
}
