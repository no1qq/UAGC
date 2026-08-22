package io.github.no1qq.uagc.engine.freeze;

import io.github.no1qq.uagc.engine.config.FreezeConfig;
import io.github.no1qq.uagc.engine.evidence.EvidenceEntry;
import io.github.no1qq.uagc.engine.evidence.EvidenceType;
import io.github.no1qq.uagc.engine.platform.EnforcementGateway;
import io.github.no1qq.uagc.engine.platform.MessageGateway;
import io.github.no1qq.uagc.engine.platform.ServerContext;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.player.PlayerDataManager;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FreezeService {

    private final Map<UUID, FreezeRecord> frozen = new ConcurrentHashMap<>();
    private final ServerContext server;
    private final MessageGateway messages;
    private final EnforcementGateway enforcement;
    private final PlayerDataManager players;
    private final FreezeStore store;

    private volatile FreezeConfig config;
    private long lastReminderTick;

    public FreezeService(ServerContext server,
                         MessageGateway messages,
                         EnforcementGateway enforcement,
                         PlayerDataManager players,
                         FreezeStore store,
                         FreezeConfig config) {
        this.server = server;
        this.messages = messages;
        this.enforcement = enforcement;
        this.players = players;
        this.store = store;
        this.config = config;
    }

    public void loadPersisted() {
        if (!config.persistAcrossReconnect()) {
            return;
        }
        try {
            long now = server.clock().currentTimeMillis();
            for (FreezeRecord record : store.load()) {
                if (!record.isExpired(now)) {
                    frozen.put(record.playerId(), record);
                }
            }
        } catch (RuntimeException exception) {
            server.error("failed to load persisted freezes", exception);
        }
    }

    public void persist() {
        if (!config.persistAcrossReconnect()) {
            return;
        }
        try {
            store.save(frozen.values());
        } catch (RuntimeException exception) {
            server.error("failed to persist freezes", exception);
        }
    }

    public void updateConfig(FreezeConfig updated) {
        this.config = updated;
    }

    public FreezeConfig config() {
        return config;
    }

    public boolean isFrozen(UUID playerId) {
        FreezeRecord record = frozen.get(playerId);
        if (record == null) {
            return false;
        }
        if (record.isExpired(server.clock().currentTimeMillis())) {
            release(playerId, "expired");
            return false;
        }
        return true;
    }

    public FreezeRecord record(UUID playerId) {
        return frozen.get(playerId);
    }

    public Collection<FreezeRecord> all() {
        return Collections.unmodifiableCollection(frozen.values());
    }

    public FreezeRecord freeze(UUID playerId,
                               String playerName,
                               String staffName,
                               String reason,
                               long durationMillis,
                               String worldName,
                               double x,
                               double y,
                               double z) {
        long now = server.clock().currentTimeMillis();
        FreezeRecord record = new FreezeRecord(playerId, playerName, staffName,
                reason == null || reason.isBlank() ? "investigation" : reason,
                now, durationMillis > 0L ? now + durationMillis : 0L, worldName, x, y, z);
        frozen.put(playerId, record);
        persist();

        enforcement.applyFreeze(playerId, true);
        messages.sendFormatted(playerId, config.frozenMessage());
        messages.sendTitle(playerId, config.frozenTitle(), config.frozenSubtitle(), 10, 70, 20);

        PlayerData data = players.get(playerId);
        if (data != null) {
            data.recordEvidence(EvidenceEntry.of(EvidenceType.FREEZE, "frozen by " + staffName)
                    .with("reason", record.reason())
                    .with("staff", staffName));
        }
        return record;
    }

    public boolean release(UUID playerId, String releasedBy) {
        FreezeRecord record = frozen.remove(playerId);
        if (record == null) {
            return false;
        }
        persist();
        enforcement.applyFreeze(playerId, false);
        if (server.isOnline(playerId)) {
            messages.sendFormatted(playerId, config.unfrozenMessage());
        }
        PlayerData data = players.get(playerId);
        if (data != null) {
            data.recordEvidence(EvidenceEntry.of(EvidenceType.FREEZE, "released by " + releasedBy)
                    .with("duration_ms", record.durationMillis(server.clock().currentTimeMillis())));
        }
        return true;
    }

    public void reapplyOnJoin(UUID playerId) {
        if (isFrozen(playerId)) {
            enforcement.applyFreeze(playerId, true);
            messages.sendFormatted(playerId, config.frozenMessage());
            messages.sendTitle(playerId, config.frozenTitle(), config.frozenSubtitle(), 10, 70, 20);
        }
    }

    public void handleQuit(UUID playerId) {
        if (!isFrozen(playerId)) {
            return;
        }
        String action = config.disconnectAction() == null ? "none" : config.disconnectAction().toLowerCase(Locale.ROOT);
        if ("release".equals(action)) {
            release(playerId, "disconnect");
        }
    }

    public boolean isCommandAllowed(String command) {
        if (!config.blockCommands()) {
            return true;
        }
        String normalized = command.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        int space = normalized.indexOf(" ");
        String label = space < 0 ? normalized : normalized.substring(0, space);
        int colon = label.indexOf(":");
        if (colon >= 0) {
            label = label.substring(colon + 1);
        }
        for (String entry : config.allowedCommands()) {
            if (entry.equalsIgnoreCase(label)) {
                return true;
            }
        }
        return false;
    }

    public void tick() {
        if (frozen.isEmpty()) {
            return;
        }
        long now = server.clock().currentTimeMillis();
        frozen.values().removeIf(record -> {
            if (record.isExpired(now)) {
                enforcement.applyFreeze(record.playerId(), false);
                if (server.isOnline(record.playerId())) {
                    messages.sendFormatted(record.playerId(), config.unfrozenMessage());
                }
                return true;
            }
            return false;
        });
        long tick = server.clock().currentTick();
        int interval = config.reminderIntervalTicks();
        if (interval > 0 && tick - lastReminderTick >= interval) {
            lastReminderTick = tick;
            for (FreezeRecord record : frozen.values()) {
                if (server.isOnline(record.playerId())) {
                    messages.sendActionBar(record.playerId(), config.frozenTitle());
                }
            }
        }
    }

    public int size() {
        return frozen.size();
    }
}
