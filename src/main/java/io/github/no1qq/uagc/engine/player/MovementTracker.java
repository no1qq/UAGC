package io.github.no1qq.uagc.engine.player;

import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.movement.Rotation;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.engine.util.RingBuffer;

public final class MovementTracker {

    private final RingBuffer<MovementSnapshot> snapshots;

    private MovementSnapshot last;
    private MovementSnapshot previous;
    private long lastTick = Long.MIN_VALUE;
    private long tickGap;
    private boolean continuous;

    private int airTicks;
    private int groundTicks;
    private int liquidTicks;
    private int climbTicks;
    private double lastGroundY = Double.NaN;
    private long lastGroundTick = Long.MIN_VALUE;
    private Vec3 lastGroundPosition;

    private double verticalDelta;
    private double previousVerticalDelta;
    private double horizontalSpeed;
    private double previousHorizontalSpeed;
    private double maxHorizontalSpeedBudget;

    public MovementTracker(int historySize) {
        this.snapshots = new RingBuffer<>(historySize);
    }

    public void update(MovementSnapshot snapshot) {
        previous = last;
        previousVerticalDelta = verticalDelta;
        previousHorizontalSpeed = horizontalSpeed;

        tickGap = lastTick == Long.MIN_VALUE ? 0L : snapshot.tick() - lastTick;
        continuous = previous != null && tickGap >= 0L && tickGap <= 1L;

        last = snapshot;
        lastTick = snapshot.tick();
        snapshots.add(snapshot);

        verticalDelta = snapshot.verticalDelta();
        horizontalSpeed = snapshot.horizontalDistance();

        if (snapshot.surface().solidBelow()) {
            groundTicks++;
            airTicks = 0;
            lastGroundY = snapshot.to().y();
            lastGroundTick = snapshot.tick();
            lastGroundPosition = snapshot.to();
        } else {
            airTicks++;
            groundTicks = 0;
        }

        if (snapshot.surface().inLiquid()) {
            liquidTicks++;
        } else {
            liquidTicks = 0;
        }

        if (snapshot.surface().onClimbable()) {
            climbTicks++;
        } else {
            climbTicks = 0;
        }
    }

    public void reset() {
        snapshots.clear();
        last = null;
        previous = null;
        lastTick = Long.MIN_VALUE;
        tickGap = 0L;
        continuous = false;
        airTicks = 0;
        groundTicks = 0;
        liquidTicks = 0;
        climbTicks = 0;
        lastGroundY = Double.NaN;
        lastGroundTick = Long.MIN_VALUE;
        lastGroundPosition = null;
        verticalDelta = 0.0D;
        previousVerticalDelta = 0.0D;
        horizontalSpeed = 0.0D;
        previousHorizontalSpeed = 0.0D;
        maxHorizontalSpeedBudget = 0.0D;
    }

    public void breakContinuity() {
        continuous = false;
        previous = null;
        previousVerticalDelta = 0.0D;
        previousHorizontalSpeed = 0.0D;
    }

    public MovementSnapshot last() {
        return last;
    }

    public MovementSnapshot previous() {
        return previous;
    }

    public RingBuffer<MovementSnapshot> history() {
        return snapshots;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public long tickGap() {
        return tickGap;
    }

    public int airTicks() {
        return airTicks;
    }

    public int groundTicks() {
        return groundTicks;
    }

    public int liquidTicks() {
        return liquidTicks;
    }

    public int climbTicks() {
        return climbTicks;
    }

    public double lastGroundY() {
        return lastGroundY;
    }

    public long lastGroundTick() {
        return lastGroundTick;
    }

    public Vec3 lastGroundPosition() {
        return lastGroundPosition;
    }

    public double verticalDelta() {
        return verticalDelta;
    }

    public double previousVerticalDelta() {
        return previousVerticalDelta;
    }

    public double horizontalSpeed() {
        return horizontalSpeed;
    }

    public double previousHorizontalSpeed() {
        return previousHorizontalSpeed;
    }

    public double maxHorizontalSpeedBudget() {
        return maxHorizontalSpeedBudget;
    }

    public void setMaxHorizontalSpeedBudget(double value) {
        this.maxHorizontalSpeedBudget = value;
    }

    public Rotation lastRotation() {
        return last == null ? Rotation.ZERO : last.toRotation();
    }

    public Vec3 lastPosition() {
        return last == null ? Vec3.ZERO : last.to();
    }
}
