package io.github.no1qq.uagc.engine.platform;

import java.util.UUID;

public interface SupportQuery {

    SupportQuery NONE = new SupportQuery() {
        @Override
        public boolean hasEntitySupportBelow(UUID playerId) {
            return false;
        }

        @Override
        public boolean hasNearbyPusher(UUID playerId) {
            return false;
        }
    };

    boolean hasEntitySupportBelow(UUID playerId);

    boolean hasNearbyPusher(UUID playerId);
}
