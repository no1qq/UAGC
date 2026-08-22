package io.github.no1qq.uagc.engine.movement;

import io.github.no1qq.uagc.engine.util.MathUtil;

public record Rotation(float yaw, float pitch) {

    public static final Rotation ZERO = new Rotation(0.0F, 0.0F);

    public float yawDifference(Rotation other) {
        return MathUtil.angleDifference(yaw, other.yaw);
    }

    public float pitchDifference(Rotation other) {
        return Math.abs(other.pitch - pitch);
    }

    public boolean isValid() {
        return MathUtil.isFinite(yaw) && MathUtil.isFinite(pitch) && pitch >= -90.0F && pitch <= 90.0F;
    }

    public Vec3 direction() {
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRadians);
        return new Vec3(-cosPitch * Math.sin(yawRadians), -Math.sin(pitchRadians), cosPitch * Math.cos(yawRadians));
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "yaw=%.2f pitch=%.2f", yaw, pitch);
    }
}
