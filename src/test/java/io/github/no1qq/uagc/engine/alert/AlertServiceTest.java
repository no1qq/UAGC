package io.github.no1qq.uagc.engine.alert;

import io.github.no1qq.uagc.engine.check.event.MovementEvent;
import io.github.no1qq.uagc.engine.config.UagcConfig;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.support.EngineHarness;
import io.github.no1qq.uagc.support.SnapshotBuilder;
import io.github.no1qq.uagc.support.StubChecks;
import io.github.no1qq.uagc.support.Surfaces;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertServiceTest {

    private EngineHarness harness;
    private PlayerData suspect;
    private PlayerData staff;

    @BeforeEach
    void setUp() {
        harness = new EngineHarness(UagcConfig.defaults(), new StubChecks.AlwaysFlags("stub", 1.0D, false));
        suspect = harness.addPlayer("suspect");
        staff = harness.addPlayer("moderator");
    }

    private void drive(int ticks) {
        for (long tick = 1L; tick <= ticks; tick++) {
            harness.clock().setTick(tick);
            harness.process(suspect, new MovementEvent(
                    SnapshotBuilder.create().tick(tick).surface(Surfaces.ground()).build()));
        }
    }

    @Test
    void staffWithPermissionReceiveAlerts() {
        harness.server().grant(staff.uuid(), AlertService.PERMISSION_VIEW);
        drive(10);
        assertTrue(harness.messages().alerts().stream()
                        .anyMatch(delivery -> delivery.recipient().equals(staff.uuid())),
                "staff holding the alert permission must be notified");
    }

    @Test
    void playersWithoutPermissionNeverReceiveAlerts() {
        drive(10);
        assertTrue(harness.messages().alerts().isEmpty(),
                "alerts must never reach players without the permission");
    }

    @Test
    void staffCanTurnAlertsOff() {
        harness.server().grant(staff.uuid(), AlertService.PERMISSION_VIEW);
        staff.alertSettings().setEnabled(false);
        drive(10);
        assertTrue(harness.messages().alerts().isEmpty(), "disabling alerts must be respected");
    }

    @Test
    void mutedChecksAreFilteredOut() {
        harness.server().grant(staff.uuid(), AlertService.PERMISSION_VIEW);
        staff.alertSettings().toggleCheck("stub");
        drive(10);
        assertTrue(harness.messages().alerts().isEmpty(), "a muted check must not reach that staff member");
    }

    @Test
    void repeatedDetectionsAreRateLimited() {
        harness.server().grant(staff.uuid(), AlertService.PERMISSION_VIEW);
        for (long tick = 1L; tick <= 40L; tick++) {
            harness.clock().setTick(tick);
            harness.process(suspect, new MovementEvent(
                    SnapshotBuilder.create().tick(tick).surface(Surfaces.ground()).build()));
        }
        long delivered = harness.messages().alerts().stream()
                .filter(delivery -> delivery.recipient().equals(staff.uuid()))
                .count();
        assertTrue(delivered < 40L, "staff must not be flooded with one alert per tick");
        assertFalse(harness.messages().alerts().isEmpty(), "but they still get told about it");
    }

    @Test
    void consoleReceivesAlertsWhenConfigured() {
        harness.server().grant(staff.uuid(), AlertService.PERMISSION_VIEW);
        drive(10);
        assertFalse(harness.messages().consoleAlerts().isEmpty());
    }
}
