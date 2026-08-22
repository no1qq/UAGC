package io.github.no1qq.uagc.engine.config;

public record DebugConfig(
        boolean enabled,
        boolean logInternalCheckFailures,
        int maxDebugSubscribers,
        int debugMessageIntervalTicks) {

    public static DebugConfig defaults() {
        return new DebugConfig(false, true, 8, 1);
    }
}
