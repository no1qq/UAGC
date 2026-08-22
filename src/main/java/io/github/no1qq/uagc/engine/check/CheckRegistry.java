package io.github.no1qq.uagc.engine.check;

import io.github.no1qq.uagc.engine.config.CheckConfig;
import io.github.no1qq.uagc.engine.config.UagcConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CheckRegistry {

    private static final RegisteredCheck[] EMPTY = new RegisteredCheck[0];

    private final List<RegisteredCheck> checks = new ArrayList<>();
    private final Map<String, RegisteredCheck> byId = new LinkedHashMap<>();
    private final Map<Class<?>, RegisteredCheck[]> dispatch = new IdentityHashMap<>();
    private boolean frozen;

    public synchronized RegisteredCheck register(Check<? extends CheckEvent, ?> check, UagcConfig config) {
        if (frozen) {
            throw new IllegalStateException("registry is frozen");
        }
        CheckDefinition definition = check.definition();
        String id = definition.id().toLowerCase(Locale.ROOT);
        if (byId.containsKey(id)) {
            throw new IllegalArgumentException("duplicate check id: " + id);
        }
        RegisteredCheck registered = new RegisteredCheck(check, checks.size(), config.checkConfig(id));
        checks.add(registered);
        byId.put(id, registered);
        return registered;
    }

    public synchronized void freeze() {
        if (frozen) {
            return;
        }
        Map<Class<?>, List<RegisteredCheck>> grouped = new HashMap<>();
        for (RegisteredCheck registered : checks) {
            grouped.computeIfAbsent(registered.check().eventType(), key -> new ArrayList<>()).add(registered);
        }
        dispatch.clear();
        for (Map.Entry<Class<?>, List<RegisteredCheck>> entry : grouped.entrySet()) {
            dispatch.put(entry.getKey(), entry.getValue().toArray(EMPTY));
        }
        frozen = true;
    }

    public RegisteredCheck[] handlersFor(Class<?> eventType) {
        RegisteredCheck[] handlers = dispatch.get(eventType);
        return handlers == null ? EMPTY : handlers;
    }

    public RegisteredCheck byId(String id) {
        return id == null ? null : byId.get(id.toLowerCase(Locale.ROOT));
    }

    public List<RegisteredCheck> all() {
        return Collections.unmodifiableList(checks);
    }

    public List<RegisteredCheck> byCategory(CheckCategory category) {
        List<RegisteredCheck> matched = new ArrayList<>();
        for (RegisteredCheck registered : checks) {
            if (registered.definition().category() == category) {
                matched.add(registered);
            }
        }
        return matched;
    }

    public int size() {
        return checks.size();
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void applyConfig(UagcConfig config) {
        for (RegisteredCheck registered : checks) {
            CheckConfig updated = config.checkConfig(registered.id());
            registered.updateConfig(updated);
        }
    }

    public List<String> ids() {
        return List.copyOf(byId.keySet());
    }
}
