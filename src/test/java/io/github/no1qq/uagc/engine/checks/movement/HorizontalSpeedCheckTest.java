package io.github.no1qq.uagc.engine.checks.movement;

import io.github.no1qq.uagc.engine.check.MovementCheckHarness;
import io.github.no1qq.uagc.engine.movement.ActivitySample;
import io.github.no1qq.uagc.engine.movement.AttributeSample;
import io.github.no1qq.uagc.engine.movement.GameModeType;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.support.SnapshotBuilder;
import io.github.no1qq.uagc.support.Surfaces;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorizontalSpeedCheckTest {

    private static final double VANILLA_SPRINT_PER_TICK = 0.2806D;

    private MovementCheckHarness<HorizontalSpeedCheck.State> harness() {
        return new MovementCheckHarness<>(new HorizontalSpeedCheck());
    }

    private Vec3 run(MovementCheckHarness<HorizontalSpeedCheck.State> harness,
                     Vec3 start, double perTick, int ticks, boolean sprinting) {
        Vec3 position = start;
        for (int tick = 1; tick <= ticks; tick++) {
            Vec3 next = new Vec3(position.x() + perTick, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .sprinting(sprinting)
                    .surface(Surfaces.ground())
                    .build());
            position = next;
        }
        return position;
    }

    @Test
    void vanillaSprintingIsNeverFlagged() {
        MovementCheckHarness<HorizontalSpeedCheck.State> harness = harness();
        run(harness, Vec3.ZERO, VANILLA_SPRINT_PER_TICK, 60, true);
        assertFalse(harness.flagged(), "sustained vanilla sprinting must never produce a violation");
    }

    @Test
    void walkingIsNeverFlagged() {
        MovementCheckHarness<HorizontalSpeedCheck.State> harness = harness();
        run(harness, Vec3.ZERO, 0.215D, 60, false);
        assertFalse(harness.flagged(), "normal walking must never produce a violation");
    }

    @Test
    void sustainedImpossibleSpeedIsFlagged() {
        MovementCheckHarness<HorizontalSpeedCheck.State> harness = harness();
        run(harness, Vec3.ZERO, 0.75D, 30, true);
        assertTrue(harness.flagged(), "moving far beyond the reachable envelope must be detected");
    }

    @Test
    void iceMovementIsNeverFlagged() {
        MovementCheckHarness<HorizontalSpeedCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        double speed = 0.0D;
        for (int tick = 1; tick <= 80; tick++) {
            speed = speed * (0.98D * 0.91D) + 0.13D * Math.pow(0.6D / 0.98D, 3.0D);
            Vec3 next = new Vec3(position.x() + speed, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .sprinting(true)
                    .surface(Surfaces.ice())
                    .build());
            position = next;
        }
        assertFalse(harness.flagged(), "ice friction is part of the model and must not be punished");
    }

    @Test
    void vehicleMovementIsSkipped() {
        MovementCheckHarness<HorizontalSpeedCheck.State> harness = harness();
        ActivitySample inVehicle = new ActivitySample(false, false, false, false, false, false,
                false, false, false, true, false, false, GameModeType.SURVIVAL, "MINECART");
        Vec3 position = Vec3.ZERO;
        for (int tick = 1; tick <= 30; tick++) {
            Vec3 next = new Vec3(position.x() + 1.4D, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .activity(inVehicle)
                    .surface(Surfaces.ground())
                    .build());
            position = next;
        }
        assertFalse(harness.flagged(), "vehicles have their own physics and are not covered by this check");
    }

    @Test
    void speedAttributeFromAnEffectOrPluginIsRespected() {
        MovementCheckHarness<HorizontalSpeedCheck.State> harness = harness();
        AttributeSample boosted = new AttributeSample(0.2D, 0.1D, 0.2D,
                AttributeSample.VANILLA_JUMP_STRENGTH, AttributeSample.VANILLA_GRAVITY,
                AttributeSample.VANILLA_STEP_HEIGHT, 1.0D, 3.0D, 1.0D,
                AttributeSample.VANILLA_ENTITY_INTERACTION_RANGE,
                AttributeSample.VANILLA_BLOCK_INTERACTION_RANGE, 0.3D, 0.0D, 0.0D);

        Vec3 position = Vec3.ZERO;
        for (int tick = 1; tick <= 60; tick++) {
            Vec3 next = new Vec3(position.x() + 0.55D, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .sprinting(true)
                    .attributes(boosted)
                    .surface(Surfaces.ground())
                    .build());
            position = next;
        }
        assertFalse(harness.flagged(), "a doubled movement speed attribute legitimately produces extra speed");
    }

    @Test
    void serverAppliedVelocityRaisesTheEnvelope() {
        MovementCheckHarness<HorizontalSpeedCheck.State> harness = harness();
        Vec3 position = run(harness, Vec3.ZERO, VANILLA_SPRINT_PER_TICK, 6, true);

        harness.player().velocity().record(new Vec3(1.2D, 0.4D, 0.0D), 7L, "knockback");
        double speed = 1.2D;
        for (int tick = 7; tick <= 20; tick++) {
            Vec3 next = new Vec3(position.x() + speed, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .sprinting(true)
                    .surface(Surfaces.ground())
                    .build());
            position = next;
            speed *= 0.6D;
        }
        assertFalse(harness.flagged(), "knockback the server itself applied must not be punished");
    }

    @Test
    void sprintJumpingIsNeverFlagged() {
        MovementCheckHarness<HorizontalSpeedCheck.State> harness = harness();
        Vec3 position = run(harness, Vec3.ZERO, VANILLA_SPRINT_PER_TICK, 5, true);

        long tick = 6L;
        for (int cycle = 0; cycle < 6; cycle++) {
            harness.player().velocity().recordJump(tick);
            double speed = 0.40D;
            for (int air = 0; air < 6; air++) {
                Vec3 next = new Vec3(position.x() + speed, position.y(), position.z());
                harness.feed(SnapshotBuilder.create()
                        .tick(tick)
                        .from(position)
                        .to(next)
                        .clientOnGround(false)
                        .sprinting(true)
                        .surface(Surfaces.air(1.0D))
                        .build());
                position = next;
                tick++;
                speed = speed * 0.91D + 0.034D;
            }
            Vec3 landing = new Vec3(position.x() + VANILLA_SPRINT_PER_TICK, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(landing)
                    .sprinting(true)
                    .surface(Surfaces.ground())
                    .build());
            position = landing;
            tick++;
        }
        assertFalse(harness.flagged(), "bunny hopping is legitimate vanilla movement");
    }
}
