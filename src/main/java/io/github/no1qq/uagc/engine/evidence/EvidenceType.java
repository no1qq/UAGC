package io.github.no1qq.uagc.engine.evidence;

import java.util.Locale;

public enum EvidenceType {
    VIOLATION,
    TELEPORT,
    VELOCITY,
    KNOCKBACK,
    SETBACK,
    EXEMPTION,
    BYPASS,
    PUNISHMENT,
    FREEZE,
    LIFECYCLE,
    STATE_CHANGE,
    INTEGRATION;

    private final String id = name().toLowerCase(Locale.ROOT);

    public String id() {
        return id;
    }
}
