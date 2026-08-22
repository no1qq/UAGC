package io.github.no1qq.uagc.engine.check;

import java.util.Locale;

public enum CheckCategory {
    MOVEMENT,
    COMBAT,
    INTERACTION,
    INVENTORY,
    PROTOCOL;

    private final String id = name().toLowerCase(Locale.ROOT);

    public String id() {
        return id;
    }

    public String bypassPermission() {
        return "uagc.bypass." + id;
    }

    public static CheckCategory fromId(String value) {
        if (value == null) {
            return null;
        }
        for (CheckCategory category : values()) {
            if (category.id.equalsIgnoreCase(value)) {
                return category;
            }
        }
        return null;
    }
}
