package io.github.no1qq.uagc.engine.checks.combat;

import io.github.no1qq.uagc.engine.check.MovementCheckHarness;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.support.SnapshotBuilder;
import io.github.no1qq.uagc.support.Surfaces;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VelocityCheckTest {

    private static final double KNOCKBACK = 0.5D;

    private MovementCheckHarness<VelocityCheck.State> harness() {
        return new MovementCheckHarness<>(new VelocityCheck());
    }

    private Vec3 knockbackBurst(MovementCheckHarness<VelocityCheck.State> harness,
                                Vec3 start, long startTick, double taken) {
        return knockbackBurst(harness, start, startTick, taken, 20);
    }

    private Vec3 knockbackBurst(MovementCheckHarness<VelocityCheck.State> harness,
                                Vec3 start, long startTick, double taken, int ticks) {
        harness.player().velocity().record(new Vec3(KNOCKBACK, 0.4D, 0.0D), startTick, "damage");
        Vec3 position = start;
        double speed = KNOCKBACK * taken;
        for (int offset = 0; offset < ticks; offset++) {
            Vec3 next = new Vec3(position.x() + speed, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(startTick + offset)
                    .from(position)
                    .to(next)
                    .surface(Surfaces.ground())
                    .build());
            position = next;
            speed *= 0.91D;
        }
        return position;
    }

    @Test
    void absorbedKnockbackIsCaughtWhenHitsComeQuickly() {
        MovementCheckHarness<VelocityCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        for (int round = 0; round < 4; round++) {
            position = knockbackBurst(harness, position, 1L + round * 5L, 0.0D, 5);
        }
        assertTrue(harness.flagged(), "a hit every five ticks must still be judged, that is normal combat");
    }

    @Test
    void takingTheKnockbackIsNeverFlaggedWhenHitsComeQuickly() {
        MovementCheckHarness<VelocityCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        for (int round = 0; round < 8; round++) {
            position = knockbackBurst(harness, position, 1L + round * 5L, 1.0D, 5);
        }
        assertFalse(harness.flagged(), "quick hits that are all taken carry no evidence");
    }

    @Test
    void takingTheFullKnockbackIsNeverFlagged() {
        MovementCheckHarness<VelocityCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        for (int round = 0; round < 6; round++) {
            position = knockbackBurst(harness, position, 1L + round * 20L, 1.0D);
        }
        assertFalse(harness.flagged(), "a player who travels the knockback must never be flagged");
    }

    @Test
    void takingMostOfTheKnockbackIsNeverFlagged() {
        MovementCheckHarness<VelocityCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        for (int round = 0; round < 6; round++) {
            position = knockbackBurst(harness, position, 1L + round * 20L, 0.8D);
        }
        assertFalse(harness.flagged(), "input and friction eat some of it, that is normal");
    }

    @Test
    void reducedKnockbackIsFlagged() {
        MovementCheckHarness<VelocityCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        for (int round = 0; round < 4; round++) {
            position = knockbackBurst(harness, position, 1L + round * 20L, 0.2D);
        }
        assertTrue(harness.flagged(), "a fifth of the knockback over several hits is the whole cheat");
    }

    @Test
    void aSingleAbsorbedHitIsNeverFlagged() {
        MovementCheckHarness<VelocityCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        position = knockbackBurst(harness, position, 1L, 0.0D);
        knockbackBurst(harness, position, 21L, 1.0D);
        assertFalse(harness.flagged(), "one hit that went nowhere can be a wall or a ladder");
    }

    @Test
    void hittingAWallIsNeverFlagged() {
        MovementCheckHarness<VelocityCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        for (int round = 0; round < 6; round++) {
            long startTick = 1L + round * 20L;
            harness.player().velocity().record(new Vec3(KNOCKBACK, 0.4D, 0.0D), startTick, "damage");
            for (int offset = 0; offset < 20; offset++) {
                Vec3 next = new Vec3(position.x(), position.y(), position.z());
                harness.feed(SnapshotBuilder.create()
                        .tick(startTick + offset)
                        .from(position)
                        .to(next)
                        .surface(Surfaces.wall())
                        .build());
                position = next;
            }
        }
        assertFalse(harness.flagged(), "a player pressed against a wall cannot travel the knockback");
    }

    @Test
    void smallNudgesAreNeverJudged() {
        MovementCheckHarness<VelocityCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        for (int round = 0; round < 6; round++) {
            long startTick = 1L + round * 20L;
            harness.player().velocity().record(new Vec3(0.05D, 0.0D, 0.0D), startTick, "push");
            for (int offset = 0; offset < 20; offset++) {
                Vec3 next = new Vec3(position.x(), position.y(), position.z());
                harness.feed(SnapshotBuilder.create()
                        .tick(startTick + offset)
                        .from(position)
                        .to(next)
                        .surface(Surfaces.ground())
                        .build());
                position = next;
            }
        }
        assertFalse(harness.flagged(), "a tiny push carries no evidence either way");
    }
}
