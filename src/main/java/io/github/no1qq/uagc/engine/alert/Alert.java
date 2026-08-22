package io.github.no1qq.uagc.engine.alert;

import io.github.no1qq.uagc.engine.violation.Violation;

public record Alert(
        Violation violation,
        int repeatCount,
        AlertSeverity severity,
        boolean suppressedByBypass) {

    public String playerName() {
        return violation.playerName();
    }

    public String checkName() {
        return violation.checkDisplayName();
    }

    public double violationLevel() {
        return violation.violationLevel();
    }

    public double confidence() {
        return violation.confidence();
    }
}
