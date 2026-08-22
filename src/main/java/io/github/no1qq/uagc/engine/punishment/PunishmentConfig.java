package io.github.no1qq.uagc.engine.punishment;

import java.util.List;

public record PunishmentConfig(
        boolean enabled,
        boolean dryRun,
        String defaultKickMessage,
        String defaultBanMessage,
        String defaultTempBanMessage,
        String banSource,
        List<PunishmentRule> rules) {

    public static PunishmentConfig defaults() {
        return new PunishmentConfig(true, false,
                "<red>UAGC</red> <gray>|</gray> <white>Removed from the server</white>\n<gray>Check: <check></gray>\n<gray>Reference: <reference></gray>",
                "<red>UAGC</red> <gray>|</gray> <white>Banned for cheating</white>\n<gray>Check: <check></gray>\n<gray>Reference: <reference></gray>",
                "<red>UAGC</red> <gray>|</gray> <white>Temporarily banned for cheating</white>\n<gray>Check: <check></gray>\n<gray>Expires: <expiry></gray>\n<gray>Reference: <reference></gray>",
                "UAGC",
                List.of());
    }
}
