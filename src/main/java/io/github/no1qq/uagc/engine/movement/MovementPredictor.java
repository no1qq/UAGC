package io.github.no1qq.uagc.engine.movement;

public final class MovementPredictor {

    public static final double VERTICAL_DRAG = 0.98D;
    public static final double AIR_MOMENTUM = 0.91D;
    public static final double SLOW_FALLING_GRAVITY = 0.01D;
    public static final double BASE_FRICTION = 0.6D;
    public static final double SPRINT_MULTIPLIER = 1.3D;
    public static final double SPRINT_JUMP_BOOST = 0.2D;
    public static final double GROUND_ACCELERATION_REFERENCE = 0.6D;

    private MovementPredictor() {
    }

    public static double predictVerticalDelta(double previousDelta, double gravity, boolean slowFalling) {
        double effectiveGravity = slowFalling && previousDelta <= 0.0D ? SLOW_FALLING_GRAVITY : gravity;
        return (previousDelta - effectiveGravity) * VERTICAL_DRAG;
    }

    public static double predictLevitationDelta(double previousDelta, int amplifier) {
        double target = 0.05D * (amplifier + 1);
        return (previousDelta + (target - previousDelta) * 0.2D) * VERTICAL_DRAG;
    }

    public static double jumpMotion(double jumpStrength, int jumpBoostAmplifier, double blockJumpFactor) {
        double motion = jumpStrength * blockJumpFactor;
        if (jumpBoostAmplifier >= 0) {
            motion += 0.1D * (jumpBoostAmplifier + 1);
        }
        return motion;
    }

    public static double groundMomentum(double friction) {
        return friction * AIR_MOMENTUM;
    }

    public static double groundAcceleration(double movementSpeed, double friction) {
        double safeFriction = friction <= 0.0D ? BASE_FRICTION : friction;
        double ratio = GROUND_ACCELERATION_REFERENCE / safeFriction;
        return movementSpeed * ratio * ratio * ratio;
    }

    public static double airAcceleration(double movementSpeed, boolean sprinting) {
        double base = sprinting ? 0.026D : 0.02D;
        double scale = movementSpeed <= 0.0D ? 1.0D : movementSpeed / AttributeSample.VANILLA_MOVEMENT_SPEED;
        return base * Math.max(1.0D, scale);
    }

    public static double terminalGroundSpeed(double movementSpeed, double friction) {
        double momentum = groundMomentum(friction);
        if (momentum >= 1.0D) {
            return Double.MAX_VALUE;
        }
        return groundAcceleration(movementSpeed, friction) / (1.0D - momentum);
    }

    public static double effectiveMovementSpeed(AttributeSample attributes, ActivitySample activity) {
        double speed = attributes.movementSpeed();
        if (speed <= 0.0D) {
            speed = AttributeSample.VANILLA_MOVEMENT_SPEED;
        }
        double sprintAdjusted = activity.sprinting() ? speed * SPRINT_MULTIPLIER : speed;
        double walkDerived = attributes.walkSpeed() > 0.0D
                ? attributes.walkSpeed() / 2.0D * (activity.sprinting() ? SPRINT_MULTIPLIER : 1.0D)
                : 0.0D;
        return Math.max(sprintAdjusted, walkDerived);
    }

    public static double sprintCapableMovementSpeed(AttributeSample attributes, ActivitySample activity) {
        double speed = effectiveMovementSpeed(attributes, activity);
        return activity.sprinting() ? speed : speed * SPRINT_MULTIPLIER;
    }

    public static double flightSpeedBound(AttributeSample attributes) {
        double flySpeed = attributes.flySpeed() > 0.0D ? attributes.flySpeed() : AttributeSample.VANILLA_FLY_SPEED;
        return flySpeed * 11.0D;
    }
}
