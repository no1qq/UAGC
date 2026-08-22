package io.github.no1qq.uagc.engine.punishment;

import io.github.no1qq.uagc.engine.check.event.MovementEvent;
import io.github.no1qq.uagc.engine.config.UagcConfig;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.support.EngineHarness;
import io.github.no1qq.uagc.support.SnapshotBuilder;
import io.github.no1qq.uagc.support.StubChecks;
import io.github.no1qq.uagc.support.Surfaces;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PunishmentServiceTest {

    private static UagcConfig withRules(List<PunishmentRule> rules, boolean dryRun) {
        UagcConfig base = UagcConfig.defaults();
        PunishmentConfig punishments = new PunishmentConfig(true, dryRun,
                base.punishments().defaultKickMessage(),
                base.punishments().defaultBanMessage(),
                base.punishments().defaultTempBanMessage(),
                "UAGC", rules);
        return new UagcConfig(base.general(), base.playerData(), base.confidence(), base.alerts(),
                base.freeze(), punishments, base.debug(), base.checks());
    }

    private void drive(EngineHarness harness, PlayerData player, int ticks) {
        for (long tick = 1L; tick <= ticks; tick++) {
            harness.clock().setTick(tick);
            harness.process(player, new MovementEvent(
                    SnapshotBuilder.create().tick(tick).surface(Surfaces.ground()).build()));
        }
    }

    @Test
    void aRuleFiresOnceTheViolationThresholdIsReached() {
        PunishmentRule rule = new PunishmentRule("stub", 5.0D, 0.5D, 1,
                PunishmentAction.KICK, "", "cheating", false, 0);
        EngineHarness harness = new EngineHarness(withRules(List.of(rule), false),
                new StubChecks.AlwaysFlags("stub", 1.0D, false));
        PlayerData player = harness.addPlayer("cheater");

        drive(harness, player, 4);
        assertTrue(harness.enforcement().kicks().isEmpty(), "the threshold must not fire early");

        drive(harness, player, 12);
        assertEquals(1, harness.enforcement().kicks().size(), "a non repeatable rule fires exactly once");
    }

    @Test
    void aPunishmentCarriesItsEvidence() {
        PunishmentRule rule = new PunishmentRule("stub", 3.0D, 0.5D, 1,
                PunishmentAction.BAN, "", "cheating", false, 0);
        EngineHarness harness = new EngineHarness(withRules(List.of(rule), false),
                new StubChecks.AlwaysFlags("stub", 1.0D, false));
        PlayerData player = harness.addPlayer("cheater");
        drive(harness, player, 10);

        assertEquals(1, harness.enforcement().bans().size());
        PunishmentRecord record = harness.enforcement().bans().getFirst().record();
        assertEquals("stub", record.checkId());
        assertTrue(record.automatic());
        assertTrue(record.confidence() > 0.0D);
        assertTrue(record.evidence().containsKey("ping"));
        assertTrue(record.evidence().containsKey("tps"));
        assertFalse(record.reference().isBlank());
    }

    @Test
    void dryRunRecordsWithoutEnforcing() {
        PunishmentRule rule = new PunishmentRule("stub", 3.0D, 0.5D, 1,
                PunishmentAction.KICK, "", "cheating", false, 0);
        EngineHarness harness = new EngineHarness(withRules(List.of(rule), true),
                new StubChecks.AlwaysFlags("stub", 1.0D, false));
        PlayerData player = harness.addPlayer("cheater");
        drive(harness, player, 10);

        assertTrue(harness.enforcement().kicks().isEmpty(), "dry run must not remove the player");
        assertEquals(1, harness.punishments().recentFor(player.uuid(), 10).size(),
                "dry run still records what would have happened");
    }

    @Test
    void aRuleForAnotherCheckIsNotApplied() {
        PunishmentRule rule = new PunishmentRule("something_else", 1.0D, 0.0D, 1,
                PunishmentAction.KICK, "", "", false, 0);
        EngineHarness harness = new EngineHarness(withRules(List.of(rule), false),
                new StubChecks.AlwaysFlags("stub", 1.0D, false));
        PlayerData player = harness.addPlayer("player");
        drive(harness, player, 20);
        assertTrue(harness.enforcement().kicks().isEmpty());
    }

    @Test
    void categoryScopedRulesMatchEveryCheckInThatCategory() {
        PunishmentRule rule = new PunishmentRule("category:movement", 3.0D, 0.5D, 1,
                PunishmentAction.KICK, "", "", false, 0);
        EngineHarness harness = new EngineHarness(withRules(List.of(rule), false),
                new StubChecks.AlwaysFlags("stub", 1.0D, false));
        PlayerData player = harness.addPlayer("player");
        drive(harness, player, 10);
        assertEquals(1, harness.enforcement().kicks().size());
    }

    @Test
    void aHighConfidenceRequirementBlocksWeakEvidence() {
        PunishmentRule rule = new PunishmentRule("stub", 1.0D, 0.95D, 1,
                PunishmentAction.KICK, "", "", false, 0);
        EngineHarness harness = new EngineHarness(withRules(List.of(rule), false),
                new StubChecks.AlwaysFlags("stub", 0.5D, false));
        PlayerData player = harness.addPlayer("player");
        drive(harness, player, 30);
        assertTrue(harness.enforcement().kicks().isEmpty(),
                "weak evidence must not satisfy a high confidence rule");
    }

    @Test
    void manualPunishmentsUseTheSameEngine() {
        EngineHarness harness = new EngineHarness(UagcConfig.defaults(),
                new StubChecks.NeverFlags("stub"));
        PlayerData player = harness.addPlayer("player");

        PunishmentRecord record = harness.punishments().punishManually(player, player.uuid(), player.name(),
                PunishmentAction.KICK, "", "staff decision", "moderator");

        assertEquals(1, harness.enforcement().kicks().size());
        assertFalse(record.automatic());
        assertEquals("moderator", record.issuedBy());
        assertEquals(record.reference(), harness.punishments().byReference(record.reference()).reference());
    }
}
