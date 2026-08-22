package io.github.no1qq.uagc.engine.platform;

import java.util.UUID;

public interface ServerContext {

    UagcClock clock();

    ServerConditions conditions();

    boolean hasPermission(UUID playerId, String node);

    boolean isOnline(UUID playerId);

    String nameOf(UUID playerId);

    void info(String message);

    void warn(String message);

    void error(String message, Throwable throwable);
}
