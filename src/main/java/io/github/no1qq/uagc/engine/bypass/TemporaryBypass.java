package io.github.no1qq.uagc.engine.bypass;

import java.util.Objects;

public record TemporaryBypass(
        BypassScope scope,
        long grantedTick,
        long expiresTick,
        long grantedAtMillis,
        String grantedBy,
        String reason) {

    public TemporaryBypass {
        Objects.requireNonNull(scope, "scope");
        grantedBy = grantedBy == null ? "unknown" : grantedBy;
        reason = reason == null ? "" : reason;
    }

    public boolean isActive(long currentTick) {
        return expiresTick < 0L || currentTick < expiresTick;
    }

    public boolean isPermanentUntilRevoked() {
        return expiresTick < 0L;
    }

    public long remainingTicks(long currentTick) {
        if (isPermanentUntilRevoked()) {
            return -1L;
        }
        return Math.max(0L, expiresTick - currentTick);
    }
}
