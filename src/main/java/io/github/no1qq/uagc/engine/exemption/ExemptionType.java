package io.github.no1qq.uagc.engine.exemption;

import io.github.no1qq.uagc.engine.check.CheckCategory;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public enum ExemptionType {
    JOIN(20, CheckCategory.MOVEMENT, CheckCategory.COMBAT, CheckCategory.INTERACTION, CheckCategory.INVENTORY, CheckCategory.PROTOCOL),
    RESPAWN(20, CheckCategory.MOVEMENT, CheckCategory.COMBAT, CheckCategory.INTERACTION),
    DEATH(20, CheckCategory.MOVEMENT, CheckCategory.COMBAT, CheckCategory.INTERACTION),
    TELEPORT(10, CheckCategory.MOVEMENT),
    WORLD_CHANGE(40, CheckCategory.MOVEMENT, CheckCategory.COMBAT, CheckCategory.INTERACTION),
    PORTAL(60, CheckCategory.MOVEMENT),
    CHUNK_LOAD(10, CheckCategory.MOVEMENT),
    SETBACK(6, CheckCategory.MOVEMENT),
    VELOCITY(8, CheckCategory.MOVEMENT),
    KNOCKBACK(8, CheckCategory.MOVEMENT),
    EXPLOSION(20, CheckCategory.MOVEMENT),
    PISTON(10, CheckCategory.MOVEMENT),
    VEHICLE(10, CheckCategory.MOVEMENT),
    VEHICLE_EXIT(20, CheckCategory.MOVEMENT),
    SLIME_BOUNCE(20, CheckCategory.MOVEMENT),
    BED_BOUNCE(20, CheckCategory.MOVEMENT),
    ELYTRA(20, CheckCategory.MOVEMENT),
    RIPTIDE(40, CheckCategory.MOVEMENT),
    LEVITATION(10, CheckCategory.MOVEMENT),
    FLIGHT_TOGGLE(20, CheckCategory.MOVEMENT),
    GAMEMODE_CHANGE(40, CheckCategory.MOVEMENT, CheckCategory.INTERACTION),
    EFFECT_CHANGE(10, CheckCategory.MOVEMENT),
    ATTRIBUTE_CHANGE(10, CheckCategory.MOVEMENT, CheckCategory.COMBAT, CheckCategory.INTERACTION),
    SLEEP(20, CheckCategory.MOVEMENT),
    SERVER_LAG(20, CheckCategory.MOVEMENT, CheckCategory.COMBAT, CheckCategory.INTERACTION, CheckCategory.INVENTORY, CheckCategory.PROTOCOL),
    PLUGIN_MOVEMENT(0, CheckCategory.MOVEMENT),
    PLUGIN_SPEED(0, CheckCategory.MOVEMENT),
    PLUGIN_TELEPORT(0, CheckCategory.MOVEMENT),
    PLUGIN_VELOCITY(0, CheckCategory.MOVEMENT),
    PLUGIN_COMBAT(0, CheckCategory.COMBAT),
    PLUGIN_INTERACTION(0, CheckCategory.INTERACTION),
    PLUGIN_INVENTORY(0, CheckCategory.INVENTORY);

    private final int defaultDurationTicks;
    private final Set<CheckCategory> categories;
    private final String id = name().toLowerCase(Locale.ROOT);

    ExemptionType(int defaultDurationTicks, CheckCategory... categories) {
        this.defaultDurationTicks = defaultDurationTicks;
        this.categories = categories.length == 0
                ? EnumSet.noneOf(CheckCategory.class)
                : EnumSet.copyOf(java.util.Arrays.asList(categories));
    }

    public int defaultDurationTicks() {
        return defaultDurationTicks;
    }

    public Set<CheckCategory> categories() {
        return categories;
    }

    public boolean affects(CheckCategory category) {
        return categories.contains(category);
    }

    public boolean isPluginProvided() {
        return name().startsWith("PLUGIN_");
    }

    public String id() {
        return id;
    }

    public static ExemptionType fromId(String value) {
        if (value == null) {
            return null;
        }
        for (ExemptionType type : values()) {
            if (type.id.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
