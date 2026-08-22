package io.github.no1qq.uagc.engine.check.event;

import io.github.no1qq.uagc.engine.check.CheckEvent;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;

public record MovementEvent(MovementSnapshot snapshot) implements CheckEvent {

    @Override
    public long tick() {
        return snapshot.tick();
    }

    @Override
    public long timeMillis() {
        return snapshot.timeMillis();
    }
}
