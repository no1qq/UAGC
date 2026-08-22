package io.github.no1qq.uagc.engine.checks.movement;

import io.github.no1qq.uagc.engine.movement.MovementPredictor;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.movement.SurfaceSample;

public final class RestrictedSpeedModel {

    public static final class Envelope {
        double value;
        boolean seeded;

        public void reset() {
            value = 0.0D;
            seeded = false;
        }
    }

    private RestrictedSpeedModel() {
    }

    public static double allowedThisTick(Envelope envelope, MovementSnapshot snapshot, double multiplier) {
        SurfaceSample surface = snapshot.surface();
        double friction = surface.friction() > 0.0D ? surface.friction() : SurfaceSample.DEFAULT_FRICTION;
        double restricted = MovementPredictor.effectiveMovementSpeed(snapshot.attributes(), snapshot.activity())
                * multiplier;
        double terminal = MovementPredictor.terminalGroundSpeed(restricted, friction);
        double actual = snapshot.horizontalDistance();

        if (!envelope.seeded) {
            envelope.seeded = true;
            envelope.value = Math.max(actual, terminal);
            return Double.MAX_VALUE;
        }

        boolean onGround = surface.solidBelow();
        double momentum = onGround ? MovementPredictor.groundMomentum(friction) : MovementPredictor.AIR_MOMENTUM;
        double acceleration = onGround
                ? MovementPredictor.groundAcceleration(restricted, friction)
                : MovementPredictor.airAcceleration(restricted, false);

        double allowed = Math.max(envelope.value * momentum + acceleration, terminal);
        envelope.value = Math.min(actual, allowed);
        return allowed;
    }
}
