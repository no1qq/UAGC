package io.github.no1qq.uagc.engine.checks.inventory;

import io.github.no1qq.uagc.engine.check.Check;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.CheckContext;
import io.github.no1qq.uagc.engine.check.CheckDefinition;
import io.github.no1qq.uagc.engine.check.CheckResult;
import io.github.no1qq.uagc.engine.check.ConfidenceModel;
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
        long firstClickTick = Long.MIN_VALUE;
        long lastClickTick = Long.MIN_VALUE;
        int clicks;
        double worstSpeed;

        void reset() {
            firstClickTick = Long.MIN_VALUE;
            clicks = 0;
            worstSpeed = 0.0D;
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

        long sessionGap = (long) context.config().option("session-gap-ticks", 20.0D);
        if (state.lastClickTick == Long.MIN_VALUE || tick - state.lastClickTick > sessionGap) {
            state.reset();
        }
        state.lastClickTick = tick;

        if (event.openedThisTick()) {
            state.reset();
            return CheckResult.passed();
        }

        int samples = context.config().optionInt("sample-ticks", 3);
        double speed = recentSpeed(player, tick, samples);
        if (speed < 0.0D) {
            state.reset();
            return CheckResult.passed();
        }

        double maximumSpeed = context.config().option("maximum-speed", 0.08D);
        if (context.isDebugWatched()) {
            double reported = speed;
            context.debug(() -> "inventory move speed=" + reported + " clicks=" + state.clicks);
        }

        if (speed <= maximumSpeed) {
            state.reset();
            return CheckResult.passed();
        }

        if (state.firstClickTick == Long.MIN_VALUE) {
            state.firstClickTick = tick;
        }
        state.clicks++;
        state.worstSpeed = Math.max(state.worstSpeed, speed);

        int requiredClicks = context.config().optionInt("required-clicks", 4);
        long minimumSpan = (long) context.config().option("minimum-span-ticks", 8.0D);
        long span = tick - state.firstClickTick;
        if (state.clicks < requiredClicks || span < minimumSpan) {
            return CheckResult.passed();
        }
        if (context.support().hasNearbyPusher(player.uuid())) {
            state.reset();
            return CheckResult.passed();
        }

        double worst = state.worstSpeed;
        int clicks = state.clicks;
        state.reset();

        double severity = ConfidenceModel.severityRatio(worst, maximumSpeed,
                context.config().option("severity-scale", 1.5D));
        return CheckResult.flag(severity, "kept moving while clicking inside an inventory screen")
                .with("speed", worst)
                .with("allowed", maximumSpeed)
                .with("clicks", clicks)
                .with("span_ticks", span)
                .with("inventory", event.inventoryType())
                .build();
    }

    private double recentSpeed(PlayerData player, long tick, int samples) {
        RingBuffer<MovementSnapshot> history = player.movement().history();
        if (history.size() < samples) {
            return -1.0D;
        }
        double total = 0.0D;
        long expected = Long.MIN_VALUE;
        for (int index = 0; index < samples; index++) {
            MovementSnapshot snapshot = history.fromEnd(index);
            if (snapshot == null || !snapshot.isFinite()) {
                return -1.0D;
            }
            if (expected == Long.MIN_VALUE) {
                if (tick - snapshot.tick() > 2L) {
                    return -1.0D;
                }
                expected = snapshot.tick();
            } else if (snapshot.tick() != expected) {
                return -1.0D;
            }
            expected--;
            if (isUnmeasurable(snapshot)) {
                return -1.0D;
            }
            total += snapshot.horizontalDistance();
        }
        if (player.velocity().appliedWithin(tick, 20L)) {
            return -1.0D;
        }
        return total / samples;
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
                || !snapshot.surface().chunkLoaded()
                || snapshot.effects().hasLevitation();
    }
}
