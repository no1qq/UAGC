package io.github.no1qq.uagc.engine.checks.inventory;

import io.github.no1qq.uagc.engine.check.EventCheckHarness;
import io.github.no1qq.uagc.engine.check.event.HeldSlotCheckEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SilentSwitchCheckTest {

    private EventCheckHarness<HeldSlotCheckEvent, SilentSwitchCheck.State> harness() {
        return new EventCheckHarness<>(new SilentSwitchCheck());
    }

    private HeldSlotCheckEvent change(long tick, int from, int to) {
        return new HeldSlotCheckEvent(tick, 1_700_000_000_000L + tick * 50L, from, to, 20);
    }

    @Test
    void twoChangesInOneTickAreFlagged() {
        EventCheckHarness<HeldSlotCheckEvent, SilentSwitchCheck.State> harness = harness();
        harness.feed(change(10L, 0, 3));
        harness.feed(change(10L, 3, 0));
        harness.feed(change(30L, 0, 3));
        harness.feed(change(30L, 3, 0));
        assertTrue(harness.flagged(), "a vanilla client syncs the held slot at most once a tick");
    }

    @Test
    void normalScrollingIsNeverFlagged() {
        EventCheckHarness<HeldSlotCheckEvent, SilentSwitchCheck.State> harness = harness();
        long tick = 1L;
        for (int index = 0; index < 20; index++) {
            harness.feed(change(tick, index % 9, (index + 1) % 9));
            tick += 4L;
        }
        assertFalse(harness.flagged(), "scrolling through the hotbar is not a silent switch");
    }

    @Test
    void switchingBackAndForthWithoutActingIsNeverFlagged() {
        EventCheckHarness<HeldSlotCheckEvent, SilentSwitchCheck.State> harness = harness();
        long tick = 1L;
        for (int index = 0; index < 12; index++) {
            harness.feed(change(tick, 0, 4));
            harness.feed(change(tick + 1L, 4, 0));
            tick += 6L;
        }
        assertFalse(harness.flagged(), "flicking between two slots proves nothing without an action in between");
    }

    @Test
    void returningRightAfterPlacingIsFlagged() {
        EventCheckHarness<HeldSlotCheckEvent, SilentSwitchCheck.State> harness = harness();
        long tick = 1L;
        for (int index = 0; index < 5; index++) {
            harness.feed(change(tick, 0, 4));
            harness.player().interaction().recordPlace(tick);
            harness.feed(change(tick + 1L, 4, 0));
            tick += 6L;
        }
        assertTrue(harness.flagged(), "swap in, place, swap straight back out is the whole module");
    }
}
