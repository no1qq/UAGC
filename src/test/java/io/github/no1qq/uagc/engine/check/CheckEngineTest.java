package io.github.no1qq.uagc.engine.check;

import io.github.no1qq.uagc.engine.check.event.MovementEvent;
import io.github.no1qq.uagc.engine.config.CheckConfig;
import io.github.no1qq.uagc.engine.config.GeneralSettings;
import io.github.no1qq.uagc.engine.config.UagcConfig;
import io.github.no1qq.uagc.engine.exemption.ExemptionType;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.violation.ViolationTracker;
import io.github.no1qq.uagc.support.EngineHarness;
import io.github.no1qq.uagc.support.SnapshotBuilder;
import io.github.no1qq.uagc.support.StubChecks;
import io.github.no1qq.uagc.support.Surfaces;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckEngineTest {

    private static UagcConfig configWith(Map<String, CheckConfig> checks) {
        UagcConfig base = UagcConfig.defaults();
        return new UagcConfig(base.general(), base.playerData(), base.confidence(), base.alerts(),
                base.freeze(), base.punishments(), base.debug(), checks);
    }

    private static MovementSnapshot snapshot(long tick) {
        return SnapshotBuilder.create()
                .tick(tick)
                .surface(Surfaces.ground())
                .build();
    }

    private static UagcConfig loggingViolations() {
        UagcConfig base = UagcConfig.defaults();
        GeneralSettings general = new GeneralSettings(true, base.general().bypassRefreshIntervalTicks(),
                base.general().lagSpikeThresholdMillis(), base.general().exemptOnLagSpike(),
                base.general().maxCheckFailuresBeforeDisable(), base.general().logPunishments(), true);
        return new UagcConfig(general, base.playerData(), base.confidence(), base.alerts(),
                base.freeze(), base.punishments(), base.debug(), base.checks());
    }

    private static long violationLines(EngineHarness harness) {
        return harness.server().infoMessages().stream()
                .filter(line -> line.startsWith("violation "))
                .count();
    }

    @Test
    void theConsoleOnlyEverSeesOneKindOfLine() {
        EngineHarness harness = new EngineHarness(loggingViolations(),
                new StubChecks.AlwaysFlags("stub", 1.0D, false));
        PlayerData player = harness.addPlayer("tester");

        for (long tick = 1L; tick <= 6L; tick++) {
            harness.clock().setTick(tick);
            harness.process(player, new MovementEvent(snapshot(tick)));
        }

        assertTrue(harness.messages().consoleAlerts().size() > 0, "the alert must reach the console");
        assertEquals(0L, violationLines(harness),
                "the alert line owns the console, the plain line must never join it");
    }

    @Test
    void thePlainLineComesBackWhenTheConsoleTookItselfOffAlerts() {
        EngineHarness harness = new EngineHarness(loggingViolations(),
                new StubChecks.AlwaysFlags("stub", 1.0D, false));
        harness.alerts().setConsoleAlerts(false);
        PlayerData player = harness.addPlayer("tester");

        for (long tick = 1L; tick <= 6L; tick++) {
            harness.clock().setTick(tick);
            harness.process(player, new MovementEvent(snapshot(tick)));
        }

        assertEquals(0, harness.messages().consoleAlerts().size(), "the console asked for no alerts");
        assertEquals(6L, violationLines(harness), "with no alerts the plain log is all there is");
    }

    @Test
    void flaggingAccumulatesViolationLevel() {
        EngineHarness harness = new EngineHarness(UagcConfig.defaults(),
                new StubChecks.AlwaysFlags("stub", 1.0D, false));
        PlayerData player = harness.addPlayer("tester");

        for (long tick = 1L; tick <= 5L; tick++) {
            harness.clock().setTick(tick);
            harness.process(player, new MovementEvent(snapshot(tick)));
        }

        ViolationTracker tracker = player.violationsIfPresent(0);
        assertNotNull(tracker);
        assertEquals(5, tracker.flagCount());
        assertTrue(tracker.rawLevel() > 4.0D);
    }

    @Test
    void detectionsBelowTheConfidenceFloorDoNotCount() {
        EngineHarness harness = new EngineHarness(UagcConfig.defaults(),
                new StubChecks.AlwaysFlags("stub", 0.05D, false));
        PlayerData player = harness.addPlayer("tester");

        for (long tick = 1L; tick <= 5L; tick++) {
            harness.clock().setTick(tick);
            harness.process(player, new MovementEvent(snapshot(tick)));
        }

        assertEquals(null, player.violationsIfPresent(0));
        assertTrue(player.evidence().entryCount() > 0, "weak detections are still kept as evidence");
    }

    @Test
    void bypassSuppressesViolationsButRemainsVisible() {
        EngineHarness harness = new EngineHarness(UagcConfig.defaults(),
                new StubChecks.AlwaysFlags("stub", 1.0D, false));
        PlayerData player = harness.addPlayer("developer");
        harness.server().grant(player.uuid(), "uagc.bypass.all");
        harness.refreshBypass(player);

        for (long tick = 1L; tick <= 5L; tick++) {
            harness.clock().setTick(tick);
            harness.process(player, new MovementEvent(snapshot(tick)));
        }

        assertEquals(null, player.violationsIfPresent(0), "a bypassed player must not accumulate violations");
        assertTrue(player.bypass().hasPermissionBypass());
        assertTrue(player.evidence().entryCount() > 0, "suppressed detections stay visible to staff");
        assertTrue(harness.messages().alerts().isEmpty());
    }

    @Test
    void exemptionsSkipTheCheckEntirely() {
        StubChecks.NeverFlags counter = new StubChecks.NeverFlags("counter");
        EngineHarness harness = new EngineHarness(UagcConfig.defaults(), counter);
        PlayerData player = harness.addPlayer("tester");

        player.exemptions().grant(ExemptionType.TELEPORT, 20, "test", "");
        harness.clock().setTick(1L);
        harness.process(player, new MovementEvent(snapshot(1L)));
        assertEquals(0, counter.invocations(), "an exempt category must not even run the check");

        harness.clock().advanceTicks(30L);
        harness.process(player, new MovementEvent(snapshot(31L)));
        assertEquals(1, counter.invocations(), "the check runs again once the exemption expires");
    }

    @Test
    void aFailingCheckIsIsolatedAndEventuallyDisabled() {
        StubChecks.AlwaysThrows broken = new StubChecks.AlwaysThrows("broken");
        StubChecks.NeverFlags healthy = new StubChecks.NeverFlags("healthy");
        EngineHarness harness = new EngineHarness(UagcConfig.defaults(), broken, healthy);
        PlayerData player = harness.addPlayer("tester");

        int limit = UagcConfig.defaults().general().maxCheckFailuresBeforeDisable();
        for (long tick = 1L; tick <= limit + 5L; tick++) {
            harness.clock().setTick(tick);
            harness.process(player, new MovementEvent(snapshot(tick)));
        }

        RegisteredCheck registered = harness.registry().byId("broken");
        assertTrue(registered.isRuntimeDisabled(), "a repeatedly failing check must be isolated");
        assertEquals(limit, broken.invocations(), "the broken check stops being invoked once disabled");
        assertEquals(limit + 5, healthy.invocations(), "the healthy check keeps running");
        assertFalse(harness.server().errors().isEmpty());
    }
}
