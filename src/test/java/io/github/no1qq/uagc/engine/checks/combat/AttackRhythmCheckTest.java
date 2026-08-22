package io.github.no1qq.uagc.engine.checks.combat;

import io.github.no1qq.uagc.engine.check.EventCheckHarness;
import io.github.no1qq.uagc.engine.check.event.AttackEvent;
import io.github.no1qq.uagc.engine.check.event.TargetSample;
import io.github.no1qq.uagc.engine.movement.AttributeSample;
import io.github.no1qq.uagc.engine.movement.Rotation;
import io.github.no1qq.uagc.engine.movement.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttackRhythmCheckTest {

    private static final UUID TARGET = UUID.randomUUID();

    private EventCheckHarness<AttackEvent, Void> harness() {
        return new EventCheckHarness<>(new AttackRhythmCheck());
    }

    private AttackEvent attack(long tick, long millis) {
        TargetSample target = new TargetSample(TARGET, "PLAYER", true,
                new Vec3(2.0D, 0.0D, 0.0D), Vec3.ZERO, 0.6D, 1.8D, List.of());
        return new AttackEvent(tick, millis, new Vec3(0.0D, 1.62D, 0.0D), Rotation.ZERO, Rotation.ZERO,
                target, AttributeSample.vanilla(), 30, false, false, false);
    }

    private void clickSequence(EventCheckHarness<AttackEvent, Void> harness, int count, long interval, long variance) {
        Random random = new Random(4242L);
        long millis = 1_700_000_000_000L;
        for (int index = 0; index < count; index++) {
            long jitter = variance == 0L ? 0L : random.nextLong(-variance, variance + 1L);
            millis += interval + jitter;
            harness.player().combat().recordAttack(TARGET, millis / 50L, millis);
            harness.feed(attack(millis / 50L, millis));
        }
    }

    @Test
    void machineLikeClickingIsFlagged() {
        EventCheckHarness<AttackEvent, Void> harness = harness();
        clickSequence(harness, 60, 90L, 0L);
        assertTrue(harness.flagged(), "perfectly even attack intervals must be detected");
    }

    @Test
    void humanClickingIsNeverFlagged() {
        EventCheckHarness<AttackEvent, Void> harness = harness();
        clickSequence(harness, 60, 90L, 25L);
        assertFalse(harness.flagged(), "normal human click variance must never produce a violation");
    }

    @Test
    void slowSteadyClickingIsNeverFlagged() {
        EventCheckHarness<AttackEvent, Void> harness = harness();
        clickSequence(harness, 60, 300L, 0L);
        assertFalse(harness.flagged(), "a slow but steady rhythm is below the automation threshold");
    }

    @Test
    void smallSamplesAreNeverFlagged() {
        EventCheckHarness<AttackEvent, Void> harness = harness();
        clickSequence(harness, 8, 90L, 0L);
        assertFalse(harness.flagged(), "a handful of clicks is not enough evidence to judge a rhythm");
    }
}
