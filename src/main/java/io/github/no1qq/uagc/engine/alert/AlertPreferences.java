package io.github.no1qq.uagc.engine.alert;

import java.util.Map;
import java.util.UUID;

public record AlertPreferences(Map<UUID, Boolean> players, Boolean console) {

    public AlertPreferences {
        players = Map.copyOf(players);
    }

    public static AlertPreferences empty() {
        return new AlertPreferences(Map.of(), null);
    }
}
