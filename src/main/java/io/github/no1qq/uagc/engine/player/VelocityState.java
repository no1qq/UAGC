package io.github.no1qq.uagc.engine.player;

import io.github.no1qq.uagc.engine.movement.Vec3;

public final class VelocityState {

    private volatile Vec3 lastApplied = Vec3.ZERO;
    private volatile long lastAppliedTick = Long.MIN_VALUE;
    private volatile String lastCause = "none";
    private volatile long lastJumpTick = Long.MIN_VALUE;

    public void record(Vec3 velocity, long tick, String cause) {
        this.lastApplied = velocity;
        this.lastAppliedTick = tick;
        this.lastCause = cause == null ? "unknown" : cause;
    }

    public void recordJump(long tick) {
        this.lastJumpTick = tick;
    }

    public Vec3 lastApplied() {
        return lastApplied;
    }

    public long lastAppliedTick() {
        return lastAppliedTick;
    }

    public String lastCause() {
        return lastCause;
    }

    public long lastJumpTick() {
        return lastJumpTick;
    }

    public boolean appliedWithin(long tick, long ticks) {
        return lastAppliedTick != Long.MIN_VALUE && tick - lastAppliedTick <= ticks;
    }

    public boolean jumpedWithin(long tick, long ticks) {
        return lastJumpTick != Long.MIN_VALUE && tick - lastJumpTick <= ticks;
    }

    public double horizontalMagnitude() {
        return lastApplied.horizontalLength();
    }

    public void reset() {
        lastApplied = Vec3.ZERO;
        lastAppliedTick = Long.MIN_VALUE;
        lastCause = "none";
        lastJumpTick = Long.MIN_VALUE;
    }
}
