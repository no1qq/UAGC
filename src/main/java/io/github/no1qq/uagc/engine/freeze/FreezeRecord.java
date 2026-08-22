package io.github.no1qq.uagc.engine.freeze;

import java.util.UUID;

public record FreezeRecord(
        UUID playerId,
        String playerName,
        String staffName,
        String reason,
        long startMillis,
        long expiresMillis,
        String worldName,
        double x,
        double y,
        double z) {

    public boolean isPermanentUntilReleased() {
        return expiresMillis <= 0L;
    }

    public boolean isExpired(long nowMillis) {
        return !isPermanentUntilReleased() && nowMillis >= expiresMillis;
    }

    public long remainingMillis(long nowMillis) {
        return isPermanentUntilReleased() ? -1L : Math.max(0L, expiresMillis - nowMillis);
    }

    public long durationMillis(long nowMillis) {
        return Math.max(0L, nowMillis - startMillis);
    }
}
