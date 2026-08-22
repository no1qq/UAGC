package io.github.no1qq.uagc.support;

import io.github.no1qq.uagc.engine.alert.AlertService;
import io.github.no1qq.uagc.engine.bypass.BypassService;
import io.github.no1qq.uagc.engine.check.Check;
import io.github.no1qq.uagc.engine.check.CheckEngine;
import io.github.no1qq.uagc.engine.check.CheckEvent;
import io.github.no1qq.uagc.engine.check.CheckRegistry;
import io.github.no1qq.uagc.engine.check.ConfidenceModel;
import io.github.no1qq.uagc.engine.config.UagcConfig;
import io.github.no1qq.uagc.engine.freeze.FreezeService;
import io.github.no1qq.uagc.engine.freeze.FreezeStore;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.player.PlayerDataManager;
import io.github.no1qq.uagc.engine.punishment.PunishmentService;

import java.util.UUID;

public final class EngineHarness {

    private final TestClock clock = new TestClock();
    private final TestServerContext server = new TestServerContext(clock);
    private final RecordingMessageGateway messages = new RecordingMessageGateway();
    private final RecordingEnforcementGateway enforcement = new RecordingEnforcementGateway();
    private final CheckRegistry registry = new CheckRegistry();
    private final PlayerDataManager players;
    private final ConfidenceModel confidence;
    private final AlertService alerts;
    private final FreezeService freeze;
    private final PunishmentService punishments;
    private final CheckEngine engine;
    private final BypassService bypass;

    private UagcConfig config;

    @SafeVarargs
    public EngineHarness(UagcConfig config, Check<? extends CheckEvent, ?>... checks) {
        this.config = config;
        for (Check<? extends CheckEvent, ?> check : checks) {
            registry.register(check, config);
        }
        registry.freeze();

        this.players = new PlayerDataManager(clock, config.playerData(), Math.max(1, registry.size()));
        this.confidence = new ConfidenceModel(config.confidence());
        this.alerts = new AlertService(players, server, messages, config.alerts());
        this.freeze = new FreezeService(server, messages, enforcement, players, FreezeStore.MEMORY_ONLY, config.freeze());
        this.punishments = new PunishmentService(server, enforcement, freeze, config.punishments(), 128);
        this.engine = new CheckEngine(registry, confidence, alerts, punishments, server, enforcement, config);
        this.bypass = new BypassService(server, players, registry, config.general().bypassRefreshIntervalTicks());
    }

    public PlayerData addPlayer(String name) {
        UUID id = UUID.randomUUID();
        server.setOnline(id, true);
        PlayerData data = players.create(id, name);
        bypass.refresh(data);
        return data;
    }

    public void refreshBypass(PlayerData data) {
        bypass.refresh(data);
    }

    public void process(PlayerData data, CheckEvent event) {
        engine.process(data, event);
    }

    public TestClock clock() {
        return clock;
    }

    public TestServerContext server() {
        return server;
    }

    public RecordingMessageGateway messages() {
        return messages;
    }

    public RecordingEnforcementGateway enforcement() {
        return enforcement;
    }

    public CheckRegistry registry() {
        return registry;
    }

    public PlayerDataManager players() {
        return players;
    }

    public AlertService alerts() {
        return alerts;
    }

    public FreezeService freeze() {
        return freeze;
    }

    public PunishmentService punishments() {
        return punishments;
    }

    public CheckEngine engine() {
        return engine;
    }

    public UagcConfig config() {
        return config;
    }
}
