package io.github.no1qq.uagc.engine.check;

import io.github.no1qq.uagc.engine.config.CheckConfig;
import io.github.no1qq.uagc.engine.platform.ServerConditions;
import io.github.no1qq.uagc.engine.platform.SupportQuery;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.player.PlayerDataSettings;
import io.github.no1qq.uagc.support.TestClock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EventCheckHarness<E extends CheckEvent, S> {

    private final Check<E, S> check;
    private final TestClock clock = new TestClock();
    private final PlayerData player;
    private final List<CheckResult> flags = new ArrayList<>();

    private S state;
    private CheckConfig config;
    private ServerConditions conditions = ServerConditions.healthy();
    private SupportQuery support = SupportQuery.NONE;

    public EventCheckHarness(Check<E, S> check) {
        this.check = check;
        this.config = CheckConfig.defaults(check.definition().id());
        this.player = new PlayerData(UUID.randomUUID(), "tester", clock, 16, PlayerDataSettings.defaults());
        this.state = check.createState();
    }

    public EventCheckHarness<E, S> withOption(String key, double value) {
        Map<String, Double> options = new LinkedHashMap<>(config.options());
        options.put(key, value);
        this.config = new CheckConfig(config.checkId(), config.enabled(), config.violationIncrement(),
                config.decayPerTick(), config.maxViolationLevel(), config.minimumConfidence(),
                config.alertThreshold(), config.setbackEnabled(), config.setbackThreshold(),
                config.cancelEnabled(), Map.copyOf(options));
        return this;
    }

    public EventCheckHarness<E, S> withConditions(ServerConditions value) {
        this.conditions = value;
        return this;
    }

    public PlayerData player() {
        return player;
    }

    public TestClock clock() {
        return clock;
    }

    public CheckResult feed(E event) {
        clock.setTick(event.tick());
        clock.setMillis(event.timeMillis());

        CheckContext context = new CheckContext(player, conditions, event.tick(), event.timeMillis(),
                DebugSink.NONE, support);
        context.prepare(check.definition().id(), config);
        CheckResult result = check.inspect(context, event, state);
        if (result != null && result.flagged()) {
            flags.add(result);
        }
        return result;
    }

    public List<CheckResult> flags() {
        return flags;
    }

    public boolean flagged() {
        return !flags.isEmpty();
    }

    public int flagCount() {
        return flags.size();
    }
}
