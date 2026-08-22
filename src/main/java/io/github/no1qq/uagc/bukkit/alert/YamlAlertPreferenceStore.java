package io.github.no1qq.uagc.bukkit.alert;

import io.github.no1qq.uagc.engine.alert.AlertPreferenceStore;
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
    public Map<UUID, Boolean> load() {
        Map<UUID, Boolean> preferences = new LinkedHashMap<>();
        if (!file.exists()) {
            return preferences;
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = configuration.getConfigurationSection("alerts");
        if (root == null) {
            return preferences;
        }
        for (String key : root.getKeys(false)) {
            try {
                preferences.put(UUID.fromString(key), root.getBoolean(key));
            } catch (IllegalArgumentException exception) {
                logger.warning("ignoring malformed alert preference: " + key);
            }
        }
        return preferences;
    }

    @Override
    public void save(Map<UUID, Boolean> preferences) {
        YamlConfiguration configuration = new YamlConfiguration();
        for (Map.Entry<UUID, Boolean> entry : preferences.entrySet()) {
            configuration.set("alerts." + entry.getKey(), entry.getValue());
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
