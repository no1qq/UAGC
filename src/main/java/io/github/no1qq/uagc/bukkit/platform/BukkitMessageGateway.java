package io.github.no1qq.uagc.bukkit.platform;

import io.github.no1qq.uagc.bukkit.message.Messages;
import io.github.no1qq.uagc.engine.alert.Alert;
import io.github.no1qq.uagc.engine.config.AlertConfig;
import io.github.no1qq.uagc.engine.platform.MessageGateway;
import io.github.no1qq.uagc.engine.violation.Violation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class BukkitMessageGateway implements MessageGateway {

    private volatile AlertConfig config;

    public BukkitMessageGateway(AlertConfig config) {
        this.config = config;
    }

    public void updateConfig(AlertConfig updated) {
        this.config = updated;
    }

    @Override
    public void sendAlert(UUID recipient, Alert alert) {
        Player player = Bukkit.getPlayer(recipient);
        if (player == null) {
            return;
        }
        player.sendMessage(buildAlert(alert));
    }

    @Override
    public void sendConsoleAlert(Alert alert) {
        Bukkit.getConsoleSender().sendMessage(buildAlert(alert));
    }

    @Override
    public void sendFormatted(UUID recipient, String format) {
        Player player = Bukkit.getPlayer(recipient);
        if (player != null) {
            player.sendMessage(Messages.parse(format));
        }
    }

    @Override
    public void sendActionBar(UUID recipient, String format) {
        Player player = Bukkit.getPlayer(recipient);
        if (player != null) {
            player.sendActionBar(Messages.parse(format));
        }
    }

    @Override
    public void sendTitle(UUID recipient, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Player player = Bukkit.getPlayer(recipient);
        if (player == null) {
            return;
        }
        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L));
        player.showTitle(Title.title(Messages.parse(title), Messages.parse(subtitle), times));
    }

    private Component buildAlert(Alert alert) {
        AlertConfig current = config;
        Map<String, String> placeholders = placeholders(alert);
        Component message = Messages.parse(current.format(), placeholders);

        String hover = current.hoverFormat();
        if (hover != null && !hover.isEmpty()) {
            Component detail = Messages.parse(hover, placeholders).append(Component.newline())
                    .append(details(alert.violation()));
            message = message.hoverEvent(HoverEvent.showText(detail));
        }
        String click = current.clickCommand();
        if (click != null && !click.isEmpty()) {
            message = message.clickEvent(ClickEvent.suggestCommand(Messages.fill(click, placeholders)));
        }
        return message;
    }

    private Component details(Violation violation) {
        Component component = Component.empty();
        boolean first = true;
        for (Map.Entry<String, String> entry : violation.details().entrySet()) {
            if (!first) {
                component = component.append(Component.newline());
            }
            component = component.append(Component.text(entry.getKey() + ": " + entry.getValue()));
            first = false;
        }
        return component;
    }

    private Map<String, String> placeholders(Alert alert) {
        Violation violation = alert.violation();
        Map<String, String> map = new LinkedHashMap<>(12);
        map.put("player", violation.playerName() == null ? "unknown" : violation.playerName());
        map.put("uuid", violation.playerId().toString());
        map.put("check", violation.checkDisplayName());
        map.put("check_id", violation.checkId());
        map.put("category", violation.category() == null ? "manual" : violation.category().id());
        map.put("vl", String.format(Locale.ROOT, "%.1f", violation.violationLevel()));
        map.put("confidence", String.format(Locale.ROOT, "%.0f%%", violation.confidence() * 100.0D));
        map.put("severity", alert.severity().id());
        map.put("ping", Integer.toString(violation.ping()));
        map.put("tps", String.format(Locale.ROOT, "%.1f", violation.tps()));
        map.put("summary", violation.summary());
        map.put("repeat", Integer.toString(alert.repeatCount()));
        return map;
    }
}
