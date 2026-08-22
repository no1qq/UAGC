package io.github.no1qq.uagc.engine.checks.movement;

import io.github.no1qq.uagc.engine.check.MovementCheckHarness;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.engine.platform.ServerConditions;
import io.github.no1qq.uagc.support.SnapshotBuilder;
import io.github.no1qq.uagc.support.Surfaces;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimerCheckTest {

    private MovementCheckHarness<TimerCheck.State> harness() {
        return new MovementCheckHarness<>(new TimerCheck());
    }

    private void feed(MovementCheckHarness<TimerCheck.State> harness, int packets, long intervalMillis) {
        Vec3 position = new Vec3(0.0D, 64.0D, 0.0D);
        long baseMillis = 1_700_000_000_000L;
        for (int index = 1; index <= packets; index++) {
            Vec3 next = new Vec3(position.x() + 0.1D, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(index)
                    .timeMillis(baseMillis + index * intervalMillis)
                    .from(position)
                    .to(next)
                    .surface(Surfaces.ground())
                    .build());
            position = next;
        }
    }

    @Test
    void normalPacketRateIsNeverFlagged() {
        MovementCheckHarness<TimerCheck.State> harness = harness();
        feed(harness, 200, 50L);
        assertFalse(harness.flagged(), "a client sending twenty packets a second is behaving normally");
    }

    @Test
    void acceleratedPacketRateIsFlagged() {
        MovementCheckHarness<TimerCheck.State> harness = harness();
        feed(harness, 200, 33L);
        assertTrue(harness.flagged(), "a client running at roughly 1.5x speed must be detected");
    }

    @Test
    void slowClientIsNeverFlagged() {
        MovementCheckHarness<TimerCheck.State> harness = harness();
        feed(harness, 200, 70L);
        assertFalse(harness.flagged(), "a client sending packets slowly is lagging, not cheating");
    }

    @Test
    void serverLagSuspendsTheMeasurement() {
        MovementCheckHarness<TimerCheck.State> harness = harness()
                .withConditions(new ServerConditions(11.0D, 95.0D, 0L, 90L, 10));
        feed(harness, 200, 33L);
        assertFalse(harness.flagged(), "packet bursts while the server is lagging must not be punished");
    }

    @Test
    void creditIsCappedSoIdlePlayersCannotBankTime() {
        MovementCheckHarness<TimerCheck.State> harness = harness();
        Vec3 position = new Vec3(0.0D, 64.0D, 0.0D);
        long millis = 1_700_000_000_000L;

        for (int index = 1; index <= 60; index++) {
            Vec3 next = new Vec3(position.x() + 0.1D, position.y(), position.z());
            millis += 900L;
            harness.feed(SnapshotBuilder.create()
                    .tick(index)
                    .timeMillis(millis)
                    .from(position)
                    .to(next)
                    .surface(Surfaces.ground())
                    .build());
            position = next;
        }
        for (int index = 61; index <= 260; index++) {
            Vec3 next = new Vec3(position.x() + 0.1D, position.y(), position.z());
            millis += 33L;
            harness.feed(SnapshotBuilder.create()
                    .tick(index)
                    .timeMillis(millis)
                    .from(position)
                    .to(next)
                    .surface(Surfaces.ground())
                    .build());
            position = next;
        }
        assertTrue(harness.flagged(), "banked idle time must not let a client run fast afterwards");
    }
}
