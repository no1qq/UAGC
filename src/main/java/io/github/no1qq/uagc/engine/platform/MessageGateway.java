package io.github.no1qq.uagc.engine.platform;

import io.github.no1qq.uagc.engine.alert.Alert;

import java.util.UUID;

public interface MessageGateway {

    void sendAlert(UUID recipient, Alert alert);

    void sendConsoleAlert(Alert alert);

    void sendFormatted(UUID recipient, String format);

    void sendActionBar(UUID recipient, String format);

    void sendTitle(UUID recipient, String title, String subtitle, int fadeIn, int stay, int fadeOut);
}
