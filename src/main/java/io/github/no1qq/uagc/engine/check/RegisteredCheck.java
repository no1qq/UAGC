package io.github.no1qq.uagc.engine.check;

import io.github.no1qq.uagc.engine.config.CheckConfig;

public final class RegisteredCheck {

    private final Check<? extends CheckEvent, ?> check;
    private final CheckDefinition definition;
    private final int index;

    private volatile CheckConfig config;
    private volatile boolean runtimeDisabled;
    private volatile String disableReason;
    private int consecutiveFailures;
    private long totalFailures;
    private long evaluations;
    private long flags;

    RegisteredCheck(Check<? extends CheckEvent, ?> check, int index, CheckConfig config) {
        this.check = check;
        this.definition = check.definition();
        this.index = index;
        this.config = config;
    }

    public Check<? extends CheckEvent, ?> check() {
        return check;
    }

    public CheckDefinition definition() {
        return definition;
    }

    public int index() {
        return index;
    }

    public String id() {
        return definition.id();
    }

    public CheckConfig config() {
        return config;
    }

    public void updateConfig(CheckConfig updated) {
        this.config = updated;
    }

    public boolean isRuntimeDisabled() {
        return runtimeDisabled;
    }

    public String disableReason() {
        return disableReason;
    }

    public boolean isActive() {
        return config.enabled() && !runtimeDisabled;
    }

    public void disableAtRuntime(String reason) {
        this.runtimeDisabled = true;
        this.disableReason = reason;
    }

    public void enableAtRuntime() {
        this.runtimeDisabled = false;
        this.disableReason = null;
        this.consecutiveFailures = 0;
    }

    int recordFailure() {
        totalFailures++;
        return ++consecutiveFailures;
    }

    void recordSuccess() {
        if (consecutiveFailures != 0) {
            consecutiveFailures = 0;
        }
        evaluations++;
    }

    void recordFlag() {
        flags++;
    }

    public long evaluations() {
        return evaluations;
    }

    public long flags() {
        return flags;
    }

    public long totalFailures() {
        return totalFailures;
    }

    public int consecutiveFailures() {
        return consecutiveFailures;
    }
}
