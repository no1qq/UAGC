package io.github.no1qq.uagc.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface UagcQuery {

    boolean isTracked(UUID playerId);

    boolean isExempt(UUID playerId, ExemptionKind kind);

    boolean isFrozen(UUID playerId);

    boolean hasBypass(UUID playerId);

    double violationLevel(UUID playerId, String checkId);

    Map<String, Double> violationLevels(UUID playerId);

    List<String> checkIds();

    List<String> activeExemptions(UUID playerId);
}
