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
import io.github.no1qq.uagc.engine.movement.SurfaceSample;
import io.github.no1qq.uagc.engine.player.PlayerData;

public final class NoWebCheck implements Check<MovementEvent, NoWebCheck.State> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("no_web", "NoWeb", CheckCategory.MOVEMENT)
            .description("a cobweb multiplies the whole movement of the next tick and drops the carried motion")
            .latencySensitive()
            .tickSensitive()
            .build();

    public static final class State {
        long lastWebTick = Long.MIN_VALUE;
        double carriedHorizontal;
        double carriedDescent;
        int horizontalExcess;
        int verticalExcess;
        double worstDescent;

        void reset() {
            lastWebTick = Long.MIN_VALUE;
            carriedHorizontal = 0.0D;
            carriedDescent = 0.0D;
            horizontalExcess = 0;
            verticalExcess = 0;
            worstDescent = 0.0D;
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
        SurfaceSample surface = snapshot.surface();

        double actual = snapshot.horizontalDistance();
        double descent = -snapshot.verticalDelta();

        if (snapshot.activity().hasAlternateMovement()
                || snapshot.activity().allowFlight()
                || snapshot.activity().gameMode().allowsFlight()
                || surface.inLiquid()
                || surface.insideSolid()
                || !surface.chunkLoaded()
                || snapshot.effects().hasLevitation()
                || !MovementApplicability.isMeasurable(player, snapshot)) {
            state.reset();
            return CheckResult.passed();
        }

        long velocityGrace = (long) context.config().option("velocity-grace-ticks", 20.0D);
        if (player.velocity().appliedWithin(event.tick(), velocityGrace)) {
            state.reset();
            return CheckResult.passed();
        }

        boolean clamped = state.lastWebTick != Long.MIN_VALUE && event.tick() - state.lastWebTick == 1L;
        if (!clamped && !surface.inCobweb()) {
            state.reset();
        }
        if (surface.inCobweb()) {
            state.lastWebTick = event.tick();
        }

        if (!clamped) {
            state.carriedHorizontal = actual;
            state.carriedDescent = Math.max(0.0D, descent);
            return CheckResult.passed();
        }

        double carriedHorizontal = state.carriedHorizontal;
        double carriedDescent = state.carriedDescent;
        state.carriedHorizontal = 0.0D;
        state.carriedDescent = 0.0D;

        CheckResult vertical = inspectDescent(context, state, snapshot, descent, carriedDescent);
        if (vertical != null) {
            return vertical;
        }
        return inspectSpeed(context, state, snapshot, actual, carriedHorizontal);
    }

    private CheckResult inspectSpeed(CheckContext context, State state, MovementSnapshot snapshot,
                                     double actual, double carried) {
        PlayerData player = context.player();
        SurfaceSample surface = snapshot.surface();
        double friction = surface.friction() > 0.0D ? surface.friction() : SurfaceSample.DEFAULT_FRICTION;
        double multiplier = context.config().option("web-multiplier", 0.25D);
        double speed = MovementPredictor.sprintCapableMovementSpeed(snapshot.attributes(), snapshot.activity());
        double momentum = surface.solidBelow()
                ? MovementPredictor.groundMomentum(friction)
                : MovementPredictor.AIR_MOMENTUM;
        double acceleration = surface.solidBelow()
                ? MovementPredictor.groundAcceleration(speed, friction)
                : MovementPredictor.airAcceleration(speed, true);

        double allowed = (carried * momentum + acceleration) * multiplier;
        double tolerance = allowed * context.config().option("relative-tolerance", 0.05D)
                + context.config().option("absolute-tolerance", 0.005D)
                + MovementApplicability.latencyTolerance(player, 0.004D);
        double limit = allowed + tolerance;

        if (context.isDebugWatched()) {
            double reported = limit;
            context.debug(() -> "no web actual=" + actual + " allowed=" + reported);
        }

        if (actual <= limit) {
            return CheckResult.passed();
        }

        state.horizontalExcess++;
        int requiredTicks = context.config().optionInt("required-streak", 2);
        if (state.horizontalExcess < requiredTicks) {
            return CheckResult.passed();
        }
        if (context.support().hasNearbyPusher(player.uuid())) {
            state.reset();
            return CheckResult.passed();
        }

        state.horizontalExcess = 0;
        double severity = ConfidenceModel.severityRatio(actual, limit,
                context.config().option("severity-scale", 0.3D));
        return CheckResult.flag(severity, "moved through a cobweb faster than it permits")
                .with("actual", actual)
                .with("allowed", limit)
                .with("multiplier", multiplier)
                .build();
    }

    private CheckResult inspectDescent(CheckContext context, State state, MovementSnapshot snapshot,
                                       double descent, double carried) {
        PlayerData player = context.player();
        double multiplier = context.config().option("web-vertical-multiplier", 0.05D);
        double gravity = snapshot.attributes().gravity() > 0.0D
                ? snapshot.attributes().gravity()
                : AttributeSample.VANILLA_GRAVITY;
        double predicted = (carried + gravity) * MovementPredictor.VERTICAL_DRAG * multiplier;
        double allowed = Math.max(predicted, context.config().option("maximum-descent", 0.02D))
                + MovementApplicability.latencyTolerance(player, 0.002D);

        if (descent <= allowed) {
            return null;
        }

        state.verticalExcess++;
        state.worstDescent = Math.max(state.worstDescent, descent);

        if (context.isDebugWatched()) {
            double reported = descent;
            context.debug(() -> "no web descent=" + reported + " allowed=" + allowed
                    + " ticks=" + state.verticalExcess);
        }

        int requiredTicks = context.config().optionInt("vertical-required-ticks", 2);
        if (state.verticalExcess < requiredTicks) {
            return null;
        }
        if (context.support().hasNearbyPusher(player.uuid())) {
            state.reset();
            return null;
        }

        double worst = state.worstDescent;
        state.verticalExcess = 0;
        state.worstDescent = 0.0D;
        double severity = ConfidenceModel.severityRatio(worst, allowed,
                context.config().option("vertical-severity-scale", 1.5D));
        return CheckResult.flag(severity, "fell through a cobweb faster than it permits")
                .with("descent", worst)
                .with("allowed", allowed)
                .build();
    }
}
