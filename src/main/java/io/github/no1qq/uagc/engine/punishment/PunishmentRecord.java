package io.github.no1qq.uagc.engine.punishment;

import io.github.no1qq.uagc.engine.check.CheckCategory;

import java.util.Map;
import java.util.UUID;

public record PunishmentRecord(
        String reference,
        UUID playerId,
        String playerName,
        String checkId,
        String checkDisplayName,
        CheckCategory category,
        PunishmentAction action,
        String value,
        String reason,
        double violationLevel,
        double confidence,
        long tick,
        long timeMillis,
        boolean automatic,
        String issuedBy,
        Map<String, String> evidence) {

    public static String newReference() {
        return Long.toUnsignedString(UUID.randomUUID().getMostSignificantBits(), 36)
                .toUpperCase(java.util.Locale.ROOT)
                .substring(0, 8);
    }

    public String describe() {
        return reference + " " + action.id() + " " + playerName
                + (checkId == null ? "" : " (" + checkId + ")")
                + (automatic ? " [auto]" : " [by " + issuedBy + "]");
    }
}
