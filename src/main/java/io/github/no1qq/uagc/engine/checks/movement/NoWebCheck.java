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

public final class NoWebCheck implements Check<MovementEvent, NoWebCheck.State> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("no_web", "NoWeb", CheckCategory.MOVEMENT)
            .description("a cobweb clamps horizontal motion to a quarter of it every tick")
            .latencySensitive()
            .tickSensitive()
            .build();

    public static final class State {
        final RestrictedSpeedModel.Envelope envelope = new RestrictedSpeedModel.Envelope();
        int consecutive;
        long enteredTick = Long.MIN_VALUE;

        void reset() {
            envelope.reset();
            consecutive = 0;
            enteredTick = Long.MIN_VALUE;
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

        if (!snapshot.surface().inCobweb()) {
            state.reset();
            return CheckResult.passed();
        }
        if (snapshot.activity().hasAlternateMovement()
                || snapshot.activity().allowFlight()
                || snapshot.activity().gameMode().allowsFlight()
                || snapshot.surface().inLiquid()
                || snapshot.surface().insideSolid()
                || !snapshot.surface().chunkLoaded()
                || !MovementApplicability.isMeasurable(player, snapshot)) {
            state.reset();
            return CheckResult.passed();
        }

        if (state.enteredTick == Long.MIN_VALUE) {
            state.enteredTick = event.tick();
        }
        long settleTicks = (long) context.config().option("settle-ticks", 3.0D);
        if (event.tick() - state.enteredTick < settleTicks) {
            return CheckResult.passed();
        }

        long velocityGrace = (long) context.config().option("velocity-grace-ticks", 20.0D);
        if (player.velocity().appliedWithin(event.tick(), velocityGrace)) {
            state.reset();
            return CheckResult.passed();
        }

        double multiplier = context.config().option("web-multiplier", 0.25D);
        double allowed = RestrictedSpeedModel.allowedThisTick(state.envelope, snapshot, multiplier);
        if (allowed == Double.MAX_VALUE) {
            return CheckResult.passed();
        }

        double actual = snapshot.horizontalDistance();
        double tolerance = allowed * context.config().option("relative-tolerance", 0.05D)
                + context.config().option("absolute-tolerance", 0.005D)
                + MovementApplicability.latencyTolerance(player, 0.004D);
        double limit = allowed + tolerance;

        if (context.isDebugWatched()) {
            double reported = limit;
            context.debug(() -> "no web actual=" + actual + " allowed=" + reported);
        }

        if (actual <= limit) {
            state.consecutive = 0;
            return CheckResult.passed();
        }

        state.consecutive++;
        int requiredStreak = context.config().optionInt("required-streak", 3);
        if (state.consecutive < requiredStreak) {
            return CheckResult.passed();
        }
        if (context.support().hasNearbyPusher(player.uuid())) {
            state.reset();
            return CheckResult.passed();
        }

        double severity = ConfidenceModel.severityRatio(actual, limit,
                context.config().option("severity-scale", 0.3D));
        return CheckResult.flag(severity, "moved through a cobweb faster than it permits")
                .with("actual", actual)
                .with("allowed", limit)
                .with("multiplier", multiplier)
                .with("streak", state.consecutive)
                .build();
    }
}
