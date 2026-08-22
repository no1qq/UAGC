package io.github.no1qq.uagc.engine.check.event;

import io.github.no1qq.uagc.engine.check.CheckEvent;
import io.github.no1qq.uagc.engine.movement.AttributeSample;
import io.github.no1qq.uagc.engine.movement.Rotation;
import io.github.no1qq.uagc.engine.movement.Vec3;

public record AttackEvent(
        long tick,
        long timeMillis,
        Vec3 eyePosition,
        Rotation rotation,
        Rotation previousRotation,
        TargetSample target,
        AttributeSample attributes,
        int ping,
        boolean sprinting,
        boolean usingRiptide,
        boolean inVehicle) implements CheckEvent {
}
