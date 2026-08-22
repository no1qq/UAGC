package io.github.no1qq.uagc.bukkit.freeze;

import io.github.no1qq.uagc.engine.freeze.FreezeRecord;
import io.github.no1qq.uagc.engine.freeze.FreezeStore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public final class YamlFreezeStore implements FreezeStore {

    private final File file;
    private final Logger logger;

    public YamlFreezeStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    @Override
    public List<FreezeRecord> load() {
        List<FreezeRecord> records = new ArrayList<>();
        if (!file.exists()) {
            return records;
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = configuration.getConfigurationSection("frozen");
        if (root == null) {
            return records;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                records.add(new FreezeRecord(
                        UUID.fromString(key),
                        section.getString("player-name", "unknown"),
                        section.getString("staff-name", "unknown"),
                        section.getString("reason", "investigation"),
                        section.getLong("start-millis"),
                        section.getLong("expires-millis"),
                        section.getString("world", "world"),
                        section.getDouble("x"),
                        section.getDouble("y"),
                        section.getDouble("z")));
            } catch (IllegalArgumentException exception) {
                logger.warning("ignoring malformed freeze entry: " + key);
            }
        }
        return records;
    }

    @Override
    public void save(Collection<FreezeRecord> records) {
        YamlConfiguration configuration = new YamlConfiguration();
        for (FreezeRecord record : records) {
            String path = "frozen." + record.playerId();
            configuration.set(path + ".player-name", record.playerName());
            configuration.set(path + ".staff-name", record.staffName());
            configuration.set(path + ".reason", record.reason());
            configuration.set(path + ".start-millis", record.startMillis());
            configuration.set(path + ".expires-millis", record.expiresMillis());
            configuration.set(path + ".world", record.worldName());
            configuration.set(path + ".x", record.x());
            configuration.set(path + ".y", record.y());
            configuration.set(path + ".z", record.z());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                logger.warning("could not create the data directory for freeze persistence");
                return;
            }
            configuration.save(file);
        } catch (IOException exception) {
            logger.warning("could not persist freeze state: " + exception.getMessage());
        }
    }
}
