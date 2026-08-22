package io.github.no1qq.uagc.engine.exemption;

import java.util.Objects;

public record ExemptionGrant(
        ExemptionType type,
        long grantedTick,
        long expiresTick,
        long grantedAtMillis,
        String source,
        String reason) {

    public ExemptionGrant {
        Objects.requireNonNull(type, "type");
        source = source == null ? "uagc" : source;
        reason = reason == null ? "" : reason;
    }

    public boolean isActive(long currentTick) {
        return currentTick < expiresTick;
    }

    public long remainingTicks(long currentTick) {
        return Math.max(0L, expiresTick - currentTick);
    }

    public long durationTicks() {
        return Math.max(0L, expiresTick - grantedTick);
    }
}
