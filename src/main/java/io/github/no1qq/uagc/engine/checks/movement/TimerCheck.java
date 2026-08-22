package io.github.no1qq.uagc.engine.checks.movement;

import io.github.no1qq.uagc.engine.check.Check;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.CheckContext;
import io.github.no1qq.uagc.engine.check.CheckDefinition;
import io.github.no1qq.uagc.engine.check.CheckResult;
import io.github.no1qq.uagc.engine.check.ConfidenceModel;
import io.github.no1qq.uagc.engine.check.event.MovementEvent;
import io.github.no1qq.uagc.engine.player.PlayerData;

public final class TimerCheck implements Check<MovementEvent, TimerCheck.State> {

    private static final double TICK_MILLIS = 50.0D;

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("timer", "Timer", CheckCategory.MOVEMENT)
            .description("spends fifty milliseconds of a real time budget per movement packet, "
                    + "so a client running its clock fast overdraws the account")
            .tickSensitive()
            .build();

    public static final class State {
        double balanceMillis;
        boolean started;
        int overdrafts;

        void reset() {
            balanceMillis = 0.0D;
            started = false;
            overdrafts = 0;
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

        if (context.conditions().isLagging()) {
            state.reset();
            return CheckResult.passed();
        }
        if (!event.snapshot().positionChanged() && !event.snapshot().rotationChanged()) {
            return CheckResult.passed();
        }

        long now = event.timeMillis();
        double clockDrift = context.config().option("clock-drift-millis", 120.0D);
        double latencyAllowance = Math.min(player.latency().lastPing(),
                context.config().option("maximum-latency-credit-millis", 1000.0D));
        double floor = now - clockDrift - latencyAllowance;

        if (!state.started) {
            state.started = true;
            state.balanceMillis = floor;
            return CheckResult.passed();
        }

        state.balanceMillis += TICK_MILLIS;

        if (state.balanceMillis <= now) {
            state.balanceMillis = Math.max(state.balanceMillis, floor);
            return CheckResult.passed();
        }

        double overdraft = state.balanceMillis - now;
        state.balanceMillis -= TICK_MILLIS;
        state.balanceMillis = Math.max(state.balanceMillis, floor);
        state.overdrafts++;

        if (context.isDebugWatched()) {
            double reported = overdraft;
            context.debug(() -> "timer overdraft=" + reported + " total=" + state.overdrafts);
        }

        double severity = ConfidenceModel.severity(overdraft, 0.0D,
                context.config().option("severity-scale", 200.0D));
        return CheckResult.flag(severity, "movement packets arrived faster than real time permits")
                .with("overdraft_millis", overdraft)
                .with("clock_drift_millis", clockDrift)
                .with("latency_credit_millis", latencyAllowance)
                .build();
    }
}
