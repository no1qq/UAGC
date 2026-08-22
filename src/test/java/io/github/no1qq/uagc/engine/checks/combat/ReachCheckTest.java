package io.github.no1qq.uagc.engine.checks.combat;

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

    private EventCheckHarness<AttackEvent, Void> harness() {
        return new EventCheckHarness<>(new ReachCheck());
    }

    private AttackEvent attackAt(double targetX, AttributeSample attributes, int ping, List<Vec3> history) {
        TargetSample target = new TargetSample(UUID.randomUUID(), "PLAYER", true,
                new Vec3(targetX, 0.0D, 0.0D), Vec3.ZERO, 0.6D, 1.8D, history);
        return new AttackEvent(1L, 1_700_000_000_000L, EYE, Rotation.ZERO, Rotation.ZERO,
                target, attributes, ping, false, false, false);
    }

    @Test
    void normalMeleeDistanceIsNeverFlagged() {
        EventCheckHarness<AttackEvent, Void> harness = harness();
        harness.feed(attackAt(2.5D, AttributeSample.vanilla(), 30, List.of()));
        assertFalse(harness.flagged(), "a normal melee hit must never produce a violation");
    }

    @Test
    void hitsBeyondTheInteractionRangeAreFlagged() {
        EventCheckHarness<AttackEvent, Void> harness = harness();
        harness.feed(attackAt(4.6D, AttributeSample.vanilla(), 30, List.of()));
        assertTrue(harness.flagged(), "attacking well beyond the interaction range must be detected");
    }

    @Test
    void anExtendedInteractionRangeAttributeIsRespected() {
        AttributeSample extended = new AttributeSample(0.2D, 0.1D, 0.1D,
                AttributeSample.VANILLA_JUMP_STRENGTH, AttributeSample.VANILLA_GRAVITY,
                AttributeSample.VANILLA_STEP_HEIGHT, 1.0D, 3.0D, 1.0D,
                6.0D, AttributeSample.VANILLA_BLOCK_INTERACTION_RANGE, 0.3D, 0.0D, 0.0D);
        EventCheckHarness<AttackEvent, Void> harness = harness();
        harness.feed(attackAt(4.6D, extended, 30, List.of()));
        assertFalse(harness.flagged(), "a plugin granted interaction range is a legitimate cause of extra reach");
    }

    @Test
    void recentTargetPositionsCompensateForLatency() {
        EventCheckHarness<AttackEvent, Void> harness = harness();
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
        EventCheckHarness<AttackEvent, Void> harness = harness();
        harness.feed(event);
        assertFalse(harness.flagged(), "vehicle position desync makes reach unreliable and it is skipped");
    }
}
