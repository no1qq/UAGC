package io.github.no1qq.uagc.engine.platform;

import io.github.no1qq.uagc.engine.util.MathUtil;

public record ServerConditions(
        double tps,
        double averageTickTimeMillis,
        long currentTick,
        long millisSinceLastTick,
        int onlinePlayers) {

    public static final double TARGET_TPS = 20.0D;

    public static ServerConditions healthy() {
        return new ServerConditions(TARGET_TPS, 5.0D, 0L, 50L, 0);
    }

    public boolean isLagging() {
        return tps < 18.0D || averageTickTimeMillis > 55.0D;
    }

    public boolean isSeverelyLagging() {
        return tps < 14.0D || averageTickTimeMillis > 90.0D;
    }

    public double tickRateRatio() {
        return MathUtil.clamp(tps / TARGET_TPS, 0.0D, 1.0D);
    }

    public double reliability() {
        if (!MathUtil.isFinite(tps)) {
            return 0.5D;
        }
        if (tps >= 19.5D) {
            return 1.0D;
        }
        if (tps <= 12.0D) {
            return 0.0D;
        }
        return MathUtil.clamp01((tps - 12.0D) / 7.5D);
    }
}
