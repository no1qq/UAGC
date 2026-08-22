package io.github.no1qq.uagc.engine.violation;

import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.movement.Rotation;
import io.github.no1qq.uagc.engine.movement.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record Violation(
        UUID playerId,
        String playerName,
        String checkId,
        String checkDisplayName,
        CheckCategory category,
        double severity,
        double reliability,
        double confidence,
        double addedViolation,
        double violationLevel,
        String summary,
        Map<String, String> details,
        long tick,
        long timeMillis,
        Vec3 position,
        Rotation rotation,
        int ping,
        double tps,
        List<String> activeExemptions,
        int streak) {

    public boolean isHighConfidence() {
        return confidence >= 0.75D;
    }

    public String describe() {
        return checkDisplayName + " vl=" + String.format(java.util.Locale.ROOT, "%.1f", violationLevel)
                + " confidence=" + String.format(java.util.Locale.ROOT, "%.0f%%", confidence * 100.0D);
    }
}
