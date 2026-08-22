package io.github.no1qq.uagc.support;

import io.github.no1qq.uagc.engine.movement.SurfaceSample;

public final class Surfaces {

    private Surfaces() {
    }

    public static SurfaceSample ground() {
        return ground(SurfaceSample.DEFAULT_FRICTION, "stone");
    }

    public static SurfaceSample ground(double friction, String block) {
        return new SurfaceSample(true, true, 0.0D, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                friction, true, block);
    }

    public static SurfaceSample ice() {
        return ground(0.98D, "ice");
    }

    public static SurfaceSample air(double distanceToGround) {
        boolean nearGround = distanceToGround < 0.5D;
        return new SurfaceSample(false, nearGround, distanceToGround, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false,
                SurfaceSample.DEFAULT_FRICTION, true, "air");
    }

    public static SurfaceSample water() {
        return new SurfaceSample(false, false, 4.0D, false, false, true, false, false, false,
                false, false, false, false, false, false, false, false, false,
                SurfaceSample.DEFAULT_FRICTION, true, "water");
    }

    public static SurfaceSample climbable() {
        return new SurfaceSample(false, false, 3.0D, false, false, false, false, false, false,
                true, false, false, false, false, false, false, false, false,
                SurfaceSample.DEFAULT_FRICTION, true, "ladder");
    }

    public static SurfaceSample cobweb() {
        return new SurfaceSample(true, true, 0.0D, false, false, false, false, false, false,
                false, false, false, false, false, false, true, false, false,
                SurfaceSample.DEFAULT_FRICTION, true, "stone");
    }

    public static SurfaceSample slime() {
        return new SurfaceSample(true, true, 0.0D, false, false, false, false, false, false,
                false, true, false, false, false, false, false, false, false,
                0.8D, true, "slime_block");
    }
}
