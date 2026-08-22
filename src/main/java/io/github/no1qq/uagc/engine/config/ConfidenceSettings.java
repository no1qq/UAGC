package io.github.no1qq.uagc.engine.config;

public record ConfidenceSettings(
        int pingComfortableMillis,
        int pingSevereMillis,
        double pingReliabilityFloor,
        double tickReliabilityFloor,
        int transitionGraceTicks,
        double transitionReliabilityFloor,
        double jitterPenaltyThreshold,
        double jitterReliabilityFloor) {

    public static ConfidenceSettings defaults() {
        return new ConfidenceSettings(120, 450, 0.45D, 0.30D, 20, 0.25D, 90.0D, 0.70D);
    }
}
