package io.github.no1qq.uagc.engine.platform;

public interface UagcClock {

    long currentTick();

    long currentTimeMillis();

    static UagcClock fixed(long tick, long millis) {
        return new UagcClock() {
            @Override
            public long currentTick() {
                return tick;
            }

            @Override
            public long currentTimeMillis() {
                return millis;
            }
        };
    }
}
