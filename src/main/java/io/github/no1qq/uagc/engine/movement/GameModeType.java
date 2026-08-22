package io.github.no1qq.uagc.engine.movement;

public enum GameModeType {
    SURVIVAL,
    CREATIVE,
    ADVENTURE,
    SPECTATOR;

    public boolean allowsFlight() {
        return this == CREATIVE || this == SPECTATOR;
    }

    public boolean ignoresCollision() {
        return this == SPECTATOR;
    }
}
