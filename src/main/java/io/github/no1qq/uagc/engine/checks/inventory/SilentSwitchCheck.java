package io.github.no1qq.uagc.engine.checks.inventory;

import io.github.no1qq.uagc.engine.check.Check;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.CheckContext;
import io.github.no1qq.uagc.engine.check.CheckDefinition;
import io.github.no1qq.uagc.engine.check.CheckResult;
import io.github.no1qq.uagc.engine.check.ConfidenceModel;
import io.github.no1qq.uagc.engine.check.event.HeldSlotCheckEvent;
import io.github.no1qq.uagc.engine.player.PlayerData;

public final class SilentSwitchCheck implements Check<HeldSlotCheckEvent, SilentSwitchCheck.State> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("silent_switch", "SilentSwitch", CheckCategory.INVENTORY)
            .description("a vanilla client syncs the held slot at most once a tick and never returns from one inside the same tick")
            .build();

    public static final class State {
        long lastChangeTick = Long.MIN_VALUE;
        int lastFrom = -1;
        int lastTo = -1;
        int samples;
        long firstSampleTick = Long.MIN_VALUE;
        long shortestReturn = Long.MAX_VALUE;
        boolean sameTick;

        void reset() {
            samples = 0;
            firstSampleTick = Long.MIN_VALUE;
            shortestReturn = Long.MAX_VALUE;
            sameTick = false;
        }
    }

    @Override
    public CheckDefinition definition() {
        return DEFINITION;
    }

    @Override
    public Class<HeldSlotCheckEvent> eventType() {
        return HeldSlotCheckEvent.class;
    }

    @Override
    public State createState() {
        return new State();
    }

    @Override
    public CheckResult inspect(CheckContext context, HeldSlotCheckEvent event, State state) {
        PlayerData player = context.player();
        long tick = event.tick();
        long gap = state.lastChangeTick == Long.MIN_VALUE ? Long.MAX_VALUE : tick - state.lastChangeTick;

        long window = (long) context.config().option("window-ticks", 200.0D);
        if (state.firstSampleTick != Long.MIN_VALUE && tick - state.firstSampleTick > window) {
            state.reset();
        }

        boolean returned = state.lastTo == event.previousSlot()
                && state.lastFrom == event.newSlot()
                && gap <= (long) context.config().option("maximum-return-ticks", 2.0D);
        boolean sameTick = gap == 0L;
        boolean acted = actedWithin(player, state.lastChangeTick, tick,
                (long) context.config().option("action-window-ticks", 3.0D));

        state.lastChangeTick = tick;
        state.lastFrom = event.previousSlot();
        state.lastTo = event.newSlot();

        if (!sameTick && !(returned && acted)) {
            return CheckResult.passed();
        }

        if (state.firstSampleTick == Long.MIN_VALUE) {
            state.firstSampleTick = tick;
        }
        state.samples++;
        state.sameTick |= sameTick;
        state.shortestReturn = Math.min(state.shortestReturn, Math.max(gap, 0L));

        if (context.isDebugWatched()) {
            boolean reported = sameTick;
            context.debug(() -> "silent switch samples=" + state.samples + " gap=" + gap + " same_tick=" + reported);
        }

        int required = state.sameTick
                ? context.config().optionInt("required-same-tick-samples", 2)
                : context.config().optionInt("required-samples", 4);
        if (state.samples < required) {
            return CheckResult.passed();
        }

        int samples = state.samples;
        long shortest = state.shortestReturn;
        boolean anySameTick = state.sameTick;
        state.reset();

        double severity = anySameTick
                ? context.config().option("same-tick-severity", 0.9D)
                : ConfidenceModel.severity(samples, required - 1,
                        context.config().option("severity-scale", 4.0D));
        return CheckResult.flag(severity, anySameTick
                        ? "changed the held slot twice inside one tick"
                        : "returned the held slot to where it started right after acting with it")
                .with("samples", samples)
                .with("shortest_return_ticks", shortest)
                .with("same_tick", anySameTick)
                .with("slot", event.newSlot())
                .build();
    }

    private boolean actedWithin(PlayerData player, long from, long to, long window) {
        return within(player.interaction().lastPlaceTick(), from, to, window)
                || within(player.interaction().lastBreakTick(), from, to, window)
                || within(player.interaction().lastInteractTick(), from, to, window)
                || within(player.combat().lastAttackTick(), from, to, window);
    }

    private boolean within(long actionTick, long from, long to, long window) {
        if (actionTick == Long.MIN_VALUE) {
            return false;
        }
        return actionTick >= from - window && actionTick <= to + window;
    }
}
