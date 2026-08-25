package io.github.no1qq.uagc.engine.check.event;

import io.github.no1qq.uagc.engine.check.CheckEvent;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;

public record MovementEvent(MovementSnapshot snapshot, boolean idle) implements CheckEvent {

    public MovementEvent(MovementSnapshot snapshot) {
        this(snapshot, false);
    }

    @Override
    public long tick() {
        return snapshot.tick();
    }

    @Override
    public long timeMillis() {
        return snapshot.timeMillis();
    }
}
