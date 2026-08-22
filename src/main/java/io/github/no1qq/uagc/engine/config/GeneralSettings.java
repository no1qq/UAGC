package io.github.no1qq.uagc.engine.config;

public record GeneralSettings(
        boolean enabled,
        int bypassRefreshIntervalTicks,
        int lagSpikeThresholdMillis,
        boolean exemptOnLagSpike,
        int maxCheckFailuresBeforeDisable,
        boolean logPunishments,
        boolean logViolationsToConsole) {

    public static GeneralSettings defaults() {
        return new GeneralSettings(true, 60, 200, true, 12, true, false);
    }
}
