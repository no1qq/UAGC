package io.github.no1qq.uagc.bukkit.listener;

import io.github.no1qq.uagc.bukkit.UagcRuntime;
import io.github.no1qq.uagc.engine.check.event.HeldSlotCheckEvent;
import io.github.no1qq.uagc.engine.check.event.InventoryClickCheckEvent;
import io.github.no1qq.uagc.engine.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InventoryListener implements Listener {

    private final UagcRuntime runtime;
    private final Map<UUID, Long> openedTicks = new ConcurrentHashMap<>();

    public InventoryListener(UagcRuntime runtime) {
        this.runtime = runtime;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        long tick = runtime.server().currentTick();
        openedTicks.put(player.getUniqueId(), tick);
        PlayerData data = runtime.players().get(player.getUniqueId());
        if (data != null) {
            data.interaction().openScreen(tick);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        PlayerData data = runtime.players().get(player.getUniqueId());
        if (data != null) {
            data.interaction().closeScreen();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (io.github.no1qq.uagc.bukkit.gui.SettingsMenuListener.menuOf(event.getInventory().getHolder()) != null) {
            return;
        }
        PlayerData data = runtime.players().get(player.getUniqueId());
        if (data == null) {
            return;
        }
        long tick = runtime.server().currentTick();
        Long opened = openedTicks.get(player.getUniqueId());
        long settle = 2L + Math.min(20L, Math.max(0, player.getPing()) / 50L);
        boolean openedThisTick = opened != null && tick - opened <= settle;

        boolean denied = runtime.engine().process(data, new InventoryClickCheckEvent(
                tick,
                System.currentTimeMillis(),
                event.getRawSlot(),
                event.getAction().name(),
                event.getInventory().getType().name(),
                event.getView().getTopInventory().getType() == InventoryType.CRAFTING,
                openedThisTick,
                player.isSprinting(),
                player.isSneaking(),
                Math.max(0, player.getPing())));
        if (denied) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeldSlot(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        PlayerData data = runtime.players().get(player.getUniqueId());
        if (data == null) {
            return;
        }
        runtime.engine().process(data, new HeldSlotCheckEvent(
                runtime.server().currentTick(),
                System.currentTimeMillis(),
                event.getPreviousSlot(),
                event.getNewSlot(),
                Math.max(0, player.getPing())));
    }

    public void forget(UUID playerId) {
        openedTicks.remove(playerId);
    }
}
