package io.github.no1qq.uagc.api;

import java.util.Locale;

public enum ExemptionKind {
    TELEPORT,
    VELOCITY,
    MOVEMENT,
    SPEED,
    COMBAT,
    INTERACTION,
    INVENTORY;

    private final String id = name().toLowerCase(Locale.ROOT);

    public String id() {
        return id;
    }
}
