package io.github.no1qq.uagc.engine.exemption;

import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.support.TestClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExemptionStateTest {

    private TestClock clock;
    private ExemptionState state;

    @BeforeEach
    void setUp() {
        clock = new TestClock();
        state = new ExemptionState(clock);
    }

    @Test
    void grantsExpireAfterTheirDuration() {
        state.grant(ExemptionType.TELEPORT, 10, "test", "unit test");
        assertTrue(state.isExempt(ExemptionType.TELEPORT));

        clock.advanceTicks(9L);
        assertTrue(state.isExempt(ExemptionType.TELEPORT));

        clock.advanceTicks(1L);
        assertFalse(state.isExempt(ExemptionType.TELEPORT));
    }

    @Test
    void categoryExemptionFollowsTheGrantedType() {
        state.grant(ExemptionType.KNOCKBACK, 10, "test", "");
        assertTrue(state.isCategoryExempt(CheckCategory.MOVEMENT));
        assertFalse(state.isCategoryExempt(CheckCategory.COMBAT));
    }

    @Test
    void joinExemptionCoversEveryCategory() {
        state.grant(ExemptionType.JOIN);
        for (CheckCategory category : CheckCategory.values()) {
            assertTrue(state.isCategoryExempt(category), "expected " + category + " to be exempt on join");
        }
    }

    @Test
    void longerGrantWins() {
        state.grant(ExemptionType.VELOCITY, 5, "short", "");
        state.grant(ExemptionType.VELOCITY, 40, "long", "");
        assertEquals("long", state.grantOf(ExemptionType.VELOCITY).source());

        state.grant(ExemptionType.VELOCITY, 2, "shorter", "");
        assertEquals("long", state.grantOf(ExemptionType.VELOCITY).source());
    }

    @Test
    void revokeClearsImmediately() {
        state.grant(ExemptionType.TELEPORT, 100, "test", "");
        state.revoke(ExemptionType.TELEPORT);
        assertFalse(state.isExempt(ExemptionType.TELEPORT));
        assertNull(state.grantOf(ExemptionType.TELEPORT));
    }

    @Test
    void tracksTicksSinceExpiry() {
        state.grant(ExemptionType.TELEPORT, 5, "test", "");
        assertEquals(0L, state.ticksSinceExpiry(ExemptionType.TELEPORT));

        clock.advanceTicks(12L);
        assertEquals(7L, state.ticksSinceExpiry(ExemptionType.TELEPORT));
    }

    @Test
    void reportsUnknownExemptionsAsNeverGranted() {
        assertEquals(Long.MAX_VALUE, state.ticksSinceExpiry(ExemptionType.PISTON));
        assertFalse(state.isExempt(null));
    }

    @Test
    void listsActiveGrantsOnly() {
        state.grant(ExemptionType.TELEPORT, 10, "a", "");
        state.grant(ExemptionType.VELOCITY, 2, "b", "");
        assertEquals(2, state.active().size());

        clock.advanceTicks(5L);
        assertEquals(1, state.active().size());
        assertNotNull(state.grantOf(ExemptionType.TELEPORT));
    }

    @Test
    void pluginExemptionsRequireAnExplicitDuration() {
        assertNull(state.grant(ExemptionType.PLUGIN_SPEED));
        assertNotNull(state.grant(ExemptionType.PLUGIN_SPEED, 40, "plugin", "ability"));
        assertTrue(state.isExempt(ExemptionType.PLUGIN_SPEED));
    }
}
