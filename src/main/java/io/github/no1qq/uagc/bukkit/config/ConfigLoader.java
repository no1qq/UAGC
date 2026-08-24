package io.github.no1qq.uagc.bukkit.config;

import io.github.no1qq.uagc.engine.config.AlertConfig;
import io.github.no1qq.uagc.engine.config.CheckConfig;
import io.github.no1qq.uagc.engine.config.ConfidenceSettings;
import io.github.no1qq.uagc.engine.config.DebugConfig;
import io.github.no1qq.uagc.engine.config.FreezeConfig;
import io.github.no1qq.uagc.engine.config.GeneralSettings;
import io.github.no1qq.uagc.engine.config.UagcConfig;
import io.github.no1qq.uagc.engine.player.PlayerDataSettings;
import io.github.no1qq.uagc.engine.punishment.PunishmentAction;
import io.github.no1qq.uagc.engine.punishment.PunishmentConfig;
import io.github.no1qq.uagc.engine.punishment.PunishmentRule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public final class ConfigLoader {

    private ConfigLoader() {
    }

    public static UagcConfig load(FileConfiguration file, Logger logger) {
        return new UagcConfig(
                loadGeneral(file.getConfigurationSection("general")),
                loadPlayerData(file.getConfigurationSection("player-data")),
                loadConfidence(file.getConfigurationSection("confidence")),
                loadAlerts(file.getConfigurationSection("alerts")),
                loadFreeze(file.getConfigurationSection("freeze")),
                loadPunishments(file.getConfigurationSection("punishments"), logger),
                loadDebug(file.getConfigurationSection("debug")),
                loadChecks(file.getConfigurationSection("checks"), logger));
    }

    private static GeneralSettings loadGeneral(ConfigurationSection section) {
        GeneralSettings defaults = GeneralSettings.defaults();
        if (section == null) {
            return defaults;
        }
        return new GeneralSettings(
                section.getBoolean("enabled", defaults.enabled()),
                section.getInt("bypass-refresh-interval-ticks", defaults.bypassRefreshIntervalTicks()),
                section.getInt("lag-spike-threshold-millis", defaults.lagSpikeThresholdMillis()),
                section.getBoolean("exempt-on-lag-spike", defaults.exemptOnLagSpike()),
                section.getInt("max-check-failures-before-disable", defaults.maxCheckFailuresBeforeDisable()),
                section.getBoolean("log-punishments", defaults.logPunishments()),
                section.getBoolean("log-violations-to-console", defaults.logViolationsToConsole()));
    }

    private static PlayerDataSettings loadPlayerData(ConfigurationSection section) {
        PlayerDataSettings defaults = PlayerDataSettings.defaults();
        if (section == null) {
            return defaults;
        }
        return new PlayerDataSettings(
                section.getInt("movement-history-size", defaults.movementHistorySize()),
                section.getInt("latency-sample-size", defaults.latencySampleSize()),
                section.getInt("click-sample-size", defaults.clickSampleSize()),
                section.getInt("evidence-entry-capacity", defaults.evidenceEntryCapacity()),
                section.getInt("evidence-violation-capacity", defaults.evidenceViolationCapacity()),
                section.getBoolean("alerts-enabled-by-default", defaults.alertsEnabledByDefault()),
                section.getDouble("default-alert-confidence", defaults.defaultAlertConfidence()),
                section.getDouble("default-alert-violation-level", defaults.defaultAlertViolationLevel()));
    }

    private static ConfidenceSettings loadConfidence(ConfigurationSection section) {
        ConfidenceSettings defaults = ConfidenceSettings.defaults();
        if (section == null) {
            return defaults;
        }
        return new ConfidenceSettings(
                section.getInt("ping-comfortable-millis", defaults.pingComfortableMillis()),
                section.getInt("ping-severe-millis", defaults.pingSevereMillis()),
                section.getDouble("ping-reliability-floor", defaults.pingReliabilityFloor()),
                section.getDouble("tick-reliability-floor", defaults.tickReliabilityFloor()),
                section.getInt("transition-grace-ticks", defaults.transitionGraceTicks()),
                section.getDouble("transition-reliability-floor", defaults.transitionReliabilityFloor()),
                section.getDouble("jitter-penalty-threshold", defaults.jitterPenaltyThreshold()),
                section.getDouble("jitter-reliability-floor", defaults.jitterReliabilityFloor()));
    }

    private static AlertConfig loadAlerts(ConfigurationSection section) {
        AlertConfig defaults = AlertConfig.defaults();
        if (section == null) {
            return defaults;
        }
        return new AlertConfig(
                section.getBoolean("enabled", defaults.enabled()),
                section.getBoolean("enabled-by-default-for-staff", defaults.enabledByDefaultForStaff()),
                section.getString("format", defaults.format()),
                section.getString("hover-format", defaults.hoverFormat()),
                section.getString("click-command", defaults.clickCommand()),
                section.getDouble("default-minimum-confidence", defaults.defaultMinimumConfidence()),
                section.getDouble("default-minimum-violation-level", defaults.defaultMinimumViolationLevel()),
                section.getInt("cooldown-ticks", defaults.cooldownTicks()),
                section.getBoolean("send-to-console", defaults.sendToConsole()),
                section.getBoolean("flag-on-alert", defaults.flagOnAlert()),
                section.getInt("flag-setback-interval-ticks", defaults.flagSetbackIntervalTicks()));
    }

    private static FreezeConfig loadFreeze(ConfigurationSection section) {
        FreezeConfig defaults = FreezeConfig.defaults();
        if (section == null) {
            return defaults;
        }
        List<String> allowed = section.getStringList("allowed-commands");
        return new FreezeConfig(
                section.getBoolean("block-movement", defaults.blockMovement()),
                section.getBoolean("block-interaction", defaults.blockInteraction()),
                section.getBoolean("block-commands", defaults.blockCommands()),
                section.getBoolean("block-damage", defaults.blockDamage()),
                section.getBoolean("persist-across-reconnect", defaults.persistAcrossReconnect()),
                section.getInt("reminder-interval-ticks", defaults.reminderIntervalTicks()),
                section.getString("frozen-title", defaults.frozenTitle()),
                section.getString("frozen-subtitle", defaults.frozenSubtitle()),
                section.getString("frozen-message", defaults.frozenMessage()),
                section.getString("unfrozen-message", defaults.unfrozenMessage()),
                section.getString("disconnect-action", defaults.disconnectAction()),
                allowed.isEmpty() ? defaults.allowedCommands() : allowed);
    }

    private static DebugConfig loadDebug(ConfigurationSection section) {
        DebugConfig defaults = DebugConfig.defaults();
        if (section == null) {
            return defaults;
        }
        return new DebugConfig(
                section.getBoolean("enabled", defaults.enabled()),
                section.getBoolean("log-internal-check-failures", defaults.logInternalCheckFailures()),
                section.getInt("max-debug-subscribers", defaults.maxDebugSubscribers()),
                section.getInt("debug-message-interval-ticks", defaults.debugMessageIntervalTicks()));
    }

    private static PunishmentConfig loadPunishments(ConfigurationSection section, Logger logger) {
        PunishmentConfig defaults = PunishmentConfig.defaults();
        if (section == null) {
            return defaults;
        }
        List<PunishmentRule> rules = new ArrayList<>();
        List<?> raw = section.getList("rules");
        if (raw != null) {
            for (Object entry : raw) {
                PunishmentRule rule = parseRule(entry, logger);
                if (rule != null) {
                    rules.add(rule);
                }
            }
        }
        return new PunishmentConfig(
                section.getBoolean("enabled", defaults.enabled()),
                section.getBoolean("dry-run", defaults.dryRun()),
                section.getString("default-kick-message", defaults.defaultKickMessage()),
                section.getString("default-ban-message", defaults.defaultBanMessage()),
                section.getString("default-temp-ban-message", defaults.defaultTempBanMessage()),
                section.getString("ban-source", defaults.banSource()),
                List.copyOf(rules));
    }

    private static PunishmentRule parseRule(Object entry, Logger logger) {
        if (!(entry instanceof Map<?, ?> map)) {
            logger.warning("ignoring malformed punishment rule entry");
            return null;
        }
        String actionName = string(map.get("action"), null);
        PunishmentAction action = PunishmentAction.fromId(actionName);
        if (action == null) {
            logger.warning("ignoring punishment rule with unknown action: " + actionName);
            return null;
        }
        return new PunishmentRule(
                string(map.get("scope"), "*"),
                number(map.get("violation-level"), 20.0D),
                number(map.get("minimum-confidence"), 0.6D),
                (int) number(map.get("minimum-flags"), 1.0D),
                action,
                string(map.get("value"), ""),
                string(map.get("reason"), ""),
                bool(map.get("repeatable"), false),
                (int) number(map.get("cooldown-ticks"), 0.0D));
    }

    private static String string(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private static double number(Object value, double fallback) {
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static boolean bool(Object value, boolean fallback) {
        return value instanceof Boolean flag ? flag : fallback;
    }

    private static Map<String, CheckConfig> loadChecks(ConfigurationSection section, Logger logger) {
        if (section == null) {
            return Map.of();
        }
        Map<String, CheckConfig> checks = new LinkedHashMap<>();
        for (String categoryKey : section.getKeys(false)) {
            ConfigurationSection categorySection = section.getConfigurationSection(categoryKey);
            if (categorySection == null) {
                continue;
            }
            for (String checkKey : categorySection.getKeys(false)) {
                ConfigurationSection checkSection = categorySection.getConfigurationSection(checkKey);
                if (checkSection == null) {
                    continue;
                }
                String id = checkKey.toLowerCase(Locale.ROOT);
                checks.put(id, parseCheck(id, checkSection));
            }
        }
        if (checks.isEmpty()) {
            logger.warning("no check configuration sections were found, using built in defaults");
        }
        return Map.copyOf(checks);
    }

    private static CheckConfig parseCheck(String id, ConfigurationSection section) {
        CheckConfig defaults = CheckConfig.defaults(id);
        Map<String, Double> options = new LinkedHashMap<>();
        ConfigurationSection optionSection = section.getConfigurationSection("options");
        if (optionSection != null) {
            for (String key : optionSection.getKeys(false)) {
                Object value = optionSection.get(key);
                if (value instanceof Number number) {
                    options.put(key, number.doubleValue());
                } else if (value instanceof Boolean flag) {
                    options.put(key, flag ? 1.0D : 0.0D);
                }
            }
        }
        return new CheckConfig(
                id,
                section.getBoolean("enabled", defaults.enabled()),
                section.getDouble("violation-increment", defaults.violationIncrement()),
                section.getDouble("decay-per-tick", defaults.decayPerTick()),
                section.getDouble("max-violation-level", defaults.maxViolationLevel()),
                section.getDouble("minimum-confidence", defaults.minimumConfidence()),
                section.getDouble("alert-threshold", defaults.alertThreshold()),
                section.getBoolean("setback-enabled", defaults.setbackEnabled()),
                section.getDouble("setback-threshold", defaults.setbackThreshold()),
                section.getBoolean("cancel-enabled", defaults.cancelEnabled()),
                Map.copyOf(options));
    }
}
