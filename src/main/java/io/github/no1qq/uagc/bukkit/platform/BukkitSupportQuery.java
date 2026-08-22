package io.github.no1qq.uagc.bukkit.platform;

import io.github.no1qq.uagc.engine.platform.SupportQuery;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.Collection;
import java.util.UUID;

public final class BukkitSupportQuery implements SupportQuery {

    private static final double SUPPORT_DEPTH = 1.0D;
    private static final double PUSH_RADIUS = 1.2D;

    @Override
    public boolean hasEntitySupportBelow(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return false;
        }
        BoundingBox box = player.getBoundingBox();
        BoundingBox below = new BoundingBox(
                box.getMinX() - 0.1D,
                box.getMinY() - SUPPORT_DEPTH,
                box.getMinZ() - 0.1D,
                box.getMaxX() + 0.1D,
                box.getMinY() + 0.1D,
                box.getMaxZ() + 0.1D);
        Collection<Entity> nearby = player.getWorld().getNearbyEntities(below,
                entity -> !entity.getUniqueId().equals(playerId));
        return !nearby.isEmpty();
    }

    @Override
    public boolean hasNearbyPusher(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return false;
        }
        Collection<Entity> nearby = player.getWorld().getNearbyEntities(
                player.getLocation(), PUSH_RADIUS, PUSH_RADIUS, PUSH_RADIUS,
                entity -> !entity.getUniqueId().equals(playerId));
        return !nearby.isEmpty();
    }
}
