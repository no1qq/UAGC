package io.github.no1qq.uagc.engine.checks.combat;

import io.github.no1qq.uagc.engine.check.Check;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.CheckContext;
import io.github.no1qq.uagc.engine.check.CheckDefinition;
import io.github.no1qq.uagc.engine.check.CheckResult;
import io.github.no1qq.uagc.engine.check.ConfidenceModel;
import io.github.no1qq.uagc.engine.check.event.MovementEvent;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.player.PlayerData;

public final class VelocityCheck implements Check<MovementEvent, VelocityCheck.State> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("velocity", "Velocity", CheckCategory.COMBAT)
            .description("a player must actually travel the knockback the server handed them")
            .latencySensitive()
            .tickSensitive()
            .build();

    public static final class State {
        final KnockbackModel.Session session = new KnockbackModel.Session();
        double failures;
        double worstRatio = 1.0D;

        void reset() {
            session.reset();
            failures = 0.0D;
            worstRatio = 1.0D;
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
        MovementSnapshot snapshot = event.snapshot();
        PlayerData player = context.player();
        long tick = event.tick();

        double minimumMagnitude = context.config().option("minimum-magnitude", 0.2D);
        double responseRatio = context.config().option("response-ratio", 0.45D);
        int minimumObservation = context.config().optionInt("minimum-observation-ticks", 2);

        if (KnockbackModel.hasNewKnockback(state.session, player)) {
            CheckResult pending = state.session.isTracking() && state.session.observedTicks() >= minimumObservation
                    ? judge(context, state)
                    : CheckResult.passed();
            KnockbackModel.begin(state.session, player, minimumMagnitude);
            return pending;
        }
        if (!state.session.isTracking()) {
            return CheckResult.passed();
        }
        if (!player.movement().isContinuous() || player.movement().tickGap() > 1L) {
            state.session.complete();
            return CheckResult.passed();
        }

        KnockbackModel.observe(state.session, snapshot, tick, responseRatio);

        double minimumRatio = context.config().option("minimum-ratio", 0.45D);
        if (!state.session.isBroken() && state.session.takenRatio() >= minimumRatio) {
            state.session.complete();
            reward(context, state);
            return CheckResult.passed();
        }

        long window = (long) context.config().option("window-ticks", 6.0D) + KnockbackModel.latencyTicks(player);
        if (state.session.observedTicks() < window) {
            return CheckResult.passed();
        }
        return judge(context, state);
    }

    private void reward(CheckContext context, State state) {
        state.failures = Math.max(0.0D, state.failures - context.config().option("buffer-decay", 0.5D));
        if (state.failures <= 0.0D) {
            state.worstRatio = 1.0D;
        }
    }

    private CheckResult judge(CheckContext context, State state) {
        boolean broken = state.session.isBroken();
        double taken = state.session.takenRatio();
        double magnitude = state.session.magnitude();
        int observed = state.session.observedTicks();
        state.session.complete();

        if (broken) {
            return CheckResult.passed();
        }

        if (context.isDebugWatched()) {
            context.debug(() -> "velocity taken=" + taken + " of " + magnitude + " over " + observed + " ticks");
        }

        double minimumRatio = context.config().option("minimum-ratio", 0.45D);
        if (taken >= minimumRatio) {
            reward(context, state);
            return CheckResult.passed();
        }

        state.failures += 1.0D;
        state.worstRatio = Math.min(state.worstRatio, taken);
        double required = context.config().option("required-samples", 2.0D);
        if (state.failures < required) {
            return CheckResult.passed();
        }

        double worst = state.worstRatio;
        double failures = state.failures;
        state.failures = 0.0D;
        state.worstRatio = 1.0D;

        double severity = ConfidenceModel.severity(minimumRatio - worst, 0.0D,
                context.config().option("severity-scale", 0.35D));
        return CheckResult.flag(severity, "took less knockback than the server applied")
                .with("taken_ratio", worst)
                .with("required_ratio", minimumRatio)
                .with("magnitude", magnitude)
                .with("observed_ticks", observed)
                .with("samples", failures)
                .build();
    }
}
