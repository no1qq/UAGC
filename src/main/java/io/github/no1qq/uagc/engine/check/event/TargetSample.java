package io.github.no1qq.uagc.engine.check.event;

import io.github.no1qq.uagc.engine.movement.Vec3;

import java.util.List;
import java.util.UUID;

public record TargetSample(
        UUID id,
        String type,
        boolean player,
        Vec3 position,
        Vec3 velocity,
        double width,
        double height,
        List<Vec3> recentPositions) {

    public double horizontalHalfWidth() {
        return width / 2.0D;
    }

    public double minimumDistanceFrom(Vec3 eye) {
        return minimumDistanceFrom(eye, 0);
    }

    public double minimumDistanceFrom(Vec3 eye, int rewindTicks) {
        double best = distanceToBox(eye, position);
        for (Vec3 historic : recentPositions) {
            best = Math.min(best, distanceToBox(eye, historic));
        }
        if (rewindTicks > 0 && velocity.isFinite() && velocity.lengthSquared() > 1.0E-8D) {
            for (int tick = 1; tick <= rewindTicks; tick++) {
                best = Math.min(best, distanceToBox(eye, position.subtract(velocity.multiply(tick))));
            }
        }
        return best;
    }

    private double distanceToBox(Vec3 eye, Vec3 feet) {
        double half = horizontalHalfWidth();
        double dx = Math.max(Math.max(feet.x() - half - eye.x(), 0.0D), eye.x() - (feet.x() + half));
        double dy = Math.max(Math.max(feet.y() - eye.y(), 0.0D), eye.y() - (feet.y() + height));
        double dz = Math.max(Math.max(feet.z() - half - eye.z(), 0.0D), eye.z() - (feet.z() + half));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
