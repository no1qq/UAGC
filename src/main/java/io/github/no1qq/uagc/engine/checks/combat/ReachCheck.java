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

public final class ReachCheck implements Check<AttackEvent, Void> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("reach", "Reach", CheckCategory.COMBAT)
            .description("measures attack distance against the interaction range the attacker actually has")
            .latencySensitive()
            .tickSensitive()
            .build();

    @Override
    public CheckDefinition definition() {
        return DEFINITION;
    }

    @Override
    public Class<AttackEvent> eventType() {
        return AttackEvent.class;
    }

    @Override
    public CheckResult inspect(CheckContext context, AttackEvent event, Void state) {
        TargetSample target = event.target();
        if (target == null || !event.eyePosition().isFinite() || !target.position().isFinite()) {
            return CheckResult.passed();
        }
        if (event.inVehicle() || event.usingRiptide()) {
            return CheckResult.passed();
        }

        AttributeSample attributes = event.attributes();
        double range = attributes.entityInteractionRange() > 0.0D
                ? attributes.entityInteractionRange()
                : AttributeSample.VANILLA_ENTITY_INTERACTION_RANGE;
        double scale = attributes.scale() > 0.0D ? attributes.scale() : 1.0D;
        double allowed = range * scale;

        double distance = target.minimumDistanceFrom(event.eyePosition());
        double tolerance = context.config().option("tolerance", 0.06D)
                + Math.min(event.ping() / 100.0D, 6.0D) * context.config().option("latency-tolerance", 0.03D);
        double limit = allowed + tolerance;

        if (context.isDebugWatched()) {
            context.debug(() -> "reach distance=" + distance + " limit=" + limit);
        }

        if (distance <= limit) {
            return CheckResult.passed();
        }

        double severity = ConfidenceModel.severity(distance, limit, context.config().option("severity-scale", 1.2D));
        return CheckResult.flag(severity, "attack distance exceeded the attacker interaction range")
                .with("distance", distance)
                .with("allowed", limit)
                .with("attribute_range", range)
                .with("scale", scale)
                .with("target", target.type())
                .with("target_samples", target.recentPositions().size())
                .with("ping", event.ping())
                .build();
    }
}
