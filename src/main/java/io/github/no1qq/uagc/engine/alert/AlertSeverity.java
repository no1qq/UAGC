package io.github.no1qq.uagc.engine.alert;

import java.util.Locale;

public enum AlertSeverity {
    ANOMALY,
    SUSPICIOUS,
    LIKELY,
    CRITICAL;

    private final String id = name().toLowerCase(Locale.ROOT);

    public String id() {
        return id;
    }

    public static AlertSeverity from(double confidence, double violationLevel, double punishThreshold) {
        double progress = punishThreshold > 0.0D ? violationLevel / punishThreshold : 0.0D;
        if (confidence >= 0.85D && progress >= 0.75D) {
            return CRITICAL;
        }
        if (confidence >= 0.65D || progress >= 0.5D) {
            return LIKELY;
        }
        if (confidence >= 0.45D || progress >= 0.25D) {
            return SUSPICIOUS;
        }
        return ANOMALY;
    }
}
