package io.github.no1qq.uagc.engine.violation;

public final class ViolationTracker {

    private final String checkId;

    private double level;
    private double peakLevel;
    private long lastUpdateTick;
    private long lastFlagTick = Long.MIN_VALUE;
    private long firstFlagTick = Long.MIN_VALUE;
    private int flagCount;
    private int streak;
    private double lastConfidence;

    public ViolationTracker(String checkId) {
        this.checkId = checkId;
    }

    public String checkId() {
        return checkId;
    }

    private void decayTo(long tick, double decayPerTick) {
        if (tick <= lastUpdateTick || level <= 0.0D) {
            lastUpdateTick = Math.max(lastUpdateTick, tick);
            return;
        }
        if (decayPerTick > 0.0D) {
            long elapsed = tick - lastUpdateTick;
            level = Math.max(0.0D, level - decayPerTick * elapsed);
        }
        lastUpdateTick = tick;
    }

    public double current(long tick, double decayPerTick) {
        decayTo(tick, decayPerTick);
        return level;
    }

    public double add(double amount, double confidence, long tick, double decayPerTick, double maxLevel) {
        decayTo(tick, decayPerTick);
        if (amount > 0.0D) {
            level = Math.min(maxLevel, level + amount);
            peakLevel = Math.max(peakLevel, level);
            if (firstFlagTick == Long.MIN_VALUE) {
                firstFlagTick = tick;
            }
            if (lastFlagTick != Long.MIN_VALUE && tick - lastFlagTick <= 40L) {
                streak++;
            } else {
                streak = 1;
            }
            lastFlagTick = tick;
            flagCount++;
            lastConfidence = confidence;
        }
        return level;
    }

    public void reset() {
        level = 0.0D;
        streak = 0;
    }

    public void resetCompletely() {
        reset();
        peakLevel = 0.0D;
        flagCount = 0;
        lastFlagTick = Long.MIN_VALUE;
        firstFlagTick = Long.MIN_VALUE;
        lastConfidence = 0.0D;
    }

    public double rawLevel() {
        return level;
    }

    public double peakLevel() {
        return peakLevel;
    }

    public int flagCount() {
        return flagCount;
    }

    public int streak() {
        return streak;
    }

    public long lastFlagTick() {
        return lastFlagTick;
    }

    public long firstFlagTick() {
        return firstFlagTick;
    }

    public double lastConfidence() {
        return lastConfidence;
    }

    public boolean hasEverFlagged() {
        return flagCount > 0;
    }
}
