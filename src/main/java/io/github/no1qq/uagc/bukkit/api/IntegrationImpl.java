package io.github.no1qq.uagc.bukkit.api;

import io.github.no1qq.uagc.api.ExemptionHandle;
import io.github.no1qq.uagc.api.ExemptionKind;
import io.github.no1qq.uagc.api.UagcIntegration;
import io.github.no1qq.uagc.bukkit.UagcRuntime;
import io.github.no1qq.uagc.engine.evidence.EvidenceEntry;
import io.github.no1qq.uagc.engine.evidence.EvidenceType;
import io.github.no1qq.uagc.engine.exemption.ExemptionGrant;
import io.github.no1qq.uagc.engine.exemption.ExemptionType;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.engine.player.PlayerData;

import java.time.Duration;
import java.util.UUID;

final class IntegrationImpl implements UagcIntegration {

    private static final int MAX_DURATION_TICKS = 20 * 60 * 10;

    private final UagcRuntime runtime;
    private final String pluginName;

    IntegrationImpl(UagcRuntime runtime, String pluginName) {
        this.runtime = runtime;
        this.pluginName = pluginName;
    }

    @Override
    public String pluginName() {
        return pluginName;
    }

    @Override
    public ExemptionHandle exempt(UUID playerId, ExemptionKind kind, Duration duration, String reason) {
        PlayerData data = runtime.players().get(playerId);
        if (data == null || kind == null) {
            return null;
        }
        ExemptionType type = UagcApiImpl.toInternal(kind);
        int ticks = toTicks(duration);
        ExemptionGrant grant = data.exemptions().grant(type, ticks, pluginName, reason);
        if (grant == null) {
            return null;
        }
        data.recordEvidence(EvidenceEntry.of(EvidenceType.INTEGRATION, pluginName + " granted " + type.id())
                .with("kind", kind.id())
                .with("duration_ticks", ticks)
                .with("reason", reason == null ? "" : reason));
        return new Handle(runtime, data, grant, kind, pluginName);
    }

    @Override
    public void reportTeleport(UUID playerId, String reason) {
        PlayerData data = runtime.players().get(playerId);
        if (data == null) {
            return;
        }
        data.exemptions().grant(ExemptionType.PLUGIN_TELEPORT, ExemptionType.TELEPORT.defaultDurationTicks(),
                pluginName, reason);
        data.exemptions().grant(ExemptionType.TELEPORT, 0, pluginName, reason);
        data.movement().breakContinuity();
        data.recordEvidence(EvidenceEntry.of(EvidenceType.INTEGRATION, pluginName + " reported a teleport")
                .with("reason", reason == null ? "" : reason));
    }

    @Override
    public void reportVelocity(UUID playerId, double x, double y, double z, String reason) {
        PlayerData data = runtime.players().get(playerId);
        if (data == null) {
            return;
        }
        Vec3 velocity = new Vec3(x, y, z);
        if (!velocity.isFinite()) {
            return;
        }
        data.velocity().record(velocity, runtime.server().currentTick(), pluginName);
        data.exemptions().grant(ExemptionType.PLUGIN_VELOCITY, ExemptionType.VELOCITY.defaultDurationTicks(),
                pluginName, reason);
        data.exemptions().grant(ExemptionType.VELOCITY, 0, pluginName, reason);
        data.recordEvidence(EvidenceEntry.of(EvidenceType.INTEGRATION, pluginName + " reported custom velocity")
                .with("vector", velocity.toString())
                .with("reason", reason == null ? "" : reason));
    }

    @Override
    public void reportCustomMovement(UUID playerId, Duration duration, String reason) {
        exempt(playerId, ExemptionKind.MOVEMENT, duration, reason);
    }

    @Override
    public void reportCustomSpeed(UUID playerId, Duration duration, String reason) {
        exempt(playerId, ExemptionKind.SPEED, duration, reason);
    }

    private static int toTicks(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return 40;
        }
        long ticks = duration.toMillis() / 50L;
        return (int) Math.min(Math.max(ticks, 1L), MAX_DURATION_TICKS);
    }

    private record Handle(UagcRuntime runtime,
                          PlayerData data,
                          ExemptionGrant grant,
                          ExemptionKind kind,
                          String source) implements ExemptionHandle {

        @Override
        public UUID playerId() {
            return data.uuid();
        }

        @Override
        public String reason() {
            return grant.reason();
        }

        @Override
        public long expiresAtMillis() {
            long remaining = grant.remainingTicks(runtime.server().currentTick());
            return System.currentTimeMillis() + remaining * 50L;
        }

        @Override
        public boolean isActive() {
            return grant.isActive(runtime.server().currentTick());
        }

        @Override
        public void revoke() {
            data.exemptions().revoke(grant.type());
        }
    }
}
