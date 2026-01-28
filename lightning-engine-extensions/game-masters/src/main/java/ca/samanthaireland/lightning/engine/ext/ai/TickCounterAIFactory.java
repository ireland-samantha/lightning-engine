package ca.samanthaireland.lightning.engine.ext.ai;

import ca.samanthaireland.game.domain.AI;
import ca.samanthaireland.game.domain.AIContext;
import ca.samanthaireland.game.backend.installation.AIFactory;

/**
 * Factory for creating TickCounterAI instances.
 *
 * <p>This is a sample AI that demonstrates the AI interface
 * by simply counting ticks and logging the count periodically.
 */
public class TickCounterAIFactory implements AIFactory {

    @Override
    public AI create(AIContext context) {
        return new TickCounterAI(context);
    }

    @Override
    public String getName() {
        return "TickCounter";
    }
}
