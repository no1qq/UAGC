package io.github.no1qq.uagc.engine.movement;

import io.github.no1qq.uagc.engine.util.MathUtil;

public record Vec3(double x, double y, double z) {

    public static final Vec3 ZERO = new Vec3(0.0D, 0.0D, 0.0D);

    public Vec3 add(Vec3 other) {
        return new Vec3(x + other.x, y + other.y, z + other.z);
    }

    public Vec3 subtract(Vec3 other) {
        return new Vec3(x - other.x, y - other.y, z - other.z);
    }

    public Vec3 multiply(double factor) {
        return new Vec3(x * factor, y * factor, z * factor);
    }

    public double horizontalLength() {
        return Math.sqrt(x * x + z * z);
    }

    public double horizontalLengthSquared() {
        return x * x + z * z;
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public double distanceTo(Vec3 other) {
        return subtract(other).length();
    }

    public double horizontalDistanceTo(Vec3 other) {
        double dx = x - other.x;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public boolean isFinite() {
        return MathUtil.isFinite(x) && MathUtil.isFinite(y) && MathUtil.isFinite(z);
    }

    public Vec3 withY(double newY) {
        return new Vec3(x, newY, z);
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "(%.3f, %.3f, %.3f)", x, y, z);
    }
}
