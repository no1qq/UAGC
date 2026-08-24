package io.github.no1qq.uagc.engine.checks.combat;

import io.github.no1qq.uagc.engine.check.Check;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.CheckContext;
import io.github.no1qq.uagc.engine.check.CheckDefinition;
import io.github.no1qq.uagc.engine.check.CheckResult;
import io.github.no1qq.uagc.engine.check.ConfidenceModel;
import io.github.no1qq.uagc.engine.check.event.AttackEvent;
import io.github.no1qq.uagc.engine.check.event.TargetSample;
import io.github.no1qq.uagc.engine.movement.AttributeSample;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;

public final class ReachCheck implements Check<AttackEvent, ReachCheck.State> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("reach", "Reach", CheckCategory.COMBAT)
            .description("measures attack distance against the interaction range the attacker actually has")
            .latencySensitive()
            .tickSensitive()
            .build();

    public static final class State {
        int consecutive;
        long lastAttackTick = Long.MIN_VALUE;
        double worstDistance;
        double worstLimit;

        void reset() {
            consecutive = 0;
            worstDistance = 0.0D;
            worstLimit = 0.0D;
        }
    }

    @Override
    public CheckDefinition definition() {
        return DEFINITION;
    }

    @Override
    public Class<AttackEvent> eventType() {
        return AttackEvent.class;
    }

    @Override
    public State createState() {
        return new State();
    }

    @Override
    public CheckResult inspect(CheckContext context, AttackEvent event, State state) {
        TargetSample target = event.target();
        if (target == null || !event.eyePosition().isFinite() || !target.position().isFinite()) {
            return CheckResult.passed();
        }
        if (event.inVehicle() || event.usingRiptide()) {
            state.reset();
            return CheckResult.passed();
        }

        long streakWindow = (long) context.config().option("streak-window-ticks", 60.0D);
        if (state.lastAttackTick != Long.MIN_VALUE && event.tick() - state.lastAttackTick > streakWindow) {
            state.reset();
        }
        state.lastAttackTick = event.tick();

        AttributeSample attributes = event.attributes();
        double range = attributes.entityInteractionRange() > 0.0D
                ? attributes.entityInteractionRange()
                : AttributeSample.VANILLA_ENTITY_INTERACTION_RANGE;
        double scale = attributes.scale() > 0.0D ? attributes.scale() : 1.0D;
        double allowed = range * scale;

        int rewindTicks = rewindTicks(context, event);
        double distance = target.minimumDistanceFrom(event.eyePosition(), rewindTicks);

        double tolerance = context.config().option("tolerance", 0.06D)
                + Math.min(event.ping() / 100.0D, 6.0D) * context.config().option("latency-tolerance", 0.03D)
                + attackerMotionAllowance(context);
        double limit = allowed + tolerance;

        if (context.isDebugWatched()) {
            context.debug(() -> "reach distance=" + distance + " limit=" + limit + " rewind=" + rewindTicks);
        }

        if (distance <= limit) {
            state.reset();
            return CheckResult.passed();
        }

        state.consecutive++;
        if (distance > state.worstDistance) {
            state.worstDistance = distance;
            state.worstLimit = limit;
        }
        int requiredStreak = context.config().optionInt("required-streak", 3);
        if (state.consecutive < requiredStreak) {
            return CheckResult.passed();
        }

        double reported = state.worstDistance;
        double reportedLimit = state.worstLimit;
        state.reset();

        double severity = ConfidenceModel.severity(reported, reportedLimit,
                context.config().option("severity-scale", 1.2D));
        return CheckResult.flag(severity, "attack distance exceeded the attacker interaction range")
                .with("distance", reported)
                .with("allowed", reportedLimit)
                .with("attribute_range", range)
                .with("scale", scale)
                .with("target", target.type())
                .with("target_samples", target.recentPositions().size())
                .with("rewind_ticks", rewindTicks)
                .with("ping", event.ping())
                .build();
    }

    private int rewindTicks(CheckContext context, AttackEvent event) {
        double minimum = context.config().option("minimum-rewind-ticks", 3.0D);
        double maximum = context.config().option("maximum-rewind-ticks", 8.0D);
        double fromPing = event.ping() / 50.0D;
        return (int) Math.max(minimum, Math.min(maximum, fromPing));
    }

    private double attackerMotionAllowance(CheckContext context) {
        MovementSnapshot last = context.player().movement().last();
        if (last == null || !last.isFinite()) {
            return 0.0D;
        }
        double cap = context.config().option("attacker-motion-cap", 0.4D);
        return Math.min(last.delta().length(), cap);
    }
}
