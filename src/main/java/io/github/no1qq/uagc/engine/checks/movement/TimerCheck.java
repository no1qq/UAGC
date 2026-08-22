package io.github.no1qq.uagc.engine.checks.movement;

import io.github.no1qq.uagc.engine.check.Check;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.CheckContext;
import io.github.no1qq.uagc.engine.check.CheckDefinition;
import io.github.no1qq.uagc.engine.check.CheckResult;
import io.github.no1qq.uagc.engine.check.ConfidenceModel;
import io.github.no1qq.uagc.engine.check.event.MovementEvent;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.util.MathUtil;

public final class TimerCheck implements Check<MovementEvent, TimerCheck.State> {

    private static final double TICK_MILLIS = 50.0D;

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("timer", "Timer", CheckCategory.MOVEMENT)
            .description("tracks whether movement packets arrive faster than real time allows")
            .tickSensitive()
            .build();

    public static final class State {
        double balance;
        long lastTimeMillis = Long.MIN_VALUE;
        int samples;

        void reset() {
            balance = 0.0D;
            samples = 0;
            lastTimeMillis = Long.MIN_VALUE;
        }
    }

    @Override
    public CheckDefinition definition() {
        return DEFINITION;
    }

    @Override
    public Class<MovementEvent> eventType() {
        return MovementEvent.class;
    }

    @Override
    public State createState() {
        return new State();
    }

    @Override
    public CheckResult inspect(CheckContext context, MovementEvent event, State state) {
        PlayerData player = context.player();

        if (context.conditions().isLagging() || player.movement().tickGap() > 1L) {
            state.reset();
            return CheckResult.passed();
        }
        if (!event.snapshot().positionChanged()) {
            return CheckResult.passed();
        }

        long now = event.timeMillis();
        if (state.lastTimeMillis == Long.MIN_VALUE) {
            state.lastTimeMillis = now;
            return CheckResult.passed();
        }

        long elapsed = now - state.lastTimeMillis;
        state.lastTimeMillis = now;

        long idleReset = (long) context.config().option("idle-reset-millis", 1000.0D);
        if (elapsed < 0L || elapsed > idleReset) {
            state.reset();
            state.lastTimeMillis = now;
            return CheckResult.passed();
        }

        double maximumCredit = context.config().option("maximum-credit-millis", 120.0D);
        double threshold = context.config().option("drift-threshold-millis", 600.0D);

        state.balance += elapsed - TICK_MILLIS;
        state.balance = MathUtil.clamp(state.balance, -threshold * 2.0D, maximumCredit);
        state.samples++;

        int minimumSamples = context.config().optionInt("minimum-samples", 40);
        if (state.samples < minimumSamples) {
            return CheckResult.passed();
        }

        if (context.isDebugWatched()) {
            double balance = state.balance;
            context.debug(() -> "timer balance=" + balance + " elapsed=" + elapsed);
        }

        if (state.balance > -threshold) {
            return CheckResult.passed();
        }

        double drift = -state.balance;
        double rate = TICK_MILLIS / Math.max(1.0D, TICK_MILLIS - drift / state.samples);
        state.balance = 0.0D;
        state.samples = 0;

        double severity = ConfidenceModel.severity(drift, threshold, context.config().option("severity-scale", 900.0D));
        return CheckResult.flag(severity, "movement packets arrived faster than real time permits")
                .with("drift_millis", drift)
                .with("estimated_rate", rate)
                .with("threshold_millis", threshold)
                .build();
    }
}
