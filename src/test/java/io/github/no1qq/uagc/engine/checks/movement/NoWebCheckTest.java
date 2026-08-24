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
    private static final double WEB_PER_TICK = 0.0325D;
    private static final double WEB_ENTRY_PER_TICK = 0.0708D;
    private static final double WEB_DESCENT_PER_TICK = 0.004D;

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

    private void fall(MovementCheckHarness<NoWebCheck.State> harness, double perTick, int ticks) {
        Vec3 position = new Vec3(0.0D, 80.0D, 0.0D);
        for (int tick = 1; tick <= ticks; tick++) {
            Vec3 next = new Vec3(position.x(), position.y() - perTick, position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .clientOnGround(false)
                    .surface(Surfaces.cobwebInAir(20.0D))
                    .build());
            position = next;
        }
    }

    @Test
    void crawlingThroughAWebIsNeverFlagged() {
        MovementCheckHarness<NoWebCheck.State> harness = harness();
        run(harness, WEB_PER_TICK, 60, Surfaces.cobweb());
        assertFalse(harness.flagged(), "a web zeroes the carried motion, so input alone is what moves you");
    }

    @Test
    void sprintingThroughAWebIsFlagged() {
        MovementCheckHarness<NoWebCheck.State> harness = harness();
        run(harness, SPRINT_PER_TICK, 20, Surfaces.cobweb());
        assertTrue(harness.flagged(), "full sprint speed inside a cobweb is the whole cheat");
    }

    @Test
    void aQuarterOfSprintSpeedIsStillFlagged() {
        MovementCheckHarness<NoWebCheck.State> harness = harness();
        run(harness, 0.07D, 20, Surfaces.cobweb());
        assertTrue(harness.flagged(), "a low no web multiplier is still twice what a web sustains");
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
        for (int tick = 1; tick <= 30; tick++) {
            double speed = switch (tick) {
                case 1 -> SPRINT_PER_TICK;
                case 2 -> WEB_ENTRY_PER_TICK;
                default -> WEB_PER_TICK;
            };
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
        assertFalse(harness.flagged(),
                "the tick you run into a web is unclamped and the one after still carries a quarter of it");
    }

    @Test
    void knockbackIntoAWebIsNeverFlagged() {
        MovementCheckHarness<NoWebCheck.State> harness = harness();
        harness.player().velocity().record(new Vec3(0.6D, 0.0D, 0.0D), 1L, "damage");
        run(harness, SPRINT_PER_TICK, 15, Surfaces.cobweb());
        assertFalse(harness.flagged(), "being thrown into a web is not no web");
    }

    @Test
    void fallingThroughAWebIsFlagged() {
        MovementCheckHarness<NoWebCheck.State> harness = harness();
        fall(harness, 0.35D, 4);
        assertTrue(harness.flagged(), "a cobweb clamps vertical motion to a twentieth, nobody falls through one");
    }

    @Test
    void aSlowSinkThroughAWebIsAlsoFlagged() {
        MovementCheckHarness<NoWebCheck.State> harness = harness();
        fall(harness, 0.06D, 6);
        assertTrue(harness.flagged(), "even a gentle no web descent is fifteen times the clamped rate");
    }

    @Test
    void sinkingThroughAWebAtTheClampedRateIsNeverFlagged() {
        MovementCheckHarness<NoWebCheck.State> harness = harness();
        fall(harness, WEB_DESCENT_PER_TICK, 60);
        assertFalse(harness.flagged(), "the vanilla crawl down a web is a few thousandths of a block a tick");
    }

    @Test
    void theFirstTickOfContactIsNeverJudged() {
        MovementCheckHarness<NoWebCheck.State> harness = harness();
        fall(harness, 0.78D, 1);
        assertFalse(harness.flagged(), "the tick a falling player touches the web was computed outside it");
    }

    @Test
    void theTickAfterTheWebIsStillClamped() {
        MovementCheckHarness<NoWebCheck.State> harness = harness();
        Vec3 position = new Vec3(0.0D, 80.0D, 0.0D);
        for (int tick = 1; tick <= 4; tick++) {
            Vec3 next = new Vec3(position.x(), position.y() - 0.4D, position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .clientOnGround(false)
                    .surface(tick == 1 ? Surfaces.cobwebInAir(20.0D) : Surfaces.air(20.0D))
                    .build());
            position = next;
        }
        assertFalse(harness.flagged(),
                "one clamped tick after leaving the web is not enough on its own");
    }
}
