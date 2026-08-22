package io.github.no1qq.uagc.engine.movement;

public record AttributeSample(
        double walkSpeed,
        double flySpeed,
        double movementSpeed,
        double jumpStrength,
        double gravity,
        double stepHeight,
        double scale,
        double safeFallDistance,
        double fallDamageMultiplier,
        double entityInteractionRange,
        double blockInteractionRange,
        double sneakingSpeed,
        double movementEfficiency,
        double waterMovementEfficiency) {

    public static final double VANILLA_WALK_SPEED = 0.2D;
    public static final double VANILLA_FLY_SPEED = 0.1D;
    public static final double VANILLA_MOVEMENT_SPEED = 0.1D;
    public static final double VANILLA_JUMP_STRENGTH = 0.42D;
    public static final double VANILLA_GRAVITY = 0.08D;
    public static final double VANILLA_STEP_HEIGHT = 0.6D;
    public static final double VANILLA_ENTITY_INTERACTION_RANGE = 3.0D;
    public static final double VANILLA_BLOCK_INTERACTION_RANGE = 4.5D;

    public static AttributeSample vanilla() {
        return new AttributeSample(VANILLA_WALK_SPEED, VANILLA_FLY_SPEED, VANILLA_MOVEMENT_SPEED,
                VANILLA_JUMP_STRENGTH, VANILLA_GRAVITY, VANILLA_STEP_HEIGHT, 1.0D, 3.0D, 1.0D,
                VANILLA_ENTITY_INTERACTION_RANGE, VANILLA_BLOCK_INTERACTION_RANGE, 0.3D, 0.0D, 0.0D);
    }

    public double movementSpeedRatio() {
        if (VANILLA_MOVEMENT_SPEED <= 0.0D) {
            return 1.0D;
        }
        return movementSpeed / VANILLA_MOVEMENT_SPEED;
    }

    public double walkSpeedRatio() {
        if (VANILLA_WALK_SPEED <= 0.0D) {
            return 1.0D;
        }
        return walkSpeed / VANILLA_WALK_SPEED;
    }

    public boolean isModified() {
        return Math.abs(walkSpeed - VANILLA_WALK_SPEED) > 1.0E-6D
                || Math.abs(movementSpeed - VANILLA_MOVEMENT_SPEED) > 1.0E-6D
                || Math.abs(jumpStrength - VANILLA_JUMP_STRENGTH) > 1.0E-6D
                || Math.abs(gravity - VANILLA_GRAVITY) > 1.0E-6D
                || Math.abs(scale - 1.0D) > 1.0E-6D
                || Math.abs(stepHeight - VANILLA_STEP_HEIGHT) > 1.0E-6D;
    }
}
