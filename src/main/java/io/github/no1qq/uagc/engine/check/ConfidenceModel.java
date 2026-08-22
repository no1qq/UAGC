package io.github.no1qq.uagc.engine.check;

import io.github.no1qq.uagc.engine.config.ConfidenceSettings;
import io.github.no1qq.uagc.engine.exemption.ExemptionType;
import io.github.no1qq.uagc.engine.platform.ServerConditions;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.util.MathUtil;

public final class ConfidenceModel {

    private static final ExemptionType[] TYPES = ExemptionType.values();

    private volatile ConfidenceSettings settings;

    public ConfidenceModel(ConfidenceSettings settings) {
        this.settings = settings;
    }

    public void updateSettings(ConfidenceSettings updated) {
        this.settings = updated;
    }

    public ConfidenceSettings settings() {
        return settings;
    }

    public double reliability(CheckDefinition definition, PlayerData player, ServerConditions conditions) {
        ConfidenceSettings current = settings;
        double reliability = 1.0D;

        if (definition.latencySensitive()) {
            reliability *= latencyFactor(player, current);
        }
        if (definition.tickSensitive()) {
            reliability *= tickFactor(conditions, current);
        }
        reliability *= transitionFactor(definition.category(), player, current);

        return MathUtil.clamp01(reliability);
    }

    public double latencyFactor(PlayerData player, ConfidenceSettings current) {
        int ping = Math.max(player.latency().lastPing(), player.latency().maxRecentPing());
        double factor;
        if (ping <= current.pingComfortableMillis()) {
            factor = 1.0D;
        } else if (ping >= current.pingSevereMillis()) {
            factor = current.pingReliabilityFloor();
        } else {
            double span = current.pingSevereMillis() - current.pingComfortableMillis();
            double progress = (ping - current.pingComfortableMillis()) / span;
            factor = MathUtil.lerp(1.0D, current.pingReliabilityFloor(), progress);
        }

        double jitter = player.latency().jitter();
        if (jitter > current.jitterPenaltyThreshold()) {
            factor *= current.jitterReliabilityFloor();
        }
        return MathUtil.clamp01(factor);
    }

    public double tickFactor(ServerConditions conditions, ConfidenceSettings current) {
        double raw = conditions.reliability();
        return MathUtil.clamp(current.tickReliabilityFloor() + raw * (1.0D - current.tickReliabilityFloor()),
                current.tickReliabilityFloor(), 1.0D);
    }

    public double transitionFactor(CheckCategory category, PlayerData player, ConfidenceSettings current) {
        int grace = current.transitionGraceTicks();
        if (grace <= 0) {
            return 1.0D;
        }
        long closest = Long.MAX_VALUE;
        for (ExemptionType type : TYPES) {
            if (!type.affects(category)) {
                continue;
            }
            long since = player.exemptions().ticksSinceExpiry(type);
            if (since < closest) {
                closest = since;
            }
        }
        if (closest >= grace) {
            return 1.0D;
        }
        double progress = (double) closest / grace;
        return MathUtil.clamp(current.transitionReliabilityFloor()
                + progress * (1.0D - current.transitionReliabilityFloor()), 0.0D, 1.0D);
    }

    public static double severity(double observed, double allowed, double scale) {
        if (!MathUtil.isFinite(observed) || !MathUtil.isFinite(allowed)) {
            return 0.0D;
        }
        double excess = observed - allowed;
        if (excess <= 0.0D) {
            return 0.0D;
        }
        if (scale <= 0.0D) {
            return 1.0D;
        }
        return MathUtil.clamp01(excess / scale);
    }

    public static double severityRatio(double observed, double allowed, double fullExcessRatio) {
        if (allowed <= 0.0D) {
            return observed > 0.0D ? 1.0D : 0.0D;
        }
        double ratio = (observed - allowed) / allowed;
        if (ratio <= 0.0D) {
            return 0.0D;
        }
        if (fullExcessRatio <= 0.0D) {
            return 1.0D;
        }
        return MathUtil.clamp01(ratio / fullExcessRatio);
    }
}
