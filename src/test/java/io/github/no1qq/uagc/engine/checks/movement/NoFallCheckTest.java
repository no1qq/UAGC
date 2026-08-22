package io.github.no1qq.uagc.engine.checks.movement;

import io.github.no1qq.uagc.engine.check.MovementCheckHarness;
import io.github.no1qq.uagc.engine.movement.AttributeSample;
import io.github.no1qq.uagc.engine.movement.MovementPredictor;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.support.SnapshotBuilder;
import io.github.no1qq.uagc.support.Surfaces;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoFallCheckTest {

    private MovementCheckHarness<NoFallCheck.State> harness() {
        return new MovementCheckHarness<>(new NoFallCheck());
    }

    private void fall(MovementCheckHarness<NoFallCheck.State> harness, boolean reportFallDistance) {
        Vec3 position = new Vec3(0.0D, 200.0D, 0.0D);
        double delta = 0.0D;
        double accumulated = 0.0D;
        for (int tick = 1; tick <= 30; tick++) {
            delta = MovementPredictor.predictVerticalDelta(delta, AttributeSample.VANILLA_GRAVITY, false);
            Vec3 next = new Vec3(position.x(), position.y() + delta, position.z());
            accumulated += -delta;
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .clientOnGround(false)
                    .fallDistance(reportFallDistance ? accumulated : 0.0D)
                    .surface(Surfaces.air(30.0D))
                    .build());
            position = next;
        }
    }

    @Test
    void anHonestFallIsNeverFlagged() {
        MovementCheckHarness<NoFallCheck.State> harness = harness();
        fall(harness, true);
        assertFalse(harness.flagged(), "a fall the server accounted for must never be flagged");
    }

    @Test
    void aFallWithoutAccumulatedDistanceIsFlagged() {
        MovementCheckHarness<NoFallCheck.State> harness = harness();
        fall(harness, false);
        assertTrue(harness.flagged(), "descending without accumulating fall distance must be detected");
    }

    @Test
    void shortDropsAreIgnored() {
        MovementCheckHarness<NoFallCheck.State> harness = harness();
        Vec3 position = new Vec3(0.0D, 70.0D, 0.0D);
        for (int tick = 1; tick <= 6; tick++) {
            Vec3 next = new Vec3(position.x(), position.y() - 0.3D, position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .clientOnGround(false)
                    .fallDistance(0.0D)
                    .surface(Surfaces.air(3.0D))
                    .build());
            position = next;
        }
        assertFalse(harness.flagged(), "a short hop is below the measurable drop threshold");
    }

    @Test
    void sustainedServerAppliedVelocitySuspendsTheCheck() {
        MovementCheckHarness<NoFallCheck.State> harness = harness();
        Vec3 position = new Vec3(0.0D, 200.0D, 0.0D);
        double delta = 0.0D;
        for (int tick = 1; tick <= 30; tick++) {
            harness.player().velocity().record(new Vec3(0.0D, 1.2D, 0.0D), tick, "plugin");
            delta = MovementPredictor.predictVerticalDelta(delta, AttributeSample.VANILLA_GRAVITY, false);
            Vec3 next = new Vec3(position.x(), position.y() + delta, position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .clientOnGround(false)
                    .fallDistance(0.0D)
                    .surface(Surfaces.air(30.0D))
                    .build());
            position = next;
        }
        assertFalse(harness.flagged(), "movement the server keeps driving itself is not evidence of NoFall");
    }

    @Test
    void theVelocityGraceWindowExpires() {
        MovementCheckHarness<NoFallCheck.State> harness = harness();
        harness.player().velocity().record(new Vec3(0.0D, 1.2D, 0.0D), 1L, "explosion");
        fall(harness, false);
        assertTrue(harness.flagged(), "a long fall well after an impulse is measured normally again");
    }
}
