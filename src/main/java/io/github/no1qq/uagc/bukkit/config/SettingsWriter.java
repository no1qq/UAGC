package io.github.no1qq.uagc.bukkit.config;

import io.github.no1qq.uagc.bukkit.UagcPlugin;

import java.util.Locale;

public final class SettingsWriter {

    public enum Result {
        APPLIED,
        UNKNOWN_PATH,
        WRONG_TYPE,
        RELOAD_FAILED
    }

    private SettingsWriter() {
    }

    public static boolean exists(UagcPlugin plugin, String path) {
        return plugin.getConfig().contains(path);
    }

    public static boolean isBoolean(UagcPlugin plugin, String path) {
        return plugin.getConfig().get(path) instanceof Boolean;
    }

    public static boolean isNumber(UagcPlugin plugin, String path) {
        return plugin.getConfig().get(path) instanceof Number;
    }

    public static boolean isInteger(UagcPlugin plugin, String path) {
        return plugin.getConfig().get(path) instanceof Integer;
    }

    public static Result toggle(UagcPlugin plugin, String path) {
        if (!exists(plugin, path)) {
            return Result.UNKNOWN_PATH;
        }
        if (!isBoolean(plugin, path)) {
            return Result.WRONG_TYPE;
        }
        return write(plugin, path, !plugin.getConfig().getBoolean(path));
    }

    public static Result setNumber(UagcPlugin plugin, String path, double value, double minimum, double maximum) {
        if (!exists(plugin, path)) {
            return Result.UNKNOWN_PATH;
        }
        if (!isNumber(plugin, path)) {
            return Result.WRONG_TYPE;
        }
        if (!Double.isFinite(value)) {
            return Result.WRONG_TYPE;
        }
        double clamped = Math.max(minimum, Math.min(maximum, value));
        Object stored = isInteger(plugin, path)
                ? (Object) (int) Math.round(clamped)
                : (Object) round(clamped);
        return write(plugin, path, stored);
    }

    public static Result setText(UagcPlugin plugin, String path, String value) {
        if (!exists(plugin, path)) {
            return Result.UNKNOWN_PATH;
        }
        if (!(plugin.getConfig().get(path) instanceof String)) {
            return Result.WRONG_TYPE;
        }
        return write(plugin, path, value);
    }

    public static Result parseAndSet(UagcPlugin plugin, String path, String rawValue) {
        if (!exists(plugin, path)) {
            return Result.UNKNOWN_PATH;
        }
        String value = rawValue.trim();
        Object current = plugin.getConfig().get(path);
        if (current instanceof Boolean) {
            if (value.equalsIgnoreCase("toggle")) {
                return toggle(plugin, path);
            }
            if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                return Result.WRONG_TYPE;
            }
            return write(plugin, path, Boolean.parseBoolean(value));
        }
        if (current instanceof Number) {
            try {
                return setNumber(plugin, path, Double.parseDouble(value), -1.0E9D, 1.0E9D);
            } catch (NumberFormatException exception) {
                return Result.WRONG_TYPE;
            }
        }
        if (current instanceof String) {
            return write(plugin, path, value);
        }
        return Result.WRONG_TYPE;
    }

    public static double step(double value) {
        double magnitude = Math.abs(value);
        if (magnitude >= 20.0D) {
            return 5.0D;
        }
        if (magnitude >= 2.0D) {
            return 1.0D;
        }
        if (magnitude >= 0.2D) {
            return 0.05D;
        }
        if (magnitude >= 0.02D) {
            return 0.005D;
        }
        return 0.001D;
    }

    public static String format(UagcPlugin plugin, String path) {
        Object value = plugin.getConfig().get(path);
        if (value instanceof Integer number) {
            return String.valueOf(number.intValue());
        }
        if (value instanceof Number number) {
            return String.format(Locale.ROOT, "%.3f", number.doubleValue());
        }
        return String.valueOf(value);
    }

    private static double round(double value) {
        return Math.round(value * 1000.0D) / 1000.0D;
    }

    private static Result write(UagcPlugin plugin, String path, Object value) {
        plugin.getConfig().set(path, value);
        plugin.saveConfig();
        return plugin.reloadUagc() ? Result.APPLIED : Result.RELOAD_FAILED;
    }
}
