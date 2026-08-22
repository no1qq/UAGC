package io.github.no1qq.uagc.bukkit.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;

public final class Messages {

    private static final boolean MINI_MESSAGE_AVAILABLE = detectMiniMessage();

    private Messages() {
    }

    private static boolean detectMiniMessage() {
        try {
            MiniMessage.miniMessage();
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }

    public static boolean miniMessageAvailable() {
        return MINI_MESSAGE_AVAILABLE;
    }

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        if (MINI_MESSAGE_AVAILABLE) {
            try {
                return MiniMessage.miniMessage().deserialize(input);
            } catch (Throwable throwable) {
                return Component.text(stripTags(input));
            }
        }
        return Component.text(stripTags(input));
    }

    public static Component parse(String input, Map<String, String> placeholders) {
        return parse(fill(input, placeholders));
    }

    public static String fill(String input, Map<String, String> placeholders) {
        if (input == null || input.isEmpty() || placeholders.isEmpty()) {
            return input;
        }
        String result = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("<" + entry.getKey() + ">", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static String stripTags(String input) {
        StringBuilder builder = new StringBuilder(input.length());
        boolean inTag = false;
        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            if (character == '<') {
                inTag = true;
                continue;
            }
            if (character == '>') {
                inTag = false;
                continue;
            }
            if (!inTag) {
                builder.append(character);
            }
        }
        return builder.toString();
    }
}
