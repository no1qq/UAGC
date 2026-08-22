package io.github.no1qq.uagc.bukkit.listener;

import io.github.no1qq.uagc.bukkit.UagcRuntime;
import io.github.no1qq.uagc.bukkit.sample.PlayerSampler;
import io.github.no1qq.uagc.engine.check.event.MovementEvent;
import io.github.no1qq.uagc.engine.exemption.ExemptionType;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.player.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class MovementListener implements Listener {

    private final UagcRuntime runtime;

    public MovementListener(UagcRuntime runtime) {
        this.runtime = runtime;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event instanceof org.bukkit.event.player.PlayerTeleportEvent) {
            return;
        }
        Player player = event.getPlayer();
        PlayerData data = runtime.players().get(player.getUniqueId());
        if (data == null) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getWorld() == null || to.getWorld() == null || from.getWorld() != to.getWorld()) {
            data.movement().breakContinuity();
            return;
        }

        runtime.bypass().refreshIfDue(data);
        data.latency().record(Math.max(0, player.getPing()));

        if (runtime.config().general().exemptOnLagSpike()
                && runtime.server().millisSinceLastTick() > runtime.config().general().lagSpikeThresholdMillis()) {
            data.exemptions().grant(ExemptionType.SERVER_LAG);
        }

        long tick = runtime.server().currentTick();
        long millis = System.currentTimeMillis();

        MovementSnapshot snapshot;
        try {
            snapshot = PlayerSampler.sample(player, from, to, tick, millis);
        } catch (RuntimeException exception) {
            runtime.server().error("failed to sample movement for " + player.getName(), exception);
            data.movement().breakContinuity();
            return;
        }

        data.movement().update(snapshot);
        runtime.engine().process(data, new MovementEvent(snapshot));

        if (snapshot.surface().solidBelow()
                && data.movement().groundTicks() > 2
                && !data.exemptions().isCategoryExempt(io.github.no1qq.uagc.engine.check.CheckCategory.MOVEMENT)) {
            data.setLastSafePosition(PlayerSampler.toVec(from));
        }
    }
}
