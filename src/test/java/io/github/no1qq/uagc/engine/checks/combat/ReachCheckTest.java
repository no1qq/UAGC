package io.github.no1qq.uagc.engine.checks.combat;

import io.github.no1qq.uagc.engine.check.CheckResult;
import io.github.no1qq.uagc.engine.check.EventCheckHarness;
import io.github.no1qq.uagc.engine.check.event.AttackEvent;
import io.github.no1qq.uagc.engine.check.event.TargetSample;
import io.github.no1qq.uagc.engine.movement.AttributeSample;
import io.github.no1qq.uagc.engine.movement.Rotation;
import io.github.no1qq.uagc.engine.movement.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReachCheckTest {

    private static final Vec3 EYE = new Vec3(0.0D, 1.62D, 0.0D);

    private EventCheckHarness<AttackEvent, ReachCheck.State> harness() {
        return new EventCheckHarness<>(new ReachCheck());
    }

    private AttackEvent attackAt(double targetX, AttributeSample attributes, int ping, List<Vec3> history) {
        return attackAt(targetX, attributes, ping, history, Vec3.ZERO, 1L);
    }

    private AttackEvent attackAt(double targetX, AttributeSample attributes, int ping, List<Vec3> history,
                                 Vec3 targetVelocity, long tick) {
        TargetSample target = new TargetSample(UUID.randomUUID(), "PLAYER", true,
                new Vec3(targetX, 0.0D, 0.0D), targetVelocity, 0.6D, 1.8D, history);
        return new AttackEvent(tick, 1_700_000_000_000L, EYE, Rotation.ZERO, Rotation.ZERO,
                target, attributes, ping, false, false, false);
    }

    private void feedRepeated(EventCheckHarness<AttackEvent, ReachCheck.State> harness,
                              double targetX, AttributeSample attributes, int ping, int hits) {
        for (int hit = 0; hit < hits; hit++) {
            harness.feed(attackAt(targetX, attributes, ping, List.of(), Vec3.ZERO, 1L + hit * 10L));
        }
    }

    @Test
    void normalMeleeDistanceIsNeverFlagged() {
        EventCheckHarness<AttackEvent, ReachCheck.State> harness = harness();
        feedRepeated(harness, 2.5D, AttributeSample.vanilla(), 30, 6);
        assertFalse(harness.flagged(), "a normal melee hit must never produce a violation");
    }

    @Test
    void hitsBeyondTheInteractionRangeAreFlagged() {
        EventCheckHarness<AttackEvent, ReachCheck.State> harness = harness();
        feedRepeated(harness, 4.6D, AttributeSample.vanilla(), 30, 3);
        assertTrue(harness.flagged(), "attacking well beyond the interaction range must be detected");
    }

    @Test
    void aSingleHitPastThreeBlocksIsFlagged() {
        EventCheckHarness<AttackEvent, ReachCheck.State> harness = harness();
        harness.feed(attackAt(3.4D, AttributeSample.vanilla(), 60, List.of()));
        assertTrue(harness.flagged(), "the first hit past the interaction range is already reach");
    }

    @Test
    void aHitDeniedByTheCheckDoesNotLand() {
        EventCheckHarness<AttackEvent, ReachCheck.State> harness = harness();
        CheckResult result = harness.feed(attackAt(3.4D, AttributeSample.vanilla(), 60, List.of()));
        assertTrue(result.requestDeny(), "a hit out of range must not do damage");
    }

    @Test
    void aHitInsideTheRangeIsNeverFlagged() {
        EventCheckHarness<AttackEvent, ReachCheck.State> harness = harness();
        for (int hit = 0; hit < 6; hit++) {
            harness.feed(attackAt(3.29D, AttributeSample.vanilla(), 120, List.of(), Vec3.ZERO, 1L + hit * 10L));
        }
        assertFalse(harness.flagged(), "three blocks to the hitbox surface is a legitimate vanilla hit");
    }

    @Test
    void aMovingTargetIsRewoundAlongItsVelocity() {
        EventCheckHarness<AttackEvent, ReachCheck.State> harness = harness();
        for (int hit = 0; hit < 6; hit++) {
            harness.feed(attackAt(3.6D, AttributeSample.vanilla(), 80, List.of(),
                    new Vec3(0.25D, 0.0D, 0.0D), 1L + hit * 10L));
        }
        assertFalse(harness.flagged(), "a mob walking away was closer when the attack was sent");
    }

    @Test
    void anExtendedInteractionRangeAttributeIsRespected() {
        AttributeSample extended = new AttributeSample(0.2D, 0.1D, 0.1D,
                AttributeSample.VANILLA_JUMP_STRENGTH, AttributeSample.VANILLA_GRAVITY,
                AttributeSample.VANILLA_STEP_HEIGHT, 1.0D, 3.0D, 1.0D,
                6.0D, AttributeSample.VANILLA_BLOCK_INTERACTION_RANGE, 0.3D, 0.0D, 0.0D);
        EventCheckHarness<AttackEvent, ReachCheck.State> harness = harness();
        harness.feed(attackAt(4.6D, extended, 30, List.of()));
        assertFalse(harness.flagged(), "a plugin granted interaction range is a legitimate cause of extra reach");
    }

    @Test
    void recentTargetPositionsCompensateForLatency() {
        EventCheckHarness<AttackEvent, ReachCheck.State> harness = harness();
        List<Vec3> history = List.of(new Vec3(2.4D, 0.0D, 0.0D), new Vec3(3.0D, 0.0D, 0.0D));
        harness.feed(attackAt(3.9D, AttributeSample.vanilla(), 200, history));
        assertFalse(harness.flagged(), "a target that was closer a moment ago explains the measured distance");
    }

    @Test
    void vehiclePassengersAreSkipped() {
        TargetSample target = new TargetSample(UUID.randomUUID(), "PLAYER", true,
                new Vec3(8.0D, 0.0D, 0.0D), Vec3.ZERO, 0.6D, 1.8D, List.of());
        AttackEvent event = new AttackEvent(1L, 1_700_000_000_000L, EYE, Rotation.ZERO, Rotation.ZERO,
                target, AttributeSample.vanilla(), 30, false, false, true);
        EventCheckHarness<AttackEvent, ReachCheck.State> harness = harness();
        harness.feed(event);
        assertFalse(harness.flagged(), "vehicle position desync makes reach unreliable and it is skipped");
    }
}
