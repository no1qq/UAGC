package io.github.no1qq.uagc.engine.checks.inventory;

import io.github.no1qq.uagc.engine.check.Check;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.CheckContext;
import io.github.no1qq.uagc.engine.check.CheckDefinition;
import io.github.no1qq.uagc.engine.check.CheckResult;
import io.github.no1qq.uagc.engine.check.event.InventoryClickCheckEvent;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.util.RingBuffer;

public final class InventoryMoveCheck implements Check<InventoryClickCheckEvent, InventoryMoveCheck.State> {

    private static final CheckDefinition DEFINITION = CheckDefinition
            .builder("inventory_move", "InventoryMove", CheckCategory.INVENTORY)
            .description("an open inventory screen releases every movement key, so a player clicking in one coasts to a stop")
            .latencySensitive()
            .build();

    public static final class State {
        long lastClickTick = Long.MIN_VALUE;
        double buffer;

        void reset() {
            buffer = 0.0D;
        }
    }

    @Override
    public CheckDefinition definition() {
        return DEFINITION;
    }

    @Override
    public Class<InventoryClickCheckEvent> eventType() {
        return InventoryClickCheckEvent.class;
    }

    @Override
    public State createState() {
        return new State();
    }

    @Override
    public CheckResult inspect(CheckContext context, InventoryClickCheckEvent event, State state) {
        PlayerData player = context.player();
        long tick = event.tick();

        long sessionGap = (long) context.config().option("session-gap-ticks", 40.0D);
        if (state.lastClickTick == Long.MIN_VALUE || tick - state.lastClickTick > sessionGap) {
            state.reset();
        }
        state.lastClickTick = tick;

        if (event.openedThisTick()) {
            state.reset();
            return CheckResult.passed();
        }

        MovementSnapshot last = player.movement().last();
        if (last == null || !last.isFinite() || tick - last.tick() > 2L || isUnmeasurable(last)) {
            return CheckResult.passed();
        }
        if (player.velocity().appliedWithin(tick, (long) context.config().option("knockback-grace-ticks", 20.0D))) {
            return CheckResult.passed();
        }

        boolean sprinting = event.sprinting() && context.config().optionBoolean("check-sprinting", true);
        boolean sneaking = event.sneaking() && context.config().optionBoolean("check-sneaking", false);
        double drive = drive(context, player, tick);
        boolean driven = drive > 0.0D;

        if (context.isDebugWatched()) {
            context.debug(() -> "inventory click sprinting=" + sprinting + " sneaking=" + sneaking
                    + " drive=" + drive);
        }

        if (!sprinting && !sneaking && !driven) {
            state.buffer = Math.max(0.0D, state.buffer - context.config().option("buffer-decay", 0.5D));
            return CheckResult.passed();
        }
        if (driven && context.support().hasNearbyPusher(player.uuid())) {
            return CheckResult.passed();
        }

        state.buffer += 1.0D;
        double required = context.config().option("required-clicks", 1.0D);
        if (state.buffer < required) {
            return CheckResult.passed();
        }
        double buffer = state.buffer;
        state.buffer = 0.0D;

        String cause = sprinting ? "sprinting" : sneaking ? "sneaking" : "still being steered";
        double severity = sprinting || sneaking
                ? context.config().option("state-severity", 1.0D)
                : Math.min(1.0D, drive / context.config().option("severity-scale", 0.06D));
        CheckResult.Builder flag = CheckResult
                .flag(severity, "was " + cause + " while clicking inside an inventory screen")
                .with("cause", cause)
                .with("sprinting", event.sprinting())
                .with("sneaking", event.sneaking())
                .with("drive", drive)
                .with("clicks", buffer)
                .with("inventory", event.inventoryType());
        if (context.config().optionBoolean("deny", true)) {
            flag.deny();
        }
        return flag.build();
    }

    private double drive(CheckContext context, PlayerData player, long tick) {
        RingBuffer<MovementSnapshot> history = player.movement().history();
        if (history.size() < 2) {
            return 0.0D;
        }
        MovementSnapshot current = history.fromEnd(0);
        MovementSnapshot previous = history.fromEnd(1);
        if (current == null || previous == null || !current.isFinite() || !previous.isFinite()) {
            return 0.0D;
        }
        if (current.tick() - previous.tick() != 1L || tick - current.tick() > 2L) {
            return 0.0D;
        }
        if (isUnmeasurable(current) || isUnmeasurable(previous)) {
            return 0.0D;
        }
        double before = previous.horizontalDistance();
        double now = current.horizontalDistance();
        double friction = current.surface().solidBelow()
                ? context.config().option("ground-friction", 0.546D)
                : context.config().option("air-friction", 0.91D);
        double allowed = before * friction + context.config().option("tolerance", 0.003D);
        return Math.max(0.0D, now - allowed);
    }

    private boolean isUnmeasurable(MovementSnapshot snapshot) {
        return snapshot.activity().hasAlternateMovement()
                || snapshot.activity().allowFlight()
                || snapshot.activity().gameMode().allowsFlight()
                || snapshot.surface().isSlippery()
                || snapshot.surface().inLiquid()
                || snapshot.surface().inCobweb()
                || snapshot.surface().onClimbable()
                || snapshot.surface().onSlime()
                || snapshot.surface().onHoney()
                || snapshot.surface().onBed()
                || snapshot.surface().inBubbleColumn()
                || snapshot.surface().insideSolid()
                || snapshot.surface().collidingHorizontally()
                || !snapshot.surface().chunkLoaded()
                || snapshot.effects().hasLevitation();
    }
}
