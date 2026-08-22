package io.github.no1qq.uagc.engine.check;

import java.util.UUID;
import java.util.function.Supplier;

public interface DebugSink {

    DebugSink NONE = new DebugSink() {
        @Override
        public boolean isWatched(UUID playerId, String checkId) {
            return false;
        }

        @Override
        public void debug(UUID playerId, String checkId, Supplier<String> message) {
        }
    };

    boolean isWatched(UUID playerId, String checkId);

    void debug(UUID playerId, String checkId, Supplier<String> message);
}
