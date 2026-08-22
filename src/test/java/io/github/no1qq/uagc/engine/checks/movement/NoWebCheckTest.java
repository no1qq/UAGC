package io.github.no1qq.uagc.engine.checks.movement;

import io.github.no1qq.uagc.engine.check.MovementCheckHarness;
import io.github.no1qq.uagc.engine.movement.SurfaceSample;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.support.SnapshotBuilder;
import io.github.no1qq.uagc.support.Surfaces;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoWebCheckTest {

    private static final double SPRINT_PER_TICK = 0.2806D;
    private static final double WEB_PER_TICK = 0.0716D;

    private MovementCheckHarness<NoWebCheck.State> harness() {
        return new MovementCheckHarness<>(new NoWebCheck());
    }

    private void run(MovementCheckHarness<NoWebCheck.State> harness,
                     double perTick, int ticks, SurfaceSample surface) {
        Vec3 position = Vec3.ZERO;
        for (int tick = 1; tick <= ticks; tick++) {
            Vec3 next = new Vec3(position.x() + perTick, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .sprinting(true)
                    .surface(surface)
                    .build());
            position = next;
        }
    }

    @Test
    void crawlingThroughAWebIsNeverFlagged() {
        MovementCheckHarness<NoWebCheck.State> harness = harness();
        run(harness, WEB_PER_TICK, 60, Surfaces.cobweb());
        assertFalse(harness.flagged(), "a quarter of normal speed is what a cobweb permits");
    }

    @Test
    void sprintingThroughAWebIsFlagged() {
        MovementCheckHarness<NoWebCheck.State> harness = harness();
        run(harness, SPRINT_PER_TICK, 60, Surfaces.cobweb());
        assertTrue(harness.flagged(), "full sprint speed inside a cobweb is the whole cheat");
    }

    @Test
    void halfSpeedThroughAWebIsFlagged() {
        MovementCheckHarness<NoWebCheck.State> harness = harness();
        run(harness, 0.14D, 60, Surfaces.cobweb());
        assertTrue(harness.flagged(), "even a partial no web value is well past the clamp");
    }

    @Test
    void sprintingOnNormalGroundIsNeverFlagged() {
        MovementCheckHarness<NoWebCheck.State> harness = harness();
        run(harness, SPRINT_PER_TICK, 60, Surfaces.ground());
        assertFalse(harness.flagged(), "this check must never touch a player outside a web");
    }

    @Test
    void enteringAWebAtSprintSpeedIsNeverFlagged() {
        MovementCheckHarness<NoWebCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        double speed = SPRINT_PER_TICK;
        for (int tick = 1; tick <= 30; tick++) {
            speed = Math.max(WEB_PER_TICK, speed * 0.546D + 0.0325D);
            Vec3 next = new Vec3(position.x() + speed, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .sprinting(true)
                    .surface(Surfaces.cobweb())
                    .build());
            position = next;
        }
        assertFalse(harness.flagged(), "running into a web bleeds speed over a few ticks first");
    }

    @Test
    void knockbackIntoAWebIsNeverFlagged() {
        MovementCheckHarness<NoWebCheck.State> harness = harness();
        harness.player().velocity().record(new Vec3(0.6D, 0.0D, 0.0D), 1L, "damage");
        run(harness, SPRINT_PER_TICK, 15, Surfaces.cobweb());
        assertFalse(harness.flagged(), "being thrown into a web is not no web");
    }
}
