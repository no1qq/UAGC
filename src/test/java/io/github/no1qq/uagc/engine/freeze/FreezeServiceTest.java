package io.github.no1qq.uagc.engine.freeze;

import io.github.no1qq.uagc.engine.config.UagcConfig;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.support.EngineHarness;
import io.github.no1qq.uagc.support.StubChecks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreezeServiceTest {

    private EngineHarness harness;
    private PlayerData player;

    @BeforeEach
    void setUp() {
        harness = new EngineHarness(UagcConfig.defaults(), new StubChecks.NeverFlags("stub"));
        player = harness.addPlayer("suspect");
    }

    private FreezeRecord freeze(long durationMillis) {
        return harness.freeze().freeze(player.uuid(), player.name(), "moderator",
                "under investigation", durationMillis, "world", 1.0D, 64.0D, 2.0D);
    }

    @Test
    void freezingRecordsWhoDidItAndWhy() {
        FreezeRecord record = freeze(0L);
        assertTrue(harness.freeze().isFrozen(player.uuid()));
        assertEquals("moderator", record.staffName());
        assertEquals("under investigation", record.reason());
        assertTrue(record.isPermanentUntilReleased());
        assertFalse(harness.messages().titles().isEmpty(), "a frozen player is told what happened");
    }

    @Test
    void releasingClearsTheState() {
        freeze(0L);
        assertTrue(harness.freeze().release(player.uuid(), "moderator"));
        assertFalse(harness.freeze().isFrozen(player.uuid()));
        assertNull(harness.freeze().record(player.uuid()));
        assertFalse(harness.freeze().release(player.uuid(), "moderator"),
                "releasing twice must report that nothing changed");
    }

    @Test
    void timedFreezesExpireOnTheirOwn() {
        freeze(1_000L);
        assertTrue(harness.freeze().isFrozen(player.uuid()));
        harness.clock().advanceMillis(1_500L);
        assertFalse(harness.freeze().isFrozen(player.uuid()), "a timed freeze must release itself");
    }

    @Test
    void freezeSurvivesUntilExplicitlyReleased() {
        freeze(0L);
        harness.clock().advanceMillis(60L * 60L * 1000L);
        assertTrue(harness.freeze().isFrozen(player.uuid()),
                "a freeze without a duration must not silently expire");
        assertNotNull(harness.freeze().record(player.uuid()));
    }

    @Test
    void allowedCommandsStayUsableWhileFrozen() {
        freeze(0L);
        assertTrue(harness.freeze().isCommandAllowed("/msg staff hello"));
        assertTrue(harness.freeze().isCommandAllowed("/minecraft:msg staff hello"));
        assertFalse(harness.freeze().isCommandAllowed("/tp 0 0 0"));
        assertFalse(harness.freeze().isCommandAllowed("/spawn"));
    }

    @Test
    void freezingIsRecordedAsEvidence() {
        freeze(0L);
        harness.freeze().release(player.uuid(), "moderator");
        assertTrue(player.evidence().entryCount() >= 2,
                "both the freeze and the release belong in the investigation trail");
    }

    @Test
    void reapplyingOnJoinNotifiesThePlayerAgain() {
        freeze(0L);
        harness.messages().clear();
        harness.freeze().reapplyOnJoin(player.uuid());
        assertFalse(harness.messages().titles().isEmpty(),
                "a returning frozen player must be told they are still frozen");
    }
}
