package io.github.no1qq.uagc.engine.punishment;

import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.evidence.EvidenceEntry;
import io.github.no1qq.uagc.engine.evidence.EvidenceType;
import io.github.no1qq.uagc.engine.freeze.FreezeService;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.engine.platform.EnforcementGateway;
import io.github.no1qq.uagc.engine.platform.ServerContext;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.util.DurationParser;
import io.github.no1qq.uagc.engine.util.RingBuffer;
import io.github.no1qq.uagc.engine.violation.Violation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class PunishmentService {

    private final ServerContext server;
    private final EnforcementGateway enforcement;
    private final FreezeService freezeService;
    private final RingBuffer<PunishmentRecord> history;
    private final Map<String, Long> ruleCooldowns = new HashMap<>();

    private volatile PunishmentConfig config;
    private volatile boolean logPunishments = true;

    public PunishmentService(ServerContext server,
                             EnforcementGateway enforcement,
                             FreezeService freezeService,
                             PunishmentConfig config,
                             int historySize) {
        this.server = server;
        this.enforcement = enforcement;
        this.freezeService = freezeService;
        this.config = config;
        this.history = new RingBuffer<>(Math.max(32, historySize));
    }

    public void updateConfig(PunishmentConfig updated, boolean shouldLog) {
        this.config = updated;
        this.logPunishments = shouldLog;
    }

    public PunishmentConfig config() {
        return config;
    }

    public boolean evaluate(PlayerData player, Violation violation, int flagCount) {
        PunishmentConfig current = config;
        if (!current.enabled() || current.rules().isEmpty()) {
            return false;
        }
        long tick = server.clock().currentTick();
        boolean applied = false;
        List<PunishmentRule> rules = current.rules();
        for (int i = 0; i < rules.size(); i++) {
            PunishmentRule rule = rules.get(i);
            if (!rule.matches(violation.checkId(), violation.category())) {
                continue;
            }
            if (!rule.isSatisfied(violation.violationLevel(), violation.confidence(), flagCount)) {
                continue;
            }
            String cooldownKey = player.uuid() + "#" + i;
            Long last = ruleCooldowns.get(cooldownKey);
            if (last != null) {
                if (!rule.repeatable()) {
                    continue;
                }
                if (rule.cooldownTicks() > 0 && tick - last < rule.cooldownTicks()) {
                    continue;
                }
            }
            ruleCooldowns.put(cooldownKey, tick);
            apply(player, buildRecord(player, violation, rule));
            applied = true;
        }
        return applied;
    }

    private PunishmentRecord buildRecord(PlayerData player, Violation violation, PunishmentRule rule) {
        Map<String, String> evidence = new LinkedHashMap<>(violation.details());
        evidence.put("ping", Integer.toString(violation.ping()));
        evidence.put("tps", String.format(Locale.ROOT, "%.2f", violation.tps()));
        evidence.put("position", violation.position().toString());
        evidence.put("streak", Integer.toString(violation.streak()));
        if (!violation.activeExemptions().isEmpty()) {
            evidence.put("exemptions", String.join(",", violation.activeExemptions()));
        }
        return new PunishmentRecord(PunishmentRecord.newReference(),
                player.uuid(),
                player.name(),
                violation.checkId(),
                violation.checkDisplayName(),
                violation.category(),
                rule.action(),
                rule.value(),
                rule.reason().isBlank() ? violation.summary() : rule.reason(),
                violation.violationLevel(),
                violation.confidence(),
                violation.tick(),
                violation.timeMillis(),
                true,
                "UAGC",
                Map.copyOf(evidence));
    }

    public PunishmentRecord punishManually(PlayerData player,
                                           UUID playerId,
                                           String playerName,
                                           PunishmentAction action,
                                           String value,
                                           String reason,
                                           String issuedBy) {
        PunishmentRecord record = new PunishmentRecord(PunishmentRecord.newReference(),
                playerId,
                playerName,
                null,
                "manual",
                null,
                action,
                value == null ? "" : value,
                reason == null || reason.isBlank() ? "staff action" : reason,
                0.0D,
                1.0D,
                server.clock().currentTick(),
                server.clock().currentTimeMillis(),
                false,
                issuedBy,
                Map.of());
        apply(player, record);
        return record;
    }

    public void apply(PlayerData player, PunishmentRecord record) {
        history.add(record);
        if (player != null) {
            player.recordEvidence(EvidenceEntry.of(EvidenceType.PUNISHMENT, record.describe())
                    .with("reference", record.reference())
                    .with("action", record.action().id())
                    .with("automatic", record.automatic()));
        }
        if (config.dryRun() && record.action().removesOrRestricts()) {
            server.info("dry run punishment " + record.describe());
            return;
        }
        try {
            execute(player, record);
            if (logPunishments) {
                server.info("punishment applied " + record.describe());
            }
        } catch (RuntimeException exception) {
            server.error("failed to execute punishment " + record.reference(), exception);
        }
    }

    private void execute(PlayerData player, PunishmentRecord record) {
        UUID playerId = record.playerId();
        switch (record.action()) {
            case ALERT, LOG -> {
            }
            case CANCEL, SETBACK -> {
                if (player != null && player.lastSafePosition() != null) {
                    enforcement.setback(playerId, player.lastSafePosition());
                    player.recordSetback(server.clock().currentTick());
                }
            }
            case FREEZE -> {
                Duration duration = DurationParser.parse(record.value(), Duration.ZERO);
                Vec3 position = player == null ? Vec3.ZERO : player.movement().lastPosition();
                freezeService.freeze(playerId, record.playerName(), record.issuedBy(), record.reason(),
                        duration == null ? 0L : duration.toMillis(), "unknown",
                        position.x(), position.y(), position.z());
            }
            case KICK -> enforcement.kick(playerId, record);
            case TEMPBAN -> enforcement.ban(playerId, record, DurationParser.parse(record.value(), Duration.ofDays(7L)));
            case BAN -> enforcement.ban(playerId, record, null);
            case COMMAND -> enforcement.runConsoleCommand(fillPlaceholders(record));
        }
    }

    private String fillPlaceholders(PunishmentRecord record) {
        return record.value()
                .replace("<player>", record.playerName() == null ? "unknown" : record.playerName())
                .replace("<uuid>", record.playerId() == null ? "" : record.playerId().toString())
                .replace("<check>", record.checkId() == null ? "manual" : record.checkId())
                .replace("<reference>", record.reference())
                .replace("<reason>", record.reason());
    }

    public List<PunishmentRecord> recent(int limit) {
        return history.newestFirst(limit);
    }

    public List<PunishmentRecord> recentFor(UUID playerId, int limit) {
        List<PunishmentRecord> matched = new ArrayList<>();
        for (int i = 0; i < history.size() && matched.size() < limit; i++) {
            PunishmentRecord record = history.fromEnd(i);
            if (playerId.equals(record.playerId())) {
                matched.add(record);
            }
        }
        return matched;
    }

    public PunishmentRecord byReference(String reference) {
        for (int i = 0; i < history.size(); i++) {
            PunishmentRecord record = history.fromEnd(i);
            if (record.reference().equalsIgnoreCase(reference)) {
                return record;
            }
        }
        return null;
    }

    public boolean unban(String playerName) {
        return enforcement.unban(playerName);
    }

    public void forget(UUID playerId) {
        ruleCooldowns.keySet().removeIf(key -> key.startsWith(playerId.toString()));
    }

    public double punishThresholdFor(String checkId, CheckCategory category) {
        double lowest = Double.MAX_VALUE;
        for (PunishmentRule rule : config.rules()) {
            if (rule.action().removesOrRestricts() && rule.matches(checkId, category)) {
                lowest = Math.min(lowest, rule.violationLevel());
            }
        }
        return lowest == Double.MAX_VALUE ? 0.0D : lowest;
    }
}
