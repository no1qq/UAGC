package io.github.no1qq.uagc.engine.punishment;

import io.github.no1qq.uagc.engine.check.CheckCategory;

import java.util.Locale;
import java.util.Objects;

public record PunishmentRule(
        String scope,
        double violationLevel,
        double minimumConfidence,
        int minimumFlags,
        PunishmentAction action,
        String value,
        String reason,
        boolean repeatable,
        int cooldownTicks) {

    public PunishmentRule {
        Objects.requireNonNull(action, "action");
        scope = scope == null || scope.isBlank() ? "*" : scope.toLowerCase(Locale.ROOT).trim();
        value = value == null ? "" : value;
        reason = reason == null ? "" : reason;
    }

    public boolean matches(String checkId, CheckCategory category) {
        if ("*".equals(scope)) {
            return true;
        }
        if (scope.startsWith("category:")) {
            return category != null && scope.substring("category:".length()).equalsIgnoreCase(category.id());
        }
        return scope.equalsIgnoreCase(checkId);
    }

    public boolean isSatisfied(double currentViolationLevel, double confidence, int flagCount) {
        return currentViolationLevel >= violationLevel
                && confidence >= minimumConfidence
                && flagCount >= minimumFlags;
    }

    public String describe() {
        return scope + " vl>=" + violationLevel + " -> " + action.id()
                + (value.isEmpty() ? "" : " (" + value + ")");
    }
}
