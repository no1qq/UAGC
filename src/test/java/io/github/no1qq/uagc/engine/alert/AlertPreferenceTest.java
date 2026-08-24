package io.github.no1qq.uagc.engine.alert;

import io.github.no1qq.uagc.engine.config.AlertConfig;
import io.github.no1qq.uagc.engine.config.UagcConfig;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.support.EngineHarness;
import io.github.no1qq.uagc.support.StubChecks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertPreferenceTest {

    private static final class RecordingStore implements AlertPreferenceStore {

        private AlertPreferences stored = AlertPreferences.empty();
        private int saves;

        @Override
        public AlertPreferences load() {
            return stored;
        }

        @Override
        public void save(AlertPreferences preferences) {
            stored = preferences;
            saves++;
        }
    }

    private EngineHarness harness;
    private RecordingStore store;
    private AlertService alerts;
    private PlayerData staff;

    @BeforeEach
    void setUp() {
        harness = new EngineHarness(UagcConfig.defaults(), new StubChecks.AlwaysFlags("stub", 1.0D, false));
        staff = harness.addPlayer("moderator");
        store = new RecordingStore();
        alerts = new AlertService(harness.players(), harness.server(), harness.messages(),
                UagcConfig.defaults().alerts(), store);
    }

    @Test
    void staffWithThePermissionStartWithAlertsOn() {
        staff.alertSettings().setEnabled(false);
        alerts.applyTo(staff, true);
        assertTrue(staff.alertSettings().enabled(), "a first time staff member must get alerts without asking");
    }

    @Test
    void playersWithoutThePermissionStartWithAlertsOff() {
        staff.alertSettings().setEnabled(true);
        alerts.applyTo(staff, false);
        assertFalse(staff.alertSettings().enabled());
    }

    @Test
    void turningAlertsOffSurvivesRejoining() {
        alerts.remember(staff.uuid(), false);
        staff.alertSettings().setEnabled(true);
        alerts.applyTo(staff, true);
        assertFalse(staff.alertSettings().enabled(), "an explicit opt out must outlive the session");
    }

    @Test
    void turningAlertsBackOnSurvivesRejoining() {
        AlertConfig offByDefault = new AlertConfig(true, false, "", "", "", 0.0D, 0.0D, 20, false, false, 10);
        AlertService service = new AlertService(harness.players(), harness.server(), harness.messages(),
                offByDefault, store);
        service.remember(staff.uuid(), true);
        staff.alertSettings().setEnabled(false);
        service.applyTo(staff, true);
        assertTrue(staff.alertSettings().enabled(), "an explicit opt in must outlive the session too");
    }

    @Test
    void oneStaffMemberOptingOutLeavesEveryoneElseAlone() {
        PlayerData other = harness.addPlayer("other");
        alerts.remember(staff.uuid(), false);
        alerts.applyTo(staff, true);
        alerts.applyTo(other, true);
        assertFalse(staff.alertSettings().enabled());
        assertTrue(other.alertSettings().enabled(), "disabling alerts must never be global");
    }

    @Test
    void preferencesAreWrittenAndReadBack() {
        alerts.remember(staff.uuid(), false);
        assertEquals(1, store.saves, "a change must be persisted immediately, not only on shutdown");

        AlertService reloaded = new AlertService(harness.players(), harness.server(), harness.messages(),
                UagcConfig.defaults().alerts(), store);
        reloaded.loadPersisted();
        staff.alertSettings().setEnabled(true);
        reloaded.applyTo(staff, true);
        assertFalse(staff.alertSettings().enabled(), "the opt out must come back after a restart");
    }

    @Test
    void consoleFollowsTheConfigUntilItIsToggled() {
        assertTrue(alerts.consoleAlertsEnabled(), "send-to-console defaults to true");
        assertFalse(alerts.toggleConsoleAlerts(), "toggling from console must turn it off");
        assertFalse(alerts.consoleAlertsEnabled());
        assertTrue(alerts.toggleConsoleAlerts(), "and toggling again must turn it back on");
    }

    @Test
    void theConsoleChoiceOutlivesARestart() {
        alerts.setConsoleAlerts(false);

        AlertService reloaded = new AlertService(harness.players(), harness.server(), harness.messages(),
                UagcConfig.defaults().alerts(), store);
        reloaded.loadPersisted();
        assertFalse(reloaded.consoleAlertsEnabled(), "a silenced console must stay silent after a restart");
    }

    @Test
    void silencingTheConsoleLeavesStaffAlertsAlone() {
        alerts.applyTo(staff, true);
        alerts.setConsoleAlerts(false);
        assertTrue(staff.alertSettings().enabled(), "the console toggle must not touch any player");
    }
}
