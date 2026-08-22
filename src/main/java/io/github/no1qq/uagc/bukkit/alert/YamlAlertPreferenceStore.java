package io.github.no1qq.uagc.bukkit.alert;

import io.github.no1qq.uagc.engine.alert.AlertPreferenceStore;
import io.github.no1qq.uagc.engine.alert.AlertPreferences;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public final class YamlAlertPreferenceStore implements AlertPreferenceStore {

    private final File file;
    private final Logger logger;

    public YamlAlertPreferenceStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    @Override
    public AlertPreferences load() {
        if (!file.exists()) {
            return AlertPreferences.empty();
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        Boolean console = configuration.isSet("console") ? configuration.getBoolean("console") : null;

        Map<UUID, Boolean> players = new LinkedHashMap<>();
        ConfigurationSection root = configuration.getConfigurationSection("players");
        if (root != null) {
            for (String key : root.getKeys(false)) {
                try {
                    players.put(UUID.fromString(key), root.getBoolean(key));
                } catch (IllegalArgumentException exception) {
                    logger.warning("ignoring malformed alert preference: " + key);
                }
            }
        }
        return new AlertPreferences(players, console);
    }

    @Override
    public void save(AlertPreferences preferences) {
        YamlConfiguration configuration = new YamlConfiguration();
        if (preferences.console() != null) {
            configuration.set("console", preferences.console());
        }
        for (Map.Entry<UUID, Boolean> entry : preferences.players().entrySet()) {
            configuration.set("players." + entry.getKey(), entry.getValue());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                logger.warning("could not create the data directory for alert preferences");
                return;
            }
            configuration.save(file);
        } catch (IOException exception) {
            logger.warning("could not persist alert preferences: " + exception.getMessage());
        }
    }
}
