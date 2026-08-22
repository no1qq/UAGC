package io.github.no1qq.uagc.engine.alert;

import io.github.no1qq.uagc.engine.config.AlertConfig;
import io.github.no1qq.uagc.engine.platform.MessageGateway;
import io.github.no1qq.uagc.engine.platform.ServerContext;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.player.PlayerDataManager;
import io.github.no1qq.uagc.engine.violation.Violation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AlertService {

    public static final String PERMISSION_VIEW = "uagc.alerts.view";

    private final PlayerDataManager players;
    private final ServerContext server;
    private final MessageGateway messages;
    private final AlertPreferenceStore store;
    private final Map<String, Long> lastAlertTick = new HashMap<>();
    private final Map<UUID, Boolean> preferences = new ConcurrentHashMap<>();

    private volatile AlertConfig config;
    private volatile Boolean consoleOverride;

    public AlertService(PlayerDataManager players, ServerContext server, MessageGateway messages, AlertConfig config) {
        this(players, server, messages, config, AlertPreferenceStore.MEMORY_ONLY);
    }

    public AlertService(PlayerDataManager players,
                        ServerContext server,
                        MessageGateway messages,
                        AlertConfig config,
                        AlertPreferenceStore store) {
        this.players = players;
        this.server = server;
        this.messages = messages;
        this.config = config;
        this.store = store;
    }

    public void loadPersisted() {
        AlertPreferences loaded = store.load();
        preferences.clear();
        preferences.putAll(loaded.players());
        consoleOverride = loaded.console();
    }

    public void persist() {
        store.save(new AlertPreferences(preferences, consoleOverride));
    }

    public void remember(UUID playerId, boolean enabled) {
        preferences.put(playerId, enabled);
        persist();
    }

    public boolean consoleAlertsEnabled() {
        Boolean override = consoleOverride;
        return override != null ? override : config.sendToConsole();
    }

    public void setConsoleAlerts(boolean enabled) {
        consoleOverride = enabled;
        persist();
    }

    public boolean toggleConsoleAlerts() {
        boolean enabled = !consoleAlertsEnabled();
        setConsoleAlerts(enabled);
        return enabled;
    }

    public void applyTo(PlayerData data, boolean hasViewPermission) {
        Boolean explicit = preferences.get(data.uuid());
        data.alertSettings().setEnabled(explicit != null
                ? explicit
                : config.enabledByDefaultForStaff() && hasViewPermission);
    }

    public void updateConfig(AlertConfig updated) {
        this.config = updated;
    }

    public AlertConfig config() {
        return config;
    }

    public void dispatch(Violation violation, double punishThreshold, boolean suppressedByBypass) {
        AlertConfig current = config;
        if (!current.enabled()) {
            return;
        }
        long tick = server.clock().currentTick();
        String key = violation.playerId() + ":" + violation.checkId();
        int repeat = 1;
        Long previous = lastAlertTick.get(key);
        if (previous != null) {
            if (tick - previous < current.cooldownTicks()) {
                return;
            }
            repeat = violation.streak();
        }
        lastAlertTick.put(key, tick);

        AlertSeverity severity = AlertSeverity.from(violation.confidence(), violation.violationLevel(), punishThreshold);
        Alert alert = new Alert(violation, repeat, severity, suppressedByBypass);

        for (PlayerData candidate : players.all()) {
            if (!isEligible(candidate, alert)) {
                continue;
            }
            messages.sendAlert(candidate.uuid(), alert);
        }
        if (consoleAlertsEnabled()) {
            messages.sendConsoleAlert(alert);
        }
    }

    private boolean isEligible(PlayerData candidate, Alert alert) {
        AlertSettings settings = candidate.alertSettings();
        if (!settings.enabled()) {
            return false;
        }
        Violation violation = alert.violation();
        if (settings.isMuted(violation.category(), violation.checkId())) {
            return false;
        }
        if (violation.confidence() < settings.minimumConfidence()) {
            return false;
        }
        if (violation.violationLevel() < settings.minimumViolationLevel()) {
            return false;
        }
        return server.hasPermission(candidate.uuid(), PERMISSION_VIEW);
    }

    public void forget(UUID playerId) {
        lastAlertTick.entrySet().removeIf(entry -> entry.getKey().startsWith(playerId.toString()));
    }

    public void clear() {
        lastAlertTick.clear();
    }
}
