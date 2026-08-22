package io.github.no1qq.uagc.engine.checks.interaction;

import io.github.no1qq.uagc.engine.check.Check;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.CheckContext;
import io.github.no1qq.uagc.engine.check.CheckDefinition;
import io.github.no1qq.uagc.engine.check.CheckResult;
import io.github.no1qq.uagc.engine.check.ConfidenceModel;
import io.github.no1qq.uagc.engine.check.event.BlockPlaceCheckEvent;
import io.github.no1qq.uagc.engine.movement.AttributeSample;
import io.github.no1qq.uagc.engine.movement.Vec3;

public final class BlockReachCheck implements Check<BlockPlaceCheckEvent, Void> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("block_reach", "BlockReach", CheckCategory.INTERACTION)
            .description("measures placement distance against the block interaction range the player actually has")
            .latencySensitive()
            .build();

    @Override
    public CheckDefinition definition() {
        return DEFINITION;
    }

    @Override
    public Class<BlockPlaceCheckEvent> eventType() {
        return BlockPlaceCheckEvent.class;
    }

    @Override
    public CheckResult inspect(CheckContext context, BlockPlaceCheckEvent event, Void state) {
        Vec3 eye = event.eyePosition();
        Vec3 block = event.againstBlockCenter() == null ? event.placedBlockCenter() : event.againstBlockCenter();
        if (eye == null || block == null || !eye.isFinite() || !block.isFinite()) {
            return CheckResult.passed();
        }

        AttributeSample attributes = event.attributes();
        double range = attributes.blockInteractionRange() > 0.0D
                ? attributes.blockInteractionRange()
                : AttributeSample.VANILLA_BLOCK_INTERACTION_RANGE;
        double scale = attributes.scale() > 0.0D ? attributes.scale() : 1.0D;
        double allowed = range * scale;

        double distance = distanceToBlockSurface(eye, block);
        double tolerance = context.config().option("tolerance", 0.1D)
                + Math.min(event.ping() / 100.0D, 6.0D) * context.config().option("latency-tolerance", 0.04D);
        double limit = allowed + tolerance;

        if (distance <= limit) {
            return CheckResult.passed();
        }

        double severity = ConfidenceModel.severity(distance, limit, context.config().option("severity-scale", 1.5D));
        return CheckResult.flag(severity, "block placement happened outside the usable interaction range")
                .with("distance", distance)
                .with("allowed", limit)
                .with("attribute_range", range)
                .with("block", event.blockType())
                .build();
    }

    private static double distanceToBlockSurface(Vec3 eye, Vec3 center) {
        double dx = Math.max(Math.abs(eye.x() - center.x()) - 0.5D, 0.0D);
        double dy = Math.max(Math.abs(eye.y() - center.y()) - 0.5D, 0.0D);
        double dz = Math.max(Math.abs(eye.z() - center.z()) - 0.5D, 0.0D);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
