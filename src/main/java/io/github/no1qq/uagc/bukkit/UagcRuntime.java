package io.github.no1qq.uagc.bukkit;

import io.github.no1qq.uagc.bukkit.alert.YamlAlertPreferenceStore;
import io.github.no1qq.uagc.bukkit.debug.DebugService;
import io.github.no1qq.uagc.bukkit.freeze.YamlFreezeStore;
import io.github.no1qq.uagc.bukkit.platform.BukkitEnforcementGateway;
import io.github.no1qq.uagc.bukkit.platform.BukkitMessageGateway;
import io.github.no1qq.uagc.bukkit.platform.BukkitServerContext;
import io.github.no1qq.uagc.bukkit.platform.BukkitSupportQuery;
import io.github.no1qq.uagc.engine.alert.AlertService;
import io.github.no1qq.uagc.engine.bypass.BypassService;
import io.github.no1qq.uagc.engine.check.CheckEngine;
import io.github.no1qq.uagc.engine.check.CheckRegistry;
import io.github.no1qq.uagc.engine.check.ConfidenceModel;
import io.github.no1qq.uagc.engine.checks.CheckBootstrap;
import io.github.no1qq.uagc.engine.config.UagcConfig;
import io.github.no1qq.uagc.engine.freeze.FreezeService;
import io.github.no1qq.uagc.engine.player.PlayerDataManager;
import io.github.no1qq.uagc.engine.punishment.PunishmentService;

import java.io.File;
import java.util.logging.Logger;

public final class UagcRuntime {

    private final BukkitServerContext server;
    private final BukkitMessageGateway messages;
    private final BukkitEnforcementGateway enforcement;
    private final BukkitSupportQuery support;
    private final DebugService debug;
    private final CheckRegistry registry;
    private final PlayerDataManager players;
    private final ConfidenceModel confidence;
    private final AlertService alerts;
    private final FreezeService freeze;
    private final PunishmentService punishments;
    private final CheckEngine engine;
    private final BypassService bypass;

    private volatile UagcConfig config;

    public UagcRuntime(UagcPlugin plugin, UagcConfig initialConfig, Logger logger) {
        this.config = initialConfig;
        this.server = new BukkitServerContext(plugin);
        this.messages = new BukkitMessageGateway(initialConfig.alerts());
        this.debug = new DebugService();
        this.support = new BukkitSupportQuery();

        this.registry = new CheckRegistry();
        CheckBootstrap.registerDefaults(registry, initialConfig);
        registry.freeze();

        this.players = new PlayerDataManager(server, initialConfig.playerData(), registry.size());
        this.confidence = new ConfidenceModel(initialConfig.confidence());
        this.alerts = new AlertService(players, server, messages, initialConfig.alerts(),
                new YamlAlertPreferenceStore(new File(plugin.getDataFolder(), "alert-preferences.yml"), logger));

        YamlFreezeStore store = new YamlFreezeStore(new File(plugin.getDataFolder(), "freezes.yml"), logger);
        this.enforcement = new BukkitEnforcementGateway(plugin, initialConfig.punishments(), plugin::refreshFreezeState);
        this.freeze = new FreezeService(server, messages, enforcement, players, store, initialConfig.freeze());
        this.punishments = new PunishmentService(server, enforcement, freeze, initialConfig.punishments(), 500);
        this.punishments.updateConfig(initialConfig.punishments(), initialConfig.general().logPunishments());

        this.engine = new CheckEngine(registry, confidence, alerts, punishments, server, enforcement, initialConfig);
        this.engine.setDebugSink(debug);
        this.engine.setSupportQuery(support);

        this.bypass = new BypassService(server, players, registry, initialConfig.general().bypassRefreshIntervalTicks());
    }

    public void applyConfig(UagcConfig updated) {
        this.config = updated;
        registry.applyConfig(updated);
        players.updateSettings(updated.playerData());
        confidence.updateSettings(updated.confidence());
        alerts.updateConfig(updated.alerts());
        messages.updateConfig(updated.alerts());
        freeze.updateConfig(updated.freeze());
        enforcement.updateConfig(updated.punishments());
        punishments.updateConfig(updated.punishments(), updated.general().logPunishments());
        engine.updateConfig(updated);
        bypass.updateInterval(updated.general().bypassRefreshIntervalTicks());
    }

    public UagcConfig config() {
        return config;
    }

    public BukkitServerContext server() {
        return server;
    }

    public BukkitMessageGateway messages() {
        return messages;
    }

    public BukkitEnforcementGateway enforcement() {
        return enforcement;
    }

    public DebugService debug() {
        return debug;
    }

    public CheckRegistry registry() {
        return registry;
    }

    public PlayerDataManager players() {
        return players;
    }

    public ConfidenceModel confidence() {
        return confidence;
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

    public BypassService bypass() {
        return bypass;
    }
}
