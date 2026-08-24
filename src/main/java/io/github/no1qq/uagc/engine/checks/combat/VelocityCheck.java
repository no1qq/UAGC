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
        int failures;
        double worstRatio = 1.0D;

        void reset() {
            session.reset();
            failures = 0;
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

        if (KnockbackModel.beginIfNew(state.session, player, minimumMagnitude)) {
            return CheckResult.passed();
        }
        if (!state.session.isTracking()) {
            return CheckResult.passed();
        }
        if (!player.movement().isContinuous() || player.movement().tickGap() > 1L) {
            state.session.complete();
            return CheckResult.passed();
        }

        KnockbackModel.observe(state.session, snapshot, tick, responseRatio);

        long window = (long) context.config().option("window-ticks", 10.0D) + KnockbackModel.latencyTicks(player);
        if (tick - state.session.appliedTick() < window) {
            return CheckResult.passed();
        }

        boolean broken = state.session.isBroken();
        double taken = state.session.takenRatio();
        double magnitude = state.session.magnitude();
        state.session.complete();

        if (broken) {
            return CheckResult.passed();
        }

        if (context.isDebugWatched()) {
            double reported = taken;
            context.debug(() -> "velocity taken=" + reported + " of " + magnitude);
        }

        double minimumRatio = context.config().option("minimum-ratio", 0.45D);
        if (taken >= minimumRatio) {
            state.failures = 0;
            state.worstRatio = 1.0D;
            return CheckResult.passed();
        }

        state.failures++;
        state.worstRatio = Math.min(state.worstRatio, taken);
        int required = context.config().optionInt("required-samples", 3);
        if (state.failures < required) {
            return CheckResult.passed();
        }

        double worst = state.worstRatio;
        int failures = state.failures;
        state.failures = 0;
        state.worstRatio = 1.0D;

        double severity = ConfidenceModel.severity(minimumRatio - worst, 0.0D,
                context.config().option("severity-scale", 0.35D));
        return CheckResult.flag(severity, "took less knockback than the server applied")
                .with("taken_ratio", worst)
                .with("required_ratio", minimumRatio)
                .with("magnitude", magnitude)
                .with("samples", failures)
                .build();
    }
}
