package io.github.no1qq.uagc.api;

import java.util.UUID;

public interface ExemptionHandle {

    UUID playerId();

    ExemptionKind kind();

    String source();

    String reason();

    long expiresAtMillis();

    boolean isActive();

    void revoke();
}
