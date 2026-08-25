package io.github.no1qq.uagc.engine.checks.inventory;

import io.github.no1qq.uagc.engine.check.MovementCheckHarness;
import io.github.no1qq.uagc.engine.movement.SurfaceSample;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.support.SnapshotBuilder;
import io.github.no1qq.uagc.support.Surfaces;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenMoveCheckTest {

    private MovementCheckHarness<ScreenMoveCheck.State> harness() {
        return new MovementCheckHarness<>(new ScreenMoveCheck());
    }

    private Vec3 walk(MovementCheckHarness<ScreenMoveCheck.State> harness,
                      Vec3 start, long fromTick, int ticks, double perTick, SurfaceSample surface) {
        Vec3 position = start;
        for (int index = 0; index < ticks; index++) {
            Vec3 next = new Vec3(position.x() + perTick, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(fromTick + index)
                    .from(position)
                    .to(next)
                    .surface(surface)
                    .build());
            position = next;
        }
        return position;
    }

    @Test
    void walkingWithAScreenOpenIsFlaggedWithoutASingleClick() {
        MovementCheckHarness<ScreenMoveCheck.State> harness = harness();
        harness.player().interaction().openScreen(1L);
        walk(harness, Vec3.ZERO, 1L, 20, 0.21D, Surfaces.ground());
        assertTrue(harness.flagged(), "queued clicks change nothing, the walking is the evidence");
    }

    @Test
    void standingStillWithAScreenOpenIsNeverFlagged() {
        MovementCheckHarness<ScreenMoveCheck.State> harness = harness();
        harness.player().interaction().openScreen(1L);
        walk(harness, Vec3.ZERO, 1L, 20, 0.0D, Surfaces.ground());
        assertFalse(harness.flagged(), "a player reading a chest is standing still, that is the normal case");
    }

    @Test
    void walkingWithNoScreenOpenIsNeverFlagged() {
        MovementCheckHarness<ScreenMoveCheck.State> harness = harness();
        walk(harness, Vec3.ZERO, 1L, 20, 0.28D, Surfaces.ground());
        assertFalse(harness.flagged(), "this check has nothing to say about a player who is just walking");
    }

    @Test
    void coastingToAStopIsNeverFlagged() {
        MovementCheckHarness<ScreenMoveCheck.State> harness = harness();
        harness.player().interaction().openScreen(1L);
        Vec3 position = Vec3.ZERO;
        double speed = 0.2806D;
        for (int index = 0; index < 20; index++) {
            speed *= 0.546D;
            position = walk(harness, position, 1L + index, 1, speed, Surfaces.ground());
        }
        assertFalse(harness.flagged(), "opening a chest at a sprint leaves momentum that friction eats");
    }

    @Test
    void iceIsNeverJudged() {
        MovementCheckHarness<ScreenMoveCheck.State> harness = harness();
        harness.player().interaction().openScreen(1L);
        walk(harness, Vec3.ZERO, 1L, 20, 0.28D, Surfaces.ice());
        assertFalse(harness.flagged(), "ice carries a player who is touching nothing");
    }

    @Test
    void knockbackIsNeverJudged() {
        MovementCheckHarness<ScreenMoveCheck.State> harness = harness();
        harness.player().interaction().openScreen(1L);
        harness.player().velocity().record(new Vec3(0.6D, 0.0D, 0.0D), 5L, "damage");
        walk(harness, Vec3.ZERO, 1L, 20, 0.28D, Surfaces.ground());
        assertFalse(harness.flagged(), "being thrown around with a screen open is not steering");
    }

    @Test
    void theTickTheScreenOpensIsNeverJudged() {
        MovementCheckHarness<ScreenMoveCheck.State> harness = harness();
        harness.player().interaction().openScreen(1L);
        walk(harness, Vec3.ZERO, 1L, 3, 0.28D, Surfaces.ground());
        assertFalse(harness.flagged(), "the client has not been told to close its keys yet");
    }
}
