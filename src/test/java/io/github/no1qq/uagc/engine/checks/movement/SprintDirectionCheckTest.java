package io.github.no1qq.uagc.engine.checks.movement;

import io.github.no1qq.uagc.engine.check.MovementCheckHarness;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.support.SnapshotBuilder;
import io.github.no1qq.uagc.support.Surfaces;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SprintDirectionCheckTest {

    private static final double SPRINT_PER_TICK = 0.2806D;

    private MovementCheckHarness<SprintDirectionCheck.State> harness() {
        return new MovementCheckHarness<>(new SprintDirectionCheck());
    }

    private void run(MovementCheckHarness<SprintDirectionCheck.State> harness,
                     double dx, double dz, float yaw, int ticks, boolean sprinting) {
        Vec3 position = Vec3.ZERO;
        for (int tick = 1; tick <= ticks; tick++) {
            Vec3 next = new Vec3(position.x() + dx, position.y(), position.z() + dz);
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .fromRotation(yaw, 0.0F)
                    .rotation(yaw, 0.0F)
                    .sprinting(sprinting)
                    .surface(Surfaces.ground())
                    .build());
            position = next;
        }
    }

    @Test
    void sprintingForwardIsNeverFlagged() {
        MovementCheckHarness<SprintDirectionCheck.State> harness = harness();
        run(harness, 0.0D, SPRINT_PER_TICK, 0.0F, 60, true);
        assertFalse(harness.flagged(), "sprinting where you look is the whole point of sprinting");
    }

    @Test
    void sprintingSidewaysIsFlagged() {
        MovementCheckHarness<SprintDirectionCheck.State> harness = harness();
        run(harness, SPRINT_PER_TICK, 0.0D, 0.0F, 60, true);
        assertTrue(harness.flagged(), "a vanilla client cannot sustain sprint while strafing");
    }

    @Test
    void sprintingBackwardsIsFlagged() {
        MovementCheckHarness<SprintDirectionCheck.State> harness = harness();
        run(harness, 0.0D, -SPRINT_PER_TICK, 0.0F, 60, true);
        assertTrue(harness.flagged(), "sprinting backwards is the clearest case of all");
    }

    @Test
    void strafingWithoutSprintingIsNeverFlagged() {
        MovementCheckHarness<SprintDirectionCheck.State> harness = harness();
        run(harness, 0.215D, 0.0D, 0.0F, 60, false);
        assertFalse(harness.flagged(), "walking sideways is completely legal");
    }

    @Test
    void diagonalSprintingIsNeverFlagged() {
        MovementCheckHarness<SprintDirectionCheck.State> harness = harness();
        double component = SPRINT_PER_TICK / Math.sqrt(2.0D);
        run(harness, component, component, 0.0F, 60, true);
        assertFalse(harness.flagged(), "sprinting forward while strafing sits at 45 degrees and is vanilla");
    }

    @Test
    void aBriefSidewaysTickIsNeverFlagged() {
        MovementCheckHarness<SprintDirectionCheck.State> harness = harness();
        run(harness, SPRINT_PER_TICK, 0.0D, 0.0F, 3, true);
        assertFalse(harness.flagged(), "a couple of ticks must not be enough, turning produces those");
    }

    @Test
    void slidingSidewaysOnIceIsNeverFlagged() {
        MovementCheckHarness<SprintDirectionCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        for (int tick = 1; tick <= 60; tick++) {
            Vec3 next = new Vec3(position.x() + SPRINT_PER_TICK, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .fromRotation(0.0F, 0.0F)
                    .rotation(0.0F, 0.0F)
                    .sprinting(true)
                    .surface(Surfaces.ice())
                    .build());
            position = next;
        }
        assertFalse(harness.flagged(), "ice carries you sideways while you turn, that is not a cheat");
    }

    @Test
    void knockbackSidewaysIsNeverFlagged() {
        MovementCheckHarness<SprintDirectionCheck.State> harness = harness();
        harness.player().velocity().record(new Vec3(0.5D, 0.0D, 0.0D), 1L, "damage");
        run(harness, SPRINT_PER_TICK, 0.0D, 0.0F, 15, true);
        assertFalse(harness.flagged(), "knockback moves you sideways while sprint is still latched on");
    }

    @Test
    void hittingAnEntityWhileSprintingIsNeverFlagged() {
        MovementCheckHarness<SprintDirectionCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        for (int tick = 1; tick <= 60; tick++) {
            boolean swinging = tick >= 10;
            if (swinging && (tick - 10) % 12 == 0) {
                harness.player().combat().recordAttack(UUID.randomUUID(), tick, tick * 50L);
            }
            double dx = swinging ? SPRINT_PER_TICK * 0.6D : 0.0D;
            double dz = swinging ? 0.0D : SPRINT_PER_TICK;
            Vec3 next = new Vec3(position.x() + dx, position.y(), position.z() + dz);
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .fromRotation(0.0F, 0.0F)
                    .rotation(0.0F, 0.0F)
                    .sprinting(true)
                    .surface(Surfaces.ground())
                    .build());
            position = next;
        }
        assertFalse(harness.flagged(),
                "hitting an entity kills your sprint, cuts your speed and lets its hitbox shove you "
                        + "sideways, none of which this check models");
    }

    @Test
    void takingDamageWhileSprintingIsNeverFlagged() {
        MovementCheckHarness<SprintDirectionCheck.State> harness = harness();
        harness.player().combat().recordDamageTaken(1L);
        run(harness, SPRINT_PER_TICK, 0.0D, 0.0F, 15, true);
        assertFalse(harness.flagged(), "being hit shoves you sideways with sprint still latched on");
    }

    @Test
    void omniSprintIsStillCaughtAfterCombatEnds() {
        MovementCheckHarness<SprintDirectionCheck.State> harness = harness();
        harness.player().combat().recordAttack(UUID.randomUUID(), 1L, 50L);
        Vec3 position = Vec3.ZERO;
        for (int tick = 30; tick <= 90; tick++) {
            Vec3 next = new Vec3(position.x() + SPRINT_PER_TICK, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .fromRotation(0.0F, 0.0F)
                    .rotation(0.0F, 0.0F)
                    .sprinting(true)
                    .surface(Surfaces.ground())
                    .build());
            position = next;
        }
        assertTrue(harness.flagged(), "the combat grace must expire, not disable the check forever");
    }

    @Test
    void standingStillWhileSprintingIsNeverFlagged() {
        MovementCheckHarness<SprintDirectionCheck.State> harness = harness();
        run(harness, 0.001D, 0.0D, 0.0F, 60, true);
        assertFalse(harness.flagged(), "a stationary player has no meaningful direction to compare");
    }
}
