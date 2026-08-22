package io.github.no1qq.uagc.bukkit.listener;

import io.github.no1qq.uagc.bukkit.UagcRuntime;
import io.github.no1qq.uagc.bukkit.sample.PlayerSampler;
import io.github.no1qq.uagc.engine.alert.AlertService;
import io.github.no1qq.uagc.engine.evidence.EvidenceEntry;
import io.github.no1qq.uagc.engine.evidence.EvidenceType;
import io.github.no1qq.uagc.engine.exemption.ExemptionType;
import io.github.no1qq.uagc.engine.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ConnectionListener implements Listener {

    private final UagcRuntime runtime;

    public ConnectionListener(UagcRuntime runtime) {
        this.runtime = runtime;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = runtime.players().getOrCreate(player.getUniqueId(), player.getName());
        data.setLastSafePosition(PlayerSampler.toVec(player.getLocation()));
        data.exemptions().grant(ExemptionType.JOIN);
        data.latency().record(Math.max(0, player.getPing()));
        runtime.alerts().applyTo(data, player.hasPermission(AlertService.PERMISSION_VIEW));
        data.recordEvidence(EvidenceEntry.of(EvidenceType.LIFECYCLE, "player joined")
                .with("world", player.getWorld().getName()));

        runtime.bypass().refresh(data);
        runtime.freeze().reapplyOnJoin(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        runtime.freeze().handleQuit(player.getUniqueId());
        runtime.players().remove(player.getUniqueId());
        runtime.alerts().forget(player.getUniqueId());
        runtime.punishments().forget(player.getUniqueId());
        runtime.debug().unsubscribe(player.getUniqueId());
    }
}
