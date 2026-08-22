package io.github.no1qq.uagc.engine.movement;

public record SurfaceSample(
        boolean solidBelow,
        boolean nearGround,
        double distanceToGround,
        boolean collidingHorizontally,
        boolean insideSolid,
        boolean inWater,
        boolean inLava,
        boolean submerged,
        boolean inBubbleColumn,
        boolean onClimbable,
        boolean onSlime,
        boolean onHoney,
        boolean onBed,
        boolean onScaffolding,
        boolean inPowderSnow,
        boolean inCobweb,
        boolean inBerryBush,
        boolean onStairsOrSlab,
        double friction,
        boolean chunkLoaded,
        String blockBelow) {

    public static final double DEFAULT_FRICTION = 0.6D;

    public static SurfaceSample unknown() {
        return new SurfaceSample(false, false, Double.NaN, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                DEFAULT_FRICTION, false, "unknown");
    }

    public boolean inLiquid() {
        return inWater || inLava;
    }

    public boolean hasSpecialMovementSurface() {
        return onSlime || onHoney || onBed || onScaffolding || inPowderSnow || inCobweb || inBerryBush
                || onClimbable || inBubbleColumn || inLiquid();
    }

    public boolean isSlippery() {
        return friction > DEFAULT_FRICTION + 1.0E-4D;
    }
}
