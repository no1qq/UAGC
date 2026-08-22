package io.github.no1qq.uagc.bukkit.listener;

import io.github.no1qq.uagc.bukkit.UagcRuntime;
import io.github.no1qq.uagc.bukkit.message.Messages;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class FreezeListener implements Listener {

    private final UagcRuntime runtime;

    public FreezeListener(UagcRuntime runtime) {
        this.runtime = runtime;
    }

    private boolean frozen(Player player) {
        return runtime.freeze().isFrozen(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!runtime.freeze().config().blockMovement() || !frozen(event.getPlayer())) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ())) {
            return;
        }
        Location held = from.clone();
        held.setYaw(to.getYaw());
        held.setPitch(to.getPitch());
        event.setTo(held);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (runtime.freeze().config().blockInteraction() && frozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (runtime.freeze().config().blockInteraction() && frozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (runtime.freeze().config().blockInteraction() && frozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (runtime.freeze().config().blockInteraction() && frozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!frozen(player)) {
            return;
        }
        if (runtime.freeze().isCommandAllowed(event.getMessage())) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage(Messages.parse(runtime.freeze().config().frozenMessage()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (runtime.freeze().config().blockDamage() && frozen(player)) {
            event.setCancelled(true);
        }
    }
}
