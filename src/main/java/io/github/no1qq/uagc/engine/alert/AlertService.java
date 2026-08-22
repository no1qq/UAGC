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

public final class AlertService {

    public static final String PERMISSION_VIEW = "uagc.alerts.view";

    private final PlayerDataManager players;
    private final ServerContext server;
    private final MessageGateway messages;
    private final Map<String, Long> lastAlertTick = new HashMap<>();

    private volatile AlertConfig config;

    public AlertService(PlayerDataManager players, ServerContext server, MessageGateway messages, AlertConfig config) {
        this.players = players;
        this.server = server;
        this.messages = messages;
        this.config = config;
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
        if (current.sendToConsole()) {
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
