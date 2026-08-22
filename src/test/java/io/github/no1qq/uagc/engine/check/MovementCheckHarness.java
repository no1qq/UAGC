package io.github.no1qq.uagc.engine.check;

import io.github.no1qq.uagc.engine.check.event.MovementEvent;
import io.github.no1qq.uagc.engine.config.CheckConfig;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.platform.ServerConditions;
import io.github.no1qq.uagc.engine.platform.SupportQuery;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.player.PlayerDataSettings;
import io.github.no1qq.uagc.support.TestClock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MovementCheckHarness<S> {

    private final Check<MovementEvent, S> check;
    private final TestClock clock = new TestClock();
    private final PlayerData player;
    private final List<CheckResult> flags = new ArrayList<>();

    private S state;
    private CheckConfig config;
    private ServerConditions conditions = ServerConditions.healthy();
    private SupportQuery support = SupportQuery.NONE;

    public MovementCheckHarness(Check<MovementEvent, S> check) {
        this.check = check;
        this.config = CheckConfig.defaults(check.definition().id());
        this.player = new PlayerData(UUID.randomUUID(), "tester", clock, 16, PlayerDataSettings.defaults());
        this.state = check.createState();
    }

    public MovementCheckHarness<S> withOption(String key, double value) {
        Map<String, Double> options = new java.util.LinkedHashMap<>(config.options());
        options.put(key, value);
        this.config = new CheckConfig(config.checkId(), config.enabled(), config.violationIncrement(),
                config.decayPerTick(), config.maxViolationLevel(), config.minimumConfidence(),
                config.alertThreshold(), config.setbackEnabled(), config.setbackThreshold(),
                config.cancelEnabled(), Map.copyOf(options));
        return this;
    }

    public MovementCheckHarness<S> withConditions(ServerConditions value) {
        this.conditions = value;
        return this;
    }

    public MovementCheckHarness<S> withSupport(SupportQuery value) {
        this.support = value;
        return this;
    }

    public PlayerData player() {
        return player;
    }

    public TestClock clock() {
        return clock;
    }

    public CheckResult feed(MovementSnapshot snapshot) {
        clock.setTick(snapshot.tick());
        clock.setMillis(snapshot.timeMillis());
        player.latency().record(snapshot.ping());
        player.movement().update(snapshot);

        CheckContext context = new CheckContext(player, conditions, snapshot.tick(), snapshot.timeMillis(),
                DebugSink.NONE, support);
        context.prepare(check.definition().id(), config);
        CheckResult result = check.inspect(context, new MovementEvent(snapshot), state);
        if (result != null && result.flagged()) {
            flags.add(result);
        }
        return result;
    }

    public List<CheckResult> flags() {
        return flags;
    }

    public int flagCount() {
        return flags.size();
    }

    public boolean flagged() {
        return !flags.isEmpty();
    }

    public void resetState() {
        this.state = check.createState();
        flags.clear();
    }
}
