package ca.samanthaireland.lightning.engine.ext.ai;

import ca.samanthaireland.game.domain.AI;
import ca.samanthaireland.game.domain.AIContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Sample AI that counts ticks.
 *
 * <p>This is a simple demonstration of the AI interface.
 * It tracks the number of ticks and logs progress every 100 ticks.
 *
 * <p>Use this as a template for creating your own AIs.
 */
@Slf4j
public class TickCounterAI implements AI {

    private final AIContext context;
    private long tickCount = 0;

    public TickCounterAI(AIContext context) {
        this.context = context;
        log.info("TickCounterAI initialized for match {}", context.getMatchId());
    }

    @Override
    public void onTick() {
        tickCount++;

        if (tickCount % 100 == 0) {
            log.info("Match {} - TickCounter: {} ticks processed",
                    context.getMatchId(), tickCount);
        }
    }

    /**
     * Get the current tick count.
     *
     * @return the number of ticks processed
     */
    public long getTickCount() {
        return tickCount;
    }
}
