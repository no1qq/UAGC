package io.github.no1qq.uagc.bukkit.platform;

import io.github.no1qq.uagc.engine.platform.ServerConditions;
import io.github.no1qq.uagc.engine.platform.ServerContext;
import io.github.no1qq.uagc.engine.platform.UagcClock;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.logging.Level;

public final class BukkitServerContext implements ServerContext, UagcClock {

    private final Plugin plugin;
    private volatile ServerConditions cached = ServerConditions.healthy();
    private volatile long lastTickMillis = System.currentTimeMillis();

    public BukkitServerContext(Plugin plugin) {
        this.plugin = plugin;
    }

    public void refresh() {
        long now = System.currentTimeMillis();
        double[] tps = Bukkit.getTPS();
        double current = tps.length > 0 ? tps[0] : ServerConditions.TARGET_TPS;
        cached = new ServerConditions(
                Math.min(current, ServerConditions.TARGET_TPS),
                Bukkit.getAverageTickTime(),
                Bukkit.getCurrentTick(),
                now - lastTickMillis,
                Bukkit.getOnlinePlayers().size());
        lastTickMillis = now;
    }

    public long millisSinceLastTick() {
        return System.currentTimeMillis() - lastTickMillis;
    }

    @Override
    public UagcClock clock() {
        return this;
    }

    @Override
    public ServerConditions conditions() {
        return cached;
    }

    @Override
    public long currentTick() {
        return Bukkit.getCurrentTick();
    }

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public boolean hasPermission(UUID playerId, String node) {
        Player player = Bukkit.getPlayer(playerId);
        return player != null && player.hasPermission(node);
    }

    @Override
    public boolean isOnline(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        return player != null && player.isOnline();
    }

    @Override
    public String nameOf(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            return player.getName();
        }
        String name = Bukkit.getOfflinePlayer(playerId).getName();
        return name == null ? playerId.toString() : name;
    }

    @Override
    public void info(String message) {
        plugin.getLogger().info(message);
    }

    @Override
    public void warn(String message) {
        plugin.getLogger().warning(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        plugin.getLogger().log(Level.SEVERE, message, throwable);
    }
}
