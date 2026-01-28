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


package ca.samanthaireland.lightning.engine.internal.core.match;

import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.core.match.Match;
import ca.samanthaireland.lightning.engine.core.match.MatchRepository;
import ca.samanthaireland.lightning.engine.core.match.MatchService;
import ca.samanthaireland.lightning.engine.ext.module.ModuleResolver;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory implementation of {@link MatchService}.
 *
 * <p>This implementation provides business logic for match operations,
 * delegating persistence to {@link MatchRepository}.
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>All enabled modules must exist before match creation</li>
 *   <li>Match must exist before deletion</li>
 * </ul>
 *
 * <p>SOLID compliance:
 * <ul>
 *   <li>SRP: Business logic only, persistence delegated to repository</li>
 *   <li>DIP: Depends on MatchRepository and ModuleResolver abstractions</li>
 * </ul>
 */
@Slf4j
public class InMemoryMatchService implements MatchService {

    private final MatchRepository matchRepository;
    private volatile ModuleResolver moduleResolver;

    public InMemoryMatchService(MatchRepository matchRepository, ModuleResolver moduleResolver) {
        this.matchRepository = Objects.requireNonNull(matchRepository, "matchRepository must not be null");
        this.moduleResolver = moduleResolver; // Can be null, set later via setModuleResolver
    }

    /**
     * Sets the module resolver. Used for late binding in container initialization.
     *
     * @param moduleResolver the module resolver
     */
    public void setModuleResolver(ModuleResolver moduleResolver) {
        this.moduleResolver = moduleResolver;
    }

    @Override
    public Match createMatch(Match match) {
        Objects.requireNonNull(match, "match must not be null");
        log.info("Creating match with id: {}", match.id());

        // Validate all enabled modules exist (if module resolver is set)
        if (moduleResolver != null && match.enabledModules() != null) {
            for (String module : match.enabledModules()) {
                if (!moduleResolver.hasModule(module)) {
                    log.warn("Module not found: {}", module);
                    throw new EntityNotFoundException(String.format("Module %s not found.", module));
                }
            }
        }

        return matchRepository.save(match);
    }

    @Override
    public void deleteMatch(long matchId) {
        log.info("Deleting match: {}", matchId);
        if (!matchRepository.existsById(matchId)) {
            throw new EntityNotFoundException(String.format("Match %d not found.", matchId));
        }
        matchRepository.deleteById(matchId);
    }

    @Override
    public Optional<Match> getMatch(long matchId) {
        return matchRepository.findById(matchId);
    }

    @Override
    public Match getMatchOrThrow(long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Match %d not found.", matchId)));
    }

    @Override
    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }

    @Override
    public boolean matchExists(long matchId) {
        return matchRepository.existsById(matchId);
    }
}
