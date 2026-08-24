package io.github.no1qq.uagc.bukkit.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public final class SettingsMenuListener implements Listener {

    public static SettingsMenu menuOf(InventoryHolder holder) {
        return holder instanceof SettingsMenu menu ? menu : null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        SettingsMenu menu = menuOf(event.getInventory().getHolder());
        if (menu == null) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != menu) {
            return;
        }
        ClickType click = event.getClick();
        if (click == ClickType.DOUBLE_CLICK || click == ClickType.NUMBER_KEY || click == ClickType.SWAP_OFFHAND) {
            return;
        }
        menu.click(player, event.getRawSlot(), click.isRightClick(), click.isShiftClick());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (menuOf(event.getInventory().getHolder()) != null) {
            event.setCancelled(true);
        }
    }
}
