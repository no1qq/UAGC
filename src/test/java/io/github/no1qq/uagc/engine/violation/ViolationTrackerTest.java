package io.github.no1qq.uagc.engine.violation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViolationTrackerTest {

    private static final double DECAY = 0.02D;
    private static final double MAX = 200.0D;

    @Test
    void accumulatesViolationsScaledByConfidence() {
        ViolationTracker tracker = new ViolationTracker("speed");
        tracker.add(1.0D, 1.0D, 0L, DECAY, MAX);
        tracker.add(1.0D, 1.0D, 1L, DECAY, MAX);
        assertEquals(1.98D, tracker.rawLevel(), 1.0E-9D);
        assertEquals(2, tracker.flagCount());
    }

    @Test
    void decaysTowardZeroOverTime() {
        ViolationTracker tracker = new ViolationTracker("speed");
        tracker.add(4.0D, 1.0D, 0L, DECAY, MAX);
        assertEquals(4.0D, tracker.rawLevel(), 1.0E-9D);
        assertEquals(2.0D, tracker.current(100L, DECAY), 1.0E-9D);
        assertEquals(0.0D, tracker.current(1000L, DECAY), 1.0E-9D);
    }

    @Test
    void neverDecaysBelowZero() {
        ViolationTracker tracker = new ViolationTracker("speed");
        tracker.add(1.0D, 1.0D, 0L, DECAY, MAX);
        assertEquals(0.0D, tracker.current(100_000L, DECAY), 1.0E-9D);
    }

    @Test
    void respectsTheConfiguredCeiling() {
        ViolationTracker tracker = new ViolationTracker("speed");
        for (int tick = 0; tick < 200; tick++) {
            tracker.add(5.0D, 1.0D, tick, 0.0D, 20.0D);
        }
        assertEquals(20.0D, tracker.rawLevel(), 1.0E-9D);
        assertEquals(20.0D, tracker.peakLevel(), 1.0E-9D);
    }

    @Test
    void tracksStreaksOfCloselySpacedFlags() {
        ViolationTracker tracker = new ViolationTracker("speed");
        tracker.add(1.0D, 1.0D, 0L, DECAY, MAX);
        tracker.add(1.0D, 1.0D, 5L, DECAY, MAX);
        tracker.add(1.0D, 1.0D, 10L, DECAY, MAX);
        assertEquals(3, tracker.streak());

        tracker.add(1.0D, 1.0D, 500L, DECAY, MAX);
        assertEquals(1, tracker.streak());
    }

    @Test
    void resetClearsLevelButKeepsHistory() {
        ViolationTracker tracker = new ViolationTracker("speed");
        tracker.add(6.0D, 0.8D, 0L, DECAY, MAX);
        tracker.reset();
        assertEquals(0.0D, tracker.rawLevel(), 1.0E-9D);
        assertTrue(tracker.hasEverFlagged());
        assertEquals(6.0D, tracker.peakLevel(), 1.0E-9D);

        tracker.resetCompletely();
        assertFalse(tracker.hasEverFlagged());
        assertEquals(0.0D, tracker.peakLevel(), 1.0E-9D);
    }

    @Test
    void lowConfidenceProducesSmallerIncrements() {
        ViolationTracker weak = new ViolationTracker("a");
        ViolationTracker strong = new ViolationTracker("b");
        weak.add(1.0D * 0.3D, 0.3D, 0L, DECAY, MAX);
        strong.add(1.0D * 0.9D, 0.9D, 0L, DECAY, MAX);
        assertTrue(strong.rawLevel() > weak.rawLevel());
    }
}
