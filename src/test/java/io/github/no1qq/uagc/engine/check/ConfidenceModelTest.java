package io.github.no1qq.uagc.engine.check;

import io.github.no1qq.uagc.engine.config.ConfidenceSettings;
import io.github.no1qq.uagc.engine.exemption.ExemptionType;
import io.github.no1qq.uagc.engine.platform.ServerConditions;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.player.PlayerDataSettings;
import io.github.no1qq.uagc.support.TestClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfidenceModelTest {

    private static final CheckDefinition SENSITIVE = CheckDefinition
            .builder("sensitive", "Sensitive", CheckCategory.MOVEMENT)
            .latencySensitive()
            .tickSensitive()
            .build();

    private static final CheckDefinition ROBUST = CheckDefinition
            .builder("robust", "Robust", CheckCategory.PROTOCOL)
            .build();

    private TestClock clock;
    private PlayerData player;
    private ConfidenceModel model;

    @BeforeEach
    void setUp() {
        clock = new TestClock();
        player = new PlayerData(UUID.randomUUID(), "tester", clock, 4, PlayerDataSettings.defaults());
        model = new ConfidenceModel(ConfidenceSettings.defaults());
    }

    @Test
    void aHealthyConnectionOnAHealthyServerIsFullyReliable() {
        player.latency().record(40);
        assertEquals(1.0D, model.reliability(SENSITIVE, player, ServerConditions.healthy()), 1.0E-9D);
    }

    @Test
    void highPingReducesButNeverEliminatesReliability() {
        player.latency().record(600);
        double reliability = model.reliability(SENSITIVE, player, ServerConditions.healthy());
        assertTrue(reliability < 0.6D, "a badly lagged connection must lower confidence");
        assertTrue(reliability > 0.0D, "high ping must never disable detection outright");
    }

    @Test
    void lowTickRateReducesButNeverEliminatesReliability() {
        player.latency().record(30);
        ServerConditions struggling = new ServerConditions(10.0D, 110.0D, 0L, 100L, 40);
        double reliability = model.reliability(SENSITIVE, player, struggling);
        assertTrue(reliability < 0.6D, "a lagging server must lower confidence");
        assertTrue(reliability >= ConfidenceSettings.defaults().tickReliabilityFloor() - 1.0E-9D);
    }

    @Test
    void robustChecksIgnoreLatencyAndTickRate() {
        player.latency().record(900);
        ServerConditions struggling = new ServerConditions(5.0D, 200.0D, 0L, 200L, 40);
        assertEquals(1.0D, model.reliability(ROBUST, player, struggling), 1.0E-9D);
    }

    @Test
    void recentStateTransitionsReduceReliability() {
        player.latency().record(30);
        player.exemptions().grant(ExemptionType.TELEPORT, 1, "test", "");
        clock.advanceTicks(2L);

        double reliability = model.reliability(SENSITIVE, player, ServerConditions.healthy());
        assertTrue(reliability < 1.0D, "measurements just after a teleport are less trustworthy");

        clock.advanceTicks(60L);
        assertEquals(1.0D, model.reliability(SENSITIVE, player, ServerConditions.healthy()), 1.0E-9D);
    }

    @Test
    void severityGrowsWithTheSizeOfTheExcess() {
        assertEquals(0.0D, ConfidenceModel.severity(1.0D, 1.0D, 0.5D), 1.0E-9D);
        assertEquals(0.5D, ConfidenceModel.severity(1.25D, 1.0D, 0.5D), 1.0E-9D);
        assertEquals(1.0D, ConfidenceModel.severity(3.0D, 1.0D, 0.5D), 1.0E-9D);
    }

    @Test
    void relativeSeverityScalesWithTheAllowedValue() {
        assertEquals(0.0D, ConfidenceModel.severityRatio(0.28D, 0.30D, 0.5D), 1.0E-9D);
        assertEquals(1.0D, ConfidenceModel.severityRatio(0.60D, 0.30D, 0.5D), 1.0E-9D);
    }
}
