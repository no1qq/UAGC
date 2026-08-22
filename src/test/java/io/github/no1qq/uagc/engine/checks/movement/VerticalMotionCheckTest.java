package io.github.no1qq.uagc.engine.checks.movement;

import io.github.no1qq.uagc.engine.check.MovementCheckHarness;
import io.github.no1qq.uagc.engine.movement.AttributeSample;
import io.github.no1qq.uagc.engine.movement.EffectSample;
import io.github.no1qq.uagc.engine.movement.MovementPredictor;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.support.SnapshotBuilder;
import io.github.no1qq.uagc.support.Surfaces;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerticalMotionCheckTest {

    private static final double GRAVITY = AttributeSample.VANILLA_GRAVITY;

    private MovementCheckHarness<VerticalMotionCheck.State> harness() {
        return new MovementCheckHarness<>(new VerticalMotionCheck());
    }

    @Test
    void naturalFreeFallIsNeverFlagged() {
        MovementCheckHarness<VerticalMotionCheck.State> harness = harness();
        Vec3 position = new Vec3(0.0D, 200.0D, 0.0D);
        double delta = 0.0D;
        for (int tick = 1; tick <= 30; tick++) {
            delta = MovementPredictor.predictVerticalDelta(delta, GRAVITY, false);
            Vec3 next = new Vec3(position.x(), position.y() + delta, position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .clientOnGround(false)
                    .surface(Surfaces.air(20.0D))
                    .build());
            position = next;
        }
        assertFalse(harness.flagged(), "a natural fall must never produce a violation");
    }

    @Test
    void jumpArcIsNeverFlagged() {
        MovementCheckHarness<VerticalMotionCheck.State> harness = harness();
        Vec3 position = new Vec3(0.0D, 64.0D, 0.0D);
        double delta = AttributeSample.VANILLA_JUMP_STRENGTH;
        for (int tick = 1; tick <= 12; tick++) {
            Vec3 next = new Vec3(position.x(), position.y() + delta, position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .clientOnGround(false)
                    .surface(Surfaces.air(3.0D))
                    .build());
            position = next;
            delta = MovementPredictor.predictVerticalDelta(delta, GRAVITY, false);
        }
        assertFalse(harness.flagged(), "a vanilla jump arc must never produce a violation");
    }

    @Test
    void hoveringInPlaceIsFlagged() {
        MovementCheckHarness<VerticalMotionCheck.State> harness = harness();
        Vec3 position = new Vec3(0.0D, 120.0D, 0.0D);
        for (int tick = 1; tick <= 10; tick++) {
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(position)
                    .clientOnGround(false)
                    .surface(Surfaces.air(15.0D))
                    .build());
        }
        assertTrue(harness.flagged(), "holding a constant altitude in mid air must be detected");
    }

    @Test
    void slowDescentThatDefiesGravityIsFlagged() {
        MovementCheckHarness<VerticalMotionCheck.State> harness = harness();
        Vec3 position = new Vec3(0.0D, 120.0D, 0.0D);
        for (int tick = 1; tick <= 12; tick++) {
            Vec3 next = new Vec3(position.x(), position.y() - 0.01D, position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .clientOnGround(false)
                    .surface(Surfaces.air(15.0D))
                    .build());
            position = next;
        }
        assertTrue(harness.flagged(), "descending far slower than gravity allows must be detected");
    }

    @Test
    void slowFallingDescentIsNotFlagged() {
        MovementCheckHarness<VerticalMotionCheck.State> harness = harness();
        EffectSample slowFalling = new EffectSample(EffectSample.NONE, EffectSample.NONE,
                EffectSample.NONE, EffectSample.NONE, true, false, false);
        Vec3 position = new Vec3(0.0D, 120.0D, 0.0D);
        double delta = 0.0D;
        for (int tick = 1; tick <= 20; tick++) {
            delta = MovementPredictor.predictVerticalDelta(delta, GRAVITY, true);
            Vec3 next = new Vec3(position.x(), position.y() + delta, position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .clientOnGround(false)
                    .surface(Surfaces.air(15.0D))
                    .effects(slowFalling)
                    .build());
            position = next;
        }
        assertFalse(harness.flagged(), "slow falling changes gravity and must be modelled, not punished");
    }

    @Test
    void hoveringOnAnEntityIsNotFlagged() {
        MovementCheckHarness<VerticalMotionCheck.State> harness = harness()
                .withSupport(new io.github.no1qq.uagc.engine.platform.SupportQuery() {
                    @Override
                    public boolean hasEntitySupportBelow(java.util.UUID playerId) {
                        return true;
                    }

                    @Override
                    public boolean hasNearbyPusher(java.util.UUID playerId) {
                        return false;
                    }
                });
        Vec3 position = new Vec3(0.0D, 120.0D, 0.0D);
        for (int tick = 1; tick <= 12; tick++) {
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(position)
                    .clientOnGround(true)
                    .surface(Surfaces.air(0.9D))
                    .build());
        }
        assertFalse(harness.flagged(), "standing on a boat or another entity must not be treated as flight");
    }

    @Test
    void swimmingIsOutsideTheGravityModel() {
        MovementCheckHarness<VerticalMotionCheck.State> harness = harness();
        Vec3 position = new Vec3(0.0D, 60.0D, 0.0D);
        for (int tick = 1; tick <= 15; tick++) {
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(position)
                    .clientOnGround(false)
                    .surface(Surfaces.water())
                    .build());
        }
        assertFalse(harness.flagged(), "liquid movement uses different physics and must be skipped");
    }

    @Test
    void climbingIsOutsideTheGravityModel() {
        MovementCheckHarness<VerticalMotionCheck.State> harness = harness();
        Vec3 position = new Vec3(0.0D, 60.0D, 0.0D);
        for (int tick = 1; tick <= 15; tick++) {
            Vec3 next = new Vec3(position.x(), position.y() + 0.12D, position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .clientOnGround(false)
                    .surface(Surfaces.climbable())
                    .build());
            position = next;
        }
        assertFalse(harness.flagged(), "ladder movement must not be measured against gravity");
    }
}
