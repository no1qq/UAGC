package io.github.no1qq.uagc.engine.checks.interaction;

import io.github.no1qq.uagc.engine.check.Check;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.CheckContext;
import io.github.no1qq.uagc.engine.check.CheckDefinition;
import io.github.no1qq.uagc.engine.check.CheckResult;
import io.github.no1qq.uagc.engine.check.event.BlockPlaceCheckEvent;

public final class InvalidPlacementCheck implements Check<BlockPlaceCheckEvent, Void> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("invalid_placement", "InvalidPlacement", CheckCategory.INTERACTION)
            .description("rejects placements made against a face that cannot support a block placement")
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
        if (event.againstBlockCenter() == null) {
            return CheckResult.passed();
        }
        if (event.againstBlockSolid()) {
            return CheckResult.passed();
        }
        return CheckResult.flag(context.config().option("severity", 0.8D),
                        "block placed against a face with no supporting block")
                .with("block", event.blockType())
                .with("face", event.faceName())
                .with("against", event.againstBlockCenter().toString())
                .build();
    }
}
