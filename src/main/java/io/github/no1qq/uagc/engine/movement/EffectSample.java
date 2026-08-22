package io.github.no1qq.uagc.engine.movement;

public record EffectSample(
        int speed,
        int slowness,
        int jumpBoost,
        int levitation,
        boolean slowFalling,
        boolean dolphinsGrace,
        boolean blindness) {

    public static final int NONE = -1;

    public static EffectSample none() {
        return new EffectSample(NONE, NONE, NONE, NONE, false, false, false);
    }

    public boolean hasSpeed() {
        return speed > NONE;
    }

    public boolean hasSlowness() {
        return slowness > NONE;
    }

    public boolean hasJumpBoost() {
        return jumpBoost > NONE;
    }

    public boolean hasLevitation() {
        return levitation > NONE;
    }

    public double speedMultiplier() {
        double multiplier = 1.0D;
        if (hasSpeed()) {
            multiplier *= 1.0D + 0.2D * (speed + 1);
        }
        if (hasSlowness()) {
            multiplier *= Math.max(0.0D, 1.0D - 0.15D * (slowness + 1));
        }
        return multiplier;
    }

    public boolean affectsVerticalMovement() {
        return hasLevitation() || slowFalling || hasJumpBoost();
    }
}
