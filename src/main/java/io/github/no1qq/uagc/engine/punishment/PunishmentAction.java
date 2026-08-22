package io.github.no1qq.uagc.engine.punishment;

import java.util.Locale;

public enum PunishmentAction {
    ALERT(false),
    LOG(false),
    SETBACK(false),
    CANCEL(false),
    FREEZE(true),
    KICK(true),
    TEMPBAN(true),
    BAN(true),
    COMMAND(true);

    private final String id = name().toLowerCase(Locale.ROOT);
    private final boolean removesOrRestricts;

    PunishmentAction(boolean removesOrRestricts) {
        this.removesOrRestricts = removesOrRestricts;
    }

    public String id() {
        return id;
    }

    public boolean removesOrRestricts() {
        return removesOrRestricts;
    }

    public static PunishmentAction fromId(String value) {
        if (value == null) {
            return null;
        }
        for (PunishmentAction action : values()) {
            if (action.id.equalsIgnoreCase(value)) {
                return action;
            }
        }
        return null;
    }
}
