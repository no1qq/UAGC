package io.github.no1qq.uagc.engine.util;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {

    private static final Pattern PATTERN = Pattern.compile("([0-9]+) *([smhdw])", Pattern.CASE_INSENSITIVE);

    private DurationParser() {
    }

    public static Duration parse(String input, Duration fallback) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        String normalized = input.toLowerCase(Locale.ROOT).trim();
        if ("permanent".equals(normalized) || "perm".equals(normalized) || "forever".equals(normalized)) {
            return null;
        }
        Matcher matcher = PATTERN.matcher(normalized);
        Duration total = Duration.ZERO;
        boolean matched = false;
        while (matcher.find()) {
            long amount;
            try {
                amount = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException exception) {
                return fallback;
            }
            total = switch (matcher.group(2)) {
                case "s" -> total.plusSeconds(amount);
                case "m" -> total.plusMinutes(amount);
                case "h" -> total.plusHours(amount);
                case "d" -> total.plusDays(amount);
                case "w" -> total.plusDays(amount * 7L);
                default -> total;
            };
            matched = true;
        }
        return matched ? total : fallback;
    }

    public static String format(Duration duration) {
        if (duration == null) {
            return "permanent";
        }
        long seconds = duration.getSeconds();
        if (seconds <= 0L) {
            return "0s";
        }
        long days = seconds / 86400L;
        long hours = (seconds % 86400L) / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remaining = seconds % 60L;
        StringBuilder builder = new StringBuilder();
        if (days > 0L) {
            builder.append(days).append("d ");
        }
        if (hours > 0L) {
            builder.append(hours).append("h ");
        }
        if (minutes > 0L) {
            builder.append(minutes).append("m ");
        }
        if (remaining > 0L && days == 0L) {
            builder.append(remaining).append("s");
        }
        return builder.toString().trim();
    }

    public static String formatTicks(long ticks) {
        if (ticks < 0L) {
            return "until revoked";
        }
        return format(Duration.ofMillis(ticks * 50L));
    }
}
