package io.github.no1qq.uagc.engine.checks;

import io.github.no1qq.uagc.engine.check.CheckRegistry;
import io.github.no1qq.uagc.engine.checks.combat.AttackRhythmCheck;
import io.github.no1qq.uagc.engine.checks.combat.ReachCheck;
import io.github.no1qq.uagc.engine.checks.interaction.BlockReachCheck;
import io.github.no1qq.uagc.engine.checks.interaction.FastBreakCheck;
import io.github.no1qq.uagc.engine.checks.interaction.InvalidPlacementCheck;
import io.github.no1qq.uagc.engine.checks.movement.GroundSpoofCheck;
import io.github.no1qq.uagc.engine.checks.movement.HorizontalSpeedCheck;
import io.github.no1qq.uagc.engine.checks.movement.NoFallCheck;
import io.github.no1qq.uagc.engine.checks.movement.TimerCheck;
import io.github.no1qq.uagc.engine.checks.movement.VerticalMotionCheck;
import io.github.no1qq.uagc.engine.checks.protocol.InvalidPositionCheck;
import io.github.no1qq.uagc.engine.config.UagcConfig;

public final class CheckBootstrap {

    private CheckBootstrap() {
    }

    public static CheckRegistry createRegistry(UagcConfig config) {
        CheckRegistry registry = new CheckRegistry();
        registerDefaults(registry, config);
        registry.freeze();
        return registry;
    }

    public static void registerDefaults(CheckRegistry registry, UagcConfig config) {
        registry.register(new InvalidPositionCheck(), config);
        registry.register(new VerticalMotionCheck(), config);
        registry.register(new HorizontalSpeedCheck(), config);
        registry.register(new GroundSpoofCheck(), config);
        registry.register(new NoFallCheck(), config);
        registry.register(new TimerCheck(), config);
        registry.register(new ReachCheck(), config);
        registry.register(new AttackRhythmCheck(), config);
        registry.register(new FastBreakCheck(), config);
        registry.register(new BlockReachCheck(), config);
        registry.register(new InvalidPlacementCheck(), config);
    }
}
