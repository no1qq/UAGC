package io.github.no1qq.uagc.engine.check;

import io.github.no1qq.uagc.engine.alert.AlertService;
import io.github.no1qq.uagc.engine.config.CheckConfig;
import io.github.no1qq.uagc.engine.config.UagcConfig;
import io.github.no1qq.uagc.engine.evidence.EvidenceEntry;
import io.github.no1qq.uagc.engine.evidence.EvidenceType;
import io.github.no1qq.uagc.engine.exemption.ExemptionGrant;
import io.github.no1qq.uagc.engine.platform.EnforcementGateway;
import io.github.no1qq.uagc.engine.platform.ServerConditions;
import io.github.no1qq.uagc.engine.platform.ServerContext;
import io.github.no1qq.uagc.engine.platform.SupportQuery;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.punishment.PunishmentService;
import io.github.no1qq.uagc.engine.util.MathUtil;
import io.github.no1qq.uagc.engine.violation.Violation;
import io.github.no1qq.uagc.engine.violation.ViolationTracker;

import java.util.ArrayList;
import java.util.List;

public final class CheckEngine {

    private final CheckRegistry registry;
    private final ConfidenceModel confidenceModel;
    private final AlertService alerts;
    private final PunishmentService punishments;
    private final ServerContext server;
    private final EnforcementGateway enforcement;

    private volatile UagcConfig config;
    private volatile DebugSink debugSink = DebugSink.NONE;
    private volatile SupportQuery supportQuery = SupportQuery.NONE;
    private volatile boolean evaluateBypassedPlayers = true;

    public CheckEngine(CheckRegistry registry,
                       ConfidenceModel confidenceModel,
                       AlertService alerts,
                       PunishmentService punishments,
                       ServerContext server,
                       EnforcementGateway enforcement,
                       UagcConfig config) {
        this.registry = registry;
        this.confidenceModel = confidenceModel;
        this.alerts = alerts;
        this.punishments = punishments;
        this.server = server;
        this.enforcement = enforcement;
        this.config = config;
    }

    public void updateConfig(UagcConfig updated) {
        this.config = updated;
    }

    public void setSupportQuery(SupportQuery query) {
        this.supportQuery = query == null ? SupportQuery.NONE : query;
    }

    public void setDebugSink(DebugSink sink) {
        this.debugSink = sink == null ? DebugSink.NONE : sink;
    }

    public CheckRegistry registry() {
        return registry;
    }

    public ConfidenceModel confidenceModel() {
        return confidenceModel;
    }

    public void process(PlayerData player, CheckEvent event) {
        UagcConfig current = config;
        if (!current.general().enabled() || player == null || event == null) {
            return;
        }
        RegisteredCheck[] handlers = registry.handlersFor(event.getClass());
        if (handlers.length == 0) {
            return;
        }
        ServerConditions conditions = server.conditions();
        CheckContext context = new CheckContext(player, conditions, event.tick(), event.timeMillis(), debugSink, supportQuery);
        for (RegisteredCheck registered : handlers) {
            evaluate(registered, context, player, event, conditions, current);
        }
    }

    private void evaluate(RegisteredCheck registered,
                          CheckContext context,
                          PlayerData player,
                          CheckEvent event,
                          ServerConditions conditions,
                          UagcConfig current) {
        if (!registered.isActive()) {
            return;
        }
        CheckDefinition definition = registered.definition();
        CheckConfig checkConfig = registered.config();
        long tick = event.tick();

        boolean bypassed = player.bypass().isBypassed(definition.category(), registered.index(), definition.id(), tick);
        if (bypassed && !evaluateBypassedPlayers) {
            return;
        }
        if (!registered.check().ignoresExemptions()
                && player.exemptions().isCategoryExempt(definition.category())) {
            return;
        }

        context.prepare(definition.id(), checkConfig);
        CheckResult result;
        try {
            Object state = stateFor(player, registered);
            result = invoke(registered, context, event, state);
            registered.recordSuccess();
        } catch (Throwable throwable) {
            handleFailure(registered, throwable, current);
            return;
        }
        if (result == null || !result.flagged()) {
            return;
        }
        registered.recordFlag();
        handleFlag(registered, player, event, conditions, checkConfig, result, bypassed, current);
    }

    @SuppressWarnings("unchecked")
    private CheckResult invoke(RegisteredCheck registered, CheckContext context, CheckEvent event, Object state) {
        Check<CheckEvent, Object> check = (Check<CheckEvent, Object>) registered.check();
        return check.inspect(context, event, state);
    }

    private Object stateFor(PlayerData player, RegisteredCheck registered) {
        int index = registered.index();
        if (index >= player.violationTrackerCount()) {
            return null;
        }
        Object state = player.checkState(index);
        if (state == null) {
            state = registered.check().createState();
            if (state != null) {
                player.setCheckState(index, state);
            }
        }
        return state;
    }

    private void handleFailure(RegisteredCheck registered, Throwable throwable, UagcConfig current) {
        int failures = registered.recordFailure();
        if (current.debug().logInternalCheckFailures()) {
            server.error("check " + registered.id() + " failed internally", throwable);
        }
        int limit = current.general().maxCheckFailuresBeforeDisable();
        if (limit > 0 && failures >= limit) {
            registered.disableAtRuntime("disabled after " + failures + " consecutive internal failures");
            server.warn("check " + registered.id() + " disabled after repeated internal failures");
        }
    }

    private void handleFlag(RegisteredCheck registered,
                            PlayerData player,
                            CheckEvent event,
                            ServerConditions conditions,
                            CheckConfig checkConfig,
                            CheckResult result,
                            boolean bypassed,
                            UagcConfig current) {
        CheckDefinition definition = registered.definition();
        long tick = event.tick();
        double reliability = confidenceModel.reliability(definition, player, conditions);
        double confidence = MathUtil.clamp01(result.severity() * reliability);

        if (confidence < checkConfig.minimumConfidence()) {
            player.recordEvidence(EvidenceEntry.of(EvidenceType.VIOLATION, "below confidence floor: " + definition.id())
                    .with("check", definition.id())
                    .with("severity", result.severity())
                    .with("reliability", reliability)
                    .with("confidence", confidence)
                    .with("summary", result.summary()));
            return;
        }

        if (bypassed) {
            player.recordEvidence(EvidenceEntry.of(EvidenceType.BYPASS, "suppressed by bypass: " + definition.id())
                    .with("check", definition.id())
                    .with("confidence", confidence)
                    .with("summary", result.summary()));
            return;
        }

        ViolationTracker tracker = player.violations(registered.index(), definition.id());
        double added = checkConfig.violationIncrement() * confidence;
        double level = tracker.add(added, confidence, tick, checkConfig.decayPerTick(), checkConfig.maxViolationLevel());

        Violation violation = new Violation(
                player.uuid(),
                player.name(),
                definition.id(),
                definition.displayName(),
                definition.category(),
                result.severity(),
                reliability,
                confidence,
                added,
                level,
                result.summary(),
                result.details(),
                tick,
                event.timeMillis(),
                player.movement().lastPosition(),
                player.movement().lastRotation(),
                player.latency().lastPing(),
                conditions.tps(),
                activeExemptionIds(player),
                tracker.streak());

        player.evidence().recordViolation(violation);

        if (current.general().logViolationsToConsole()) {
            server.info("violation " + player.name() + " " + violation.describe());
        }

        double punishThreshold = punishments.punishThresholdFor(definition.id(), definition.category());
        boolean alerted = level >= checkConfig.alertThreshold();
        if (alerted) {
            alerts.dispatch(violation, punishThreshold, false);
        }

        if (result.requestSetback() && checkConfig.setbackEnabled() && level >= checkConfig.setbackThreshold()) {
            applySetback(player, violation);
        } else if (alerted && shouldFlagOnAlert(definition, player, tick, current)) {
            applySetback(player, violation);
        }

        punishments.evaluate(player, violation, tracker.flagCount());
    }

    private boolean shouldFlagOnAlert(CheckDefinition definition, PlayerData player, long tick, UagcConfig current) {
        if (!current.alerts().flagOnAlert() || definition.category() != CheckCategory.MOVEMENT) {
            return false;
        }
        long interval = current.alerts().flagSetbackIntervalTicks();
        long last = player.lastSetbackTick();
        return last == Long.MIN_VALUE || tick - last >= interval;
    }

    private void applySetback(PlayerData player, Violation violation) {
        if (player.lastSafePosition() == null) {
            return;
        }
        enforcement.setback(player.uuid(), player.lastSafePosition());
        player.recordSetback(violation.tick());
        player.recordEvidence(EvidenceEntry.of(EvidenceType.SETBACK, "setback from " + violation.checkId())
                .with("to", player.lastSafePosition().toString())
                .with("confidence", violation.confidence()));
    }

    private List<String> activeExemptionIds(PlayerData player) {
        List<ExemptionGrant> grants = player.exemptions().active();
        if (grants.isEmpty()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>(grants.size());
        for (ExemptionGrant grant : grants) {
            ids.add(grant.type().id());
        }
        return ids;
    }

    public void setEvaluateBypassedPlayers(boolean value) {
        this.evaluateBypassedPlayers = value;
    }
}
