package io.github.no1qq.uagc.engine.check.event;

import io.github.no1qq.uagc.engine.check.CheckEvent;

public record InventoryClickCheckEvent(
        long tick,
        long timeMillis,
        int slot,
        String action,
        String inventoryType,
        boolean ownInventory,
        boolean openedThisTick,
        boolean sprinting,
        boolean sneaking,
        int ping) implements CheckEvent {
}
