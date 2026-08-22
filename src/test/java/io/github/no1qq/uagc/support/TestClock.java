package io.github.no1qq.uagc.support;

import io.github.no1qq.uagc.engine.platform.UagcClock;

public final class TestClock implements UagcClock {

    private long tick;
    private long millis = 1_700_000_000_000L;

    @Override
    public long currentTick() {
        return tick;
    }

    @Override
    public long currentTimeMillis() {
        return millis;
    }

    public void advanceTicks(long amount) {
        tick += amount;
        millis += amount * 50L;
    }

    public void advanceMillis(long amount) {
        millis += amount;
    }

    public void setTick(long value) {
        this.tick = value;
    }

    public void setMillis(long value) {
        this.millis = value;
    }
}
