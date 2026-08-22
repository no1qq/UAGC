package io.github.no1qq.uagc.engine.checks.combat;

import io.github.no1qq.uagc.engine.check.Check;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.CheckContext;
import io.github.no1qq.uagc.engine.check.CheckDefinition;
import io.github.no1qq.uagc.engine.check.CheckResult;
import io.github.no1qq.uagc.engine.check.ConfidenceModel;
import io.github.no1qq.uagc.engine.check.event.AttackEvent;
import io.github.no1qq.uagc.engine.player.CombatState;

public final class AttackRhythmCheck implements Check<AttackEvent, Void> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("attack_rhythm", "AttackRhythm", CheckCategory.COMBAT)
            .description("looks for attack intervals that are too regular to come from a human hand")
            .latencySensitive()
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
        CombatState combat = context.player().combat();

        int minimumSamples = context.config().optionInt("minimum-samples", 24);
        if (combat.intervalCount() < minimumSamples) {
            return CheckResult.passed();
        }

        double deviation = combat.intervalDeviation();
        double clicksPerSecond = combat.clicksPerSecond();
        double minimumCps = context.config().option("minimum-cps", 7.0D);
        double maximumDeviation = context.config().option("maximum-deviation-millis", 6.0D);

        if (context.isDebugWatched()) {
            context.debug(() -> "rhythm cps=" + clicksPerSecond + " deviation=" + deviation
                    + " samples=" + combat.intervalCount());
        }

        if (clicksPerSecond < minimumCps || deviation > maximumDeviation) {
            return CheckResult.passed();
        }

        double severity = ConfidenceModel.severity(maximumDeviation - deviation, 0.0D,
                context.config().option("severity-scale", maximumDeviation));
        return CheckResult.flag(severity, "attack intervals were unnaturally consistent")
                .with("deviation_millis", deviation)
                .with("mean_interval_millis", combat.meanInterval())
                .with("clicks_per_second", clicksPerSecond)
                .with("samples", combat.intervalCount())
                .build();
    }
}
