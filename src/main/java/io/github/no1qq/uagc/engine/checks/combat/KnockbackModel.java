package io.github.no1qq.uagc.engine.checks.combat;

import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.engine.player.PlayerData;

public final class KnockbackModel {

    public static final class Session {
        long appliedTick = Long.MIN_VALUE;
        long sourceTick = Long.MIN_VALUE;
        double magnitude;
        double directionX;
        double directionZ;
        double peak;
        long responseTick = Long.MIN_VALUE;
        boolean broken;

        public void complete() {
            appliedTick = Long.MIN_VALUE;
            magnitude = 0.0D;
            directionX = 0.0D;
            directionZ = 0.0D;
            peak = 0.0D;
            responseTick = Long.MIN_VALUE;
            broken = false;
        }

        public void reset() {
            complete();
            sourceTick = Long.MIN_VALUE;
        }

        public boolean isTracking() {
            return appliedTick != Long.MIN_VALUE;
        }

        public boolean isBroken() {
            return broken;
        }

        public double magnitude() {
            return magnitude;
        }

        public double peak() {
            return peak;
        }

        public long appliedTick() {
            return appliedTick;
        }

        public long responseTick() {
            return responseTick;
        }

        public double takenRatio() {
            return magnitude <= 0.0D ? 1.0D : peak / magnitude;
        }
    }

    private KnockbackModel() {
    }

    public static boolean beginIfNew(Session session, PlayerData player, double minimumMagnitude) {
        long applied = player.velocity().lastAppliedTick();
        if (applied == Long.MIN_VALUE || applied == session.sourceTick) {
            return false;
        }
        Vec3 velocity = player.velocity().lastApplied();
        double magnitude = velocity.horizontalLength();
        session.complete();
        session.sourceTick = applied;
        if (!velocity.isFinite() || magnitude < minimumMagnitude) {
            return false;
        }
        session.appliedTick = applied;
        session.magnitude = magnitude;
        session.directionX = velocity.x() / magnitude;
        session.directionZ = velocity.z() / magnitude;
        return true;
    }

    public static void observe(Session session, MovementSnapshot snapshot, long tick, double responseRatio) {
        if (!session.isTracking() || session.broken) {
            return;
        }
        if (isDisturbed(snapshot)) {
            session.broken = true;
            return;
        }
        Vec3 delta = snapshot.delta();
        double along = delta.x() * session.directionX + delta.z() * session.directionZ;
        if (along > session.peak) {
            session.peak = along;
        }
        if (session.responseTick == Long.MIN_VALUE && along >= session.magnitude * responseRatio) {
            session.responseTick = tick;
        }
    }

    public static boolean isDisturbed(MovementSnapshot snapshot) {
        return snapshot.activity().hasAlternateMovement()
                || snapshot.activity().allowFlight()
                || snapshot.activity().gameMode().allowsFlight()
                || snapshot.surface().collidingHorizontally()
                || snapshot.surface().insideSolid()
                || snapshot.surface().inLiquid()
                || snapshot.surface().inCobweb()
                || snapshot.surface().onClimbable()
                || snapshot.surface().onSlime()
                || snapshot.surface().onHoney()
                || snapshot.surface().onBed()
                || !snapshot.surface().chunkLoaded();
    }

    public static long latencyTicks(PlayerData player) {
        return Math.min(20L, Math.max(0, player.latency().lastPing()) / 50L);
    }
}
