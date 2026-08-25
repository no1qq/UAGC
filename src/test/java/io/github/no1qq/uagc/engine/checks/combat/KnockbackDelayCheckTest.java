package io.github.no1qq.uagc.engine.checks.combat;

import io.github.no1qq.uagc.engine.check.MovementCheckHarness;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.support.SnapshotBuilder;
import io.github.no1qq.uagc.support.Surfaces;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnockbackDelayCheckTest {

    private static final double KNOCKBACK = 0.5D;

    private MovementCheckHarness<KnockbackDelayCheck.State> harness() {
        return new MovementCheckHarness<>(new KnockbackDelayCheck());
    }

    private Vec3 hit(MovementCheckHarness<KnockbackDelayCheck.State> harness,
                     Vec3 start, long startTick, int heldTicks, int ping) {
        harness.player().velocity().record(new Vec3(KNOCKBACK, 0.4D, 0.0D), startTick, "damage");
        Vec3 position = start;
        double speed = 0.0D;
        for (int offset = 0; offset < 25; offset++) {
            if (offset == heldTicks) {
                speed = KNOCKBACK;
            }
            Vec3 next = new Vec3(position.x() + speed, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(startTick + offset)
                    .from(position)
                    .to(next)
                    .ping(ping)
                    .surface(Surfaces.ground())
                    .build());
            position = next;
            if (offset >= heldTicks) {
                speed *= 0.91D;
            }
        }
        return position;
    }

    @Test
    void knockbackTakenImmediatelyIsNeverFlagged() {
        MovementCheckHarness<KnockbackDelayCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        for (int round = 0; round < 6; round++) {
            position = hit(harness, position, 1L + round * 25L, 1, 0);
        }
        assertFalse(harness.flagged(), "knockback that lands on the next tick is vanilla");
    }

    @Test
    void latencyIsNotMistakenForADelay() {
        MovementCheckHarness<KnockbackDelayCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        for (int round = 0; round < 6; round++) {
            position = hit(harness, position, 1L + round * 25L, 6, 250);
        }
        assertFalse(harness.flagged(), "a 250 ms round trip explains six ticks on its own");
    }

    @Test
    void heldBackKnockbackIsFlagged() {
        MovementCheckHarness<KnockbackDelayCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        for (int round = 0; round < 3; round++) {
            position = hit(harness, position, 1L + round * 25L, 6, 0);
        }
        assertTrue(harness.flagged(), "three hundred milliseconds of absorbed timing at no ping is the module");
    }

    @Test
    void aDelayHiddenBetweenCleanHitsIsStillFlagged() {
        MovementCheckHarness<KnockbackDelayCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        for (int round = 0; round < 6; round++) {
            position = hit(harness, position, 1L + round * 25L, round % 2 == 0 ? 6 : 1, 0);
        }
        assertTrue(harness.flagged(), "taking every second hit on time must not wipe the evidence");
    }

    @Test
    void oneLateHitIsNeverFlagged() {
        MovementCheckHarness<KnockbackDelayCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        position = hit(harness, position, 1L, 6, 0);
        hit(harness, position, 26L, 1, 0);
        assertFalse(harness.flagged(), "a single late response can be a dropped packet");
    }
}
