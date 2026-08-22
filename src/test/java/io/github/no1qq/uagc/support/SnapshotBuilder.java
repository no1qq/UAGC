package io.github.no1qq.uagc.support;

import io.github.no1qq.uagc.engine.movement.ActivitySample;
import io.github.no1qq.uagc.engine.movement.AttributeSample;
import io.github.no1qq.uagc.engine.movement.EffectSample;
import io.github.no1qq.uagc.engine.movement.GameModeType;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.movement.Rotation;
import io.github.no1qq.uagc.engine.movement.SurfaceSample;
import io.github.no1qq.uagc.engine.movement.Vec3;

public final class SnapshotBuilder {

    private long tick;
    private long timeMillis = 1_700_000_000_000L;
    private Vec3 from = Vec3.ZERO;
    private Vec3 to = Vec3.ZERO;
    private Rotation fromRotation = Rotation.ZERO;
    private Rotation toRotation = Rotation.ZERO;
    private boolean clientOnGround = true;
    private double fallDistance;
    private int ping = 30;
    private SurfaceSample surface = Surfaces.ground();
    private ActivitySample activity = ActivitySample.idle();
    private AttributeSample attributes = AttributeSample.vanilla();
    private EffectSample effects = EffectSample.none();

    public static SnapshotBuilder create() {
        return new SnapshotBuilder();
    }

    public SnapshotBuilder tick(long value) {
        this.tick = value;
        this.timeMillis = 1_700_000_000_000L + value * 50L;
        return this;
    }

    public SnapshotBuilder timeMillis(long value) {
        this.timeMillis = value;
        return this;
    }

    public SnapshotBuilder from(Vec3 value) {
        this.from = value;
        return this;
    }

    public SnapshotBuilder to(Vec3 value) {
        this.to = value;
        return this;
    }

    public SnapshotBuilder move(Vec3 origin, double dx, double dy, double dz) {
        this.from = origin;
        this.to = new Vec3(origin.x() + dx, origin.y() + dy, origin.z() + dz);
        return this;
    }

    public SnapshotBuilder rotation(float yaw, float pitch) {
        this.toRotation = new Rotation(yaw, pitch);
        return this;
    }

    public SnapshotBuilder fromRotation(float yaw, float pitch) {
        this.fromRotation = new Rotation(yaw, pitch);
        return this;
    }

    public SnapshotBuilder clientOnGround(boolean value) {
        this.clientOnGround = value;
        return this;
    }

    public SnapshotBuilder fallDistance(double value) {
        this.fallDistance = value;
        return this;
    }

    public SnapshotBuilder ping(int value) {
        this.ping = value;
        return this;
    }

    public SnapshotBuilder surface(SurfaceSample value) {
        this.surface = value;
        return this;
    }

    public SnapshotBuilder activity(ActivitySample value) {
        this.activity = value;
        return this;
    }

    public SnapshotBuilder sprinting(boolean value) {
        this.activity = new ActivitySample(value, activity.sneaking(), activity.swimming(), activity.gliding(),
                activity.climbing(), activity.riptiding(), activity.flying(), activity.allowFlight(),
                activity.insideVehicle(), activity.sleeping(), activity.dead(), activity.gameMode(),
                activity.vehicleType());
        return this;
    }

    public SnapshotBuilder gameMode(GameModeType mode) {
        this.activity = new ActivitySample(activity.sprinting(), activity.sneaking(), activity.swimming(),
                activity.gliding(), activity.climbing(), activity.riptiding(), activity.flying(),
                activity.allowFlight(), activity.insideVehicle(), activity.sleeping(), activity.dead(),
                mode, activity.vehicleType());
        return this;
    }

    public SnapshotBuilder attributes(AttributeSample value) {
        this.attributes = value;
        return this;
    }

    public SnapshotBuilder effects(EffectSample value) {
        this.effects = value;
        return this;
    }

    public MovementSnapshot build() {
        return new MovementSnapshot(tick, timeMillis, from, to, fromRotation, toRotation,
                clientOnGround, fallDistance, ping, surface, activity, attributes, effects);
    }
}
