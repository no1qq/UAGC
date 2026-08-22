package io.github.no1qq.uagc.bukkit.debug;

import io.github.no1qq.uagc.bukkit.message.Messages;
import io.github.no1qq.uagc.engine.check.DebugSink;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class DebugService implements DebugSink {

    private final Map<UUID, Subscription> subscriptions = new ConcurrentHashMap<>();

    private record Subscription(UUID target, String checkId) {
        boolean matches(UUID playerId, String check) {
            if (target != null && !target.equals(playerId)) {
                return false;
            }
            return checkId == null || checkId.equalsIgnoreCase(check);
        }
    }

    public void subscribe(UUID staffId, UUID target, String checkId) {
        subscriptions.put(staffId, new Subscription(target,
                checkId == null ? null : checkId.toLowerCase(Locale.ROOT)));
    }

    public boolean unsubscribe(UUID staffId) {
        return subscriptions.remove(staffId) != null;
    }

    public boolean isSubscribed(UUID staffId) {
        return subscriptions.containsKey(staffId);
    }

    public Set<UUID> subscribers() {
        return Collections.unmodifiableSet(subscriptions.keySet());
    }

    public int size() {
        return subscriptions.size();
    }

    public void clear() {
        subscriptions.clear();
    }

    @Override
    public boolean isWatched(UUID playerId, String checkId) {
        if (subscriptions.isEmpty()) {
            return false;
        }
        for (Subscription subscription : subscriptions.values()) {
            if (subscription.matches(playerId, checkId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void debug(UUID playerId, String checkId, Supplier<String> message) {
        if (subscriptions.isEmpty()) {
            return;
        }
        String rendered = null;
        for (Map.Entry<UUID, Subscription> entry : subscriptions.entrySet()) {
            if (!entry.getValue().matches(playerId, checkId)) {
                continue;
            }
            Player staff = Bukkit.getPlayer(entry.getKey());
            if (staff == null) {
                continue;
            }
            if (rendered == null) {
                rendered = message.get();
            }
            staff.sendMessage(Messages.parse("<dark_gray>[<gold>debug</gold>]</dark_gray> <gray>"
                    + checkId + "</gray> <white>" + rendered + "</white>"));
        }
    }
}
