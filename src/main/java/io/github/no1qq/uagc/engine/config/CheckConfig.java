package io.github.no1qq.uagc.engine.config;

import java.util.Map;

public record CheckConfig(
        String checkId,
        boolean enabled,
        double violationIncrement,
        double decayPerTick,
        double maxViolationLevel,
        double minimumConfidence,
        double alertThreshold,
        boolean setbackEnabled,
        double setbackThreshold,
        boolean cancelEnabled,
        Map<String, Double> options) {

    public static CheckConfig defaults(String checkId) {
        return new CheckConfig(checkId, true, 1.0D, 0.02D, 200.0D, 0.30D, 3.0D, false, 6.0D, false, Map.of());
    }

    public double option(String key, double fallback) {
        Double value = options.get(key);
        return value == null ? fallback : value;
    }

    public int optionInt(String key, int fallback) {
        Double value = options.get(key);
        return value == null ? fallback : (int) Math.round(value);
    }

    public boolean optionBoolean(String key, boolean fallback) {
        Double value = options.get(key);
        return value == null ? fallback : value != 0.0D;
    }

    public CheckConfig withEnabled(boolean value) {
        return new CheckConfig(checkId, value, violationIncrement, decayPerTick, maxViolationLevel,
                minimumConfidence, alertThreshold, setbackEnabled, setbackThreshold, cancelEnabled, options);
    }
}
