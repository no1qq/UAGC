package io.github.no1qq.uagc.engine.checks.protocol;

import io.github.no1qq.uagc.engine.check.Check;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.CheckContext;
import io.github.no1qq.uagc.engine.check.CheckDefinition;
import io.github.no1qq.uagc.engine.check.CheckResult;
import io.github.no1qq.uagc.engine.check.event.MovementEvent;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.movement.Rotation;
import io.github.no1qq.uagc.engine.movement.Vec3;

public final class InvalidPositionCheck implements Check<MovementEvent, Void> {

    private static final double WORLD_LIMIT = 3.0E7D;
    private static final double VERTICAL_LIMIT = 2.0E4D;

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("invalid_position", "InvalidPosition", CheckCategory.PROTOCOL)
            .description("rejects positions and rotations that the protocol can never legitimately produce")
            .build();

    @Override
    public CheckDefinition definition() {
        return DEFINITION;
    }

    @Override
    public Class<MovementEvent> eventType() {
        return MovementEvent.class;
    }

    @Override
    public boolean ignoresExemptions() {
        return true;
    }

    @Override
    public CheckResult inspect(CheckContext context, MovementEvent event, Void state) {
        MovementSnapshot snapshot = event.snapshot();
        Vec3 to = snapshot.to();
        Rotation rotation = snapshot.toRotation();

        if (!to.isFinite()) {
            return CheckResult.flag(1.0D, "position contains a non finite component")
                    .with("x", Double.toString(to.x()))
                    .with("y", Double.toString(to.y()))
                    .with("z", Double.toString(to.z()))
                    .build();
        }
        if (Math.abs(to.x()) > WORLD_LIMIT || Math.abs(to.z()) > WORLD_LIMIT) {
            return CheckResult.flag(1.0D, "position outside the maximum world coordinate range")
                    .with("x", to.x())
                    .with("z", to.z())
                    .build();
        }
        if (Math.abs(to.y()) > VERTICAL_LIMIT) {
            return CheckResult.flag(1.0D, "position outside the plausible vertical range")
                    .with("y", to.y())
                    .build();
        }
        if (!rotation.isValid()) {
            return CheckResult.flag(1.0D, "rotation outside the protocol defined range")
                    .with("yaw", Double.toString(rotation.yaw()))
                    .with("pitch", Double.toString(rotation.pitch()))
                    .build();
        }
        return CheckResult.passed();
    }
}
