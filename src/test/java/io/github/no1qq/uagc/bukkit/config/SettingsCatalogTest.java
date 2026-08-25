package io.github.no1qq.uagc.bukkit.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsCatalogTest {

    @Test
    void everySettingTheMenuShowsHasARealNameAndDescription() {
        List<String> shown = List.of(
                "general.enabled",
                "general.log-violations-to-console",
                "general.exempt-on-lag-spike",
                "alerts.enabled",
                "alerts.flag-on-alert",
                "alerts.send-to-console",
                "alerts.cooldown-ticks",
                "alerts.flag-setback-interval-ticks",
                "alerts.default-minimum-confidence",
                "punishments.enabled",
                "punishments.dry-run",
                "debug.enabled");
        for (String path : shown) {
            SettingsCatalog.Label label = SettingsCatalog.of(path);
            assertFalse(label.description().isEmpty(), path + " has no description");
            assertFalse(label.name().contains("-"), path + " still reads like a config key");
            assertFalse(label.name().contains("."), path + " still reads like a config key");
        }
    }

    @Test
    void sharedCheckFieldsResolveForEveryCategory() {
        assertEquals("alert threshold", SettingsCatalog.name("checks.combat.reach.alert-threshold"));
        assertEquals("check enabled", SettingsCatalog.name("checks.movement.no_web.enabled"));
        assertEquals("violation level cap",
                SettingsCatalog.name("checks.protocol.invalid_position.max-violation-level"));
        assertFalse(SettingsCatalog.description("checks.movement.timer.decay-per-tick").isEmpty());
    }

    @Test
    void aCheckOwnWordingWinsOverTheSharedOne() {
        assertEquals("web speed multiplier",
                SettingsCatalog.name("checks.movement.no_web.options.web-multiplier"));
        assertEquals("minimum air gap",
                SettingsCatalog.name("checks.movement.ground_spoof.options.minimum-distance"));
        assertEquals("minimum distance",
                SettingsCatalog.name("checks.movement.sprint_direction.options.minimum-distance"));
        assertEquals("severity scale",
                SettingsCatalog.name("checks.movement.timer.options.severity-scale"));
    }

    @Test
    void anUnknownKeyFallsBackToItsOwnWords() {
        SettingsCatalog.Label label = SettingsCatalog.of("checks.movement.no_web.options.not-a-real-option");
        assertEquals("not a real option", label.name());
        assertTrue(label.description().isEmpty());
    }

    @Test
    void categoriesReadAsEnglish() {
        assertEquals("movement check", SettingsCatalog.category("movement"));
        assertEquals("protocol check", SettingsCatalog.category("protocol"));
    }
}
