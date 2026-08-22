package io.github.no1qq.uagc.engine.checks.movement;

import io.github.no1qq.uagc.engine.check.MovementCheckHarness;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.support.SnapshotBuilder;
import io.github.no1qq.uagc.support.Surfaces;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroundSpoofCheckTest {

    private MovementCheckHarness<GroundSpoofCheck.State> harness() {
        return new MovementCheckHarness<>(new GroundSpoofCheck());
    }

    @Test
    void claimingGroundHighInTheAirIsFlagged() {
        MovementCheckHarness<GroundSpoofCheck.State> harness = harness();
        Vec3 position = new Vec3(0.0D, 120.0D, 0.0D);
        for (int tick = 1; tick <= 12; tick++) {
            Vec3 next = new Vec3(position.x(), position.y() - 0.08D, position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .clientOnGround(true)
                    .surface(Surfaces.air(6.0D))
                    .build());
            position = next;
        }
        assertTrue(harness.flagged(), "a client claiming ground six blocks up must be detected");
    }

    @Test
    void agreeingWithTheServerIsNeverFlagged() {
        MovementCheckHarness<GroundSpoofCheck.State> harness = harness();
        Vec3 position = new Vec3(0.0D, 64.0D, 0.0D);
        for (int tick = 1; tick <= 30; tick++) {
            Vec3 next = new Vec3(position.x() + 0.2D, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .clientOnGround(true)
                    .surface(Surfaces.ground())
                    .build());
            position = next;
        }
        assertFalse(harness.flagged(), "matching ground state must never produce a violation");
    }

    @Test
    void smallDesyncNearTheGroundIsTolerated() {
        MovementCheckHarness<GroundSpoofCheck.State> harness = harness();
        Vec3 position = new Vec3(0.0D, 64.0D, 0.0D);
        for (int tick = 1; tick <= 30; tick++) {
            Vec3 next = new Vec3(position.x() + 0.2D, position.y(), position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .clientOnGround(true)
                    .surface(Surfaces.air(0.2D))
                    .build());
            position = next;
        }
        assertFalse(harness.flagged(), "a fraction of a block of desync is normal and must be tolerated");
    }

    @Test
    void honestAirborneClientIsNeverFlagged() {
        MovementCheckHarness<GroundSpoofCheck.State> harness = harness();
        Vec3 position = new Vec3(0.0D, 120.0D, 0.0D);
        for (int tick = 1; tick <= 20; tick++) {
            Vec3 next = new Vec3(position.x(), position.y() - 0.3D, position.z());
            harness.feed(SnapshotBuilder.create()
                    .tick(tick)
                    .from(position)
                    .to(next)
                    .clientOnGround(false)
                    .surface(Surfaces.air(8.0D))
                    .build());
            position = next;
        }
        assertFalse(harness.flagged(), "a client that admits it is airborne is not spoofing");
    }
}
