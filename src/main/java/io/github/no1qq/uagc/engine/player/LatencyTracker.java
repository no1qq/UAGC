package io.github.no1qq.uagc.engine.player;

import io.github.no1qq.uagc.engine.util.MathUtil;

public final class LatencyTracker {

    private final double[] samples;
    private int count;
    private int cursor;
    private int lastPing;
    private int maxRecentPing;

    public LatencyTracker(int sampleSize) {
        this.samples = new double[Math.max(4, sampleSize)];
    }

    public void record(int ping) {
        if (ping < 0 || ping > 60000) {
            return;
        }
        lastPing = ping;
        samples[cursor] = ping;
        cursor = (cursor + 1) % samples.length;
        if (count < samples.length) {
            count++;
        }
        int max = 0;
        for (int i = 0; i < count; i++) {
            max = Math.max(max, (int) samples[i]);
        }
        maxRecentPing = max;
    }

    public int lastPing() {
        return lastPing;
    }

    public int maxRecentPing() {
        return maxRecentPing;
    }

    public double averagePing() {
        return MathUtil.mean(samples, count);
    }

    public double jitter() {
        return MathUtil.standardDeviation(samples, count);
    }

    public int sampleCount() {
        return count;
    }

    public void reset() {
        count = 0;
        cursor = 0;
        lastPing = 0;
        maxRecentPing = 0;
    }
}
