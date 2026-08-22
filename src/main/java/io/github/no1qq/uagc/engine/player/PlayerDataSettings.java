package io.github.no1qq.uagc.engine.player;

public record PlayerDataSettings(
        int movementHistorySize,
        int latencySampleSize,
        int clickSampleSize,
        int evidenceEntryCapacity,
        int evidenceViolationCapacity,
        boolean alertsEnabledByDefault,
        double defaultAlertConfidence,
        double defaultAlertViolationLevel) {

    public static PlayerDataSettings defaults() {
        return new PlayerDataSettings(40, 20, 40, 64, 48, true, 0.35D, 1.0D);
    }
}
