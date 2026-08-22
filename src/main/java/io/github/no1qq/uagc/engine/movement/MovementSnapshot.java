package io.github.no1qq.uagc.engine.movement;

import java.util.Objects;

public record MovementSnapshot(
        long tick,
        long timeMillis,
        Vec3 from,
        Vec3 to,
        Rotation fromRotation,
        Rotation toRotation,
        boolean clientOnGround,
        double fallDistance,
        int ping,
        SurfaceSample surface,
        ActivitySample activity,
        AttributeSample attributes,
        EffectSample effects) {

    public MovementSnapshot {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(fromRotation, "fromRotation");
        Objects.requireNonNull(toRotation, "toRotation");
        Objects.requireNonNull(surface, "surface");
        Objects.requireNonNull(activity, "activity");
        Objects.requireNonNull(attributes, "attributes");
        Objects.requireNonNull(effects, "effects");
    }

    public Vec3 delta() {
        return to.subtract(from);
    }

    public double horizontalDistance() {
        return to.horizontalDistanceTo(from);
    }

    public double verticalDelta() {
        return to.y() - from.y();
    }

    public boolean positionChanged() {
        return from.x() != to.x() || from.y() != to.y() || from.z() != to.z();
    }

    public boolean rotationChanged() {
        return fromRotation.yaw() != toRotation.yaw() || fromRotation.pitch() != toRotation.pitch();
    }

    public boolean isFinite() {
        return from.isFinite() && to.isFinite() && fromRotation.isValid() && toRotation.isValid();
    }

    public boolean groundStateAgrees() {
        return clientOnGround == surface.solidBelow();
    }
}
