package io.github.no1qq.uagc.engine.checks.movement;

import io.github.no1qq.uagc.engine.check.Check;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.CheckContext;
import io.github.no1qq.uagc.engine.check.CheckDefinition;
import io.github.no1qq.uagc.engine.check.CheckResult;
import io.github.no1qq.uagc.engine.check.ConfidenceModel;
import io.github.no1qq.uagc.engine.check.event.MovementEvent;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.movement.SurfaceSample;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.engine.player.PlayerData;

public final class SprintDirectionCheck implements Check<MovementEvent, SprintDirectionCheck.State> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("sprint_direction", "SprintDirection", CheckCategory.MOVEMENT)
            .description("a sprinting player must be travelling roughly where they are looking, "
                    + "because a vanilla client only sustains sprint on forward input")
            .tickSensitive()
            .build();

    public static final class State {
        int consecutive;
        long lastFlaggedTick;

        void reset() {
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

        if (!snapshot.activity().sprinting()) {
            state.reset();
            return CheckResult.passed();
        }
        if (MovementApplicability.hasAlternateHorizontalPhysics(snapshot)
                || !MovementApplicability.isMeasurable(player, snapshot)) {
            state.reset();
            return CheckResult.passed();
        }

        SurfaceSample surface = snapshot.surface();
        if (!surface.solidBelow()) {
            state.reset();
            return CheckResult.passed();
        }
        if (surface.friction() > SurfaceSample.DEFAULT_FRICTION + 0.01D) {
            state.reset();
            return CheckResult.passed();
        }
        if (surface.collidingHorizontally()) {
            state.reset();
            return CheckResult.passed();
        }

        double travelled = snapshot.horizontalDistance();
        double minimumDistance = context.config().option("minimum-distance", 0.08D);
        if (travelled < minimumDistance) {
            state.reset();
            return CheckResult.passed();
        }

        long velocityGrace = (long) context.config().option("velocity-grace-ticks", 20.0D);
        if (player.velocity().appliedWithin(event.tick(), velocityGrace)) {
            state.reset();
            return CheckResult.passed();
        }

        float maximumTurn = (float) context.config().option("maximum-turn-degrees", 40.0D);
        if (Math.abs(snapshot.fromRotation().yawDifference(snapshot.toRotation())) > maximumTurn) {
            state.reset();
            return CheckResult.passed();
        }

        Vec3 delta = snapshot.delta();
        Vec3 facing = snapshot.toRotation().direction();
        double facingLength = facing.horizontalLength();
        if (facingLength <= 0.0D) {
            state.reset();
            return CheckResult.passed();
        }

        double alignment = (delta.x() * facing.x() + delta.z() * facing.z()) / (travelled * facingLength);
        alignment = Math.max(-1.0D, Math.min(1.0D, alignment));
        double offBy = Math.toDegrees(Math.acos(alignment));

        double allowed = context.config().option("maximum-offset-degrees", 60.0D);

        if (context.isDebugWatched()) {
            double reported = offBy;
            context.debug(() -> "sprint direction off=" + reported + " allowed=" + allowed
                    + " travelled=" + travelled);
        }

        if (offBy <= allowed) {
            state.reset();
            return CheckResult.passed();
        }

        state.consecutive++;
        int requiredStreak = context.config().optionInt("required-streak", 6);
        if (state.consecutive < requiredStreak) {
            return CheckResult.passed();
        }
        if (context.support().hasNearbyPusher(player.uuid())) {
            state.reset();
            return CheckResult.passed();
        }

        state.lastFlaggedTick = event.tick();
        double severity = ConfidenceModel.severityRatio(offBy, allowed,
                context.config().option("severity-scale", 0.6D));
        return CheckResult.flag(severity, "sprinting while travelling away from the facing direction")
                .with("offset_degrees", offBy)
                .with("allowed_degrees", allowed)
                .with("travelled", travelled)
                .with("yaw", snapshot.toRotation().yaw())
                .with("streak", state.consecutive)
                .build();
    }
}
