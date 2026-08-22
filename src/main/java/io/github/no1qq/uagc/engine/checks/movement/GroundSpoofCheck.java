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

public final class GroundSpoofCheck implements Check<MovementEvent, GroundSpoofCheck.State> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("ground_spoof", "GroundSpoof", CheckCategory.MOVEMENT)
            .description("detects a client repeatedly claiming to stand on ground while the server sees open air")
            .latencySensitive()
            .tickSensitive()
            .build();

    public static final class State {
        int consecutive;
        double maximumDistance;

        void reset() {
            consecutive = 0;
            maximumDistance = 0.0D;
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
                || !MovementApplicability.isMeasurable(player, snapshot)) {
            state.reset();
            return CheckResult.passed();
        }
        if (!snapshot.clientOnGround() || snapshot.surface().solidBelow()) {
            state.reset();
            return CheckResult.passed();
        }

        double distance = snapshot.surface().distanceToGround();
        double minimumDistance = context.config().option("minimum-distance", 0.6D);
        if (!Double.isFinite(distance) || distance < minimumDistance) {
            state.reset();
            return CheckResult.passed();
        }

        state.consecutive++;
        state.maximumDistance = Math.max(state.maximumDistance, distance);

        int requiredStreak = context.config().optionInt("required-streak", 4);
        if (state.consecutive < requiredStreak) {
            return CheckResult.passed();
        }
        if (context.support().hasEntitySupportBelow(player.uuid())) {
            state.reset();
            return CheckResult.passed();
        }

        double severity = ConfidenceModel.severity(state.maximumDistance, minimumDistance,
                context.config().option("severity-scale", 2.0D));
        return CheckResult.flag(severity, "client reported standing on ground while airborne")
                .with("distance_to_ground", distance)
                .with("maximum_distance", state.maximumDistance)
                .with("streak", state.consecutive)
                .with("air_ticks", player.movement().airTicks())
                .build();
    }
}
