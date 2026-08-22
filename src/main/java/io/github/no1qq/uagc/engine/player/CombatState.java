package io.github.no1qq.uagc.engine.player;

import io.github.no1qq.uagc.engine.util.MathUtil;

import java.util.UUID;

public final class CombatState {

    private final double[] intervals;
    private int intervalCount;
    private int intervalCursor;

    private long lastAttackTick = Long.MIN_VALUE;
    private long lastAttackMillis = Long.MIN_VALUE;
    private long lastDamageTakenTick = Long.MIN_VALUE;
    private UUID lastTargetId;
    private int targetSwitches;
    private long targetSwitchWindowStartTick = Long.MIN_VALUE;
    private int attacksInWindow;
    private long windowStartMillis = Long.MIN_VALUE;

    public CombatState(int sampleSize) {
        this.intervals = new double[Math.max(8, sampleSize)];
    }

    public void recordAttack(UUID targetId, long tick, long millis) {
        if (lastAttackMillis != Long.MIN_VALUE) {
            double interval = millis - lastAttackMillis;
            if (interval > 0.0D && interval < 2000.0D) {
                intervals[intervalCursor] = interval;
                intervalCursor = (intervalCursor + 1) % intervals.length;
                if (intervalCount < intervals.length) {
                    intervalCount++;
                }
            } else if (interval >= 2000.0D) {
                resetIntervals();
            }
        }
        if (windowStartMillis == Long.MIN_VALUE || millis - windowStartMillis > 1000L) {
            windowStartMillis = millis;
            attacksInWindow = 1;
        } else {
            attacksInWindow++;
        }
        if (targetId != null && !targetId.equals(lastTargetId)) {
            if (targetSwitchWindowStartTick == Long.MIN_VALUE || tick - targetSwitchWindowStartTick > 100L) {
                targetSwitchWindowStartTick = tick;
                targetSwitches = 1;
            } else {
                targetSwitches++;
            }
            lastTargetId = targetId;
        }
        lastAttackTick = tick;
        lastAttackMillis = millis;
    }

    public void recordDamageTaken(long tick) {
        this.lastDamageTakenTick = tick;
    }

    public void resetIntervals() {
        intervalCount = 0;
        intervalCursor = 0;
    }

    public void reset() {
        resetIntervals();
        lastAttackTick = Long.MIN_VALUE;
        lastAttackMillis = Long.MIN_VALUE;
        lastDamageTakenTick = Long.MIN_VALUE;
        lastTargetId = null;
        targetSwitches = 0;
        targetSwitchWindowStartTick = Long.MIN_VALUE;
        attacksInWindow = 0;
        windowStartMillis = Long.MIN_VALUE;
    }

    public int intervalCount() {
        return intervalCount;
    }

    public double meanInterval() {
        return MathUtil.mean(intervals, intervalCount);
    }

    public double intervalDeviation() {
        return MathUtil.standardDeviation(intervals, intervalCount);
    }

    public double[] intervalsSnapshot() {
        double[] copy = new double[intervalCount];
        System.arraycopy(intervals, 0, copy, 0, intervalCount);
        return copy;
    }

    public double clicksPerSecond() {
        double mean = meanInterval();
        return mean <= 0.0D ? 0.0D : 1000.0D / mean;
    }

    public int attacksInWindow() {
        return attacksInWindow;
    }

    public long lastAttackTick() {
        return lastAttackTick;
    }

    public long lastAttackMillis() {
        return lastAttackMillis;
    }

    public long lastDamageTakenTick() {
        return lastDamageTakenTick;
    }

    public UUID lastTargetId() {
        return lastTargetId;
    }

    public int targetSwitches() {
        return targetSwitches;
    }

    public boolean isInCombat(long tick, long windowTicks) {
        return lastAttackTick != Long.MIN_VALUE && tick - lastAttackTick <= windowTicks;
    }
}
