package io.github.no1qq.uagc.engine.checks.inventory;

import io.github.no1qq.uagc.engine.check.Check;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.CheckContext;
import io.github.no1qq.uagc.engine.check.CheckDefinition;
import io.github.no1qq.uagc.engine.check.CheckResult;
import io.github.no1qq.uagc.engine.check.event.MovementEvent;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.player.PlayerData;

public final class ScreenMoveCheck implements Check<MovementEvent, ScreenMoveCheck.State> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("screen_move", "ScreenMove", CheckCategory.INVENTORY)
            .description("a player steering while a container screen is open, whatever their clicks look like")
            .latencySensitive()
            .build();

    public static final class State {
        int streak;
        double worst;

        void reset() {
            streak = 0;
            worst = 0.0D;
        }
    }

    @Override
    public CheckDefinition definition() {
        return DEFINITION;
    }

    @Override
    public Class<MovementEvent> eventType() {
        return MovementEvent.class;
    }

    @Override
    public State createState() {
        return new State();
    }

    @Override
    public CheckResult inspect(CheckContext context, MovementEvent event, State state) {
        PlayerData player = context.player();
        if (!player.interaction().screenOpen()) {
            state.reset();
            return CheckResult.passed();
        }

        long tick = event.tick();
        long settle = (long) context.config().option("settle-ticks", 4.0D)
                + Math.min(20L, Math.max(0, player.latency().lastPing()) / 50L);
        if (tick - player.interaction().screenOpenTick() < settle) {
            state.reset();
            return CheckResult.passed();
        }
        if (player.velocity().appliedWithin(tick, (long) context.config().option("knockback-grace-ticks", 20.0D))) {
            state.reset();
            return CheckResult.passed();
        }

        MovementSnapshot snapshot = event.snapshot();
        MovementSnapshot previous = player.movement().previous();
        if (previous == null || !snapshot.isFinite() || !previous.isFinite()
                || snapshot.tick() - previous.tick() != 1L
                || InventoryMoveCheck.isUnsteerable(snapshot) || InventoryMoveCheck.isUnsteerable(previous)) {
            state.reset();
            return CheckResult.passed();
        }

        boolean sprinting = snapshot.activity().sprinting()
                && context.config().optionBoolean("check-sprinting", true);
        double drive = InventoryMoveCheck.drive(context, snapshot, previous);

        if (context.isDebugWatched()) {
            context.debug(() -> "screen move drive=" + drive + " sprinting=" + sprinting
                    + " open=" + (tick - player.interaction().screenOpenTick()) + " ticks");
        }

        if (!sprinting && drive <= 0.0D) {
            state.reset();
            return CheckResult.passed();
        }

        state.streak++;
        state.worst = Math.max(state.worst, drive);
        int required = context.config().optionInt("required-streak", 3);
        if (state.streak < required) {
            return CheckResult.passed();
        }
        if (context.support().hasNearbyPusher(player.uuid())) {
            state.reset();
            return CheckResult.passed();
        }

        double worst = state.worst;
        int streak = state.streak;
        state.reset();

        double severity = sprinting
                ? context.config().option("state-severity", 1.0D)
                : Math.min(1.0D, worst / context.config().option("severity-scale", 0.06D));
        return CheckResult.flag(severity, "kept steering while a container screen was open")
                .with("drive", worst)
                .with("sprinting", sprinting)
                .with("ticks", streak)
                .with("open_ticks", tick - player.interaction().screenOpenTick())
                .build();
    }
}
