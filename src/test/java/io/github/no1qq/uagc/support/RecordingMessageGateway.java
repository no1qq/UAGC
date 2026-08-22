package io.github.no1qq.uagc.support;

import io.github.no1qq.uagc.engine.alert.Alert;
import io.github.no1qq.uagc.engine.platform.MessageGateway;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RecordingMessageGateway implements MessageGateway {

    public record Delivery(UUID recipient, Alert alert) {
    }

    private final List<Delivery> alerts = new ArrayList<>();
    private final List<Alert> consoleAlerts = new ArrayList<>();
    private final List<String> messages = new ArrayList<>();
    private final List<String> actionBars = new ArrayList<>();
    private final List<String> titles = new ArrayList<>();

    @Override
    public void sendAlert(UUID recipient, Alert alert) {
        alerts.add(new Delivery(recipient, alert));
    }

    @Override
    public void sendConsoleAlert(Alert alert) {
        consoleAlerts.add(alert);
    }

    @Override
    public void sendFormatted(UUID recipient, String format) {
        messages.add(recipient + ":" + format);
    }

    @Override
    public void sendActionBar(UUID recipient, String format) {
        actionBars.add(recipient + ":" + format);
    }

    @Override
    public void sendTitle(UUID recipient, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        titles.add(recipient + ":" + title);
    }

    public List<Delivery> alerts() {
        return alerts;
    }

    public List<Alert> consoleAlerts() {
        return consoleAlerts;
    }

    public List<String> messages() {
        return messages;
    }

    public List<String> actionBars() {
        return actionBars;
    }

    public List<String> titles() {
        return titles;
    }

    public void clear() {
        alerts.clear();
        consoleAlerts.clear();
        messages.clear();
        actionBars.clear();
        titles.clear();
    }
}
