package io.github.no1qq.uagc.engine.checks.movement;

import io.github.no1qq.uagc.engine.check.Check;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.CheckContext;
import io.github.no1qq.uagc.engine.check.CheckDefinition;
import io.github.no1qq.uagc.engine.check.CheckResult;
import io.github.no1qq.uagc.engine.check.ConfidenceModel;
import io.github.no1qq.uagc.engine.check.event.MovementEvent;
import io.github.no1qq.uagc.engine.movement.MovementPredictor;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.movement.SurfaceSample;
import io.github.no1qq.uagc.engine.player.PlayerData;

public final class HorizontalSpeedCheck implements Check<MovementEvent, HorizontalSpeedCheck.State> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("horizontal_speed", "HorizontalSpeed", CheckCategory.MOVEMENT)
            .description("bounds horizontal movement by the fastest envelope the current player state can produce")
            .latencySensitive()
            .tickSensitive()
            .build();

    public static final class State {
        double envelope;
        boolean seeded;
        int consecutive;

        void reset() {
            envelope = 0.0D;
            seeded = false;
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

        if (MovementApplicability.hasAlternateHorizontalPhysics(snapshot)
                || !MovementApplicability.isMeasurable(player, snapshot)) {
            state.reset();
            return CheckResult.passed();
        }

        SurfaceSample surface = snapshot.surface();
        double friction = surface.friction() > 0.0D ? surface.friction() : SurfaceSample.DEFAULT_FRICTION;
        double movementSpeed = MovementPredictor.effectiveMovementSpeed(snapshot.attributes(), snapshot.activity());
        double terminal = MovementPredictor.terminalGroundSpeed(movementSpeed, friction);
        double actual = snapshot.horizontalDistance();

        if (!state.seeded) {
            state.seeded = true;
            state.envelope = Math.max(actual, terminal);
            return CheckResult.passed();
        }

        boolean onGround = surface.solidBelow();
        double momentum = onGround ? MovementPredictor.groundMomentum(friction) : MovementPredictor.AIR_MOMENTUM;
        double acceleration = onGround
                ? MovementPredictor.groundAcceleration(movementSpeed, friction)
                : MovementPredictor.airAcceleration(movementSpeed, snapshot.activity().sprinting());

        double envelope = state.envelope * momentum + acceleration;
        if (player.velocity().jumpedWithin(event.tick(), 1L) && snapshot.activity().sprinting()) {
            envelope += MovementPredictor.SPRINT_JUMP_BOOST;
        }
        double allowed = Math.max(envelope, terminal);

        if (player.velocity().appliedWithin(event.tick(), 10L)) {
            allowed = Math.max(allowed, player.velocity().horizontalMagnitude());
        }

        double tolerance = allowed * context.config().option("relative-tolerance", 0.03D)
                + context.config().option("absolute-tolerance", 0.005D)
                + MovementApplicability.latencyTolerance(player, 0.006D);

        double limit = allowed + tolerance;
        state.envelope = Math.min(actual, allowed);

        if (context.isDebugWatched()) {
            double reportedLimit = limit;
            context.debug(() -> "horizontal actual=" + actual + " allowed=" + reportedLimit
                    + " ground=" + onGround + " friction=" + friction);
        }

        if (actual <= limit) {
            state.consecutive = 0;
            return CheckResult.passed();
        }

        state.consecutive++;
        int requiredStreak = context.config().optionInt("required-streak", 2);
        if (state.consecutive < requiredStreak) {
            return CheckResult.passed();
        }
        if (context.support().hasNearbyPusher(player.uuid())) {
            state.reset();
            return CheckResult.passed();
        }

        double severity = ConfidenceModel.severityRatio(actual, limit, context.config().option("severity-scale", 0.35D));
        return CheckResult.flag(severity, "horizontal movement exceeded the reachable speed envelope")
                .with("actual", actual)
                .with("allowed", limit)
                .with("terminal", terminal)
                .with("friction", friction)
                .with("on_ground", onGround)
                .with("sprinting", snapshot.activity().sprinting())
                .with("movement_speed", movementSpeed)
                .with("streak", state.consecutive)
                .setback()
                .build();
    }
}
