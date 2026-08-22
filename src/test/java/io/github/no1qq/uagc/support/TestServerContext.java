package io.github.no1qq.uagc.support;

import io.github.no1qq.uagc.engine.platform.ServerConditions;
import io.github.no1qq.uagc.engine.platform.ServerContext;
import io.github.no1qq.uagc.engine.platform.UagcClock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class TestServerContext implements ServerContext {

    private final TestClock clock;
    private final Set<String> permissions = new HashSet<>();
    private final Set<UUID> online = new HashSet<>();
    private final List<String> infoMessages = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    private ServerConditions conditions = ServerConditions.healthy();

    public TestServerContext(TestClock clock) {
        this.clock = clock;
    }

    @Override
    public UagcClock clock() {
        return clock;
    }

    @Override
    public ServerConditions conditions() {
        return conditions;
    }

    public void setConditions(ServerConditions value) {
        this.conditions = value;
    }

    public void grant(UUID playerId, String node) {
        permissions.add(playerId + "#" + node);
    }

    public void revoke(UUID playerId, String node) {
        permissions.remove(playerId + "#" + node);
    }

    public void setOnline(UUID playerId, boolean value) {
        if (value) {
            online.add(playerId);
        } else {
            online.remove(playerId);
        }
    }

    @Override
    public boolean hasPermission(UUID playerId, String node) {
        return permissions.contains(playerId + "#" + node);
    }

    @Override
    public boolean isOnline(UUID playerId) {
        return online.contains(playerId);
    }

    @Override
    public String nameOf(UUID playerId) {
        return "player-" + playerId.toString().substring(0, 8);
    }

    @Override
    public void info(String message) {
        infoMessages.add(message);
    }

    @Override
    public void warn(String message) {
        warnings.add(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        errors.add(message);
    }

    public List<String> infoMessages() {
        return infoMessages;
    }

    public List<String> warnings() {
        return warnings;
    }

    public List<String> errors() {
        return errors;
    }
}
