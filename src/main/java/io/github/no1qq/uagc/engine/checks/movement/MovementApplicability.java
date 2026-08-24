package io.github.no1qq.uagc.engine.checks.movement;

import io.github.no1qq.uagc.engine.movement.ActivitySample;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.movement.SurfaceSample;
import io.github.no1qq.uagc.engine.player.PlayerData;

public final class MovementApplicability {

    private MovementApplicability() {
    }

    public static boolean hasAlternateVerticalPhysics(MovementSnapshot snapshot) {
        ActivitySample activity = snapshot.activity();
        SurfaceSample surface = snapshot.surface();
        return activity.hasAlternateMovement()
                || activity.allowFlight()
                || activity.gameMode().allowsFlight()
                || surface.hasSpecialMovementSurface()
                || surface.insideSolid()
                || !surface.chunkLoaded()
                || snapshot.effects().hasLevitation();
    }

    public static boolean hasAlternateGroundClaim(MovementSnapshot snapshot) {
        ActivitySample activity = snapshot.activity();
        SurfaceSample surface = snapshot.surface();
        return activity.hasAlternateMovement()
                || activity.allowFlight()
                || activity.gameMode().allowsFlight()
                || surface.onClimbable()
                || surface.onScaffolding()
                || surface.onSlime()
                || surface.onHoney()
                || surface.onBed()
                || surface.inPowderSnow()
                || surface.inBerryBush()
                || surface.inBubbleColumn()
                || surface.inLiquid()
                || surface.insideSolid()
                || !surface.chunkLoaded()
                || snapshot.effects().hasLevitation();
    }

    public static boolean hasAlternateHorizontalPhysics(MovementSnapshot snapshot) {
        ActivitySample activity = snapshot.activity();
        SurfaceSample surface = snapshot.surface();
        return activity.hasAlternateMovement()
                || activity.allowFlight()
                || activity.gameMode().allowsFlight()
                || surface.inLiquid()
                || surface.onClimbable()
                || surface.inCobweb()
                || surface.inBubbleColumn()
                || surface.onSlime()
                || surface.onHoney()
                || surface.onBed()
                || surface.insideSolid()
                || !surface.chunkLoaded();
    }

    public static boolean isMeasurable(PlayerData player, MovementSnapshot snapshot) {
        return snapshot.isFinite() && player.movement().isContinuous() && player.movement().tickGap() <= 1L;
    }

    public static double latencyTolerance(PlayerData player, double perHundredMillis) {
        int ping = Math.max(player.latency().lastPing(), 0);
        return Math.min(ping / 100.0D, 8.0D) * perHundredMillis;
    }
}
