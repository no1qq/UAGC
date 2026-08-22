package io.github.no1qq.uagc.engine.config;

import io.github.no1qq.uagc.engine.player.PlayerDataSettings;
import io.github.no1qq.uagc.engine.punishment.PunishmentConfig;

import java.util.Map;

public record UagcConfig(
        GeneralSettings general,
        PlayerDataSettings playerData,
        ConfidenceSettings confidence,
        AlertConfig alerts,
        FreezeConfig freeze,
        PunishmentConfig punishments,
        DebugConfig debug,
        Map<String, CheckConfig> checks) {

    public static UagcConfig defaults() {
        return new UagcConfig(
                GeneralSettings.defaults(),
                PlayerDataSettings.defaults(),
                ConfidenceSettings.defaults(),
                AlertConfig.defaults(),
                FreezeConfig.defaults(),
                PunishmentConfig.defaults(),
                DebugConfig.defaults(),
                Map.of());
    }

    public CheckConfig checkConfig(String checkId) {
        CheckConfig config = checks.get(checkId);
        return config == null ? CheckConfig.defaults(checkId) : config;
    }
}
