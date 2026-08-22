package io.github.no1qq.uagc.engine.checks.interaction;

import io.github.no1qq.uagc.engine.check.EventCheckHarness;
import io.github.no1qq.uagc.engine.check.event.BlockBreakCheckEvent;
import io.github.no1qq.uagc.engine.movement.AttributeSample;
import io.github.no1qq.uagc.engine.movement.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastBreakCheckTest {

    private EventCheckHarness<BlockBreakCheckEvent, Void> harness() {
        return new EventCheckHarness<>(new FastBreakCheck());
    }

    private BlockBreakCheckEvent breakEvent(long startTick, long finishTick, double destroySpeed,
                                            boolean creative, int ping) {
        return new BlockBreakCheckEvent(finishTick, 1_700_000_000_000L + finishTick * 50L,
                new Vec3(0.0D, 1.62D, 0.0D), new Vec3(1.5D, 0.5D, 0.5D), "stone",
                destroySpeed, startTick, destroySpeed >= 1.0D, creative,
                AttributeSample.vanilla(), ping);
    }

    @Test
    void breakingAtTheExpectedSpeedIsNeverFlagged() {
        EventCheckHarness<BlockBreakCheckEvent, Void> harness = harness();
        harness.feed(breakEvent(0L, 25L, 0.04D, false, 30));
        assertFalse(harness.flagged(), "breaking a block at the expected pace must never be flagged");
    }

    @Test
    void breakingFarTooFastIsFlagged() {
        EventCheckHarness<BlockBreakCheckEvent, Void> harness = harness();
        harness.feed(breakEvent(0L, 3L, 0.04D, false, 30));
        assertTrue(harness.flagged(), "finishing a twenty five tick block in three ticks must be detected");
    }

    @Test
    void creativeModeIsSkipped() {
        EventCheckHarness<BlockBreakCheckEvent, Void> harness = harness();
        harness.feed(breakEvent(0L, 1L, 0.04D, true, 30));
        assertFalse(harness.flagged(), "creative mode breaks blocks instantly by design");
    }

    @Test
    void instantBreakBlocksAreSkipped() {
        EventCheckHarness<BlockBreakCheckEvent, Void> harness = harness();
        harness.feed(breakEvent(0L, 0L, 1.0D, false, 30));
        assertFalse(harness.flagged(), "blocks the tool destroys in one tick are not evidence of cheating");
    }

    @Test
    void missingDamageStartIsSkipped() {
        EventCheckHarness<BlockBreakCheckEvent, Void> harness = harness();
        harness.feed(breakEvent(Long.MIN_VALUE, 5L, 0.04D, false, 30));
        assertFalse(harness.flagged(), "without a recorded start the duration cannot be measured");
    }

    @Test
    void highLatencyWidensTheAllowance() {
        EventCheckHarness<BlockBreakCheckEvent, Void> harness = harness();
        harness.feed(breakEvent(0L, 20L, 0.04D, false, 300));
        assertFalse(harness.flagged(), "latency shifts the observed timing and must be tolerated");
    }
}
