package io.github.no1qq.uagc.engine.check;

import io.github.no1qq.uagc.engine.config.CheckConfig;
import io.github.no1qq.uagc.engine.platform.ServerConditions;
import io.github.no1qq.uagc.engine.platform.SupportQuery;
import io.github.no1qq.uagc.engine.player.PlayerData;

import java.util.function.Supplier;

public final class CheckContext {

    private final PlayerData player;
    private final ServerConditions conditions;
    private final long tick;
    private final long timeMillis;
    private final DebugSink debugSink;
    private final SupportQuery supportQuery;

    private CheckConfig config;
    private String checkId;

    public CheckContext(PlayerData player,
                        ServerConditions conditions,
                        long tick,
                        long timeMillis,
                        DebugSink debugSink,
                        SupportQuery supportQuery) {
        this.supportQuery = supportQuery == null ? SupportQuery.NONE : supportQuery;
        this.player = player;
        this.conditions = conditions;
        this.tick = tick;
        this.timeMillis = timeMillis;
        this.debugSink = debugSink == null ? DebugSink.NONE : debugSink;
    }

    void prepare(String checkId, CheckConfig config) {
        this.checkId = checkId;
        this.config = config;
    }

    public PlayerData player() {
        return player;
    }

    public CheckConfig config() {
        return config;
    }

    public ServerConditions conditions() {
        return conditions;
    }

    public long tick() {
        return tick;
    }

    public long timeMillis() {
        return timeMillis;
    }

    public SupportQuery support() {
        return supportQuery;
    }

    public boolean isDebugWatched() {
        return debugSink.isWatched(player.uuid(), checkId);
    }

    public void debug(Supplier<String> message) {
        debugSink.debug(player.uuid(), checkId, message);
    }
}
