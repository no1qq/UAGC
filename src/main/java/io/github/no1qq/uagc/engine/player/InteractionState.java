package io.github.no1qq.uagc.engine.player;

public final class InteractionState {

    private long damageStartTick = Long.MIN_VALUE;
    private long damageStartMillis = Long.MIN_VALUE;
    private long damagedBlockKey = Long.MIN_VALUE;
    private long lastBreakTick = Long.MIN_VALUE;
    private long lastPlaceTick = Long.MIN_VALUE;
    private long lastInteractTick = Long.MIN_VALUE;
    private int placesInWindow;
    private long placeWindowStartTick = Long.MIN_VALUE;

    public void beginBlockDamage(long blockKey, long tick, long millis) {
        if (blockKey != damagedBlockKey) {
            damagedBlockKey = blockKey;
            damageStartTick = tick;
            damageStartMillis = millis;
        }
    }

    public void abortBlockDamage() {
        damagedBlockKey = Long.MIN_VALUE;
        damageStartTick = Long.MIN_VALUE;
        damageStartMillis = Long.MIN_VALUE;
    }

    public long damageStartTickFor(long blockKey) {
        return damagedBlockKey == blockKey ? damageStartTick : Long.MIN_VALUE;
    }

    public long damageStartMillisFor(long blockKey) {
        return damagedBlockKey == blockKey ? damageStartMillis : Long.MIN_VALUE;
    }

    public void recordBreak(long tick) {
        this.lastBreakTick = tick;
        abortBlockDamage();
    }

    public void recordPlace(long tick) {
        this.lastPlaceTick = tick;
        if (placeWindowStartTick == Long.MIN_VALUE || tick - placeWindowStartTick > 20L) {
            placeWindowStartTick = tick;
            placesInWindow = 1;
        } else {
            placesInWindow++;
        }
    }

    public void recordInteract(long tick) {
        this.lastInteractTick = tick;
    }

    public void reset() {
        abortBlockDamage();
        lastBreakTick = Long.MIN_VALUE;
        lastPlaceTick = Long.MIN_VALUE;
        lastInteractTick = Long.MIN_VALUE;
        placesInWindow = 0;
        placeWindowStartTick = Long.MIN_VALUE;
    }

    public long lastBreakTick() {
        return lastBreakTick;
    }

    public long lastPlaceTick() {
        return lastPlaceTick;
    }

    public long lastInteractTick() {
        return lastInteractTick;
    }

    public int placesInWindow() {
        return placesInWindow;
    }
}
