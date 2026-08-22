package io.github.no1qq.uagc.engine.checks.movement;

import io.github.no1qq.uagc.engine.check.Check;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.CheckContext;
import io.github.no1qq.uagc.engine.check.CheckDefinition;
import io.github.no1qq.uagc.engine.check.CheckResult;
import io.github.no1qq.uagc.engine.check.ConfidenceModel;
import io.github.no1qq.uagc.engine.check.event.MovementEvent;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.player.PlayerData;

public final class NoFallCheck implements Check<MovementEvent, NoFallCheck.State> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("no_fall", "NoFall", CheckCategory.MOVEMENT)
            .description("compares the descent the server observed against the fall distance the player accumulated")
            .latencySensitive()
            .tickSensitive()
            .build();

    public static final class State {
        double observedDescent;
        int consecutive;

        void reset() {
            observedDescent = 0.0D;
            consecutive = 0;
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

        if (MovementApplicability.hasAlternateVerticalPhysics(snapshot)
                || !MovementApplicability.isMeasurable(player, snapshot)
                || snapshot.effects().slowFalling()
                || player.velocity().appliedWithin(event.tick(), 10L)) {
            state.reset();
            return CheckResult.passed();
        }
        if (snapshot.surface().solidBelow() || snapshot.surface().nearGround()) {
            state.reset();
            return CheckResult.passed();
        }

        double descent = -snapshot.verticalDelta();
        if (descent > 0.0D) {
            state.observedDescent += descent;
        }

        double minimumDrop = context.config().option("minimum-drop", 4.0D);
        if (state.observedDescent < minimumDrop) {
            return CheckResult.passed();
        }

        double reported = snapshot.fallDistance();
        double ratio = context.config().option("reported-ratio", 0.5D);
        double expected = state.observedDescent * ratio;
        if (reported >= expected) {
            state.consecutive = 0;
            return CheckResult.passed();
        }

        state.consecutive++;
        int requiredStreak = context.config().optionInt("required-streak", 3);
        if (state.consecutive < requiredStreak) {
            return CheckResult.passed();
        }
        if (context.support().hasEntitySupportBelow(player.uuid())) {
            state.reset();
            return CheckResult.passed();
        }

        double severity = ConfidenceModel.severity(state.observedDescent - reported, minimumDrop,
                context.config().option("severity-scale", 12.0D));
        return CheckResult.flag(severity, "observed descent was not reflected in the accumulated fall distance")
                .with("observed_descent", state.observedDescent)
                .with("reported_fall_distance", reported)
                .with("streak", state.consecutive)
                .with("air_ticks", player.movement().airTicks())
                .build();
    }
}
