package io.github.no1qq.uagc.engine.checks.interaction;

import io.github.no1qq.uagc.engine.check.Check;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.CheckContext;
import io.github.no1qq.uagc.engine.check.CheckDefinition;
import io.github.no1qq.uagc.engine.check.CheckResult;
import io.github.no1qq.uagc.engine.check.ConfidenceModel;
import io.github.no1qq.uagc.engine.check.event.BlockBreakCheckEvent;

public final class FastBreakCheck implements Check<BlockBreakCheckEvent, Void> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("fast_break", "FastBreak", CheckCategory.INTERACTION)
            .description("compares block break time against the destroy speed of the tool actually held")
            .latencySensitive()
            .tickSensitive()
            .build();

    @Override
    public CheckDefinition definition() {
        return DEFINITION;
    }

    @Override
    public Class<BlockBreakCheckEvent> eventType() {
        return BlockBreakCheckEvent.class;
    }

    @Override
    public CheckResult inspect(CheckContext context, BlockBreakCheckEvent event, Void state) {
        if (event.creative() || event.instantBreak() || !event.hasDamageStart()) {
            return CheckResult.passed();
        }
        double expected = event.expectedTicks();
        if (!Double.isFinite(expected) || expected <= 1.0D) {
            return CheckResult.passed();
        }

        long elapsed = event.elapsedTicks();
        if (elapsed < 0L) {
            return CheckResult.passed();
        }

        double latencyTicks = Math.min(event.ping() / 50.0D, 8.0D);
        double tolerance = context.config().option("tolerance-ticks", 2.0D) + latencyTicks;
        double minimumExpected = expected - tolerance;

        if (context.isDebugWatched()) {
            context.debug(() -> "fastbreak elapsed=" + elapsed + " expected=" + expected
                    + " block=" + event.blockType());
        }

        if (elapsed >= minimumExpected) {
            return CheckResult.passed();
        }

        double missing = minimumExpected - elapsed;
        double severity = ConfidenceModel.severity(missing, 0.0D,
                Math.max(1.0D, expected * context.config().option("severity-scale-ratio", 0.6D)));
        return CheckResult.flag(severity, "block broke faster than the held tool allows")
                .with("elapsed_ticks", elapsed)
                .with("expected_ticks", expected)
                .with("allowed_ticks", minimumExpected)
                .with("block", event.blockType())
                .with("destroy_speed", event.destroySpeedPerTick())
                .build();
    }
}
