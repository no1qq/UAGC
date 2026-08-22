package io.github.no1qq.uagc.engine.check.event;

import io.github.no1qq.uagc.engine.check.CheckEvent;
import io.github.no1qq.uagc.engine.movement.AttributeSample;
import io.github.no1qq.uagc.engine.movement.Rotation;
import io.github.no1qq.uagc.engine.movement.Vec3;

public record BlockPlaceCheckEvent(
        long tick,
        long timeMillis,
        Vec3 eyePosition,
        Rotation rotation,
        Vec3 placedBlockCenter,
        Vec3 againstBlockCenter,
        String blockType,
        String faceName,
        boolean againstBlockSolid,
        AttributeSample attributes,
        int ping) implements CheckEvent {
}
