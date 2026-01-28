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


package ca.samanthaireland.lightning.engine.core;

import ca.samanthaireland.lightning.engine.core.command.CommandPayload;
import ca.samanthaireland.lightning.engine.core.match.Match;
import ca.samanthaireland.lightning.engine.core.match.Player;
import ca.samanthaireland.lightning.engine.core.session.PlayerSession;
import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;

import java.util.List;
import java.util.Optional;

/**
 * Main simulation interface combining tick control, module installation,
 * command dispatch, and match/player/session management.
 *
 * <p>Extends focused interfaces for clients that only need specific capabilities:
 * <ul>
 *   <li>{@link TickController} - tick advancement and auto-play</li>
 *   <li>{@link ModuleInstaller} - module installation</li>
 *   <li>{@link CommandDispatcher} - command enqueueing</li>
 * </ul>
 */
public interface GameSimulation extends TickController, ModuleInstaller, CommandDispatcher {

    // Match operations
    Match createMatch(Match match);
    Optional<Match> getMatch(long matchId);
    List<Match> getAllMatches();
    void deleteMatch(long matchId);

    // Player operations
    void createPlayer(Player player);
    Optional<Player> getPlayer(long playerId);
    List<Player> getAllPlayers();
    void deletePlayer(long playerId);

    // Session operations (replaces PlayerMatch)
    PlayerSession joinMatch(long playerId, long matchId);
    Optional<PlayerSession> getSession(long playerId, long matchId);
    List<PlayerSession> getSessionsByMatch(long matchId);
    List<PlayerSession> getSessionsByPlayer(long playerId);
    void leaveMatch(long playerId, long matchId);
}
