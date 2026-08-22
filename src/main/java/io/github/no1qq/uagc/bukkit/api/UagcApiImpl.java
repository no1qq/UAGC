package io.github.no1qq.uagc.bukkit.api;

import io.github.no1qq.uagc.api.ExemptionKind;
import io.github.no1qq.uagc.api.UagcApi;
import io.github.no1qq.uagc.api.UagcIntegration;
import io.github.no1qq.uagc.api.UagcQuery;
import io.github.no1qq.uagc.bukkit.UagcRuntime;
import io.github.no1qq.uagc.engine.bypass.BypassState;
import io.github.no1qq.uagc.engine.check.RegisteredCheck;
import io.github.no1qq.uagc.engine.exemption.ExemptionGrant;
import io.github.no1qq.uagc.engine.exemption.ExemptionType;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.violation.ViolationTracker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class UagcApiImpl implements UagcApi, UagcQuery {

    private final UagcRuntime runtime;
    private final Map<String, UagcIntegration> integrations = new ConcurrentHashMap<>();

    public UagcApiImpl(UagcRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public String apiVersion() {
        return UagcApi.API_VERSION;
    }

    @Override
    public boolean isEnabled() {
        return runtime.config().general().enabled();
    }

    @Override
    public UagcIntegration integration(String pluginName) {
        String name = pluginName == null || pluginName.isBlank() ? "unknown" : pluginName;
        return integrations.computeIfAbsent(name, key -> new IntegrationImpl(runtime, key));
    }

    @Override
    public UagcQuery query() {
        return this;
    }

    static ExemptionType toInternal(ExemptionKind kind) {
        return switch (kind) {
            case TELEPORT -> ExemptionType.PLUGIN_TELEPORT;
            case VELOCITY -> ExemptionType.PLUGIN_VELOCITY;
            case MOVEMENT -> ExemptionType.PLUGIN_MOVEMENT;
            case SPEED -> ExemptionType.PLUGIN_SPEED;
            case COMBAT -> ExemptionType.PLUGIN_COMBAT;
            case INTERACTION -> ExemptionType.PLUGIN_INTERACTION;
            case INVENTORY -> ExemptionType.PLUGIN_INVENTORY;
        };
    }

    @Override
    public boolean isTracked(UUID playerId) {
        return runtime.players().get(playerId) != null;
    }

    @Override
    public boolean isExempt(UUID playerId, ExemptionKind kind) {
        PlayerData data = runtime.players().get(playerId);
        return data != null && data.exemptions().isExempt(toInternal(kind));
    }

    @Override
    public boolean isFrozen(UUID playerId) {
        return runtime.freeze().isFrozen(playerId);
    }

    @Override
    public boolean hasBypass(UUID playerId) {
        PlayerData data = runtime.players().get(playerId);
        if (data == null) {
            return false;
        }
        BypassState state = data.bypass();
        return state.hasAnyBypass(runtime.server().currentTick());
    }

    @Override
    public double violationLevel(UUID playerId, String checkId) {
        PlayerData data = runtime.players().get(playerId);
        if (data == null) {
            return 0.0D;
        }
        RegisteredCheck registered = runtime.registry().byId(checkId);
        if (registered == null) {
            return 0.0D;
        }
        ViolationTracker tracker = data.violationsIfPresent(registered.index());
        if (tracker == null) {
            return 0.0D;
        }
        return tracker.current(runtime.server().currentTick(), registered.config().decayPerTick());
    }

    @Override
    public Map<String, Double> violationLevels(UUID playerId) {
        PlayerData data = runtime.players().get(playerId);
        if (data == null) {
            return Map.of();
        }
        Map<String, Double> levels = new LinkedHashMap<>();
        long tick = runtime.server().currentTick();
        for (RegisteredCheck registered : runtime.registry().all()) {
            ViolationTracker tracker = data.violationsIfPresent(registered.index());
            if (tracker == null || !tracker.hasEverFlagged()) {
                continue;
            }
            levels.put(registered.id(), tracker.current(tick, registered.config().decayPerTick()));
        }
        return Map.copyOf(levels);
    }

    @Override
    public List<String> checkIds() {
        return runtime.registry().ids();
    }

    @Override
    public List<String> activeExemptions(UUID playerId) {
        PlayerData data = runtime.players().get(playerId);
        if (data == null) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (ExemptionGrant grant : data.exemptions().active()) {
            ids.add(grant.type().id());
        }
        return List.copyOf(ids);
    }
}
