package io.github.no1qq.uagc.engine.player;

import io.github.no1qq.uagc.engine.platform.UagcClock;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDataManager {

    private final Map<UUID, PlayerData> players = new ConcurrentHashMap<>();
    private final UagcClock clock;

    private volatile PlayerDataSettings settings;
    private volatile int checkCount;

    public PlayerDataManager(UagcClock clock, PlayerDataSettings settings, int checkCount) {
        this.clock = clock;
        this.settings = settings;
        this.checkCount = checkCount;
    }

    public PlayerData create(UUID uuid, String name) {
        PlayerData data = new PlayerData(uuid, name, clock, checkCount, settings);
        players.put(uuid, data);
        return data;
    }

    public PlayerData get(UUID uuid) {
        return uuid == null ? null : players.get(uuid);
    }

    public PlayerData getOrCreate(UUID uuid, String name) {
        PlayerData existing = players.get(uuid);
        if (existing != null) {
            existing.setName(name);
            return existing;
        }
        return create(uuid, name);
    }

    public PlayerData remove(UUID uuid) {
        return uuid == null ? null : players.remove(uuid);
    }

    public Collection<PlayerData> all() {
        return Collections.unmodifiableCollection(players.values());
    }

    public PlayerData byName(String name) {
        if (name == null) {
            return null;
        }
        for (PlayerData data : players.values()) {
            if (name.equalsIgnoreCase(data.name())) {
                return data;
            }
        }
        return null;
    }

    public int size() {
        return players.size();
    }

    public void clear() {
        players.clear();
    }

    public void updateSettings(PlayerDataSettings updated) {
        this.settings = updated;
    }

    public void updateCheckCount(int count) {
        this.checkCount = count;
    }

    public PlayerDataSettings settings() {
        return settings;
    }
}
