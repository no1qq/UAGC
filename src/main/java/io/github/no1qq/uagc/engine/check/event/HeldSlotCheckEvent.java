package io.github.no1qq.uagc.engine.check.event;

import io.github.no1qq.uagc.engine.check.CheckEvent;

public record HeldSlotCheckEvent(
        long tick,
        long timeMillis,
        int previousSlot,
        int newSlot,
        int ping) implements CheckEvent {
}
