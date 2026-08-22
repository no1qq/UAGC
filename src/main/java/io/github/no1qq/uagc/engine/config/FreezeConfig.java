package io.github.no1qq.uagc.engine.config;

import java.util.List;

public record FreezeConfig(
        boolean blockMovement,
        boolean blockInteraction,
        boolean blockCommands,
        boolean blockDamage,
        boolean persistAcrossReconnect,
        int reminderIntervalTicks,
        String frozenTitle,
        String frozenSubtitle,
        String frozenMessage,
        String unfrozenMessage,
        String disconnectAction,
        List<String> allowedCommands) {

    public static FreezeConfig defaults() {
        return new FreezeConfig(true, true, true, false, true, 60,
                "<red>You are frozen",
                "<gray>Do not log out",
                "<red>You have been frozen by staff. Logging out will be treated as evading the investigation.",
                "<green>You are no longer frozen.",
                "none",
                List.of("msg", "r", "reply", "tell"));
    }
}
