package io.github.no1qq.uagc.engine.checks.movement;

import io.github.no1qq.uagc.engine.check.Check;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.CheckContext;
import io.github.no1qq.uagc.engine.check.CheckDefinition;
import io.github.no1qq.uagc.engine.check.CheckResult;
import io.github.no1qq.uagc.engine.check.ConfidenceModel;
import io.github.no1qq.uagc.engine.check.event.MovementEvent;
import io.github.no1qq.uagc.engine.movement.AttributeSample;
import io.github.no1qq.uagc.engine.movement.MovementPredictor;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.player.MovementTracker;
import io.github.no1qq.uagc.engine.player.PlayerData;

public final class VerticalMotionCheck implements Check<MovementEvent, VerticalMotionCheck.State> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("vertical_motion", "VerticalMotion", CheckCategory.MOVEMENT)
            .description("compares airborne vertical movement against the gravity model the server expects")
            .latencySensitive()
            .tickSensitive()
            .build();

    public static final class State {
        int consecutive;
        int airborneSamples;

        void reset() {
            consecutive = 0;
            airborneSamples = 0;
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
        MovementTracker tracker = player.movement();

        if (MovementApplicability.hasAlternateVerticalPhysics(snapshot)
                || !MovementApplicability.isMeasurable(player, snapshot)) {
            state.reset();
            return CheckResult.passed();
        }
        if (snapshot.surface().solidBelow() || snapshot.surface().nearGround()) {
            state.reset();
            return CheckResult.passed();
        }
        if (player.velocity().appliedWithin(event.tick(), 4L) || player.velocity().jumpedWithin(event.tick(), 1L)) {
            state.reset();
            return CheckResult.passed();
        }

        state.airborneSamples++;
        int minimumSamples = context.config().optionInt("minimum-airborne-samples", 2);
        if (state.airborneSamples <= minimumSamples) {
            return CheckResult.passed();
        }

        double previousDelta = tracker.previousVerticalDelta();
        double gravity = snapshot.attributes().gravity() > 0.0D
                ? snapshot.attributes().gravity()
                : AttributeSample.VANILLA_GRAVITY;
        double predicted = MovementPredictor.predictVerticalDelta(previousDelta, gravity,
                snapshot.effects().slowFalling());
        double actual = snapshot.verticalDelta();
        double tolerance = context.config().option("tolerance", 0.006D)
                + MovementApplicability.latencyTolerance(player, 0.004D);

        double excess = actual - (predicted + tolerance);
        if (context.isDebugWatched()) {
            double reported = excess;
            context.debug(() -> "vertical actual=" + actual + " predicted=" + predicted + " excess=" + reported);
        }
        if (excess <= 0.0D) {
            state.consecutive = 0;
            return CheckResult.passed();
        }

        state.consecutive++;
        int requiredStreak = context.config().optionInt("required-streak", 2);
        if (state.consecutive < requiredStreak) {
            return CheckResult.passed();
        }
        if (context.support().hasEntitySupportBelow(player.uuid())) {
            state.reset();
            return CheckResult.passed();
        }

        double severity = ConfidenceModel.severity(excess, 0.0D, context.config().option("severity-scale", 0.12D));
        return CheckResult.flag(severity, "vertical movement exceeded the predicted gravity envelope")
                .with("actual", actual)
                .with("predicted", predicted)
                .with("excess", excess)
                .with("previous", previousDelta)
                .with("air_ticks", tracker.airTicks())
                .with("streak", state.consecutive)
                .with("distance_to_ground", snapshot.surface().distanceToGround())
                .setback()
                .build();
    }
}
