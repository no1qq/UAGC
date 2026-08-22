package io.github.no1qq.uagc.engine.checks.movement;

import io.github.no1qq.uagc.engine.check.MovementCheckHarness;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.support.SnapshotBuilder;
import io.github.no1qq.uagc.support.Surfaces;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoSlowCheckTest {

    private static final double SPRINT_PER_TICK = 0.2806D;
    private static final double SLOWED_PER_TICK = 0.0440D;

    private MovementCheckHarness<NoSlowCheck.State> harness() {
        return new MovementCheckHarness<>(new NoSlowCheck());
    }

    private void run(MovementCheckHarness<NoSlowCheck.State> harness,
                     double perTick, int ticks, boolean usingItem, boolean sprinting) {
        Vec3 position = Vec3.ZERO;
        for (int tick = 1; tick <= ticks; tick++) {
            Vec3 next = new Vec3(position.x() + perTick, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .usingItem(usingItem)
                    .sprinting(sprinting)
                    .surface(Surfaces.ground())
                    .build());
            position = next;
        }
    }

    @Test
    void eatingAtTheSlowedSpeedIsNeverFlagged() {
        MovementCheckHarness<NoSlowCheck.State> harness = harness();
        run(harness, SLOWED_PER_TICK, 60, true, false);
        assertFalse(harness.flagged(), "a fifth of walking speed is exactly what using an item allows");
    }

    @Test
    void sprintingAtFullSpeedWhileUsingAnItemIsFlagged() {
        MovementCheckHarness<NoSlowCheck.State> harness = harness();
        run(harness, SPRINT_PER_TICK, 60, true, true);
        assertTrue(harness.flagged(), "full sprint speed while using an item is the whole cheat");
    }

    @Test
    void walkingAtFullSpeedWhileUsingAnItemIsFlagged() {
        MovementCheckHarness<NoSlowCheck.State> harness = harness();
        run(harness, 0.2158D, 60, true, false);
        assertTrue(harness.flagged(), "even walking speed is far past what an item user may do");
    }

    @Test
    void fullSpeedWithoutUsingAnItemIsNeverFlagged() {
        MovementCheckHarness<NoSlowCheck.State> harness = harness();
        run(harness, SPRINT_PER_TICK, 60, false, true);
        assertFalse(harness.flagged(), "this check must never touch a player who is not using an item");
    }

    @Test
    void decelerationIntoTheUseIsNeverFlagged() {
        MovementCheckHarness<NoSlowCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        double speed = SPRINT_PER_TICK;
        for (int tick = 1; tick <= 30; tick++) {
            speed = Math.max(SLOWED_PER_TICK, speed * 0.546D + 0.02D);
            Vec3 next = new Vec3(position.x() + speed, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .usingItem(true)
                    .surface(Surfaces.ground())
                    .build());
            position = next;
        }
        assertFalse(harness.flagged(),
                "a player who starts eating at full sprint coasts down over several ticks");
    }

    @Test
    void knockbackWhileUsingAnItemIsNeverFlagged() {
        MovementCheckHarness<NoSlowCheck.State> harness = harness();
        harness.player().velocity().record(new Vec3(0.6D, 0.0D, 0.0D), 1L, "damage");
        run(harness, SPRINT_PER_TICK, 15, true, false);
        assertFalse(harness.flagged(), "being knocked back while eating is not no slow");
    }

    @Test
    void iceDoesNotProduceAFalseFlag() {
        MovementCheckHarness<NoSlowCheck.State> harness = harness();
        Vec3 position = Vec3.ZERO;
        double speed = SPRINT_PER_TICK;
        for (int tick = 1; tick <= 60; tick++) {
            speed = Math.max(0.0424D, speed * 0.8918D + 0.00459D);
            Vec3 next = new Vec3(position.x() + speed, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .usingItem(true)
                    .surface(Surfaces.ice())
                    .build());
            position = next;
        }
        assertFalse(harness.flagged(),
                "ice keeps you sliding for a long time while you eat, the model follows its friction");
    }
}
