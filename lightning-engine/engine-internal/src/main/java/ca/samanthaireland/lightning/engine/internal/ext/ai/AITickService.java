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
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that manages per-match AI instances and invokes their tick methods.
 *
 * <p>AI instances are created lazily when first needed for a match and cached.
 * The cache is invalidated when matches or AI change.
 */
@Slf4j
public class AITickService {

    private final AIManager aiManager;
    private final MatchService matchService;

    /**
     * Cache of AI instances per match.
     * Key: matchId, Value: List of AI instances for that match.
     */
    private final Map<Long, List<AI>> matchAIs = new ConcurrentHashMap<>();

    public AITickService(AIManager aiManager, MatchService matchService) {
        this.aiManager = aiManager;
        this.matchService = matchService;
    }

    /**
     * Called each tick to execute all AI for all active matches.
     *
     * @param tick the current tick number
     */
    public void onTick(long tick) {
        List<Match> matches = matchService.getAllMatches();
        int totalExecuted = 0;

        for (Match match : matches) {
            if (match.enabledAIs() == null || match.enabledAIs().isEmpty()) {
                continue;
            }

            List<AI> ais = getOrCreateAIs(match);
            for (AI ai : ais) {
                try {
                    ai.onTick();
                    totalExecuted++;
                } catch (Exception e) {
                    log.error("Error executing AI onTick for match {}: {}",
                            match.id(), e.getMessage(), e);
                }
            }
        }

        if (totalExecuted > 0) {
            log.trace("Tick {}: executed {} AIs", tick, totalExecuted);
        }
    }

    /**
     * Get or create AI instances for a match.
     *
     * @param match the match
     * @return list of AI instances
     */
    private List<AI> getOrCreateAIs(Match match) {
        return matchAIs.computeIfAbsent(match.id(), matchId -> {
            List<AI> instances = new ArrayList<>();
            for (String aiName : match.enabledAIs()) {
                AI ai = aiManager.createForMatch(aiName, matchId);
                if (ai != null) {
                    instances.add(ai);
                    log.debug("Created AI '{}' for match {}", aiName, matchId);
                } else {
                    log.warn("AI '{}' not found for match {}", aiName, matchId);
                }
            }
            return instances;
        });
    }

    /**
     * Invalidate the cache for a specific match.
     *
     * <p>Call this when a match is deleted or its AI change.
     *
     * @param matchId the match ID
     */
    public void invalidateMatch(long matchId) {
        matchAIs.remove(matchId);
        log.debug("Invalidated AI cache for match {}", matchId);
    }

    /**
     * Clear all cached AI instances.
     *
     * <p>Call this when AI are reloaded.
     */
    public void invalidateAll() {
        matchAIs.clear();
        log.debug("Cleared all AI caches");
    }
}
