package io.github.no1qq.uagc.api;

import java.time.Duration;
import java.util.UUID;

public interface UagcIntegration {

    String pluginName();

    ExemptionHandle exempt(UUID playerId, ExemptionKind kind, Duration duration, String reason);

    void reportTeleport(UUID playerId, String reason);

    void reportVelocity(UUID playerId, double x, double y, double z, String reason);

    void reportCustomMovement(UUID playerId, Duration duration, String reason);

    void reportCustomSpeed(UUID playerId, Duration duration, String reason);
}
