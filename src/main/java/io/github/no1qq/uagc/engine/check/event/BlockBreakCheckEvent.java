package io.github.no1qq.uagc.engine.check.event;

import io.github.no1qq.uagc.engine.check.CheckEvent;
import io.github.no1qq.uagc.engine.movement.AttributeSample;
import io.github.no1qq.uagc.engine.movement.Vec3;

public record BlockBreakCheckEvent(
        long tick,
        long timeMillis,
        Vec3 eyePosition,
        Vec3 blockCenter,
        String blockType,
        double destroySpeedPerTick,
        long damageStartTick,
        boolean instantBreak,
        boolean creative,
        AttributeSample attributes,
        int ping) implements CheckEvent {

    public long elapsedTicks() {
        return damageStartTick == Long.MIN_VALUE ? -1L : tick - damageStartTick;
    }

    public boolean hasDamageStart() {
        return damageStartTick != Long.MIN_VALUE;
    }

    public double expectedTicks() {
        if (destroySpeedPerTick <= 0.0D) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.ceil(1.0D / destroySpeedPerTick);
    }
}
