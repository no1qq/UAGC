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

public final class NoSlowCheck implements Check<MovementEvent, NoSlowCheck.State> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("no_slow", "NoSlow", CheckCategory.MOVEMENT)
            .description("a player using an item moves at a fifth of their normal input speed")
            .latencySensitive()
            .tickSensitive()
            .build();

    public static final class State {
        final RestrictedSpeedModel.Envelope envelope = new RestrictedSpeedModel.Envelope();
        int consecutive;
        long startedTick = Long.MIN_VALUE;

        long lastUsingTick = Long.MIN_VALUE;
        int cycles;
        int shortestGap;
        int longestGap;
        int gapTicks;
        double gapDistance;

        void resetSpeed() {
            envelope.reset();
            consecutive = 0;
            startedTick = Long.MIN_VALUE;
        }

        void clearCycles() {
            cycles = 0;
            shortestGap = Integer.MAX_VALUE;
            longestGap = 0;
            gapTicks = 0;
            gapDistance = 0.0D;
        }

        void resetBlink() {
            clearCycles();
            lastUsingTick = Long.MIN_VALUE;
        }

        void recordGap(int gap) {
            cycles++;
            shortestGap = Math.min(shortestGap, gap);
            longestGap = Math.max(longestGap, gap);
        }

        void reset() {
            resetSpeed();
            resetBlink();
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
                || !MovementApplicability.isMeasurable(player, snapshot)
                || !snapshot.surface().solidBelow()) {
            state.reset();
            return CheckResult.passed();
        }

        long velocityGrace = (long) context.config().option("velocity-grace-ticks", 20.0D);
        if (player.velocity().appliedWithin(event.tick(), velocityGrace)) {
            state.reset();
            return CheckResult.passed();
        }

        boolean usingItem = snapshot.activity().usingItem();
        CheckResult blink = inspectBlink(context, event, state, usingItem);
        if (blink != null) {
            return blink;
        }

        if (!usingItem) {
            state.resetSpeed();
            return CheckResult.passed();
        }

        if (state.startedTick == Long.MIN_VALUE) {
            state.startedTick = event.tick();
        }
        long settleTicks = (long) context.config().option("settle-ticks", 4.0D);
        if (event.tick() - state.startedTick < settleTicks) {
            return CheckResult.passed();
        }

        double multiplier = context.config().option("use-item-multiplier", 0.2D);
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
            context.debug(() -> "no slow actual=" + actual + " allowed=" + reported);
        }

        if (actual <= limit) {
            state.consecutive = 0;
            return CheckResult.passed();
        }

        state.consecutive++;
        int requiredStreak = context.config().optionInt("required-streak", 4);
        if (state.consecutive < requiredStreak) {
            return CheckResult.passed();
        }
        if (context.support().hasNearbyPusher(player.uuid())) {
            state.reset();
            return CheckResult.passed();
        }

        double severity = ConfidenceModel.severityRatio(actual, limit,
                context.config().option("severity-scale", 0.3D));
        return CheckResult.flag(severity, "moved at full speed while using an item")
                .with("actual", actual)
                .with("allowed", limit)
                .with("multiplier", multiplier)
                .with("sprinting", snapshot.activity().sprinting())
                .with("streak", state.consecutive)
                .build();
    }

    private CheckResult inspectBlink(CheckContext context, MovementEvent event, State state, boolean usingItem) {
        MovementSnapshot snapshot = event.snapshot();
        int maximumGap = context.config().optionInt("blink-maximum-gap-ticks", 12);

        if (!usingItem) {
            if (state.lastUsingTick == Long.MIN_VALUE || event.tick() - state.lastUsingTick > maximumGap) {
                state.resetBlink();
                return null;
            }
            state.gapTicks++;
            state.gapDistance += snapshot.horizontalDistance();
            return null;
        }

        int gap = state.lastUsingTick == Long.MIN_VALUE
                ? 0
                : (int) (event.tick() - state.lastUsingTick - 1L);
        state.lastUsingTick = event.tick();
        if (gap > maximumGap) {
            state.clearCycles();
            return null;
        }
        int minimumGap = context.config().optionInt("blink-minimum-gap-ticks", 1);
        if (gap < minimumGap) {
            return null;
        }
        state.recordGap(gap);

        int requiredCycles = context.config().optionInt("blink-required-cycles", 8);
        if (state.cycles < requiredCycles || state.gapTicks <= 0) {
            return null;
        }
        int jitter = context.config().optionInt("blink-gap-jitter-ticks", 1);
        if (state.longestGap - state.shortestGap > jitter) {
            state.clearCycles();
            return null;
        }

        SurfaceSample surface = snapshot.surface();
        double friction = surface.friction() > 0.0D ? surface.friction() : SurfaceSample.DEFAULT_FRICTION;
        double multiplier = context.config().option("use-item-multiplier", 0.2D);
        double restricted = MovementPredictor.terminalGroundSpeed(
                MovementPredictor.effectiveMovementSpeed(snapshot.attributes(), snapshot.activity()) * multiplier,
                friction);
        double meanGapSpeed = state.gapDistance / state.gapTicks;
        double ratio = context.config().option("blink-speed-ratio", 1.6D);

        if (context.isDebugWatched()) {
            int cycles = state.cycles;
            context.debug(() -> "no slow blink cycles=" + cycles + " gap=" + gap
                    + " speed=" + meanGapSpeed + " restricted=" + restricted);
        }

        if (restricted == Double.MAX_VALUE || meanGapSpeed <= restricted * ratio) {
            state.clearCycles();
            return null;
        }
        if (context.support().hasNearbyPusher(context.player().uuid())) {
            state.resetBlink();
            return null;
        }

        int cycles = state.cycles;
        double severity = ConfidenceModel.severityRatio(meanGapSpeed, restricted * ratio,
                context.config().option("blink-severity-scale", 0.4D));
        state.clearCycles();
        return CheckResult.flag(severity, "item use was released on a fixed interval to shed the slowdown")
                .with("cycles", cycles)
                .with("gap_ticks", gap)
                .with("gap_speed", meanGapSpeed)
                .with("restricted", restricted)
                .with("multiplier", multiplier)
                .with("sprinting", snapshot.activity().sprinting())
                .build();
    }
}
