package io.github.no1qq.uagc.bukkit.command;

import io.github.no1qq.uagc.bukkit.UagcRuntime;
import io.github.no1qq.uagc.engine.bypass.TemporaryBypass;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.RegisteredCheck;
import io.github.no1qq.uagc.engine.evidence.EvidenceEntry;
import io.github.no1qq.uagc.engine.exemption.ExemptionGrant;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.util.DurationParser;
import io.github.no1qq.uagc.engine.violation.Violation;
import io.github.no1qq.uagc.engine.violation.ViolationTracker;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.List;

final class InspectionRenderer {

    private InspectionRenderer() {
    }

    static void liveState(CommandSourceStack source, PlayerData data) {
        MovementSnapshot last = data.movement().last();
        CommandSupport.sendRaw(source, "<gray>  ping: <white>" + data.latency().lastPing()
                + "ms</white> <gray>avg <white>" + CommandSupport.formatDouble(data.latency().averagePing())
                + "ms</white> <gray>jitter <white>"
                + CommandSupport.formatDouble(data.latency().jitter()) + "ms</white>");
        if (last == null) {
            CommandSupport.sendRaw(source, "<gray>  no movement has been sampled yet</gray>");
        } else {
            CommandSupport.sendRaw(source, "<gray>  position: <white>" + last.to() + "</white>");
            CommandSupport.sendRaw(source, "<gray>  rotation: <white>" + last.toRotation() + "</white>");
            CommandSupport.sendRaw(source, "<gray>  horizontal: <white>"
                    + CommandSupport.formatDouble(data.movement().horizontalSpeed())
                    + "</white> <gray>vertical: <white>"
                    + CommandSupport.formatDouble(data.movement().verticalDelta()) + "</white>");
            CommandSupport.sendRaw(source, "<gray>  ground: client=<white>" + last.clientOnGround()
                    + "</white> server=<white>" + last.surface().solidBelow()
                    + "</white> <gray>air ticks <white>" + data.movement().airTicks() + "</white>");
            CommandSupport.sendRaw(source, "<gray>  surface: <white>" + last.surface().blockBelow()
                    + "</white> <gray>friction <white>"
                    + CommandSupport.formatDouble(last.surface().friction()) + "</white>");
            CommandSupport.sendRaw(source, "<gray>  activity: sprint=<white>" + last.activity().sprinting()
                    + "</white> fly=<white>" + last.activity().flying()
                    + "</white> vehicle=<white>" + last.activity().insideVehicle() + "</white>");
        }
        CommandSupport.sendRaw(source, "<gray>  last applied velocity: <white>" + data.velocity().lastApplied()
                + "</white> <dark_gray>" + data.velocity().lastCause() + "</dark_gray>");
        CommandSupport.sendRaw(source, "<gray>  setbacks: <white>" + data.setbackCount() + "</white>");
    }

    static void bypass(CommandSourceStack source, UagcRuntime runtime, PlayerData data) {
        long tick = runtime.server().currentTick();
        boolean any = data.bypass().hasAnyBypass(tick);
        CommandSupport.sendRaw(source, "<gray>  bypass: " + (any ? "<gold>active</gold>" : "<white>none</white>"));
        if (!any) {
            return;
        }
        if (data.bypass().permissionAll()) {
            CommandSupport.sendRaw(source, "<gray>    permission <white>uagc.bypass.all</white>");
        } else {
            for (CheckCategory category : CheckCategory.values()) {
                if (data.bypass().permissionCategory(category)) {
                    CommandSupport.sendRaw(source, "<gray>    permission <white>"
                            + category.bypassPermission() + "</white>");
                }
            }
            for (RegisteredCheck registered : runtime.registry().all()) {
                if (data.bypass().permissionCheck(registered.index())) {
                    CommandSupport.sendRaw(source, "<gray>    permission <white>"
                            + registered.definition().bypassPermission() + "</white>");
                }
            }
        }
        for (TemporaryBypass entry : data.bypass().activeTemporary(tick)) {
            CommandSupport.sendRaw(source, "<gray>    temporary <white>" + entry.scope().describe()
                    + "</white> <gray>by <white>" + entry.grantedBy() + "</white> <gray>for <white>"
                    + DurationParser.formatTicks(entry.remainingTicks(tick)) + "</white>"
                    + (entry.reason().isEmpty() ? "" : " <dark_gray>" + entry.reason() + "</dark_gray>"));
        }
    }

    static void violations(CommandSourceStack source, UagcRuntime runtime, PlayerData data) {
        long tick = runtime.server().currentTick();
        boolean any = false;
        for (RegisteredCheck registered : runtime.registry().all()) {
            ViolationTracker tracker = data.violationsIfPresent(registered.index());
            if (tracker == null || !tracker.hasEverFlagged()) {
                continue;
            }
            any = true;
            double level = tracker.current(tick, registered.config().decayPerTick());
            CommandSupport.sendRaw(source, "<gray>  <white>" + registered.id() + "</white> vl <yellow>"
                    + CommandSupport.formatDouble(level) + "</yellow> <gray>peak <white>"
                    + CommandSupport.formatDouble(tracker.peakLevel()) + "</white> <gray>flags <white>"
                    + tracker.flagCount() + "</white> <gray>confidence <white>"
                    + CommandSupport.formatPercent(tracker.lastConfidence()) + "</white>");
        }
        if (!any) {
            CommandSupport.sendRaw(source, "<gray>  no checks have flagged this player</gray>");
        }
    }

    static void evidence(CommandSourceStack source, PlayerData data, int limit) {
        List<Violation> violations = data.evidence().recentViolations(limit);
        for (Violation violation : violations) {
            CommandSupport.sendRaw(source, "<gray>  <white>" + violation.checkId() + "</white> vl <yellow>"
                    + CommandSupport.formatDouble(violation.violationLevel()) + "</yellow> <gray>confidence <white>"
                    + CommandSupport.formatPercent(violation.confidence()) + "</white> <dark_gray>"
                    + violation.summary() + "</dark_gray>");
        }
        List<EvidenceEntry> entries = data.evidence().recentEntries(limit);
        for (EvidenceEntry entry : entries) {
            CommandSupport.sendRaw(source, "<dark_gray>  [" + entry.type().id() + "] "
                    + entry.summary() + "</dark_gray>");
        }
        if (violations.isEmpty() && entries.isEmpty()) {
            CommandSupport.sendRaw(source, "<gray>  nothing has been recorded yet</gray>");
        }
    }

    static void exemptions(CommandSourceStack source, UagcRuntime runtime, PlayerData data) {
        long tick = runtime.server().currentTick();
        List<ExemptionGrant> grants = data.exemptions().active();
        if (grants.isEmpty()) {
            CommandSupport.sendRaw(source, "<gray>  none</gray>");
            return;
        }
        for (ExemptionGrant grant : grants) {
            CommandSupport.sendRaw(source, "<gray>  <white>" + grant.type().id() + "</white> <gray>from <white>"
                    + grant.source() + "</white> <gray>for <white>"
                    + DurationParser.formatTicks(grant.remainingTicks(tick)) + "</white>"
                    + (grant.reason().isEmpty() ? "" : " <dark_gray>" + grant.reason() + "</dark_gray>"));
        }
    }
}
