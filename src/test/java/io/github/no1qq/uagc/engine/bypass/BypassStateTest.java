package io.github.no1qq.uagc.engine.bypass;

import io.github.no1qq.uagc.engine.check.CheckCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BypassStateTest {

    private BypassState state;

    @BeforeEach
    void setUp() {
        state = new BypassState();
    }

    private boolean[] categories(CheckCategory... granted) {
        boolean[] flags = new boolean[CheckCategory.values().length];
        for (CheckCategory category : granted) {
            flags[category.ordinal()] = true;
        }
        return flags;
    }

    @Test
    void categoryBypassIsScopedToThatCategory() {
        state.applyPermissions(false, categories(CheckCategory.MOVEMENT), new boolean[4], 0L);
        assertTrue(state.isBypassed(CheckCategory.MOVEMENT, 0, "speed", 0L));
        assertFalse(state.isBypassed(CheckCategory.COMBAT, 1, "reach", 0L),
                "a movement bypass must leave combat checks active");
    }

    @Test
    void checkBypassIsScopedToThatCheck() {
        boolean[] checks = new boolean[4];
        checks[2] = true;
        state.applyPermissions(false, categories(), checks, 0L);
        assertTrue(state.isBypassed(CheckCategory.MOVEMENT, 2, "speed", 0L));
        assertFalse(state.isBypassed(CheckCategory.MOVEMENT, 1, "fly", 0L));
    }

    @Test
    void bypassAllCoversEverything() {
        state.applyPermissions(true, categories(), new boolean[4], 0L);
        for (CheckCategory category : CheckCategory.values()) {
            assertTrue(state.isBypassed(category, 0, "any", 0L));
        }
        assertTrue(state.hasPermissionBypass());
    }

    @Test
    void temporaryBypassExpires() {
        state.grantTemporary(new TemporaryBypass(BypassScope.category(CheckCategory.MOVEMENT),
                0L, 100L, 0L, "staff", "testing"));
        assertTrue(state.isBypassed(CheckCategory.MOVEMENT, 0, "speed", 50L));
        assertFalse(state.isBypassed(CheckCategory.MOVEMENT, 0, "speed", 100L));
    }

    @Test
    void temporaryBypassCanBePermanentUntilRevoked() {
        state.grantTemporary(new TemporaryBypass(BypassScope.all(), 0L, -1L, 0L, "staff", ""));
        assertTrue(state.isBypassed(CheckCategory.COMBAT, 0, "reach", 999_999L));
        assertEquals(1, state.revokeAllTemporary());
        assertFalse(state.isBypassed(CheckCategory.COMBAT, 0, "reach", 999_999L));
    }

    @Test
    void grantingTheSameScopeTwiceReplacesTheOlderGrant() {
        state.grantTemporary(new TemporaryBypass(BypassScope.check("speed"), 0L, 50L, 0L, "a", ""));
        state.grantTemporary(new TemporaryBypass(BypassScope.check("speed"), 0L, 500L, 0L, "b", ""));
        assertEquals(1, state.activeTemporary(10L).size());
        assertEquals("b", state.activeTemporary(10L).getFirst().grantedBy());
    }

    @Test
    void revokingTemporaryLeavesPermissionBypassIntact() {
        state.applyPermissions(true, categories(), new boolean[4], 0L);
        state.grantTemporary(new TemporaryBypass(BypassScope.all(), 0L, 100L, 0L, "staff", ""));
        state.revokeAllTemporary();
        assertTrue(state.hasPermissionBypass(), "revoking a temporary grant must not touch permissions");
    }

    @Test
    void clearResetsEverything() {
        state.applyPermissions(true, categories(), new boolean[4], 0L);
        state.grantTemporary(new TemporaryBypass(BypassScope.all(), 0L, 100L, 0L, "staff", ""));
        state.clear();
        assertFalse(state.hasAnyBypass(0L));
        assertTrue(state.needsRefresh(0L, 20L));
    }
}
