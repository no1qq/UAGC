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

public final class KnockbackDelayCheck implements Check<MovementEvent, KnockbackDelayCheck.State> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("knockback_delay", "KnockbackDelay", CheckCategory.COMBAT)
            .description("knockback that arrives whole but late, held back by the client to absorb the timing")
            .latencySensitive()
            .tickSensitive()
            .build();

    public static final class State {
        final KnockbackModel.Session session = new KnockbackModel.Session();
        int failures;
        long worstDelay;

        void reset() {
            session.reset();
            failures = 0;
            worstDelay = 0L;
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

        long window = (long) context.config().option("window-ticks", 12.0D) + KnockbackModel.latencyTicks(player);
        boolean responded = state.session.responseTick() != Long.MIN_VALUE;
        if (!responded && tick - state.session.appliedTick() < window) {
            return CheckResult.passed();
        }

        boolean broken = state.session.isBroken();
        long latency = KnockbackModel.latencyTicks(player);
        long delay = responded
                ? state.session.responseTick() - state.session.appliedTick() - latency
                : Long.MIN_VALUE;
        state.session.complete();

        if (broken || !responded) {
            return CheckResult.passed();
        }

        long maximumDelay = (long) context.config().option("maximum-delay-ticks", 3.0D);

        if (context.isDebugWatched()) {
            long reported = delay;
            context.debug(() -> "knockback delay=" + reported + " ticks latency=" + latency);
        }

        if (delay <= maximumDelay) {
            state.failures = 0;
            state.worstDelay = 0L;
            return CheckResult.passed();
        }

        state.failures++;
        state.worstDelay = Math.max(state.worstDelay, delay);
        int required = context.config().optionInt("required-samples", 2);
        if (state.failures < required) {
            return CheckResult.passed();
        }

        long worst = state.worstDelay;
        int failures = state.failures;
        state.failures = 0;
        state.worstDelay = 0L;

        double severity = ConfidenceModel.severity(worst - maximumDelay, 0.0D,
                context.config().option("severity-scale", 4.0D));
        return CheckResult.flag(severity, "knockback was held back before it was applied")
                .with("delay_ticks", worst)
                .with("delay_millis", worst * 50L)
                .with("allowed_ticks", maximumDelay)
                .with("latency_ticks", latency)
                .with("samples", failures)
                .build();
    }
}
