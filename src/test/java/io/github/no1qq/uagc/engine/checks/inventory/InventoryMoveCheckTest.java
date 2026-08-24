package io.github.no1qq.uagc.engine.checks.inventory;

import io.github.no1qq.uagc.engine.check.EventCheckHarness;
import io.github.no1qq.uagc.engine.check.event.InventoryClickCheckEvent;
import io.github.no1qq.uagc.engine.movement.SurfaceSample;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.support.SnapshotBuilder;
import io.github.no1qq.uagc.support.Surfaces;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryMoveCheckTest {

    private EventCheckHarness<InventoryClickCheckEvent, InventoryMoveCheck.State> harness() {
        return new EventCheckHarness<>(new InventoryMoveCheck());
    }

    private Vec3 walk(EventCheckHarness<InventoryClickCheckEvent, InventoryMoveCheck.State> harness,
                      Vec3 start, long fromTick, int ticks, double perTick, SurfaceSample surface) {
        Vec3 position = start;
        for (int index = 0; index < ticks; index++) {
            Vec3 next = new Vec3(position.x() + perTick, position.y(), position.z());
            harness.player().movement().update(SnapshotBuilder.create()
                    .tick(fromTick + index)
                    .from(position)
                    .to(next)
                    .surface(surface)
                    .build());
            position = next;
        }
        return position;
    }

    private InventoryClickCheckEvent click(long tick) {
        return new InventoryClickCheckEvent(tick, 1_700_000_000_000L + tick * 50L, 13,
                "PICKUP_ALL", "CHEST", false, false, 20);
    }

    @Test
    void clickingWhileSprintingAcrossSeveralTicksIsFlagged() {
        EventCheckHarness<InventoryClickCheckEvent, InventoryMoveCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        long tick = 1L;
        for (int round = 0; round < 5; round++) {
            position = walk(harness, position, tick, 3, 0.2806D, Surfaces.ground());
            tick += 3L;
            harness.feed(click(tick - 1L));
        }
        assertTrue(harness.flagged(), "an open inventory screen releases the movement keys, this player never slowed");
    }

    @Test
    void clickingWhileStandingStillIsNeverFlagged() {
        EventCheckHarness<InventoryClickCheckEvent, InventoryMoveCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        long tick = 1L;
        for (int round = 0; round < 8; round++) {
            position = walk(harness, position, tick, 3, 0.0D, Surfaces.ground());
            tick += 3L;
            harness.feed(click(tick - 1L));
        }
        assertFalse(harness.flagged(), "sorting a chest while standing still is the normal case");
    }

    @Test
    void coastingToAStopIsNeverFlagged() {
        EventCheckHarness<InventoryClickCheckEvent, InventoryMoveCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        long tick = 1L;
        double speed = 0.2806D;
        for (int round = 0; round < 8; round++) {
            for (int index = 0; index < 3; index++) {
                speed *= 0.546D;
                position = walk(harness, position, tick, 1, speed, Surfaces.ground());
                tick++;
            }
            harness.feed(click(tick - 1L));
        }
        assertFalse(harness.flagged(),
                "a player who opened the screen at a sprint still carries momentum for a moment");
    }

    @Test
    void iceIsNeverJudged() {
        EventCheckHarness<InventoryClickCheckEvent, InventoryMoveCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        long tick = 1L;
        for (int round = 0; round < 6; round++) {
            position = walk(harness, position, tick, 3, 0.2806D, Surfaces.ice());
            tick += 3L;
            harness.feed(click(tick - 1L));
        }
        assertFalse(harness.flagged(), "ice keeps a player sliding long after the keys are released");
    }

    @Test
    void knockbackIsNeverJudged() {
        EventCheckHarness<InventoryClickCheckEvent, InventoryMoveCheck.State> harness = harness();
        harness.player().velocity().record(new Vec3(0.6D, 0.0D, 0.0D), 1L, "damage");
        Vec3 position = Vec3.ZERO;
        long tick = 1L;
        for (int round = 0; round < 6; round++) {
            position = walk(harness, position, tick, 3, 0.2806D, Surfaces.ground());
            tick += 3L;
            harness.feed(click(tick - 1L));
        }
        assertFalse(harness.flagged(), "being knocked around with a screen open is not the player walking");
    }

    @Test
    void aBurstOfClicksInOneMomentIsNeverFlagged() {
        EventCheckHarness<InventoryClickCheckEvent, InventoryMoveCheck.State> harness = harness();
        walk(harness, Vec3.ZERO, 1L, 4, 0.2806D, Surfaces.ground());
        for (int index = 0; index < 6; index++) {
            harness.feed(click(5L));
        }
        assertFalse(harness.flagged(), "six clicks in one moment cannot outlive momentum, the span is what matters");
    }
}
