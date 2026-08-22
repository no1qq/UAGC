package io.github.no1qq.uagc.bukkit.compat;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Attributes {

    private static final List<String> UNRESOLVED = new ArrayList<>();

    public static final Attribute MOVEMENT_SPEED = resolve("MOVEMENT_SPEED", "GENERIC_MOVEMENT_SPEED");
    public static final Attribute JUMP_STRENGTH = resolve("JUMP_STRENGTH", "GENERIC_JUMP_STRENGTH");
    public static final Attribute GRAVITY = resolve("GRAVITY", "GENERIC_GRAVITY");
    public static final Attribute STEP_HEIGHT = resolve("STEP_HEIGHT", "GENERIC_STEP_HEIGHT");
    public static final Attribute SCALE = resolve("SCALE", "GENERIC_SCALE");
    public static final Attribute SAFE_FALL_DISTANCE = resolve("SAFE_FALL_DISTANCE", "GENERIC_SAFE_FALL_DISTANCE");
    public static final Attribute FALL_DAMAGE_MULTIPLIER =
            resolve("FALL_DAMAGE_MULTIPLIER", "GENERIC_FALL_DAMAGE_MULTIPLIER");
    public static final Attribute ENTITY_INTERACTION_RANGE =
            resolve("ENTITY_INTERACTION_RANGE", "PLAYER_ENTITY_INTERACTION_RANGE");
    public static final Attribute BLOCK_INTERACTION_RANGE =
            resolve("BLOCK_INTERACTION_RANGE", "PLAYER_BLOCK_INTERACTION_RANGE");
    public static final Attribute SNEAKING_SPEED = resolve("SNEAKING_SPEED", "PLAYER_SNEAKING_SPEED");
    public static final Attribute MOVEMENT_EFFICIENCY = resolve("MOVEMENT_EFFICIENCY", "GENERIC_MOVEMENT_EFFICIENCY");
    public static final Attribute WATER_MOVEMENT_EFFICIENCY =
            resolve("WATER_MOVEMENT_EFFICIENCY", "GENERIC_WATER_MOVEMENT_EFFICIENCY");

    private Attributes() {
    }

    public static double value(Player player, Attribute attribute, double fallback) {
        if (attribute == null) {
            return fallback;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return fallback;
        }
        double value = instance.getValue();
        return Double.isFinite(value) ? value : fallback;
    }

    public static List<String> unresolved() {
        return Collections.unmodifiableList(UNRESOLVED);
    }

    private static Attribute resolve(String current, String legacy) {
        Attribute attribute = lookup(current);
        if (attribute == null) {
            attribute = lookup(legacy);
        }
        if (attribute == null) {
            UNRESOLVED.add(current);
        }
        return attribute;
    }

    private static Attribute lookup(String name) {
        try {
            Field field = Attribute.class.getField(name);
            Object value = field.get(null);
            return value instanceof Attribute found ? found : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }
}
